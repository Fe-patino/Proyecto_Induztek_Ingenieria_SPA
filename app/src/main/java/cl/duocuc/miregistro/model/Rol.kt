package cl.duocuc.miregistro.model



data class Rol(
    val id: Long,
    val nombre: String
) {
    companion object {

        const val ID_TECNICO = 1L
        const val ID_SUPERVISOR = 2L
    }
}
