package space.siy.music_analyzer_kt

import org.apache.commons.math3.stat.descriptive.moment.Variance
import kotlin.math.abs
import kotlin.math.ceil

internal object EventDetector {
    /**
     * ビート信号のオフセットを増やしながらソースと相関を求め、
     * 最も相関の高いオフセットの位置を返す
     */
    private fun ShortArray.getBeatCorrelation(tempo: Int, sampleRate: Int): Int {
        val maxOffset = samplesPerBeat(tempo, sampleRate) - 1
        val correlation = FloatArray(maxOffset)
        forEachIndexed { index, sample ->
            correlation[index % maxOffset] += abs(sample.toFloat())
        }
        return correlation.maxIndex()
    }

    private fun FloatArray.maxIndex(): Int {
        var max = 0f
        var index = 0
        forEachIndexed { i, sample ->
            if (max < sample) {
                max = sample
                index = i
            }
        }
        return index
    }

    data class VolumeBlock(val volume: Float, val range: IntRange)

    private fun ShortArray.getVolumeBlocks(blockSize: Int, offset: Int): List<VolumeBlock> {
        val res = FloatArray(ceil(this.size / blockSize.toFloat()).toInt())
        forEachIndexed { index, sample ->
            res[((index - offset) / blockSize).coerceAtLeast(0).coerceAtMost(res.size - 1)] += abs(sample.toFloat())
        }
        return res.mapIndexed { index, volume ->
            VolumeBlock(
                volume,
                index * blockSize + offset until (index + 1) * blockSize + offset
            )
        }
    }

    fun ShortArray.estimateSection(tempo: Int, sampleRate: Int, offset: Int, chunkSize: Int = 4): List<VolumeBlock> {
        val samplesPerBeat = samplesPerBeat(tempo, sampleRate)
        val list = Array<List<VolumeBlock>>(chunkSize * 2) { _ -> emptyList() }
        for (offsetIndex in 0 until chunkSize * 2) {
            list[offsetIndex] =
                getVolumeBlocks(samplesPerBeat * chunkSize, (offsetIndex * samplesPerBeat / 2f).toInt() + offset)
        }
        val v = Variance()
        return list.maxBy { l -> v.evaluate(l.map { it.volume.toDouble() }.toDoubleArray()) } ?: emptyList()
    }

    private fun samplesPerBeat(tempo: Int, sampleRate: Int) = (60f / tempo * sampleRate).toInt()

    private fun analyzeEvents(volumeBlocks: List<VolumeBlock>, sampleRate: Int, beatOffset: Int): List<Event> {
        val diffs = ArrayList<Pair<Int, Float>>()
        diffs.add(Pair(0, volumeBlocks[0].volume))
        for (i in 1 until volumeBlocks.size) {
            diffs.add(Pair(i, volumeBlocks[i].volume - volumeBlocks[i - 1].volume))
        }
        val res = ArrayList<Event>()
        res.addAll(
            diffs.sortedByDescending { it.second }.take(10).map {
                val pos = volumeBlocks[it.first].range.first
                Event.VolumeUp(pos / sampleRate.toFloat(), beatOffset, abs(it.second))
            }
        )
        res.addAll(diffs.sortedBy { it.second }.take(10)
            .map {
                Event.VolumeDown(
                    volumeBlocks[it.first].range.first / sampleRate.toFloat(),
                    beatOffset,
                    abs(it.second)
                )
            }
        )
        return res
    }

    private fun detectStartEnd(data: ShortArray, sampleRate: Int): List<Event> {
        return listOf((Event.StartPos(data.indexOfFirst { it > 200 } / sampleRate.toFloat())),
            Event.EndPos(data.indexOfLast { it > 200 } / sampleRate.toFloat()))
    }

    fun detect(data: ShortArray, lowPassed: ShortArray, sampleRate: Int, tempo: Int): List<Event> {
        val estimatedOffset = lowPassed.getBeatCorrelation(tempo, sampleRate)
        val estimatedSection1 = data.estimateSection(tempo, sampleRate, estimatedOffset)
        val estimatedSection2 = data.estimateSection(tempo, sampleRate, estimatedOffset, 2)
        return analyzeEvents(estimatedSection1, sampleRate, 0) +
                analyzeEvents(estimatedSection2, sampleRate, 2) +
                detectStartEnd(data, sampleRate)
    }
}
