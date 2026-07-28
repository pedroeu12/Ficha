package com.pedroeu.ficha

import com.pedroeu.ficha.domain.AbilityScoreGeneration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AbilityScoreGenerationTest {

    @Test
    fun `standard array holds the six official values`() {
        assertEquals(listOf(15, 14, 13, 12, 10, 8), AbilityScoreGeneration.STANDARD_ARRAY)
    }

    @Test
    fun `point buy costs match the official table`() {
        assertEquals(0, AbilityScoreGeneration.pointBuyCost(8))
        assertEquals(5, AbilityScoreGeneration.pointBuyCost(13))
        assertEquals(7, AbilityScoreGeneration.pointBuyCost(14))
        assertEquals(9, AbilityScoreGeneration.pointBuyCost(15))
    }

    @Test
    fun `a common point buy spread costs exactly the budget`() {
        val spread = listOf(15, 15, 15, 8, 8, 8)
        assertEquals(27, AbilityScoreGeneration.pointsSpent(spread))
        assertEquals(0, AbilityScoreGeneration.pointsRemaining(spread))
    }

    @Test
    fun `all eights leaves the full budget`() {
        val spread = List(6) { 8 }
        assertEquals(27, AbilityScoreGeneration.pointsRemaining(spread))
    }

    @Test
    fun `rolling produces six scores in the four d6 drop lowest range`() {
        val rolls = AbilityScoreGeneration.rollScores(Random(42))
        assertEquals(6, rolls.size)
        assertTrue(rolls.all { it in 3..18 })
    }
}
