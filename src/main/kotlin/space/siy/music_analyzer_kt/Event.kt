package space.siy.music_analyzer_kt

sealed class Event(val positionInSec: Float, val beatOffset: Int) {
    class VolumeUp(positionInSec: Float, beatOffset: Int, val diff: Float) :
        Event(positionInSec, beatOffset)

    class VolumeDown(positionInSec: Float, beatOffset: Int, val diff: Float) :
        Event(positionInSec, beatOffset)

    class StartPos(positionInSec: Float) :
        Event(positionInSec, -1)

    class EndPos(positionInSec: Float) :
        Event(positionInSec, -1)
}
