package com.example.chessarena.engine

class TTEntry(
    var hash: Long = 0L,
    var depth: Int = 0,
    var score: Int = 0,
    var flag: Int = 0,    // EXACT=0, LOWERBOUND=1, UPPERBOUND=2
    var bestMoveEncoded: Int = 0,  // 编码为 Int
    var age: Int = 0
)

class TranspositionTable(size: Int = 1048576) {
    private val table = Array(size) { TTEntry() }
    private val mask = size - 1

    init {
        // 确保 size 是 2 的幂，用于快速求模
        require(size and (size - 1) == 0) { "Table size must be a power of 2" }
    }

    fun probe(hash: Long): TTEntry? {
        val index = (hash xor (hash ushr 32)).toInt() and mask
        val entry = table[index]
        return if (entry.hash == hash) entry else null
    }

    fun store(hash: Long, depth: Int, score: Int, flag: Int, bestMoveEncoded: Int, age: Int) {
        val index = (hash xor (hash ushr 32)).toInt() and mask
        val entry = table[index]
        
        // 替换策略：新深度 >= 旧深度，或槽位旧条目过期，或者为不同的哈希值
        val isDifferent = entry.hash != hash
        val isOldEntryExpired = age - entry.age > 2
        
        if (!isDifferent || depth >= entry.depth || isOldEntryExpired) {
            entry.hash = hash
            entry.depth = depth
            entry.score = score
            entry.flag = flag
            entry.bestMoveEncoded = bestMoveEncoded
            entry.age = age
        }
    }

    fun clear() {
        for (entry in table) {
            entry.hash = 0L
            entry.depth = 0
            entry.score = 0
            entry.flag = 0
            entry.bestMoveEncoded = 0
            entry.age = 0
        }
    }

    companion object {
        const val EXACT = 0
        const val LOWERBOUND = 1
        const val UPPERBOUND = 2

        fun encodeMove(from: Int, to: Int): Int {
            return (from shl 8) or to
        }

        fun decodeMove(encoded: Int): Pair<Int, Int> {
            val from = (encoded ushr 8) and 0xFF
            val to = encoded and 0xFF
            return Pair(from, to)
        }
    }
}
