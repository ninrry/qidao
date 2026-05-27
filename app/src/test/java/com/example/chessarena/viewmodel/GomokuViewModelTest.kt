package com.example.chessarena.viewmodel

import com.example.chessarena.engine.ChessEngine
import com.example.chessarena.engine.Difficulty
import com.example.chessarena.engine.EngineResult
import com.example.chessarena.game.gomoku.Stone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class GomokuViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun aiUsesCurrentBoardWhenPlayerDoesNotTakeCenter() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = GomokuViewModel(
            engine = ScriptedEngine("7,7"),
            dispatcher = mainDispatcherRule.testDispatcher
        )
        advanceUntilIdle()

        viewModel.onDifficultySelected(Difficulty.GOMOKU_SENIOR)
        advanceUntilIdle()
        viewModel.onPositionClick(row = 6, col = 6)
        advanceUntilIdle()

        val state = viewModel.uiState.value.gameState!!
        assertEquals(Stone.BLACK, state.stoneAt(col = 6, row = 6))
        assertEquals(Stone.WHITE, state.stoneAt(col = 7, row = 7))
        assertEquals(Stone.BLACK, state.currentTurn)
        assertFalse(viewModel.uiState.value.isAiThinking)
        assertNull(viewModel.uiState.value.gameOverMessage)
    }

    @Test
    fun illegalAiMoveFallsBackAndUnlocksPlayerTurn() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = GomokuViewModel(
            engine = ScriptedEngine("7,7"),
            dispatcher = mainDispatcherRule.testDispatcher
        )
        advanceUntilIdle()

        viewModel.onDifficultySelected(Difficulty.GOMOKU_SENIOR)
        advanceUntilIdle()
        viewModel.onPositionClick(row = 7, col = 7)
        advanceUntilIdle()

        val state = viewModel.uiState.value.gameState!!
        assertEquals(Stone.BLACK, state.stoneAt(col = 7, row = 7))
        assertNotEquals(Stone.WHITE, state.stoneAt(col = 7, row = 7))
        assertEquals(1, state.board.count { it == Stone.WHITE })
        assertEquals(Stone.BLACK, state.currentTurn)
        assertFalse(viewModel.uiState.value.isAiThinking)
        assertNull(viewModel.uiState.value.gameOverMessage)
    }

    @Test
    fun repeatedIllegalAiMovesDoNotCreateDuplicatesAcrossFiveRounds() =
        runTest(mainDispatcherRule.testDispatcher) {
            val viewModel = GomokuViewModel(
                engine = ScriptedEngine("7,7", "7,7", "7,7", "7,7", "7,7"),
                dispatcher = mainDispatcherRule.testDispatcher
            )
            advanceUntilIdle()

            viewModel.onDifficultySelected(Difficulty.GOMOKU_SENIOR)
            advanceUntilIdle()

            val playerMoves = listOf(
                7 to 7,
                14 to 0,
                0 to 14,
                14 to 14,
                0 to 0
            )

            for ((row, col) in playerMoves) {
                viewModel.onPositionClick(row = row, col = col)
                advanceUntilIdle()

                val uiState = viewModel.uiState.value
                assertEquals(Stone.BLACK, uiState.gameState!!.currentTurn)
                assertFalse(uiState.isAiThinking)
                assertNull(uiState.gameOverMessage)
            }

            val finalState = viewModel.uiState.value.gameState!!
            assertEquals(10, finalState.moveCount)
            assertEquals(finalState.moveCount, finalState.board.count { it != null })
            assertEquals(5, finalState.board.count { it == Stone.BLACK })
            assertEquals(5, finalState.board.count { it == Stone.WHITE })
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class ScriptedEngine(
    vararg moves: String
) : ChessEngine {
    private val scriptedMoves = ArrayDeque(moves.toList())

    override suspend fun initialize() = Unit

    override suspend fun setDifficulty(difficulty: Difficulty) = Unit

    override suspend fun getBestMove(fen: String, moves: List<String>): EngineResult {
        return EngineResult(
            bestMove = scriptedMoves.removeFirstOrNull() ?: "7,7",
            evaluation = 0,
            depth = 1,
            thinkingTime = 0L
        )
    }

    override suspend fun getEvaluation(fen: String): Int = 0

    override fun stop() = Unit

    override fun destroy() = Unit
}
