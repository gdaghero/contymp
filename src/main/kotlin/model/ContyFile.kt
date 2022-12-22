package model

import model.output.LineOut

data class ContyFile(
    val lines: List<LineOut>,
    val properties: List<ContyProperty>
)
