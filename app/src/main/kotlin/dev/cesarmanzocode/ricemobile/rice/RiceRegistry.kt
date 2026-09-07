package dev.cesarmanzocode.ricemobile.rice

import dev.cesarmanzocode.ricemobile.rice.arctic.ArcticGlassRice
import dev.cesarmanzocode.ricemobile.rice.ember.EmberForgeRice
import dev.cesarmanzocode.ricemobile.rice.ivory.IvoryPaperRice
import dev.cesarmanzocode.ricemobile.rice.monochrome.MonochromeRice
import dev.cesarmanzocode.ricemobile.rice.violet.VioletNightRice

/**
 * Fixed list of five rices, in persisted-ID order (contract §6.1). No plugin loading, no
 * reflection: `require` fails fast if IDs are ever duplicated or reordered by mistake.
 */
object RiceRegistry {
    val all: List<Rice> = listOf(
        MonochromeRice,
        ArcticGlassRice,
        EmberForgeRice,
        IvoryPaperRice,
        VioletNightRice,
    )

    init {
        require(all.map { it.id }.toSet().size == all.size) { "RiceRegistry ids must be unique" }
        require(all.map { it.id } == RiceId.entries) { "RiceRegistry must list every RiceId, in order" }
    }

    fun of(id: RiceId): Rice = all.first { it.id == id }
}
