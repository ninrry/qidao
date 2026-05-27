package com.example.chessarena.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 棋道实时音频合成管理器
 *
 * 通过 Android AudioTrack 实时进行 PCM 正弦波 + 衰减包络 + 物理噪声的物理撞击声学合成。
 * 优雅实现纯单机无音频资源依赖的清脆落子声、沉重吃子声、古风将军警报及国风胜利/失败琶音，
 * 彻底消除外部 wav/mp3 文件对包大小的占用。
 */
object SoundManager {
    private const val TAG = "SoundManager"
    private const val SAMPLE_RATE = 44100
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    enum class SoundType {
        MOVE,       // 落子：木头敲击，短，脆
        CAPTURE,    // 吃子：木头敲击，略显厚重，带双重碰撞震颤
        CHECK,      // 将军：清脆中国风和弦/三和弦音
        WIN,        // 胜利：愉悦的中国风大调五声音阶琶音 (宫商角徵羽)
        LOSE        // 失败：伤感的羽调式下行五声音阶
    }

    fun play(type: SoundType) {
        scope.launch {
            try {
                val audioData = when (type) {
                    SoundType.MOVE -> generateMoveSound()
                    SoundType.CAPTURE -> generateCaptureSound()
                    SoundType.CHECK -> generateCheckSound()
                    SoundType.WIN -> generateWinSound()
                    SoundType.LOSE -> generateLoseSound()
                }
                playPcm(audioData)
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to play synthesized sound", t)
            }
        }
    }

    /**
     * 底层 PCM 静态加载与播放
     */
    private fun playPcm(pcmData: ShortArray) {
        try {
            // 在静态模式 (MODE_STATIC) 下，bufferSize 必须与实际写入的短整数数组的字节大小精确匹配 (即 pcmData.size * 2)
            // 绝不能因为对比 minBufSize 而扩大 bufferSize。否则在部分系统或模拟器底层，会因为数据未填满缓冲区而导致 play() 挂起或闪退。
            val bufferSize = pcmData.size * 2
            if (bufferSize <= 0) return

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            // 必须在初始化状态成功时才可进行后续的 write 和 play 动作，进行极致防御
            if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack.play()

                // 延迟结束后自动释放 AudioTrack
                scope.launch {
                    val durationMs = (pcmData.size.toFloat() / SAMPLE_RATE * 1000).toLong()
                    delay(durationMs + 100)
                    try {
                        if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                            audioTrack.stop()
                        }
                    } catch (ignored: Throwable) {}
                    try {
                        audioTrack.release()
                    } catch (ignored: Throwable) {}
                }
            } else {
                try {
                    audioTrack.release()
                } catch (ignored: Throwable) {}
            }
        } catch (e: Throwable) {
            // 全量顶级捕捉，在任何无音频外设、模拟器底层崩溃或不兼容环境下，静默记录日志，杜绝应用闪退
            Log.w(TAG, "Unable to create AudioTrack", e)
        }
    }

    /**
     * 生成落子声（木质清脆碰撞）
     * 基频约 450Hz 指数级衰减，碰撞最初 10ms 引入细微的高频白噪声提供木纹碰撞质感。
     */
    private fun generateMoveSound(): ShortArray {
        val durationSec = 0.09
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(totalSamples)

        val baseFreq = 460.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // 棋盘极速衰减包络
            val envelope = exp(-40.0 * t)
            
            // 频偏滑落：打击瞬间受压，频率略高，随后滑落
            val freq = baseFreq + 220.0 * exp(-120.0 * t)
            
            // 正弦波
            val sine = sin(2.0 * PI * freq * t)
            
            // 白噪声物理摩擦感
            val noiseEnvelope = exp(-200.0 * t)
            val noise = (Random.nextDouble(-1.0, 1.0) * noiseEnvelope)
            
            val sampleVal = (sine * 0.82 + noise * 0.18) * envelope
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    /**
     * 生成吃子声（木头撞击的多重碎裂感）
     * 基频 340Hz，比普通落子更沉重，且 30ms 处追加一个反弹的二次微弱撞击包络。
     */
    private fun generateCaptureSound(): ShortArray {
        val durationSec = 0.16
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(totalSamples)

        val baseFreq = 340.0
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            
            // 第一次重击和 30ms 后的微弱反弹余震
            val envelope1 = exp(-30.0 * t)
            val envelope2 = if (t > 0.03) exp(-22.0 * (t - 0.03)) * 0.6 else 0.0
            val envelope = envelope1 + envelope2

            val freq = baseFreq + 180.0 * exp(-90.0 * t)
            val sine = sin(2.0 * PI * freq * t)
            
            // 明显更重的碰撞噪声
            val noiseEnvelope = exp(-100.0 * t)
            val noise = (Random.nextDouble(-1.0, 1.0) * noiseEnvelope)
            
            val sampleVal = (sine * 0.72 + noise * 0.28) * envelope
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    /**
     * 生成将军警示音（古风大三和弦清脆齐鸣）
     * C5 (523Hz), E5 (659Hz), G5 (784Hz) 融合共鸣。
     */
    private fun generateCheckSound(): ShortArray {
        val durationSec = 0.28
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(totalSamples)

        val freq1 = 523.25
        val freq2 = 659.25
        val freq3 = 783.99

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val envelope = exp(-10.0 * t)

            // 三频混音，模拟古代铜铃/琴音齐鸣
            val wave = (sin(2.0 * PI * freq1 * t) + 
                        sin(2.0 * PI * freq2 * t) * 0.75 + 
                        sin(2.0 * PI * freq3 * t) * 0.5) / 2.25

            val sampleVal = wave * envelope
            pcm[i] = (sampleVal * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    /**
     * 生成胜利音效（国风五声宫调上行琶音）
     * 宫(523Hz) -> 商(587Hz) -> 角(659Hz) -> 徵(784Hz) -> 羽(880Hz) -> 宫(1046Hz) 快速递进
     */
    private fun generateWinSound(): ShortArray {
        val durationSec = 1.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(totalSamples)

        val notes = doubleArrayOf(523.25, 587.33, 659.25, 783.99, 880.00, 1046.50)
        val noteDelay = 0.08

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var waveSum = 0.0
            
            for (n in notes.indices) {
                val onset = n * noteDelay
                if (t >= onset) {
                    val dt = t - onset
                    val noteEnvelope = exp(-5.0 * dt)
                    waveSum += sin(2.0 * PI * notes[n] * dt) * noteEnvelope * 0.22
                }
            }
            
            pcm[i] = (waveSum * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }

    /**
     * 生成失败音效（下行悲伤羽调五声音阶琶音）
     * 羽(880Hz) -> 徵(784Hz) -> 角(659Hz) -> 商(587Hz) -> 宫(523Hz) -> 羽(440Hz) -> 角(330Hz) 叹息式滑落
     */
    private fun generateLoseSound(): ShortArray {
        val durationSec = 1.3
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val pcm = ShortArray(totalSamples)

        val notes = doubleArrayOf(880.00, 783.99, 659.25, 587.33, 523.25, 440.00, 330.00)
        val noteDelay = 0.12

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var waveSum = 0.0
            
            for (n in notes.indices) {
                val onset = n * noteDelay
                if (t >= onset) {
                    val dt = t - onset
                    // 最后一个低音有更慢更深沉的衰减
                    val decayRate = if (n == notes.size - 1) 2.0 else 6.0
                    val noteEnvelope = exp(-decayRate * dt)
                    waveSum += sin(2.0 * PI * notes[n] * dt) * noteEnvelope * 0.2
                }
            }
            
            pcm[i] = (waveSum * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return pcm
    }
}
