package com.urbanblade.mobile.core.access

/** Experiencia visual compartida con web y API. Los permisos los valida Laravel. */
object UserExperience {
    val priority = listOf("administrador", "recepcionista", "barbero", "ingeniero", "cliente")
    fun primaryRole(roles: List<String>): String = priority.firstOrNull { it in roles } ?: "guest"
    fun isClient(roles: List<String>): Boolean = primaryRole(roles) == "cliente"
    fun isEngineer(roles: List<String>): Boolean = primaryRole(roles) == "ingeniero"
    fun canBook(roles: List<String>): Boolean = primaryRole(roles) in listOf("administrador", "recepcionista", "cliente")
    fun label(roles: List<String>): String = when (primaryRole(roles)) {
        "administrador" -> "Administrador"
        "recepcionista" -> "Recepción"
        "barbero" -> "Barbero"
        "ingeniero" -> "Ingeniero"
        "cliente" -> "Cliente"
        else -> "Cuenta"
    }
}
