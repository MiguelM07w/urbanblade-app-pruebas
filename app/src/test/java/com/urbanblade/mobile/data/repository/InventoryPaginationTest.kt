package com.urbanblade.mobile.data.repository

import com.urbanblade.mobile.core.network.UrbanBladeApi
import com.urbanblade.mobile.data.model.InventoryMeta
import com.urbanblade.mobile.data.model.InventoryProductRow
import com.urbanblade.mobile.data.model.InventoryResponse
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class InventoryPaginationTest {
    @Test
    fun `incluye todas las paginas y no duplica productos`() = runTest {
        val api = mock<UrbanBladeApi>()
        val first = InventoryProductRow(id = "p1", nombre = "Primero")
        val second = InventoryProductRow(id = "p2", nombre = "Segundo")
        whenever(api.inventoryProducts(1)).thenReturn(InventoryResponse(listOf(first), InventoryMeta(lastPage = 2, total = 2)))
        whenever(api.inventoryProducts(2)).thenReturn(InventoryResponse(listOf(first, second)))
        val result = UrbanRepository(api).inventoryProducts()
        assertEquals(listOf("p1", "p2"), result.data.map { it.id })
        assertEquals(2, result.meta.total)
    }
}
