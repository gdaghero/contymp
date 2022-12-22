package core

import data.CsvReader
import data.TxtWriter
import model.Currency
import model.input.body.BodyColumnIn
import model.input.body.BodyEntryIn
import model.input.body.BodyLineIn
import model.input.header.HeaderColumnIn
import model.input.header.HeaderEntryIn
import model.input.header.HeaderLineIn
import model.output.ColumnOut
import model.output.EntryOut
import model.output.LineOut
import model.toCompany
import model.toIVA
import model.toIVASubtotal
import java.time.Month
import java.time.format.TextStyle
import java.util.*

class CsvConverter(fileName: String) : Converter<HeaderLineIn, LineOut> {

    private val reader: CsvReader = CsvReader(fileName)
    private val header: HeaderLineIn = convertHeaderLine()

    private val lineWriter by lazy {
        val emp = header.entries.readString(HeaderColumnIn.EMPRESA)
        val month = Month.of(header.entries.readInt(HeaderColumnIn.MES))
            .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
            .uppercase()
        val year = header.entries.readString(HeaderColumnIn.ANO)
        TxtWriter("${emp}_${month}_${year}")
    }

    private val errorWriter by lazy { TxtWriter("log") }

    override fun convert() {
        try {
            tryConvert()
        } catch (throwable: Throwable) {
            writeError(throwable)
        } finally {
            if (errorWriter.isEmpty()) {
                errorWriter.writeLine("No errors found")
            }
            errorWriter.close()
            lineWriter.close()
        }
    }

    private fun tryConvert() {
        reader.skip(lines = 2)
        while (reader.hasNext()) {
            val currentLine = reader.nextLine().toBodyLine()
            writeLineForIVA(currentLine)
            writeLineForSubtotal(currentLine)
            if (reader.hasNext()) {
                val nextLine = reader.peek().toBodyLine()
                if (isMultipleLineInvoice(currentLine, nextLine)) {
                    reader.skip(1)
                    writeLineForIVA(nextLine)
                    writeLineForSubtotal(nextLine)
                    writeLineForTotal(currentLine, nextLine)
                } else {
                    writeLineForTotal(currentLine)
                }
            } else {
                writeLineForTotal(currentLine)
            }
        }
    }

    private fun writeLineForIVA(line: BodyLineIn) {
        val company = header.entries.readString(HeaderColumnIn.EMPRESA).toCompany()
        val ivaType = line.entries.readString(BodyColumnIn.TIPO_IVA).toIVA(company)
        val iva = line.entries.readFloat(BodyColumnIn.IVA).toDecimalFormat()
        val entries = buildCommonEntries(line).apply {
            add(EntryOut(columnOut = ColumnOut.TOTAL, value = iva))
            add(EntryOut(columnOut = ColumnOut.DEBE, value = ivaType))
            add(EntryOut(columnOut = ColumnOut.HABER, value = ""))
        }
        writeEntries(entries)
    }

    private fun writeLineForSubtotal(line: BodyLineIn) {
        val company = header.entries.readString(HeaderColumnIn.EMPRESA).toCompany()
        val ivaType = line.entries.readString(BodyColumnIn.TIPO_IVA).toIVASubtotal(company)
        val subtotal = line.entries.readFloat(BodyColumnIn.SUBTOTAL).toDecimalFormat()
        val entries = buildCommonEntries(line).apply {
            add(EntryOut(columnOut = ColumnOut.TOTAL, value = subtotal))
            add(EntryOut(columnOut = ColumnOut.DEBE, value = ivaType))
            add(EntryOut(columnOut = ColumnOut.HABER, value = ""))
        }
        writeEntries(entries)
    }

    private fun writeLineForTotal(line: BodyLineIn) {
        val company = header.entries.readString(HeaderColumnIn.EMPRESA).toCompany()
        val subtotal = line.entries.readFloat(BodyColumnIn.SUBTOTAL)
        val iva = line.entries.readFloat(BodyColumnIn.IVA)
        val entries = buildCommonEntries(line).apply {
            add(EntryOut(columnOut = ColumnOut.TOTAL, value = (subtotal + iva).toDecimalFormat()))
            add(EntryOut(columnOut = ColumnOut.DEBE, value = ""))
            add(EntryOut(columnOut = ColumnOut.HABER, value = company.cash))
        }
        writeEntries(entries)
    }

    private fun writeLineForTotal(line: BodyLineIn, nextLine: BodyLineIn) {
        val company = header.entries.readString(HeaderColumnIn.EMPRESA).toCompany()
        val totalFirst = with(line.entries) {
            readFloat(BodyColumnIn.IVA) + readFloat(BodyColumnIn.SUBTOTAL)
        }
        val totalNext = with(nextLine.entries) {
            readFloat(BodyColumnIn.IVA) + readFloat(BodyColumnIn.SUBTOTAL)
        }
        val entries = buildCommonEntries(line).apply {
            add(EntryOut(columnOut = ColumnOut.TOTAL, value = (totalFirst + totalNext).toDecimalFormat()))
            add(EntryOut(columnOut = ColumnOut.DEBE, value = ""))
            add(EntryOut(columnOut = ColumnOut.HABER, value = company.cash))
        }
        writeEntries(entries)
    }

    private fun buildCommonEntries(line: BodyLineIn): MutableList<EntryOut> {
        val day = line.entries.readString(BodyColumnIn.DIA)
        val rut = line.entries.readString(BodyColumnIn.RUT)
        val concept = line.entries.readString(BodyColumnIn.CONCEPTO)
        val number = line.entries.readString(BodyColumnIn.NUMERO_COMPROBANTE)
        val book = header.entries.readString(HeaderColumnIn.LIBRO)
        return mutableListOf<EntryOut>().apply {
            add(EntryOut(columnOut = ColumnOut.DIA, value = day))
            add(EntryOut(columnOut = ColumnOut.RUC, value = rut))
            add(EntryOut(columnOut = ColumnOut.CONCEPTO, value = "$concept $number"))
            add(EntryOut(columnOut = ColumnOut.MONEDA, value = Currency.UYU().value))
            add(EntryOut(columnOut = ColumnOut.LIBRO, value = book))
        }
    }

    private fun List<HeaderEntryIn>.readString(column: HeaderColumnIn) =
        first { it.column == column }.value
            .ifBlank { error("Field ${column.name} cannot be blank") }

    private fun List<BodyEntryIn>.readString(column: BodyColumnIn) =
        first { it.column == column }.value
            .ifBlank { error("Field ${column.name} cannot be blank") }

    private fun List<BodyEntryIn>.readFloat(column: BodyColumnIn): Float =
        first { it.column == column }.value
            .ifBlank { error("Field ${column.name} cannot be blank") }
            .toFloat()

    private fun List<HeaderEntryIn>.readInt(column: HeaderColumnIn): Int =
        first { it.column == column }.value
            .ifBlank { error("Field ${column.name} cannot be blank") }
            .toInt()

    private fun writeEntries(entries: List<EntryOut>) {
        val lineOut = entries
            .sortedBy { it.columnOut.ordinal }
            .map { it.value }
            .joinToString(DELIMITER)

        lineWriter.writeLine(lineOut)
        println(lineOut)
    }

    private fun writeError(throwable: Throwable) {
        errorWriter.writeLine("Error: ${throwable.message}")
        errorWriter.writeLine(SEPARATOR)
        reader.last()?.let {
            errorWriter.writeLine(
                it.toBodyLine().entries.joinToString("\n") { entry ->
                    "${entry.column.name}: ${entry.value}"
                }
            )

        }
        errorWriter.writeLine(SEPARATOR)
        throwable.stackTrace.forEach {
            errorWriter.writeLine(it.toString())
        }
    }

    private fun String.toBodyLine(): BodyLineIn {
        val line = split(DELIMITER)
        val columns = BodyColumnIn.values()
        val entries = columns.mapIndexed { index, _ ->
            BodyEntryIn(column = columns[index], value = line[index])
        }
        return BodyLineIn(entries)
    }

    private fun convertHeaderLine(): HeaderLineIn {
        reader.skip(lines = 1)
        val line = reader.nextLine().split(DELIMITER)
        val columns = HeaderColumnIn.values()
        val entries = columns.mapIndexed { index, _ ->
            HeaderEntryIn(column = columns[index], value = line[index])
        }
        return HeaderLineIn(entries)
    }

    private fun isMultipleLineInvoice(current: BodyLineIn, next: BodyLineIn): Boolean {
        val currentInvoice = current.entries.readString(BodyColumnIn.NUMERO_COMPROBANTE)
        val nextInvoice = next.entries.readString(BodyColumnIn.NUMERO_COMPROBANTE)
        return currentInvoice == nextInvoice
    }

    companion object {
        private const val DELIMITER = ","
        private const val SEPARATOR = "======================================================================"
    }
}
