package com.aurcus.companion.data

import android.content.Context
import org.json.JSONObject

data class DexMeta(
    val file: String,
    val sizeBytes: Long,
    val sha256: String,
    val dexVersion: String,
    val classCount: Int,
    val gamePackage: String,
    val sourceClass: String,
    val keyCount: Int
)

data class FeatureRule(val feature: String, val tokens: Set<String>)

/**
 * Indeks kunci teks UI yang diekstrak dari classes.dex klien Aurcus Online (kelas v1.m).
 * Kunci dikelompokkan ke kategori menu dengan aturan token di assets/dex_ui_keys.json,
 * sehingga jumlah per kategori dihitung dari data dex, bukan angka tulisan tangan.
 */
class DexKeyIndex(val meta: DexMeta, private val rules: List<FeatureRule>, val keys: List<String>) {
    private val byFeature: Map<String, List<String>> = keys.groupBy { classify(it) }
    private val keySet: Set<String> = keys.toHashSet()

    fun classify(key: String): String {
        val tokens = key.split('_')
        for (rule in rules) if (tokens.any { it in rule.tokens }) return rule.feature
        return FALLBACK
    }

    fun contains(key: String): Boolean = key in keySet

    fun keysOf(featureId: String): List<String> = byFeature[featureId].orEmpty()

    fun countOf(featureId: String): Int = byFeature[featureId]?.size ?: 0

    /** Cari kunci; semua kata dalam [query] harus ada di kunci (tanpa membedakan huruf besar/kecil). */
    fun search(query: String, featureId: String? = null, limit: Int = 60): List<String> {
        val parts = query.trim().uppercase().split(' ', '_', '-').filter { it.isNotEmpty() }
        if (parts.isEmpty()) return emptyList()
        val pool = if (featureId == null) keys else keysOf(featureId)
        return pool.asSequence().filter { key -> parts.all { key.contains(it) } }.take(limit).toList()
    }

    companion object {
        const val FALLBACK = "umum"
        private const val ASSET = "dex_ui_keys.json"

        @Volatile private var cached: DexKeyIndex? = null

        fun load(context: Context): DexKeyIndex {
            cached?.let { return it }
            return synchronized(this) {
                cached ?: (runCatching {
                    context.applicationContext.assets.open(ASSET).bufferedReader().use { parse(it.readText()) }
                }.getOrNull() ?: empty()).also { cached = it }
            }
        }

        fun empty(): DexKeyIndex =
            DexKeyIndex(DexMeta("(tidak ada)", 0L, "", "-", 0, "-", "-", 0), emptyList(), emptyList())

        fun parse(json: String): DexKeyIndex {
            val root = JSONObject(json)
            val m = root.getJSONObject("meta")
            val meta = DexMeta(
                file = m.optString("file"), sizeBytes = m.optLong("sizeBytes"), sha256 = m.optString("sha256"),
                dexVersion = m.optString("dexVersion"), classCount = m.optInt("classCount"),
                gamePackage = m.optString("gamePackage"), sourceClass = m.optString("sourceClass"),
                keyCount = m.optInt("keyCount")
            )
            val ruleArray = root.getJSONArray("rules")
            val rules = (0 until ruleArray.length()).map { i ->
                val r = ruleArray.getJSONObject(i)
                val tokens = r.getJSONArray("tokens")
                FeatureRule(r.getString("feature"), (0 until tokens.length()).map { tokens.getString(it) }.toSet())
            }
            val keyArray = root.getJSONArray("keys")
            val keys = (0 until keyArray.length()).map { keyArray.getString(it) }
            return DexKeyIndex(meta, rules, keys)
        }
    }
}
