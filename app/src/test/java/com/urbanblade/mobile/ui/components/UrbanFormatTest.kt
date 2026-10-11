package com.urbanblade.mobile.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Formatos de dinero y porcentaje en un solo lugar. Antes cada pantalla tenia su copia
 * (once versiones de money() con locales y decimales distintos); estas pruebas fijan el texto
 * que deben producir todas.
 */
class UrbanFormatTest {

    @Test
    fun elDineroLlevaCentavosYSeparadorDeMiles() {
        assertEquals("\$1,234.50", UrbanFormat.money(1234.5))
        assertEquals("\$0.00", UrbanFormat.money(0.0))
    }

    @Test
    fun sinDatosElDineroNoImprimeNaN() {
        assertEquals("\$0.00", UrbanFormat.money(null))
        assertEquals("\$0.00", UrbanFormat.money(Double.NaN))
        assertEquals("\$0.00", UrbanFormat.money(Double.POSITIVE_INFINITY))
    }

    @Test
    fun laCifraCortaNoLlevaCentavos() {
        assertEquals("\$1,234", UrbanFormat.moneyShort(1234.4))
    }

    @Test
    fun losPorcentajesLlevanSuSigno() {
        assertEquals("12.6%", UrbanFormat.percent(12.6))
        assertEquals("-1.6%", UrbanFormat.percent(-1.6))
    }

    @Test
    fun laVariacionEsRelativaAlPeriodoAnterior() {
        assertEquals(12.6, UrbanFormat.delta(112.6, 100.0)!!, 0.001)
        assertEquals(-50.0, UrbanFormat.delta(50.0, 100.0)!!, 0.001)
    }

    @Test
    fun sinPeriodoAnteriorNoHayVariacionQueMostrar() {
        assertNull(UrbanFormat.delta(100.0, 0.0))
        assertNull(UrbanFormat.delta(null, 100.0))
        assertNull(UrbanFormat.delta(100.0, null))
        assertNull(UrbanFormat.delta(Double.NaN, 100.0))
    }
}
