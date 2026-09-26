// Adapted from SkyblockAPI (https://github.com/SkyblockAPI/SkyblockAPI), Copyright (c) 2025, SkyblockAPI contributors, MIT License.
package org.solidhax.shiro.utils.skyblock

import net.minecraft.network.chat.Component

enum class TabWidget(pattern: String) {
    AREA("(?:Area|Dungeon): (?<area>.*)"),
    PROFILE("Profile: (?<profile>.*)"),
    PET("Pet:"),
    DAILY_QUESTS("Daily Quests:"),
    SKILLS("Skills:(?: (?<avg>[\\d.]+)| (?<skill>.+) (?<level>\\S+): (?<progress>[\\d,.kMB%]+))?"),
    ELECTION("Election: (?<election>.*)"),
    BESTIARY("Bestiary:"),
    COLLECTION("Collection:"),
    STATS("Stats:"),
    EVENT("Event: (?<event>.*)"),
    EVENT_TRACKERS("Event Trackers:"),
    ACTIVE_EFFECTS("Active Effects:(?: \\((?<amount>\\d+)\\))?"),
    TIMERS("Timers:"),
    FIRE_SALE("Fire Sales: \\((?<amount>[\\d,.]+)\\)"),
    MINIONS("Minions: (?<amount>.*)"),
    PITY("Pity:"),
    SLAYER("Slayer:"),

    FORGES("Forges:(?: \\((?<active>[\\d,.]+)/(?<max>[\\d,.]+)\\))?"),
    COMMISSIONS("Commissions:"),
    POWDERS("Powders:"),
    CRYSTALS("Crystals:"),
    MINING_EVENT("Mining Event: (?<event>.*)"),
    FROZEN_CORPSES("Frozen Corpses:"),
    PICKAXE_ABILITY("Pickaxe Ability:"),
    WORMS("Worms:"),

    AGATHA_CONTEST("Agatha's Contest:"),
    STARBORN_TEMPLE("Starborn Temple:"),
    FOREST_WHISPERS("Forest Whispers: (?<amount>[\\dkmbKMB,.]+)"),
    DESERT_WHISPERS("Desert Whispers: (?<amount>[\\dkmbKMB,.]+)"),
    MOONGLADE_BEACON("Moonglade Beacon: (?<amount>[\\d,.]+) Stacks?"),
    TORRHUS_BEACON("Torrhus Beacon: (?<amount>[\\d,.]+) Stacks?"),
    SHARD_TRAPS("Shard Traps"),

    SALTS("Salts:"),

    COMPOSTER("Composter:"),
    JACOBS_CONTEST("Jacob's Contest:(?: (?<time>.*))?"),
    PESTS("Pests:(?: (?<amount>\\d+))?"),
    PEST_TRAPS("Pest Traps: (?<amount>[\\d,.]+)/(?<max>[\\d,.]+)"),
    VISITORS("Visitors: \\((?<amount>\\d+)\\)"),
    TRAPPER("Trapper:"),
    CROP_MILESTONES("Crop Milestones:"),

    REPUTATION("(?<type>Mage|Barbarian) Reputation:"),
    TROPHY_FISH("Trophy Fish:"),
    FACTION_QUESTS("Faction Quests:"),

    DRAGON("Dragon: \\((?<type>.+)\\)"),

    TROPHY_FROGS("Trophy Frogs:"),

    DOWNED("Downed: (?<status>.*)"),
    TEAM_DEATHS("Team Deaths: (?<amount>\\d+)"),
    DISCOVERIES("Discoveries: (?<amount>\\d+)"),
    PUZZLES("Puzzles: \\((?<amount>\\d+)\\)"),
    RNG_METER("RNG Meter"),
    PARTY("Party: (?<party>.*)"),
    DUNGEONS("Dungeons:"),
    ESSENCE("Essence:"),

    GOOD_TO_KNOW("Good to know:"),
    SHEN("Shen: \\((?<duration>[\\ddmsh,]+)\\)"),
    ADVERTISEMENT("Advertisement:");

    val regex = Regex(pattern)

    private val state get() = TabListUtils.widgets[this]

    val isActive: Boolean get() = state != null

    val title: String? get() = state?.title

    val lines: List<String> get() = state?.lines.orEmpty()

    val values: Map<String, String> get() = state?.values.orEmpty()

    val components: List<Component> get() = state?.components.orEmpty()

    operator fun get(key: String): String? = values[key]

    fun group(name: String): String? =
        try {
            state?.match?.groups?.get(name)?.value
        } catch (_: IllegalArgumentException) {
            null
        }
}
