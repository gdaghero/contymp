package model

sealed class Currency {

    abstract val value: Int

    data class UYU(override val value: Int = 0) : Currency()
}
