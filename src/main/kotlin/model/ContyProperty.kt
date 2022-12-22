package model

@Suppress("SpellCheckingInspection")
sealed class ContyProperty {

    abstract val name: String
    abstract val value: String

    data class From(
        override val name: String = "Desde",
        override val value: String
    ) : ContyProperty()

    data class To(
        override val name: String = "Hasta",
        override val value: String
    ) : ContyProperty()

    data class DeleteExistent(
        override val name: String = "BorraExistentes",
        override val value: String
    ) : ContyProperty()

    data class Imported(
        override val name: String = "Importado",
        override val value: String
    ) : ContyProperty()
}
