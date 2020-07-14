package ui

class EventList(val name: String, val data: List<Event>) {
    data class Event(val label: String, val pos: Int)
}
