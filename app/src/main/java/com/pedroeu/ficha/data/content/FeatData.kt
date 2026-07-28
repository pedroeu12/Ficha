package com.pedroeu.ficha.data.content

import com.pedroeu.ficha.data.model.Feat

object FeatData {

    val ORIGIN_FEATS: List<Feat> = listOf(
        Feat("alert", "Alert", "You gain a bonus to Initiative equal to your Proficiency Bonus, and you can swap your Initiative with a willing ally's."),
        Feat("crafter", "Crafter", "You gain proficiency with three Artisan's Tools, get a 20% discount on nonmagical goods, and can craft items faster during a long rest."),
        Feat("healer", "Healer", "You can use a Healer's Kit as a Utilize action to restore hit points, and rerolling a 1 on any healing die is allowed."),
        Feat("lucky", "Lucky", "You have Luck Points equal to your Proficiency Bonus, which you can spend to gain Advantage on a d20 Test or impose Disadvantage on an attack against you."),
        Feat("magic_initiate_cleric", "Magic Initiate (Cleric)", "You learn two cantrips and one level 1 spell from the Cleric spell list. You can cast the level 1 spell once per long rest without a slot. Wisdom is your spellcasting ability for them."),
        Feat("magic_initiate_druid", "Magic Initiate (Druid)", "You learn two cantrips and one level 1 spell from the Druid spell list. You can cast the level 1 spell once per long rest without a slot. Wisdom is your spellcasting ability for them."),
        Feat("magic_initiate_wizard", "Magic Initiate (Wizard)", "You learn two cantrips and one level 1 spell from the Wizard spell list. You can cast the level 1 spell once per long rest without a slot. Intelligence is your spellcasting ability for them."),
        Feat("musician", "Musician", "You gain proficiency with three Musical Instruments, and after a rest you can grant Heroic Inspiration to allies equal to your Proficiency Bonus."),
        Feat("savage_attacker", "Savage Attacker", "Once per turn when you hit with a weapon, you can reroll the weapon's damage dice and use either total."),
        Feat("skilled", "Skilled", "You gain proficiency in any combination of three skills or tools of your choice."),
        Feat("tavern_brawler", "Tavern Brawler", "Your Unarmed Strike deals 1d4 damage, you can reroll a 1 on that die, and you can push a creature 5 feet once per turn."),
        Feat("tough", "Tough", "Your hit point maximum increases by twice your character level, and increases by 2 each time you gain a level."),
    )

    fun byId(id: String): Feat? = ORIGIN_FEATS.find { it.id == id }
}
