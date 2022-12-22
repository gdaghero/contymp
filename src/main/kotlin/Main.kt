import core.CsvConverter

fun main(args: Array<String>) {
    val converter = CsvConverter(args.first())
    converter.convert()
}
