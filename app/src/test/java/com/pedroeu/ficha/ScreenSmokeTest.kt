package com.pedroeu.ficha

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Proves the interface can be drawn inside a unit test at all.
 *
 * Everything in [ScreenInvariantTest] depends on this working, and when the harness breaks it
 * breaks in a way that looks like a hundred unrelated failures — so it is worth having one
 * test whose only job is to say whether the screen can be rendered.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ScreenSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a composable can be drawn and read back`() {
        compose.setContent { Text("the sheet is on the table") }
        compose.onNodeWithText("the sheet is on the table").assertIsDisplayed()
    }
}
