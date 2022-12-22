package model

sealed class Company {

    abstract val cash: Int
    abstract val subtotalMin: Int
    abstract val subtotalBas: Int
    abstract val ivaMin: Int
    abstract val ivaBas: Int

    data class DelSacramento(
        override val cash: Int = 1111,
        override val subtotalMin: Int = 1141,
        override val subtotalBas: Int = 1142,
        override val ivaMin: Int = 113522,
        override val ivaBas: Int = 113512
    ) : Company()

    data class Davit(
        override val cash: Int = 11111,
        override val subtotalMin: Int = 11411,
        override val subtotalBas: Int = 11412,
        override val ivaMin: Int = 11331,
        override val ivaBas: Int = 11332
    ) : Company()

    data class Abuelita(
        override val cash: Int = 11111,
        override val subtotalMin: Int = 11411,
        override val subtotalBas: Int = 11412,
        override val ivaMin: Int = 11331,
        override val ivaBas: Int = 11332
    ) : Company()
}

fun String.toCompany() = when (this) {
    "DEL SACRAMENTO" -> Company.DelSacramento()
    "ABUELITA" -> Company.Abuelita()
    "DAVIT" -> Company.Davit()
    else -> error("Company not defined")
}

fun String.toIVA(company: Company) = when (this) {
    "M" -> company.ivaMin
    "B" -> company.ivaBas
    else -> error("IVA not defined")
}

fun String.toIVASubtotal(company: Company) = when (this) {
    "M" -> company.subtotalMin
    "B" -> company.subtotalBas
    else -> error("IVA not defined")
}
