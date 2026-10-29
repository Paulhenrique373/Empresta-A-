package com.example.emprestaa

/** Resolves scanner output against the equipment records already stored by the app. */
internal object EquipmentCodeLookup {
    fun find(equipments: List<Equipamento>, scannedValue: String): Equipamento? {
        val code = scannedValue.trim()
        if (code.isEmpty()) return null
        return equipments.firstOrNull {
            it.codigo.trim().equals(code, ignoreCase = true) || it.id.trim().equals(code, ignoreCase = true)
        }
    }

    fun canRequest(equipment: Equipamento): Boolean = equipment.status == "Disponível"

    fun canReturn(equipment: Equipamento): Boolean = equipment.status == "Em uso"
}
