package com.pedroeu.ficha

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pedroeu.ficha.data.content.ProgressionData
import com.pedroeu.ficha.domain.ChoiceGrants
import com.pedroeu.ficha.domain.ChoiceResolver
import com.pedroeu.ficha.domain.PlayerCharacter
import com.pedroeu.ficha.ui.components.ChoiceEditDialog
import com.pedroeu.ficha.ui.theme.FichaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Tapping Agonizing Blast in the editor, with the character changing underneath it. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class AgonizingBlastLiveTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun diagnose() {
        val start = SheetAudit.character("warlock", 5, "fiend")
        var character by mutableStateOf(start)

        compose.setContent {
            FichaTheme {
                // Exactly what the Features tab does: re-resolve from the character on every
                // pass, and hand the dialog the live answer rather than a remembered one.
                val live = ChoiceResolver.all(character)
                    .first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }
                ChoiceEditDialog(
                    character = character,
                    resolved = live,
                    onDismiss = {},
                    onToggle = { optionId ->
                        character = ChoiceGrants.toggle(
                            character, live.choice, live.level, optionId,
                        )
                    },
                )
            }
        }

        fun taken() = ChoiceResolver.latestSelectionFor(
            character, ProgressionData.INVOCATION_CHOICE_ID,
        )

        fun tap(name: String) {
            compose.onAllNodesWithText(name).onFirst()
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.OnClick)
            compose.waitForIdle()
        }

        println(">>> count=${ChoiceResolver.all(character).first { it.choice.id == ProgressionData.INVOCATION_CHOICE_ID }.choice.count}")
        println(">>> start: ${taken()}")
        tap("Agonizing Blast"); println(">>> after tick:   ${taken()}")
        tap("Agonizing Blast"); println(">>> after untick: ${taken()}")
        tap("Agonizing Blast"); println(">>> after tick:   ${taken()}")
        tap("Devil's Sight");   println(">>> plus devils:  ${taken()}")
        tap("Agonizing Blast"); println(">>> untick again: ${taken()}")
    }
}
