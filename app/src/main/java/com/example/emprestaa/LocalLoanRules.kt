package com.example.emprestaa

import java.util.UUID

/** Local-only business rules shared by the teacher flows and unit tests. */
internal object LocalLoanRules {
    fun register(
        database: BancoLocal,
        teacher: String,
        room: String,
        selectedCodes: List<String>,
        timestamp: String,
        loanId: String = UUID.randomUUID().toString()
    ): BancoLocal {
        val responsible = teacher.trim()
        val destination = room.trim()
        val codes = selectedCodes.map(String::trim).filter(String::isNotEmpty).distinct()
        require(responsible.isNotEmpty()) { "Informe o nome do professor." }
        require(destination.isNotEmpty()) { "Informe a sala." }
        require(codes.isNotEmpty()) { "Selecione ao menos um equipamento." }
        require(codes.size == selectedCodes.size) { "A seleção contém códigos vazios ou repetidos." }

        val itemsByCode = database.equipamentos.associateBy { it.codigo.lowercase() }
        require(codes.all { itemsByCode[it.lowercase()]?.status == "Disponível" }) {
            "Um dos equipamentos não está disponível. Atualize a seleção e tente novamente."
        }
        require(codes.none { code -> database.emprestimos.any { loan ->
            loan.devolucao == null && code in loan.codigos && code !in loan.devolvidos
        } }) { "Já existe um empréstimo ativo para um dos equipamentos." }
        require(database.emprestimos.none { it.id == loanId }) { "Não foi possível gerar um identificador único para o pedido." }

        val loan = Emprestimo(loanId, responsible, destination, codes, timestamp)
        return database.copy(
            equipamentos = database.equipamentos.map { if (it.codigo in codes) it.copy(status = "Em uso") else it },
            emprestimos = listOf(loan) + database.emprestimos
        )
    }

    fun returnItem(
        database: BancoLocal,
        loanId: String,
        code: String,
        responsible: String,
        timestamp: String
    ): BancoLocal {
        val loan = database.emprestimos.firstOrNull { it.id == loanId }
            ?: error("Empréstimo não encontrado.")
        require(code in loan.codigos) { "Este equipamento não pertence ao empréstimo selecionado." }
        require(loan.devolucao == null && code !in loan.devolvidos) { "Este equipamento já foi devolvido." }
        val equipment = database.equipamentos.firstOrNull { it.codigo == code }
            ?: error("Equipamento não encontrado no catálogo.")
        require(EquipmentCodeLookup.canReturn(equipment)) { "O equipamento não possui empréstimo ativo." }

        val returnedCodes = (loan.devolvidos + code).distinct()
        val completed = loan.codigos.all { it in returnedCodes }
        val hasOpenMaintenance = database.problemas.any { it.codigo == code }
        return database.copy(
            emprestimos = database.emprestimos.map {
                if (it.id == loanId) it.copy(
                    devolucao = if (completed) timestamp else null,
                    devolvidoPor = responsible.trim().ifEmpty { "Professor" },
                    devolvidos = returnedCodes
                ) else it
            },
            equipamentos = database.equipamentos.map {
                if (it.codigo == code) it.copy(status = if (hasOpenMaintenance) "Manutenção" else "Disponível") else it
            }
        )
    }
}
