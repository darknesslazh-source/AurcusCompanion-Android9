#!/usr/bin/env python3
"""Bangun ulang app/src/main/assets/dex_ui_keys.json dari sebuah classes.dex.

Pemakaian:
    pip install androguard
    python3 tools/gen_dex_index.py path/ke/classes.dex [app/src/main/assets/dex_ui_keys.json]

Skrip ini hanya MEMBACA dex (tidak menjalankannya). Kunci diambil dari const-string di kelas
tabel teks UI (v1.m pada klien yang dianalisis); ganti SOURCE_CLASS bila nama kelas berubah
pada versi game lain. Aturan kategori (RULES) ikut disimpan di JSON, jadi aplikasi memakai
aturan yang sama persis dengan skrip ini.
"""
import hashlib, json, re, sys
from loguru import logger
logger.remove()
from androguard.core.dex import DEX

SOURCE_CLASS = "Lv1/m;"
GAME_PACKAGE = "com.asobimo.aurcus"

RULES=[
 ("trade",["TRADE"]),
 ("auction",["AUCTION"]),
 ("housing",["HOUSE","HOUSING","FURNITURE"]),
 ("guild",["GUILD"]),
 ("pvp",["PVP","ARENA"]),
 ("party",["PARTY","PT","FRIEND","BLOCK","CHAT"]),
 ("toko",["SHOP","GACHA","PAYMENT","PURCHASE","COIN","BILLING","BUY","SELL","PRICE","MONEY","DEPOSIT","WITHDRAW"]),
 ("misi",["MISSION","QUEST","TREASURE","FIELD"]),
 ("ranking",["ACHIEVEMENT","TITLE","RANK","RANKING"]),
 ("revive",["DEAD","REVIVAL","REVIVE","RESPAWN"]),
 ("skill",["SKILL","JOB","ABILITY","STATUS","BUFF","BASE","BONUS","PARAMETER","PARAM"]),
 ("equipment",["EQUIP","EQUIPMENT","ENFORCE","EVO","EVOLVE","REMODEL","ELEMENT","SLOT","ACCESSORY","WEAPON","ATTRIBUTE","REINFORCE"]),
 ("crafting",["MAKE","MAKER","CRAFT","MATERIAL","RECIPE"]),
 ("channel",["CHANNEL","WARP","MAP"]),
 ("config",["CONFIG","OPTION","SETTING","APK","NOTIFICATION","TERMS"]),
 ("akun",["CHARACTER","LOBBY","AUTHENTICATE","ACCOUNT","LOGIN"]),
 ("inventori",["ITEM","BAG","INVENTORY","WAREHOUSE","DISCARD","STACK","EXTRACT"]),
]

def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    raw = open(sys.argv[1], "rb").read()
    out_path = sys.argv[2] if len(sys.argv) > 2 else "app/src/main/assets/dex_ui_keys.json"
    dex = DEX(raw)
    keys = set()
    for cls in dex.get_classes():
        if cls.get_name() != SOURCE_CLASS:
            continue
        for method in cls.get_methods():
            code = method.get_code()
            if not code:
                continue
            for ins in code.get_bc().get_instructions():
                if ins.get_name().startswith("const-string"):
                    value = ins.get_output().split(",")[-1].strip().strip("'\"")
                    if re.fullmatch(r"[A-Z][A-Z0-9_]{2,}", value):
                        keys.add(value)
    keys = sorted(keys)
    if not keys:
        sys.exit("Tidak ada kunci ditemukan; periksa SOURCE_CLASS.")
    meta = {"file": "classes.dex", "sizeBytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "dexVersion": raw[4:7].decode(), "classCount": len(dex.get_classes()),
            "gamePackage": GAME_PACKAGE, "sourceClass": SOURCE_CLASS[1:-1].replace("/", "."), "keyCount": len(keys)}
    data = {"meta": meta, "rules": [{"feature": f, "tokens": t} for f, t in RULES], "keys": keys}
    with open(out_path, "w") as fh:
        json.dump(data, fh, separators=(",", ":"))
    print(f"{len(keys)} kunci -> {out_path}")

if __name__ == "__main__":
    main()
