package com.aurcus.companion.data

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipInputStream

class DexFormatException(message: String) : Exception(message)

/** Hasil membaca tabel string dari satu atau beberapa berkas dex. */
data class DexScan(
    val dexVersion: String,
    val sizeBytes: Long,
    val dexFileCount: Int,
    val stringCount: Int,
    /** String berbentuk UPPER_SNAKE (3-80 karakter ASCII) yang ditemukan di tabel string. */
    val shapedStrings: Set<String>
)

/** Ringkasan pemindaian yang disimpan agar tab Dex dan menu tetap menampilkan hasil terakhir. */
data class DexScanSummary(
    val fileName: String,
    val dexVersion: String,
    val dexFileCount: Int,
    val stringCount: Int,
    val bundledFound: Int,
    val bundledTotal: Int,
    val foundByFeature: Map<String, Int>,
    val candidateCount: Int,
    val candidateSamples: List<String>,
    val scannedAt: Long
)

/**
 * Pembaca tabel string dex (format dex 035-039). Hanya membaca data statis: tidak memuat,
 * tidak menjalankan kode dex, dan tidak terhubung ke game.
 */
object DexFileReader {
    private const val MAX_DEX_BYTES = 48 * 1024 * 1024
    private const val MAX_ZIP_DEX_FILES = 12
    private val DEX_ENTRY = Regex("classes\\d*\\.dex")

    /** Menerima berkas .dex atau .apk (membaca classes*.dex di dalamnya). */
    fun scanStream(input: InputStream): DexScan {
        val buffered = BufferedInputStream(input, 64 * 1024)
        buffered.mark(8)
        val head = ByteArray(4)
        val read = buffered.read(head)
        buffered.reset()
        val isZip = read == 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()
        return if (isZip) scanZip(buffered) else scan(readLimited(buffered, MAX_DEX_BYTES))
    }

    private fun scanZip(input: InputStream): DexScan {
        var merged: DexScan? = null
        var count = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory || !DEX_ENTRY.matches(entry.name)) continue
                if (++count > MAX_ZIP_DEX_FILES) break
                val scan = scan(readLimited(zip, MAX_DEX_BYTES))
                merged = merged?.let {
                    DexScan(it.dexVersion, it.sizeBytes + scan.sizeBytes, it.dexFileCount + scan.dexFileCount,
                        it.stringCount + scan.stringCount, it.shapedStrings + scan.shapedStrings)
                } ?: scan
            }
        }
        return merged ?: throw DexFormatException("Tidak ada classes.dex di dalam arsip ini")
    }

    fun scan(bytes: ByteArray): DexScan {
        if (bytes.size < 0x70) throw DexFormatException("Berkas terlalu kecil untuk berkas dex")
        val magicOk = bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() &&
            bytes[2] == 'x'.code.toByte() && bytes[3] == '\n'.code.toByte() && bytes[7] == 0.toByte()
        if (!magicOk) throw DexFormatException("Bukan berkas dex (header \"dex\\n\" tidak ditemukan)")
        val version = String(bytes, 4, 3, Charsets.US_ASCII)
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val stringCount = buf.getInt(56)
        val stringOffset = buf.getInt(60)
        if (stringCount < 0 || stringCount > 2_000_000 || stringOffset < 0x70 ||
            stringOffset.toLong() + stringCount.toLong() * 4 > bytes.size
        ) throw DexFormatException("Tabel string dex rusak atau tidak didukung")

        val found = HashSet<String>()
        for (i in 0 until stringCount) {
            val dataOffset = buf.getInt(stringOffset + i * 4)
            if (dataOffset < 0 || dataOffset >= bytes.size) continue
            var p = dataOffset
            while (p < bytes.size && (bytes[p].toInt() and 0x80) != 0) p++ // lewati ukuran uleb128
            p++
            var end = p
            while (end < bytes.size && bytes[end].toInt() != 0) end++
            val len = end - p
            if (len < 3 || len > 80 || end >= bytes.size) continue
            if (isUpperSnake(bytes, p, end)) found.add(String(bytes, p, len, Charsets.US_ASCII))
        }
        return DexScan(version, bytes.size.toLong(), 1, stringCount, found)
    }

    private fun isUpperSnake(bytes: ByteArray, start: Int, end: Int): Boolean {
        val first = bytes[start].toInt()
        if (first < 'A'.code || first > 'Z'.code) return false
        for (j in start + 1 until end) {
            val c = bytes[j].toInt()
            val ok = (c >= 'A'.code && c <= 'Z'.code) || (c >= '0'.code && c <= '9'.code) || c == '_'.code
            if (!ok) return false
        }
        return true
    }

    private fun readLimited(input: InputStream, limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > limit) throw DexFormatException("Berkas terlalu besar (maks ${limit / (1024 * 1024)} MB per dex)")
            out.write(chunk, 0, n)
        }
        return out.toByteArray()
    }

    /**
     * Cocokkan hasil pindai dengan indeks bawaan. Kandidat = string UPPER_SNAKE yang tidak ada di
     * indeks, kata awalnya sudah dikenal di indeks, dan cocok ke kategori selain "umum". Sebagian
     * kandidat bisa berasal dari library, jadi hanya penanda untuk diperiksa manual.
     */
    fun summarize(index: DexKeyIndex, scan: DexScan, fileName: String, now: Long): DexScanSummary {
        val found = index.keys.filter { it in scan.shapedStrings }
        val byFeature = found.groupingBy { index.classify(it) }.eachCount()
        val knownPrefixes = index.keys.map { it.substringBefore('_') }.toHashSet()
        val candidates = scan.shapedStrings.filter {
            !index.contains(it) && it.substringBefore('_') in knownPrefixes &&
                index.classify(it) != DexKeyIndex.FALLBACK
        }.sorted()
        return DexScanSummary(
            fileName = fileName, dexVersion = scan.dexVersion, dexFileCount = scan.dexFileCount,
            stringCount = scan.stringCount, bundledFound = found.size, bundledTotal = index.keys.size,
            foundByFeature = byFeature, candidateCount = candidates.size,
            candidateSamples = candidates.take(40), scannedAt = now
        )
    }
}
