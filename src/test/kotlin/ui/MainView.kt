package ui

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.*
import javafx.scene.paint.Paint
import javafx.scene.shape.Line
import javafx.scene.text.Font
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine
import kotlin.concurrent.thread

class MainView(val samples: List<FloatArray>, val tempo: Int, val event: List<EventList>) : Pane() {
    val waveformViews = samples.mapIndexed { index, sample -> WaveformView(sample).apply { layoutY = index * 200.0 } }
    val line = Line(0.0, 0.0, 0.0, 400.0).apply {
        stroke = Paint.valueOf("red")
    }
    val audioFormat = AudioFormat(44100f, 16, 1, true, false)
    val info = DataLine.Info(SourceDataLine::class.java, audioFormat)
    val audioLine = AudioSystem.getLine(info) as SourceDataLine
    val sampleBufferCount = (44100 * 0.002).toInt()
    var pos = 0

    init {
        waveformViews.forEach {
            children.add(it)
        }
        event.forEach {
            children.add(Pane().apply {
                children.add(Label(it.name))
                it.data.forEachIndexed { index, event ->
                    children.add(Label(event.label).apply {
                        layoutX = event.pos * widthPerSample
                        layoutY = 70 + (index / 20) * 30.0
                        font = Font.font("Arial", 25.0)
                        textFill = Paint.valueOf("white")
                        background = Background(BackgroundFill(Paint.valueOf("red"), CornerRadii.EMPTY, Insets.EMPTY))
                    })
                }
            })
        }
        children.add(line)
        children.add(Button("Play: bpm:${tempo}").apply {
            font = Font.font("Arial", 20.0)
            setOnAction {
                audioLine.open(audioFormat)
                audioLine.start()
                thread {
                    val start = System.currentTimeMillis()
                    while (true) {
                        val buf = ByteBuffer.allocate(sampleBufferCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in pos until pos + sampleBufferCount) {
                            buf.putShort((samples[0][i] * Short.MAX_VALUE).toShort())
                        }
                        val arr = buf.toArray()
                        Platform.runLater {
                            line.translateX = (System.currentTimeMillis() - start) / 1000.0 * 44100.0 * widthPerSample
                            this@MainView.layout()
                        }
                        audioLine.write(arr, 0, arr.size)
                        pos += sampleBufferCount
                    }
                }
            }
        })
        minWidth = samples[0].size / blockPerSample * widthPerBlock
        height = 200.0
        println("mainview ready")
    }

    fun draw(offset: Double) {
        waveformViews.forEach {
            it.layoutX = offset
            it.offset = offset
            it.draw()
        }
    }

}

fun ByteBuffer.toArray(): ByteArray {
    val arr = ByteArray(limit())
    position(0)
    get(arr)
    return arr
}
