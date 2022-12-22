package core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun Float.toDecimalFormat(): String = DecimalFormat(
    "#.00",
    DecimalFormatSymbols(Locale.US)
).format(this)
