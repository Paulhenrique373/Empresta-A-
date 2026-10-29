package com.example.emprestaa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EquipmentCodeLookupTest {
    private val available = Equipamento("1", "NB-001", "Notebook escolar", "Notebook")
    private val inUse = Equipamento("2", "TB-002", "Tablet escolar", "Tablet", "Em uso")
    private val maintenance = Equipamento("3", "NB-003", "Notebook em reparo", "Notebook", "Manutenção")

    @Test
    fun findsRegisteredCodeIgnoringWhitespaceAndCase() {
        assertEquals(available, EquipmentCodeLookup.find(listOf(available), "  nb-001 "))
    }

    @Test
    fun findsEquipmentByInternalIdForQrLabels() {
        assertEquals(available, EquipmentCodeLookup.find(listOf(available), "1"))
    }

    @Test
    fun unknownAndEmptyCodesDoNotCreateEquipment() {
        val equipment = listOf(available)
        assertNull(EquipmentCodeLookup.find(equipment, "UNKNOWN"))
        assertNull(EquipmentCodeLookup.find(equipment, "  "))
        assertEquals(1, equipment.size)
    }

    @Test
    fun requestAndReturnActionsRespectEquipmentStatus() {
        assertTrue(EquipmentCodeLookup.canRequest(available))
        assertFalse(EquipmentCodeLookup.canRequest(inUse))
        assertFalse(EquipmentCodeLookup.canRequest(maintenance))
        assertTrue(EquipmentCodeLookup.canReturn(inUse))
        assertFalse(EquipmentCodeLookup.canReturn(available))
        assertFalse(EquipmentCodeLookup.canReturn(maintenance))
    }
}
