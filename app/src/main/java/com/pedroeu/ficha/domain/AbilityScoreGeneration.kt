package com.pedroeu.ficha.domain

import kotlin.random.Random

enum class ScoreMethod(val displayName: String, val description: String) {
    STANDARD_ARRAY("Standard Array", "Assign the fixed set 15, 14, 13, 12, 10, 8 to your abilities."),
    POINT_BUY("Point Buy", "Spend 27 points to raise scores from 8 to 15."),
    ROLL("Roll 4d6", "Roll four d6 for each score and drop the lowest die."),
    MANUAL("Manual Entry", "Type each score directly, for a DM-approved custom spread."),
}

object AbilityScoreGeneration {

    val STANDARD_ARRAY = listOf(15, 14, 13, 12, 10, 8)

    const val POINT_BUY_BUDGET = 27
    const val POINT_BUY_MIN = 8
    const val POINT_BUY_MAX = 15

    /** Cumulative point-buy cost of raising a score from 8 to the given value. */
    private val POINT_BUY_COST = mapOf(
        8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9,
    )

    fun pointBuyCost(score: Int): Int = POINT_BUY_COST[score] ?: 0

    fun pointsSpent(scores: Collection<Int>): Int = scores.sumOf { pointBuyCost(it) }

    fun pointsRemaining(scores: Collection<Int>): Int = POINT_BUY_BUDGET - pointsSpent(scores)

    /** Rolls 4d6 and drops the lowest die, once per ability. */
    fun rollScores(random: Random = Random.Default): List<Int> = List(6) {
        val dice = List(4) { random.nextInt(1, 7) }
        dice.sortedDescending().take(3).sum()
    }
}
