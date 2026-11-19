package com.example.emprestaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import com.example.emprestaa.ui.theme.EmprestaAêTheme
import com.example.emprestaa.data.AuthViewModel
import com.example.emprestaa.data.AuthState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.emprestaa.data.EquipmentViewModel
import com.example.emprestaa.data.DefectViewModel
import com.example.emprestaa.data.DefeitoInsert
import com.example.emprestaa.data.LoanViewModel
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
data class Equipamento(val id: String, val codigo: String, val nome: String, val tipo: String, val status: String = "Disponível")
@Serializable
data class Emprestimo(val id: String, val professor: String, val sala: String, val codigos: List<String>, val inicio: String, val devolucao: String? = null, val devolvidoPor: String? = null, val devolvidos: List<String> = emptyList())
@Serializable
data class Problema(val id: String, val codigo: String, val descricao: String, val data: String)
@Serializable
data class BancoLocal(val equipamentos: List<Equipamento>, val emprestimos: List<Emprestimo>, val problemas: List<Problema>)

private fun agora(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EmprestaAppRoot() }
    }
}

@Composable
private fun EmprestaAppRoot(authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("empresta-ae-local", 0) }
    var modo by remember { mutableStateOf(prefs.getString("modo_visual", "Aparelho") ?: "Aparelho") }
    var nomeProfessor by remember { mutableStateOf(prefs.getString("nome_professor", "") ?: "") }
    var demonstracaoAtiva by remember { mutableStateOf(prefs.getString("supabase_user_id", null) != null) }
    val dark = modo == "Escuro" || (modo == "Aparelho" && isSystemInDarkTheme())
    EmprestaAêTheme(darkTheme = dark) {
        if (demonstracaoAtiva) {
            EmprestaApp(
                modoVisual = modo,
                nomeProfessor = nomeProfessor,
                onModoVisual = { modo = it; prefs.edit().putString("modo_visual", it).apply() },
                onSair = {
                    authViewModel.logout()
                    prefs.edit()
                        .remove("supabase_user_id")
                        .remove("nome_professor")
                        .apply()
                    nomeProfessor = ""
                    demonstracaoAtiva = false
                }
            )
        } else TelaLogin(authViewModel, onContinuarDemonstracao = { user ->
            prefs.edit().putString("supabase_user_id", user.id).putString("nome_professor", user.nome).apply()
            nomeProfessor = user.nome; demonstracaoAtiva = true
        })
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmprestaApp(modoVisual: String, nomeProfessor: String, onModoVisual: (String) -> Unit, onSair: () -> Unit, equipmentViewModel: EquipmentViewModel = viewModel()) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("empresta-ae-local", 0) }
    var banco by remember { mutableStateOf(BancoLocal(emptyList(), emptyList(), emptyList())) }
    val equipamentosState by equipmentViewModel.state.collectAsState()
    val userId = prefs.getString("supabase_user_id", null).orEmpty()
    val defectViewModel: DefectViewModel = viewModel(key = "defeitos-$userId")
    val defectState by defectViewModel.state.collectAsState()
    val loanViewModel: LoanViewModel = viewModel(key = "emprestimos-$userId")
    val loanState by loanViewModel.state.collectAsState()
    LaunchedEffect(userId) { equipmentViewModel.carregar(); defectViewModel.carregar() }
    LaunchedEffect(userId) { if (userId.isNotBlank()) loanViewModel.carregar(userId) }
    LaunchedEffect(loanState) {
        if (!loanState.loading && loanState.error == null && userId.isNotBlank()) {
            val mapped = loanState.rows.map { row -> Emprestimo(row.id.toString(), row.usuarioId, "", listOf(row.equipamentoId.toString()), row.retirada, row.devolucao, devolvidos = if (row.status == "Devolvido") listOf(row.equipamentoId.toString()) else emptyList()) }
            banco = banco.copy(emprestimos = mapped)
        }
    }
    LaunchedEffect(equipamentosState) {
        val remote = equipamentosState.items
        if (!equipamentosState.loading && equipamentosState.error == null) banco = banco.copy(equipamentos = remote.map { Equipamento(it.id.toString(), it.etiqueta, "${it.marca} ${it.modelo}", it.tipo, it.status) })
    }
    LaunchedEffect(defectState.rows, equipamentosState.items) {
        if (defectState.error == null && !defectState.loading) banco = banco.copy(problemas = defectState.rows.map { d ->
            Problema(d.id.toString(), equipamentosState.items.firstOrNull { it.id == d.equipamentoId }?.etiqueta ?: d.equipamentoId.toString(), "${d.tipo}: ${d.descricao} (${d.status})", d.data)
        })
    }

    var aba by remember { mutableStateOf("Início") }
    var pesquisa by remember { mutableStateOf("") }
    var filtroEquipamento by remember { mutableStateOf("Todos") }
    var filtroAtividade by remember { mutableStateOf("Todos") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    val selecionados = remember { mutableStateListOf<String>() }
    var mostrarProblema by remember { mutableStateOf(false) }
    var scannerVisible by remember { mutableStateOf(false) }
    var codigoEscaneado by remember { mutableStateOf<String?>(null) }
    var equipamentoEtiqueta by remember { mutableStateOf<Equipamento?>(null) }
    var problemaCodigo by remember { mutableStateOf("") }
    var problemaDescricao by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensagem) {
        mensagem?.let { snackbarHostState.showSnackbar(it); mensagem = null }
    }
    if (scannerVisible) {
        BarcodeScannerScreen(onClose = { scannerVisible = false }, onCodeRead = { codigo -> codigoEscaneado = codigo; scannerVisible = false })
    } else Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = {
        TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Devices, contentDescription = "Empresta Aê", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(9.dp).size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Empresta Aê", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text(if (nomeProfessor.isBlank()) "Professor" else nomeProfessor, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f))
            }
        } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary))
    }, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp, windowInsets = NavigationBarDefaults.windowInsets) {
            listOf("Início" to "Início", "Pedir" to "Pedir", "Itens" to "Equipamentos", "Atividade" to "Atividade", "Ajustes" to "Ajustes").forEach { (rótulo, destino) ->
                val icon = when (destino) { "Início" -> Icons.Default.Home; "Pedir" -> Icons.AutoMirrored.Filled.Assignment; "Equipamentos" -> Icons.Default.Devices; "Atividade" -> Icons.Default.Search; else -> Icons.Default.MoreHoriz }
                NavigationBarItem(selected = aba == destino, onClick = { aba = destino; pesquisa = "" }, icon = { Icon(icon, contentDescription = rótulo) }, label = { Text(rótulo, fontSize = 11.sp, maxLines = 1) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
            }
        }
    }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.widthIn(max = 760.dp).fillMaxHeight().align(Alignment.TopCenter)) {
                when (aba) {
                    "Início" -> Inicio(banco, nomeProfessor, onIr = { aba = it })
                    "Pedir" -> Emprestar(banco, selecionados, onConfirmar = {
                        val escolhidos = selecionados.toList()
                        val equipment = equipamentosState.items.firstOrNull { it.etiqueta == escolhidos.firstOrNull() }
                        if (userId.isBlank()) mensagem = "Entre novamente para realizar empréstimos."
                        else if (escolhidos.size != 1) mensagem = "Selecione um equipamento por operação para garantir consistência."
                        else if (equipment == null) mensagem = "Equipamento não localizado no catálogo do Supabase."
                        else loanViewModel.emprestar(equipment.id, userId)
                    }, onProblema = { problemaCodigo = ""; mostrarProblema = true }, onEscanear = { scannerVisible = true })
                    "Equipamentos" -> Column {
                        when { equipamentosState.loading -> LinearProgressIndicator(Modifier.fillMaxWidth()); equipamentosState.error != null -> Text("Falha ao carregar Supabase: ${equipamentosState.error}", color = MaterialTheme.colorScheme.error); else -> Unit }
                        Inventario(banco, pesquisa, { pesquisa = it }, filtroEquipamento, { filtroEquipamento = it }, onReportar = { codigo -> problemaCodigo = codigo; mostrarProblema = true }, onEscanear = { scannerVisible = true }, onEtiqueta = { equipamentoEtiqueta = it })
                    }
                    "Atividade" -> Column {
                        if (loanState.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        loanState.error?.let { Text("Falha ao consultar histórico: $it", color = MaterialTheme.colorScheme.error) }
                        Historico(banco, pesquisa, { pesquisa = it }, filtroAtividade, { filtroAtividade = it }, nomeProfessor, onDevolver = { em, _ ->
                            val id = em.id.toLongOrNull()
                            if (id == null || userId.isBlank()) mensagem = "Empréstimo ou sessão inválidos."
                            else loanViewModel.devolver(id, userId)
                        })
                    }
                    else -> Mais(banco, nomeProfessor, modoVisual, onModoVisual, onProblema = { problemaCodigo = ""; mostrarProblema = true }, onSair = onSair)
                }
            }
        }
    }

    if (mostrarProblema) AlertDialog(onDismissRequest = { mostrarProblema = false }, title = { Text("Registrar problema") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(problemaCodigo, { problemaCodigo = it.uppercase() }, label = { Text("Código do equipamento") }, singleLine = true)
        OutlinedTextField(problemaDescricao, { problemaDescricao = it }, label = { Text("Descreva o problema") }, minLines = 2)
    } }, confirmButton = { TextButton(onClick = {
        val codigo = problemaCodigo.trim(); val existe = banco.equipamentos.any { it.codigo.equals(codigo, true) }
        if (!existe || problemaDescricao.isBlank()) mensagem = "Informe um equipamento cadastrado e descreva o problema."
        else {
            val equipamento = banco.equipamentos.first { it.codigo.equals(codigo, true) }
            val userId = prefs.getString("supabase_user_id", null)
            if (userId == null || equipamento.id.toLongOrNull() == null) mensagem = "A sessão ou identificação do equipamento não está disponível."
            else defectViewModel.registrar(DefeitoInsert(equipamento.id.toLong(), userId, "Outros", problemaDescricao.trim())) {
                mostrarProblema = false; problemaCodigo = ""; problemaDescricao = ""; mensagem = "Defeito registrado no Supabase."
            }
        }
    }) { Text("Registrar") } }, dismissButton = { TextButton(onClick = { mostrarProblema = false }) { Text("Cancelar") } })

    equipamentoEtiqueta?.let { equipamento ->
        EquipmentLabelDialog(
            equipment = equipamento,
            onDismiss = { equipamentoEtiqueta = null },
            onShare = { label ->
                runCatching { shareEquipmentLabel(context, equipamento, label) }
                    .onSuccess { equipamentoEtiqueta = null }
                    .onFailure { mensagem = it.localizedMessage ?: "Não foi possível compartilhar a etiqueta." }
            },
            onPrint = { label ->
                runCatching { printEquipmentLabel(context, equipamento, label) }
                    .onSuccess { equipamentoEtiqueta = null }
                    .onFailure { mensagem = it.localizedMessage ?: "Não foi possível abrir a impressão." }
            }
        )
    }

    codigoEscaneado?.let { codigo ->
        val equipamento = EquipmentCodeLookup.find(banco.equipamentos, codigo)
        val historico = equipamento?.let { eq -> banco.emprestimos.filter { eq.codigo in it.codigos }.take(3) }.orEmpty()
        AlertDialog(
            onDismissRequest = { codigoEscaneado = null },
            title = { Text(if (equipamento == null) "Equipamento não encontrado" else "Equipamento identificado") },
            text = {
                if (equipamento == null) Text("Equipamento não encontrado. Verifique se o código está cadastrado no sistema.\n\nCódigo lido: $codigo")
                else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(equipamento.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Patrimônio / código: ${equipamento.codigo}")
                    Text("Categoria: ${equipamento.tipo}")
                    Text("Situação: ${equipamento.status}")
                    if (historico.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Histórico recente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        historico.forEach { Text("${it.inicio} · ${if (it.devolucao == null) "Em aberto" else "Concluído"}", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            },
            confirmButton = {
                if (equipamento != null) TextButton(onClick = { pesquisa = equipamento.codigo; aba = "Equipamentos"; codigoEscaneado = null }) { Text("Ver no catálogo") }
            },
            dismissButton = {
                Row {
                    if (equipamento != null && EquipmentCodeLookup.canRequest(equipamento)) TextButton(onClick = {
                        if (equipamento.codigo !in selecionados) selecionados.add(equipamento.codigo)
                        aba = "Pedir"
                        codigoEscaneado = null
                    }) { Text(if (equipamento.codigo in selecionados) "Já selecionado" else "Preparar pedido") }
                    if (equipamento != null && EquipmentCodeLookup.canReturn(equipamento)) TextButton(onClick = {
                        pesquisa = equipamento.codigo
                        aba = "Atividade"
                        codigoEscaneado = null
                    }) { Text("Ver devolução") }
                    TextButton(onClick = { codigoEscaneado = null }) { Text("Fechar") }
                }
            }
        )
    }

}












