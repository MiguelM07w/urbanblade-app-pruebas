package com.urbanblade.mobile.e2e

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput

/**
 * Acciones de "robot" para las pruebas instrumentadas (T145): esperar, tocar y escribir como un usuario.
 *
 * Las pantallas cargan datos de la red, así que nada se toca de inmediato: cada acción espera (hasta
 * [TIMEOUT_MS]) a que el elemento aparezca y, si es un botón, a que esté habilitado. Así las pruebas no
 * fallan "a veces" por ir más rápido que la app.
 */
const val TIMEOUT_MS = 10_000L

/** Espera a que haya en pantalla al menos un elemento con ese texto. */
fun ComposeTestRule.waitForText(text: String, substring: Boolean = false, timeoutMs: Long = TIMEOUT_MS) {
    waitUntil(timeoutMs) { onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
}

/** Espera a que desaparezca de la pantalla todo elemento con ese texto. */
fun ComposeTestRule.waitForNoText(text: String, timeoutMs: Long = TIMEOUT_MS) {
    waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
}

/** ¿Hay en pantalla algún elemento con ese texto? */
fun ComposeTestRule.hasNodeWithText(text: String): Boolean =
    onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

/** Toca el primer elemento con ese texto (un botón, una tarjeta, una casilla...). */
fun ComposeTestRule.tapText(text: String, substring: Boolean = false) {
    waitForText(text, substring)
    onAllNodesWithText(text, substring = substring)[0].scrollIntoViewIfPossible().performClick()
    waitForIdle()
}

/** Toca un botón en cuanto se habilita (p. ej. "Reservar cita" espera a que se acepte la política). */
fun ComposeTestRule.tapWhenEnabled(text: String) {
    val button = hasText(text) and isEnabled()
    waitUntil(TIMEOUT_MS) { onAllNodes(button).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(button)[0].scrollIntoViewIfPossible().performClick()
    waitForIdle()
}

/** Toca una pestaña de la barra de navegación inferior ("Inicio", "Mis citas", "Cuenta"...). */
fun ComposeTestRule.tapTab(label: String) {
    val tab = hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
    waitUntil(TIMEOUT_MS) { onAllNodes(tab).fetchSemanticsNodes().isNotEmpty() }
    onAllNodes(tab)[0].performClick()
    waitForIdle()
}

/**
 * Escribe en el campo de texto número [index] de la pantalla (0 = el primero). En UrbanTextField la
 * etiqueta es un texto aparte, encima del campo, así que el campo se ubica por su posición.
 */
fun ComposeTestRule.typeIntoField(index: Int, text: String) {
    waitUntil(TIMEOUT_MS) { onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size > index }
    onAllNodes(hasSetTextAction())[index].scrollIntoViewIfPossible().performTextInput(text)
    waitForIdle()
}

/** Desplaza la pantalla hasta el elemento si está dentro de una lista con scroll; si no, no hace nada. */
private fun SemanticsNodeInteraction.scrollIntoViewIfPossible(): SemanticsNodeInteraction {
    try {
        performScrollTo()
    } catch (_: AssertionError) {
        // No tiene un contenedor con scroll: ya está a la vista.
    }
    return this
}
