package com.aurcus.companion.domain

import com.aurcus.companion.data.Item

data class ItemDelta(val attackDelta: Int, val defenseDelta: Int, val magicDelta: Int)

object EquipmentComparator {
    fun compare(current: Item, candidate: Item) = ItemDelta(
        attackDelta = candidate.attack - current.attack,
        defenseDelta = candidate.defense - current.defense,
        magicDelta = candidate.magic - current.magic
    )
}
