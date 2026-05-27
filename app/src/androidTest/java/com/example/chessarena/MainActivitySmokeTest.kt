package com.example.chessarena

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivitySmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeScreenShowsGameEntrypoints() {
        composeTestRule.onNodeWithText("中国象棋").assertIsDisplayed()
        composeTestRule.onNodeWithText("连珠五子棋").assertIsDisplayed()
        composeTestRule.onNodeWithText("对 局 偏 好").assertIsDisplayed()
    }
}
