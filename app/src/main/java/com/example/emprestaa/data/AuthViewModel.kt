package com.example.emprestaa.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object SignedOut : AuthState
    data object Loading : AuthState
    data class SignedIn(val user: SessaoUsuario) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    private val _state = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val state = _state.asStateFlow()
    fun login(email: String, password: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        runCatching { repository.entrar(email.trim(), password) }
            .onSuccess { _state.value = AuthState.SignedIn(it) }
            .onFailure { _state.value = AuthState.Error(it.message ?: "Falha ao entrar.") }
    }
    fun logout() = viewModelScope.launch {
        runCatching { repository.sair() } // sem internet, o app ainda sai
        _state.value = AuthState.SignedOut
    }
    fun reset(email: String) = viewModelScope.launch {
        _state.value = AuthState.Loading
        runCatching { repository.recuperar(email.trim()) }
            .onSuccess { _state.value = AuthState.Error("Se o e-mail estiver cadastrado, você receberá as instruções de recuperação.") }
            .onFailure { _state.value = AuthState.Error(it.message ?: "Não foi possível enviar a recuperação.") }
    }
}

data class EquipmentUiState(val loading: Boolean = false, val items: List<EquipamentoRow> = emptyList(), val error: String? = null)
class EquipmentViewModel(private val repository: EquipamentoRepository = EquipamentoRepository()) : ViewModel() {
    private val _state = MutableStateFlow(EquipmentUiState())
    val state = _state.asStateFlow()
    fun carregar() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.listar() }.onSuccess { _state.value = EquipmentUiState(items = it) }
            .onFailure { _state.value = _state.value.copy(loading = false, error = it.message ?: "Falha de conexão") }
    }
}

data class DefectUiState(val loading: Boolean = false, val error: String? = null, val success: String? = null, val rows: List<DefeitoRow> = emptyList())
class DefectViewModel(private val repository: DefeitoRepository = DefeitoRepository()) : ViewModel() {
    private val _state = MutableStateFlow(DefectUiState())
    val state = _state.asStateFlow()
    fun carregar() = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.listar() }.onSuccess { _state.value = DefectUiState(rows = it) }
            .onFailure { _state.value = DefectUiState(error = it.message ?: "Falha ao consultar defeitos") }
    }
    fun atualizarStatus(id: Long, status: String) = viewModelScope.launch {
        val anterior = _state.value
        _state.value = anterior.copy(loading = true, error = null)
        runCatching { repository.atualizarStatus(id, status); repository.listar() }
            .onSuccess { _state.value = DefectUiState(rows = it) }
            .onFailure { _state.value = anterior.copy(loading = false, error = it.message ?: "Falha ao atualizar defeito") }
    }
    fun registrar(row: DefeitoInsert, onDone: () -> Unit) = viewModelScope.launch {
        _state.value = DefectUiState(loading = true)
        runCatching { repository.registrar(row) }.onSuccess {
            carregar(); _state.value = _state.value.copy(success = "Defeito registrado no sistema."); onDone()
        }.onFailure { _state.value = DefectUiState(error = it.message ?: "Falha ao registrar defeito.") }
    }
}

data class LoanUiState(val loading: Boolean = false, val rows: List<EmprestimoRow> = emptyList(), val error: String? = null)
class LoanViewModel(private val repository: EmprestimoRepository = EmprestimoRepository()) : ViewModel() {
    private val _state = MutableStateFlow(LoanUiState())
    val state = _state.asStateFlow()
    fun carregar(uid: String, gestao: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { if (gestao) repository.listarGestao() else repository.historico(uid) }
            .onSuccess { _state.value = LoanUiState(rows = it) }
            .onFailure { _state.value = LoanUiState(error = it.message ?: "Falha ao consultar empréstimos") }
    }
    fun emprestar(equipmentId: Long, uid: String, gestao: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.retirar(equipmentId); if (gestao) repository.listarGestao() else repository.historico(uid) }
            .onSuccess { _state.value = LoanUiState(rows = it) }
            .onFailure { _state.value = LoanUiState(rows = _state.value.rows, error = it.message ?: "Falha ao realizar empréstimo") }
    }
    fun devolver(id: Long, uid: String, gestao: Boolean = false) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        runCatching { repository.devolver(id); if (gestao) repository.listarGestao() else repository.historico(uid) }
            .onSuccess { _state.value = LoanUiState(rows = it) }
            .onFailure { _state.value = LoanUiState(rows = _state.value.rows, error = it.message ?: "Falha ao devolver equipamento") }
    }
}