package net.dodian.uber.game.skills.smithing

object MysticSmeltingButtons {
    // Edit the ids for each bar here. Each block is one visible furnace option.
    val all = NamedSmeltingButtonSet(
        bronze = SmeltingButtonSet(
            displayName = "Bronze",
            barId = 2349,
            oneButtonId = 3987,
            fiveButtonId = 3986,
            tenButtonId = 2807,
            xButtonId = 2414,
        ),
        iron = SmeltingButtonSet(
            displayName = "Iron",
            barId = 2351,
            oneButtonId = 3991,
            fiveButtonId = 3990,
            tenButtonId = 3989,
            xButtonId = 3988,
        ),
        silver = SmeltingButtonSet(
            displayName = "Silver",
            barId = 2355,
            oneButtonId = 3995,
            fiveButtonId = 3994,
            tenButtonId = 3993,
            xButtonId = 3992,
        ),
        steel = SmeltingButtonSet(
            displayName = "Steel",
            barId = 2353,
            oneButtonId = 3999,
            fiveButtonId = 3998,
            tenButtonId = 3997,
            xButtonId = 3996,
        ),
        gold = SmeltingButtonSet(
            displayName = "Gold",
            barId = 2357,
            oneButtonId = 4003,
            fiveButtonId = 4002,
            tenButtonId = 4001,
            xButtonId = 4000,
        ),
        mithril = SmeltingButtonSet(
            displayName = "Mithril",
            barId = 2359,
            oneButtonId = 7441,
            fiveButtonId = 7440,
            tenButtonId = 6397,
            xButtonId = 4158,
        ),
        adamant = SmeltingButtonSet(
            displayName = "Adamant",
            barId = 2361,
            oneButtonId = 7446,
            fiveButtonId = 7444,
            tenButtonId = 7443,
            xButtonId = 7442,
        ),
        rune = SmeltingButtonSet(
            displayName = "Rune",
            barId = 2363,
            oneButtonId = 7450,
            fiveButtonId = 7449,
            tenButtonId = 7448,
            xButtonId = 7447,
        ),
    )

    val bronze: SmeltingButtonSet get() = all.bronze
    val iron: SmeltingButtonSet get() = all.iron
    val silver: SmeltingButtonSet get() = all.silver
    val steel: SmeltingButtonSet get() = all.steel
    val gold: SmeltingButtonSet get() = all.gold
    val mithril: SmeltingButtonSet get() = all.mithril
    val adamant: SmeltingButtonSet get() = all.adamant
    val rune: SmeltingButtonSet get() = all.rune
}
