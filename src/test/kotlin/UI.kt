import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.stage.Stage
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import space.siy.music_analyzer_kt.Event
import space.siy.music_analyzer_kt.MusicAnalyzer
import ui.EventList
import ui.MainView
import java.io.File
import java.nio.ShortBuffer
import kotlin.math.ceil

class Main : Application() {
    override fun start(primaryStage: Stage) {
        val targetFile =
            File("")
        val grabber =
            FFmpegFrameGrabber(targetFile).apply {
                sampleRate = 44100
                audioChannels = 1
                sampleMode = FrameGrabber.SampleMode.SHORT
                start()
            }

        val data = ShortBuffer.allocate(ceil(grabber.lengthInTime / 1000_000f).toInt() * 44100)
        while (true) {
            val frame = grabber.grabSamples() ?: break
            val buf = frame.samples[0] as ShortBuffer
            data.put(buf)
        }

        val analyzer = MusicAnalyzer(data.toArray(), grabber.sampleRate)
        grabber.stop()
        val res = analyzer.analyze()

        val displayData = listOf(EventList("event", res.events.mapIndexed {index, event ->
            when {
                event is Event.VolumeUp -> EventList.Event("up ${index}", (event.positionInSec * 44100).toInt())
                event is Event.VolumeDown -> EventList.Event("d ${index}", (event.positionInSec * 44100).toInt())
                else -> EventList.Event("e", (event.positionInSec * 44100).toInt())
            }
        }))

        println("${targetFile.name} : $res")
        primaryStage.scene = Scene(ScrollPane().apply {
            maxWidth = 1000.0
            val pane = MainView(listOf(data.toArray().map { it / 2f / Short.MAX_VALUE }.toFloatArray()), res.tempo, displayData)
            hvalueProperty().addListener { _, _, n -> pane.draw(n.toDouble() * (pane.width - width)) }
            content = pane
        })
        primaryStage.title = "Analyzer - ${targetFile.name}"
        primaryStage.show()
    }
}

fun main(args: Array<String>) {
    Application.launch(Main::class.java, *args)
}

