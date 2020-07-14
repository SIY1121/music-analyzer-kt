import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FrameGrabber
import space.siy.music_analyzer_kt.MusicAnalyzer
import java.io.File
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

fun main(args: Array<String>) {
    avutil.av_log_set_level(avutil.AV_LOG_QUIET)
    var log = ""
    File("src/test/resources/test-music")
        .listFiles { _, name -> !name.endsWith(".gitkeep") }
        .forEach {targetFile ->
            System.gc()
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
            println("${targetFile.name} : $res")
            log += "${targetFile.name} : $res \n"
        }

    File("${SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(Date())}.log").writeText(log)
}

fun ShortBuffer.toArray(): ShortArray {
    val arr = ShortArray(limit())
    position(0)
    get(arr)
    return arr
}

fun FloatArray.toBuffer(): FloatBuffer {
    val buf = FloatBuffer.allocate(size)
    buf.put(this)
    buf.position(0)
    return buf
}
