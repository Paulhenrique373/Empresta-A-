package com.example.emprestaa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLoanRulesTest {
    private val notebook = Equipamento("nb1", "NB-001", "Notebook", "Notebook")
    private val tablet = Equipamento("tb1", "TB-001", "Tablet", "Tablet")
    private val maintenance = Equipamento("nb2", "NB-002", "Notebook em manutenção", "Notebook", "Manutenção")

    private fun database() = BancoLocal(listOf(notebook, tablet, maintenance), emptyList(), emptyList())

    @Test
    fun registersLoanAndMarksEquipmentInUse() {
        val result = LocalLoanRules.register(database(), "  Ana Silva ", " Sala 4 ", listOf("NB-001"), "29/10/2026 10:00", "loan-1")
        assertEquals("Em uso", result.equipamentos.first { it.codigo == "NB-001" }.status)
        assertEquals("Ana Silva", result.emprestimos.single().professor)
        assertEquals("Sala 4", result.emprestimos.single().sala)
    }

    @Test
    fun refusesUnavailableAndMaintenanceEquipment() {
        val inUse = LocalLoanRules.register(database(), "Ana", "4", listOf("NB-001"), "now", "loan-1")
        assertFalse(runCatching { LocalLoanRules.register(inUse, "Bia", "5", listOf("NB-001"), "later", "loan-2") }.isSuccess)
        assertFalse(runCatching { LocalLoanRules.register(database(), "Bia", "5", listOf("NB-002"), "later", "loan-2") }.isSuccess)
    }

    @Test
    fun refusesMissingDetailsAndDuplicateSelectedCodes() {
        val db = database()
        assertFalse(runCatching { LocalLoanRules.register(db, "", "4", listOf("NB-001"), "now") }.isSuccess)
        assertFalse(runCatching { LocalLoanRules.register(db, "Ana", "", listOf("NB-001"), "now") }.isSuccess)
        assertFalse(runCatching { LocalLoanRules.register(db, "Ana", "4", listOf("NB-001", "NB-001"), "now") }.isSuccess)
    }

    @Test
    fun returnsItemAndMakesItAvailable() {
        val loaned = LocalLoanRules.register(database(), "Ana", "4", listOf("NB-001"), "now", "loan-1")
        val returned = LocalLoanRules.returnItem(loaned, "loan-1", "NB-001", "Ana", "later")
        assertEquals("Disponível", returned.equipamentos.first { it.codigo == "NB-001" }.status)
        assertEquals("later", returned.emprestimos.single().devolucao)
        assertTrue("NB-001" in returned.emprestimos.single().devolvidos)
    }

    @Test
    fun preventsReturnWithoutActiveLoanAndDuplicateReturn() {
        val db = database()
        assertFalse(runCatching { LocalLoanRules.returnItem(db, "missing", "NB-001", "Ana", "now") }.isSuccess)
        val loaned = LocalLoanRules.register(db, "Ana", "4", listOf("NB-001"), "now", "loan-1")
        val returned = LocalLoanRules.returnItem(loaned, "loan-1", "NB-001", "Ana", "later")
        assertFalse(runCatching { LocalLoanRules.returnItem(returned, "loan-1", "NB-001", "Ana", "again") }.isSuccess)
    }

    @Test
    fun partialReturnKeepsLoanOpenAndReturnsEachDeviceIndependently() {
        val loaned = LocalLoanRules.register(database(), "Ana", "4", listOf("NB-001", "TB-001"), "now", "loan-1")
        val partial = LocalLoanRules.returnItem(loaned, "loan-1", "NB-001", "Ana", "later")
        assertEquals(null, partial.emprestimos.single().devolucao)
        assertEquals("Disponível", partial.equipamentos.first { it.codigo == "NB-001" }.status)
        assertEquals("Em uso", partial.equipamentos.first { it.codigo == "TB-001" }.status)
        val completed = LocalLoanRules.returnItem(partial, "loan-1", "TB-001", "Ana", "end")
        assertEquals("end", completed.emprestimos.single().devolucao)
    }
}
