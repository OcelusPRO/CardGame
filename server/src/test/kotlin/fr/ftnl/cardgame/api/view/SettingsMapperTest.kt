package fr.ftnl.cardgame.api.view

import fr.ftnl.cardgame.api.dto.GameSettingsInput
import fr.ftnl.cardgame.domain.game.AnswerMode
import fr.ftnl.cardgame.domain.game.GameSettings
import fr.ftnl.cardgame.domain.game.SelectionMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SettingsMapperTest {

    private val current = GameSettings()

    @Test
    fun `an empty payload changes nothing`() {
        assertEquals(current, SettingsMapper.merge(current, GameSettingsInput()))
    }

    @Test
    fun `only the fields sent are applied`() {
        val merged = SettingsMapper.merge(current, GameSettingsInput(rounds = 12))

        assertEquals(12, merged.rounds)
        assertEquals(current.handSize, merged.handSize)
    }

    @Test
    fun `modes are read case insensitively`() {
        val merged = SettingsMapper.merge(
            current,
            GameSettingsInput(selectionMode = "czar", answerMode = "free_text"),
        )

        assertEquals(SelectionMode.CZAR, merged.selectionMode)
        assertEquals(AnswerMode.FREE_TEXT, merged.answerMode)
    }

    @Test
    fun `the scoring is tunable field by field`() {
        val merged = SettingsMapper.merge(current, GameSettingsInput(pointsPerVote = 2, unanimityBonus = 0))

        assertEquals(2, merged.scoring.pointsPerVote)
        assertEquals(0, merged.scoring.unanimityBonus)
    }

    @Test
    fun `self voting is opt in`() {
        assertEquals(false, current.allowSelfVote)
        assertTrue(SettingsMapper.merge(current, GameSettingsInput(allowSelfVote = true)).allowSelfVote)
    }

    @Test
    fun `letting the czar answer is opt in and exposed on the view`() {
        assertEquals(false, current.czarAnswers)

        val merged = SettingsMapper.merge(current, GameSettingsInput(czarAnswers = true))

        assertTrue(merged.czarAnswers)
        assertTrue(SettingsMapper.toView(merged).czarAnswers)
    }

    @Test
    fun `an unknown mode is refused`() {
        assertFailsWith<IllegalArgumentException> {
            SettingsMapper.merge(current, GameSettingsInput(selectionMode = "roulette"))
        }
    }

    @Test
    fun `an out of range value is refused by the domain`() {
        assertFailsWith<IllegalArgumentException> {
            SettingsMapper.merge(current, GameSettingsInput(handSize = 99))
        }
    }

    @Test
    fun `an impossible number of rounds is refused by the domain`() {
        assertFailsWith<IllegalArgumentException> {
            SettingsMapper.merge(current, GameSettingsInput(rounds = 0))
        }
    }

    @Test
    fun `the view exposes the scoring the players are told about`() {
        val view = SettingsMapper.toView(current)

        assertEquals(current.scoring.unanimityBonus, view.unanimityBonus)
        assertEquals(current.scoring.pointsPerVote, view.pointsPerVote)
        assertEquals(current.rounds, view.rounds)
        assertEquals(current.allowSelfVote, view.allowSelfVote)
    }
}
