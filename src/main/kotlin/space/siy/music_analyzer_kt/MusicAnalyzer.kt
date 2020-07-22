package space.siy.music_analyzer_kt

import uk.me.berndporr.iirj.Butterworth
import kotlin.math.abs

class MusicAnalyzer(val samples: ShortArray, val sampleRate: Int) {
    private val highPassedSamples = ShortArray(samples.size)
    private val lowPassedSamples = ShortArray(samples.size)

    data class Result(val tempo: Int, val events: List<Event>,val maxVolume: Float)

    fun analyze(): Result {
        prepareSamples()
        val mixed = lowPassedSamples.mapIndexed { index, sample -> (sample + highPassedSamples[index]).toShort() }
            .toShortArray()
        val tempo = TempoDetector.estimate(mixed, sampleRate)
        val events = EventDetector.detect(samples, lowPassedSamples, sampleRate, tempo.tempo)
        val maxVolume = abs((samples.maxBy { abs(it.toInt()) } ?: 0) / Short.MAX_VALUE.toFloat())
        return Result(tempo.tempo, events, maxVolume)
    }

    /**
     * 解析に必要なサンプルを準備する
     */
    private fun prepareSamples() {
        val highPass = Butterworth().apply {
            highPass(4, 44100.0, 10000.0)
        }
        val lowPass = Butterworth().apply {
            lowPass(4, 44100.0, 100.0)
        }
        samples.map { it.toDouble() }.forEachIndexed { index, sample ->
            highPassedSamples[index] = highPass.filter(sample).toShort()
            lowPassedSamples[index] = highPass.filter(sample).toShort()
        }
    }
}
