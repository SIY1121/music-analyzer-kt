package space.siy.music_analyzer_kt

import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

internal object TempoDetector {
    /**
     * ピークの位置と値を保持する
     */
    data class Peak(val position: Int, val value: Short)

    /**
     * 与えられたサンプルをブロックに分割し
     * そのブロック内で最大パワーのサンプル値を
     * 検出したリストを返す
     */
    private fun ShortArray.getPeaks(sampleRate: Int): List<Peak> {
        val blockSize = sampleRate / 2
        val blockCount = this.size / blockSize
        val arr = ShortArray(blockSize)
        val res = ArrayList<Peak>()
        val buf = ShortBuffer.wrap(this)
        for (i in 0 until blockCount) {
            buf.get(arr, 0, kotlin.math.min(blockSize, buf.remaining()))
            val max = arr.maxBy { abs(it.toInt()) } ?: continue
            val index = arr.indexOf(max) + i * blockSize
            res.add(Peak(index, max))
        }
        return res
    }

    /**
     * テンポと根拠のピークを返す
     */
    data class EstimatedTempo(val tempo: Int, val peaks: List<Peak>)

    /**
     * 与えられたピークのリストから
     * テンポを推測する
     */
    private fun List<Peak>.estimateTempo(sampleRate: Int): List<EstimatedTempo> {
        val map = HashMap<Int, MutableList<Peak>>()

        this.subList(0, this.size - 2).forEachIndexed { index, peak ->
            for (i in index + 1 until min(index + 11, this.size)) {
                var tempo = (60f * sampleRate) / (this[i].position - peak.position)
                while (tempo < 100) {
                    tempo *= 2
                }
                while (tempo > 200) {
                    tempo /= 2
                }
                val tempoRounded = tempo.roundToInt()
                if (map[tempoRounded] == null) map[tempoRounded] = ArrayList()
                map[tempoRounded]?.add(peak)
            }
        }

        return map.map { EstimatedTempo(it.key, it.value) }.sortedByDescending { it.peaks.size }
    }

    fun estimate(data: ShortArray, sampleRate: Int): EstimatedTempo {
        val peaks = data.getPeaks(sampleRate)
        return peaks.sortedByDescending { it.value }.take(peaks.size / 2).sortedBy { it.position }.estimateTempo(sampleRate).first()
    }
}
