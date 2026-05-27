package com.example.chessarena.engine

import com.example.chessarena.game.xiangqi.PieceType
import com.example.chessarena.game.xiangqi.Side
import com.example.chessarena.game.xiangqi.Piece
import com.example.chessarena.game.xiangqi.XiangqiState
import com.example.chessarena.game.gomoku.Stone
import com.example.chessarena.game.gomoku.GomokuState
import kotlin.random.Random

object ZobristHash {
    private const val SEED = 42L

    // 象棋 Zobrist
    // pieceType(7) * side(2) * position(90)
    private val xiangqiTable = Array(7) { Array(2) { LongArray(90) } }
    val xiangqiTurnKey: Long

    // 五子棋 Zobrist
    // stone(2) * position(225)
    private val gomokuTable = Array(2) { LongArray(225) }
    val gomokuTurnKey: Long

    init {
        val random = Random(SEED)
        
        // 初始化象棋哈希值
        for (p in 0 until 7) {
            for (s in 0 until 2) {
                for (pos in 0 until 90) {
                    xiangqiTable[p][s][pos] = random.nextLong()
                }
            }
        }
        xiangqiTurnKey = random.nextLong()

        // 初始化五子棋哈希值
        for (s in 0 until 2) {
            for (pos in 0 until 225) {
                gomokuTable[s][pos] = random.nextLong()
            }
        }
        gomokuTurnKey = random.nextLong()
    }

    // 获取象棋棋子-方-位置的哈希值
    fun getXiangqiKey(type: PieceType, side: Side, index: Int): Long {
        val p = type.ordinal
        val s = if (side == Side.RED) 0 else 1
        return xiangqiTable[p][s][index]
    }

    // 获取五子棋棋子-位置的哈希值
    fun getGomokuKey(stone: Stone, index: Int): Long {
        val s = if (stone == Stone.BLACK) 0 else 1
        return gomokuTable[s][index]
    }

    // 计算象棋初始哈希值
    fun computeXiangqiHash(state: XiangqiState): Long {
        var hash = 0L
        for (i in state.board.indices) {
            val piece = state.board[i] ?: continue
            hash = hash xor getXiangqiKey(piece.type, piece.side, i)
        }
        if (state.currentTurn == Side.BLACK) {
            hash = hash xor xiangqiTurnKey
        }
        return hash
    }

    // 计算五子棋初始哈希值
    fun computeGomokuHash(state: GomokuState): Long {
        var hash = 0L
        for (i in state.board.indices) {
            val stone = state.board[i] ?: continue
            hash = hash xor getGomokuKey(stone, i)
        }
        if (state.currentTurn == Stone.WHITE) {
            hash = hash xor gomokuTurnKey
        }
        return hash
    }
}
