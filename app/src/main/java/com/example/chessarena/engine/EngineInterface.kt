package com.example.chessarena.engine

/**
 * 棋类引擎通用接口
 * 用于统一象棋（Pikafish/UCI协议）和五子棋（Rapfi/Gomocup协议）的AI引擎调用
 */
interface ChessEngine {
    /** 初始化引擎，加载资源 */
    suspend fun initialize()

    /** 设置引擎难度等级 */
    suspend fun setDifficulty(difficulty: Difficulty)

    /**
     * 获取引擎推荐的最佳走法
     * @param fen 当前局面的FEN字符串（象棋）或棋盘描述
     * @param moves 已走过的步骤列表
     * @return 引擎计算结果
     */
    suspend fun getBestMove(fen: String, moves: List<String>): EngineResult

    /**
     * 获取当前局面的评估分数
     * @param fen 当前局面的FEN字符串
     * @return 评估分数（厘兵值，正数表示己方优势）
     */
    suspend fun getEvaluation(fen: String): Int

    /** 停止当前计算 */
    fun stop()

    /** 销毁引擎，释放资源 */
    fun destroy()
}

/**
 * 引擎计算结果
 * @param bestMove 最佳走法（UCI格式如"h2e2"，或Gomocup格式如"7,7"）
 * @param evaluation 评估分数（厘兵值或分数）
 * @param depth 搜索深度
 * @param thinkingTime 思考时间（毫秒）
 */
data class EngineResult(
    val bestMove: String,
    val evaluation: Int,
    val depth: Int,
    val thinkingTime: Long
)

/**
 * 难度等级枚举
 * 象棋和五子棋分别定义不同的难度级别
 *
 * 象棋难度基于UCI协议的Skill Level参数控制：
 *   - 通过 "setoption name Skill Level value X" 设置
 *   - 同时配合搜索深度和时间限制来调节强度
 *
 * 五子棋难度基于Gomocup协议的搜索深度和时间限制：
 *   - 通过 "INFO timeout_turn X" 控制每步思考时间
 *   - 通过 "INFO max_depth X" 控制搜索深度
 */
enum class Difficulty(
    val displayName: String,
    val description: String,
    /** UCI Skill Level 或引擎搜索深度参数 */
    val engineParam: Int,
    /** 最大思考时间（毫秒） */
    val maxThinkTime: Long
) {
    // ========== 象棋难度等级 ==========
    /** 市级名手 - 胡同李大爷 */
    XIANGQI_SENIOR("胡同李大爷", "业余棋手，擅长飞相局与仙人指路，代表普通街头业余棋力", 0, 500L),
    /** 省级名手 - 网络省一高手 */
    XIANGQI_REGIONAL("网络省一高手", "各大平台的顶级棋手，中局攻杀凌厉，擅长急进中兵", 4, 1000L),
    /** 省级大师 - 象棋大师(省冠) */
    XIANGQI_PROFESSIONAL("象棋大师(省冠)", "省级比赛冠军实力，缜密布局与战术嵌套，棋风沉稳扎实", 8, 2000L),
    /** 国家大师 - 国家大师 孟辰 */
    XIANGQI_MASTER("国家大师 孟辰", "著名的“特大杀手”，开局精研，中后局攻杀力量惊人，推演极其强悍", 14, 4000L),
    /** 特级大师 - 特级大师 许银川 */
    XIANGQI_GRANDMASTER("特级大师 许银川", "六届全国冠军，“太极神功”残局功力天下第一，防守极其细腻滴水不漏", 18, 6000L),
    /** 全国冠军 - 第一人 王天一 */
    XIANGQI_CHAMPION("第一人 王天一", "当今棋坛第一人，“外星人”算力深不见底，中局残局冷酷无情收割", 20, 9000L),

    // ========== 五子棋难度等级 ==========
    /** 市级名手 - 高校棋社社长 */
    GOMOKU_SENIOR("高校棋社社长", "业余一段，具备基础局部防守视野，适合五子棋爱好者过招", 2, 500L),
    /** 省级名手 - 省赛常青树 */
    GOMOKU_COMPETITOR("省赛常青树", "业余三段，大局观极佳，熟悉各类局部战术做棋与禁手诱捕陷阱", 4, 900L),
    /** 国家大师 - 国家大师 祁观 */
    GOMOKU_PROFESSIONAL("国家大师 祁观", "全国五子棋锦标赛冠军，算力深厚，擅长精妙的活三与冲四嵌套", 6, 1600L),
    /** 特级大师 - 世界冠军 曹冬 */
    GOMOKU_MASTER("世界冠军 曹冬", "中国五子棋元老与世界锦标赛冠军，具备极高水准的全局VCF/VCT连杀规划", 8, 2600L),
    /** 全国冠军 - 终极棋圣 中村茂 */
    GOMOKU_TOP("终极棋圣 中村茂", "日本五子棋界泰斗，四十载不败神话，战术极限推演，无限制引擎驱动", 0, 4000L);

    companion object {
        /** 获取象棋的所有难度等级 */
        fun xiangqiDifficulties(): List<Difficulty> = listOf(
            XIANGQI_SENIOR, XIANGQI_REGIONAL, XIANGQI_PROFESSIONAL,
            XIANGQI_MASTER, XIANGQI_GRANDMASTER, XIANGQI_CHAMPION
        )

        /** 获取五子棋的所有难度等级 */
        fun gomokuDifficulties(): List<Difficulty> = listOf(
            GOMOKU_SENIOR, GOMOKU_COMPETITOR, GOMOKU_PROFESSIONAL,
            GOMOKU_MASTER, GOMOKU_TOP
        )
    }
}
