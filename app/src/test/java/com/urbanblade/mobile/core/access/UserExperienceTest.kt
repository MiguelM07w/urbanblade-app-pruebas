package com.urbanblade.mobile.core.access

import org.junit.Assert.*
import org.junit.Test

class UserExperienceTest {
    @Test fun `cada rol mantiene su experiencia y personal gana al cliente sin depender del orden`() {
        UserExperience.priority.forEach { role ->
            assertEquals(role, UserExperience.primaryRole(listOf("cliente", role)))
            assertEquals(role, UserExperience.primaryRole(listOf(role, "cliente")))
        }
        assertEquals("recepcionista", UserExperience.primaryRole(listOf("barbero", "recepcionista")))
        assertEquals("Barbero", UserExperience.label(listOf("ingeniero", "barbero")))
        assertTrue(UserExperience.isEngineer(listOf("cliente", "ingeniero")))
        assertFalse(UserExperience.isClient(listOf("cliente", "ingeniero")))
        assertFalse(UserExperience.canBook(listOf("cliente", "ingeniero")))
        assertEquals("guest", UserExperience.primaryRole(listOf("desconocido")))
        assertFalse(UserExperience.canBook(emptyList()))
    }
}
