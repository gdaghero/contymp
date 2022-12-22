package data

import java.io.BufferedWriter
import java.io.File

class TxtWriter(fileName: String) : Writer {

    private var lineCount: Int = 0
    private val writer: BufferedWriter by lazy {
        File(System.getProperty("user.dir"), "$fileName.txt")
            .bufferedWriter()
    }

    override fun writeLine(line: String) {
        writer.append(line)
        writer.append("\r\n")
        lineCount++
    }

    override fun close() {
        writer.flush()
        writer.close()
    }

    override fun isEmpty(): Boolean = lineCount == 0
}

interface Writer {
    fun writeLine(line: String)
    fun close()
    fun isEmpty(): Boolean
}
