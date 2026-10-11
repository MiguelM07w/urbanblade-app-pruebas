package com.urbanblade.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La variacion de un KPI no siempre tiene el mismo color: subir ingresos es bueno, subir
 * cancelaciones no. La pantalla lo decide con positiveIsGood.
 */
class UrbanTrendTest {

    @Test
    fun subirEsBuenoCuandoMasEsMejor() {
        assertEquals(StatusTone.SUCCESS, trendTone(12.6))
    }

    @Test
    fun bajarEsMaloCuandoMasEsMejor() {
        assertEquals(StatusTone.DANGER, trendTone(-1.6))
    }

    @Test
    fun enCancelacionesOGastosSubirEsMalo() {
        assertEquals(StatusTone.DANGER, trendTone(3.0, positiveIsGood = false))
        assertEquals(StatusTone.SUCCESS, trendTone(-3.0, positiveIsGood = false))
    }

    @Test
    fun sinCambioNoHayTono() {
        assertEquals(StatusTone.NEUTRAL, trendTone(0.0))
    }
}
