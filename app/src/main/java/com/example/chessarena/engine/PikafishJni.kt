package com.example.chessarena.engine

object PikafishJni {
    val isAvailable: Boolean

    init {
        isAvailable = try {
            System.loadLibrary("pikafish")
            true
        } catch (_: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * 启动原生 Pikafish 引擎。
     * @return 返回大小为 2 的 IntArray: [0] 代表 stdin-write 管道的文件描述符 (FD)，[1] 代表 stdout-read 管道的文件描述符 (FD)
     */
    external fun startEngine(): IntArray?

    /**
     * 向引擎发送 quit 信号，并彻底销毁 Native 引擎线程和管道
     */
    external fun stopEngine()
}
