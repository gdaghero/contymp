package data

import java.io.File

class CsvReader(fileName: String) {

    private val iterator = File(fileName)
        .bufferedReader()
        .lineSequence()
        .toList()
        .listIterator()

    private var currentLine: String? = null

    fun peek(): String {
        val next = iterator.next()
        iterator.previous()
        return next
    }

    fun nextLine(): String =
        iterator.next().also {
            currentLine = it
        }

    fun skip(lines: Int) {
        repeat(lines) { iterator.next() }
    }

    fun last() = currentLine

    fun hasNext(): Boolean =
        iterator.hasNext()
}
