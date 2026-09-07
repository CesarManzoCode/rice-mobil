package dev.cesarmanzocode.ricemobile.rice

/**
 * Persistent rice identity (contract §1/§5). Order here is Monochrome -> Arctic -> Ember ->
 * Ivory -> Violet and matches [dev.cesarmanzocode.ricemobile.rice.RiceRegistry]. Never use
 * `enum.ordinal` or `enum.valueOf` on persisted/untrusted data: [fromPersisted] is total.
 */
enum class RiceId(val persisted: String) {
    Monochrome("monochrome"),
    ArcticGlass("arctic_glass"),
    EmberForge("ember_forge"),
    IvoryPaper("ivory_paper"),
    VioletNight("violet_night");

    companion object {
        val Default = Monochrome

        /** Unknown or corrupt persisted value falls back to [Default] (contract §11). */
        fun fromPersisted(value: String?): RiceId = entries.firstOrNull { it.persisted == value } ?: Default
    }
}
