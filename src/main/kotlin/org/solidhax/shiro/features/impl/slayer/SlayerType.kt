package org.solidhax.shiro.features.impl.slayer

enum class SlayerType(val displayName: String, val shortName: String, val names: List<String> = listOf(displayName)) {
    REVENANT("Revenant Horror", "Rev", listOf("Revenant Horror", "Atoned Horror")),
    TARANTULA("Tarantula Broodfather", "Tara", listOf("Tarantula Broodfather", "Conjoined Brood")),
    SVEN("Sven Packmaster", "Sven"),
    VOIDGLOOM("Voidgloom Seraph", "Void"),
    INFERNO("Inferno Demonlord", "Blaze"),
    VAMPIRE("Riftstalker Bloodfiend", "Vamp", listOf("Bloodfiend"));

    fun tierOf(name: String): String? {
        if (names.drop(1).any { it in name }) return "V"
        return TIER_REGEX.find(name)?.groupValues?.get(1)
    }

    companion object {
        private val TIER_REGEX = Regex("""(?:^|\s)([IVX]{1,4})\s""")

        fun fromName(name: String): SlayerType? = entries.find { type -> type.names.any { it in name } }
    }
}
