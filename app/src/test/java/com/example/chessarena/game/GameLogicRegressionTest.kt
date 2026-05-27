package com.example.chessarena.game

import com.example.chessarena.engine.Difficulty
import com.example.chessarena.engine.GomokuEngine
import com.example.chessarena.game.gomoku.GomokuState
import com.example.chessarena.game.xiangqi.FenParser
import com.example.chessarena.game.xiangqi.Position
import com.example.chessarena.game.xiangqi.Side
import com.example.chessarena.game.xiangqi.XiangqiRules
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameLogicRegressionTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun gomokuEngineReadsBoardStringAndAvoidsOccupiedCenter() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val engine = GomokuEngine(dispatcher = testDispatcher)
        engine.initialize()
        engine.setDifficulty(Difficulty.GOMOKU_SENIOR)

        val result = engine.getBestMove(boardWithOccupiedCenter(), emptyList())

        assertNotEquals("7,7", result.bestMove)
        val parsedMove = GomokuEngine.parseMove(result.bestMove)
        assertTrue(parsedMove != null)
        assertTrue(parsedMove!!.first in 0 until GomokuState.SIZE)
        assertTrue(parsedMove.second in 0 until GomokuState.SIZE)
    }

    @Test
    fun xiangqiInitialPositionHasLegalMoves() {
        val state = FenParser.parse(FenParser.INITIAL_FEN)

        assertTrue(XiangqiRules.getAllLegalMoves(state).isNotEmpty())
    }

    @Test
    fun xiangqiMoveThatExposesFlyingGeneralsIsIllegal() {
        val state = FenParser.parse(
            "4k4/9/9/9/9/4R4/9/9/9/4K4 w - - 0 1"
        )

        assertFalse(
            XiangqiRules.isLegalMove(
                state,
                from = Position(col = 4, row = 5),
                to = Position(col = 0, row = 5)
            )
        )
    }

    @Test
    fun xiangqiDetectsCheckmate() {
        val state = FenParser.parse(
            "4k4/3RRR3/9/9/9/4N4/9/9/9/4K4 b - - 0 1"
        )

        assertTrue(XiangqiRules.isInCheck(state, Side.BLACK))
        assertTrue(XiangqiRules.isCheckmate(state, Side.BLACK))
    }

    @Test
    fun xiangqiDetectsStalemate() {
        val state = FenParser.parse(
            "4k4/3R1R3/9/9/9/4N4/9/9/9/4K4 b - - 0 1"
        )

        assertFalse(XiangqiRules.isInCheck(state, Side.BLACK))
        assertTrue(XiangqiRules.isStalemate(state, Side.BLACK))
    }

    private fun boardWithOccupiedCenter(): String {
        return (0 until GomokuState.SIZE).joinToString(";") { row ->
            (0 until GomokuState.SIZE).joinToString(",") { col ->
                if (row == 7 && col == 7) "1" else "0"
            }
        }
    }
}
