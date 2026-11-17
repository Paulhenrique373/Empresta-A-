package com.example.emprestaa

import org.junit.Assert.assertEquals
import org.junit.Test

class EquipmentLabelGeneratorTest {
    @Test
    fun qrLabelUsesStableInternalEquipmentId() {
        val equipment = Equipamento("internal-42", "NOTE-042", "Notebook", "Notebook")
        assertEquals("internal-42", EquipmentLabelGenerator.valueFor(equipment, LabelFormat.QR))
        assertEquals("internal-42", EquipmentLabelGenerator.valueFor(equipment, LabelFormat.QR))
    }

    @Test
    fun code128LabelUsesEquipmentCode() {
        val equipment = Equipamento("internal-42", "NOTE-042", "Notebook", "Notebook")
        assertEquals("NOTE-042", EquipmentLabelGenerator.valueFor(equipment, LabelFormat.CODE_128))
    }
}
