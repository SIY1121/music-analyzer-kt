package ui

import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.paint.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val blockPerSample = (44100 * 0.01).toInt()
const val widthPerBlock = 1.0
const val widthPerSample = widthPerBlock / blockPerSample

class WaveformView(val samples: FloatArray) : Canvas(1000.0, 200.0) {
    var offset = 0.0

    init {
        draw()
    }

    fun draw() {
        graphicsContext2D.run {
            clearRect(0.0,0.0,width,height)
            var max = 0f
            var min = 0f
            fill = Color.BLUE
            val startIndex = (offset / widthPerBlock * blockPerSample).toInt()
            for (i in startIndex until (startIndex + blockPerSample * 1000)) {
                max = max(max, samples[i])
                min = min(min, samples[i])
                if (i % blockPerSample == 0) {
                    drawLevel(max, min, i - startIndex)
                    max = 0f
                    min = 0f
                }
            }
        }
    }

    fun GraphicsContext.drawLevel(max: Float, min: Float, i: Int) {
        fillRect(
            (i / blockPerSample) * widthPerBlock,
            height / 2 * (1 - abs(max)),
            widthPerBlock,
            height / 2 * abs(max) + height / 2 * abs(min)
        )
    }
}
