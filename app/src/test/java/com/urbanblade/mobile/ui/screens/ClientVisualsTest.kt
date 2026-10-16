package com.urbanblade.mobile.ui.screens

import com.urbanblade.mobile.data.model.BarberItem
import com.urbanblade.mobile.data.model.BarberUser
import com.urbanblade.mobile.data.model.ServiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClientVisualsTest {

    @Test fun `la cita confirmada muestra el estado de pago validado por el servidor`() {
        assertEquals("Cita aprobada y pago resuelto. Te esperamos a la hora de tu cita.", clientStateHint("confirmada", true))
        assertEquals("Cita aprobada. Resuelve el pago antes del servicio; consulta las opciones aquí o en recepción.", clientStateHint("confirmada", false))
        assertEquals("Cita aprobada. Consulta el estado de tu pago en tus comprobantes o en recepción.", clientStateHint("confirmada"))
    }

    @Test
    fun `los servicios reales del catalogo se clasifican por su nombre`() {
        assertEquals(ServiceKind.AFEITADO, serviceKind("Afeitado Clásico"))
        assertEquals(ServiceKind.BARBA, serviceKind("Arreglo de Barba"))
        assertEquals(ServiceKind.BARBA, serviceKind("Diseño de Barba"))
        assertEquals(ServiceKind.COMBO, serviceKind("Combo Corte + Barba"))
        assertEquals(ServiceKind.COMBO, serviceKind("Combo Premium"))
        assertEquals(ServiceKind.INFANTIL, serviceKind("Corte Infantil"))
        assertEquals(ServiceKind.CORTE, serviceKind("Corte a Navaja"))
        assertEquals(ServiceKind.CORTE, serviceKind("Corte Mohicano"))
    }

    @Test
    fun `el barbero del muro se traduce del id de usuario al de su perfil`() {
        val barbers = listOf(
            BarberItem(id = "perfil-1", slug = "luis-gonzalez", user = BarberUser("usuario-1", "Luis")),
            BarberItem(id = "perfil-2", user = BarberUser("usuario-2", "Ana"))
        )
        assertEquals("perfil-1", resolveBarberId(barbers, "usuario-1"))
        assertEquals("perfil-2", resolveBarberId(barbers, "perfil-2"))
        assertEquals("perfil-1", resolveBarberId(barbers, "luis-gonzalez"))
        assertNull(resolveBarberId(barbers, "desconocido"))
        assertNull(resolveBarberId(barbers, ""))
    }

    @Test
    fun `keratina, spa, tinte y cejas son cuidado, no corte`() {
        listOf("Keratina Express", "Spa Capilar", "Tinte / Color", "Perfilado de Cejas", "Tratamiento Capilar").forEach {
            assertEquals(it, ServiceKind.TRATAMIENTO, serviceKind(it))
        }
        assertEquals(ServiceKind.CORTE, serviceKind("Skin Fade"))
        assertEquals(ServiceKind.CORTE, serviceKind("Pompadour"))
    }

    @Test
    fun `el catalogo se filtra por tipo y por busqueda sin acentos`() {
        val services = listOf(
            ServiceItem("1", "Diseño de Barba", 150.0, 25, "Perfilado con navaja"),
            ServiceItem("2", "Corte Clásico", 180.0, 30, "Tijera y máquina"),
            ServiceItem("3", "Combo Corte + Barba", 320.0, 60),
            ServiceItem("4", "Spa Capilar", 280.0, 40)
        )
        assertEquals(listOf("1"), filterServices(services, "diseno", null).map { it.id })
        assertEquals(listOf("1", "2"), filterServices(services, "NAVAJA", null).map { it.id } + filterServices(services, "maquina", null).map { it.id })
        assertEquals(listOf("2"), filterServices(services, "", ServiceFilter.CORTES).map { it.id })
        assertEquals(listOf("4"), filterServices(services, "", ServiceFilter.CUIDADO).map { it.id })
        assertEquals(listOf("3"), filterServices(services, "barba", ServiceFilter.COMBOS).map { it.id })
        assertEquals(4, filterServices(services, "  ", null).size)
    }

    @Test
    fun `un nombre desconocido cae en corte`() {
        assertEquals(ServiceKind.CORTE, serviceKind("Servicio especial"))
        assertEquals(ServiceKind.TRATAMIENTO, serviceKind("Tratamiento capilar"))
    }
}
