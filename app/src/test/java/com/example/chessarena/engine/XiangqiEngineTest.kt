package com.example.chessarena.engine

import com.example.chessarena.game.xiangqi.FenParser
import com.example.chessarena.game.xiangqi.XiangqiRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class XiangqiEngineTest {

    private lateinit var engine: XiangqiEngine

    @Before
    fun setUp() = runTest {
        engine = XiangqiEngine()
        engine.initialize()
        engine.setDifficulty(Difficulty.XIANGQI_SENIOR)
    }

    @Test
    fun engineReturnsOnlyLegalMoves() = runTest {
        val fen = "4k4/9/9/9/9/4P4/9/9/9/4K4 w - - 0 1"
        val state = FenParser.parse(fen)

        val result = engine.getBestMove(fen, emptyList())
        val move = XiangqiEngine.uciToPositions(result.bestMove)

        assertNotNull(move)
        assertTrue(XiangqiRules.isLegalMove(state, move!!.first, move.second))
    }
}
