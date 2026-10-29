package com.example.emprestaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.emprestaa.ui.theme.EmprestaAêTheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val DB_KEY = "empresta-ae:database:v1"
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
data class Equipamento(val id: String, val codigo: String, val nome: String, val tipo: String, val status: String = "Disponível")
@Serializable
data class Emprestimo(val id: String, val professor: String, val sala: String, val codigos: List<String>, val inicio: String, val devolucao: String? = null, val devolvidoPor: String? = null, val devolvidos: List<String> = emptyList())
@Serializable
data class Problema(val id: String, val codigo: String, val descricao: String, val data: String)
@Serializable
data class BancoLocal(val equipamentos: List<Equipamento>, val emprestimos: List<Emprestimo>, val problemas: List<Problema>)

private fun bancoInicial() = BancoLocal(
    equipamentos = listOf(
        Equipamento("e1", "NB-001", "Notebook Dell Latitude", "Notebook"),
        Equipamento("e2", "NB-002", "Notebook Lenovo ThinkPad", "Notebook"),
        Equipamento("e3", "NB-003", "Notebook Acer Aspire", "Notebook"),
        Equipamento("TB-001", "TB-001", "Tablet Samsung Galaxy Tab", "Tablet"),
        Equipamento("TB-002", "TB-002", "Tablet Lenovo Tab", "Tablet"),
        Equipamento("NB-004", "NB-004", "Notebook HP ProBook", "Notebook", "Manutenção")
    ), emprestimos = emptyList(), problemas = emptyList()
)
private fun agora(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EmprestaAppRoot() }
    }
}

@Composable
private fun EmprestaAppRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("empresta-ae-local", 0) }
    var modo by remember { mutableStateOf(prefs.getString("modo_visual", "Aparelho") ?: "Aparelho") }
    var nomeProfessor by remember { mutableStateOf(prefs.getString("nome_professor", "") ?: "") }
    var demonstracaoAtiva by remember { mutableStateOf(false) }
    val dark = modo == "Escuro" || (modo == "Aparelho" && isSystemInDarkTheme())
    EmprestaAêTheme(darkTheme = dark) {
        if (demonstracaoAtiva) {
            EmprestaApp(
                modoVisual = modo,
                nomeProfessor = nomeProfessor,
                onModoVisual = { modo = it; prefs.edit().putString("modo_visual", it).apply() },
                onNomeProfessor = { nomeProfessor = it; prefs.edit().putString("nome_professor", it).apply() },
                onSair = { demonstracaoAtiva = false }
            )
        } else TelaLogin(onContinuarDemonstracao = { demonstracaoAtiva = true })
    }
}

@Composable
private fun TelaLogin(onContinuarDemonstracao: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erroUsuario by remember { mutableStateOf(false) }
    var erroSenha by remember { mutableStateOf(false) }
    var mostrarSenha by remember { mutableStateOf(false) }
    var erroAcesso by remember { mutableStateOf(false) }
    var mostrarRecuperacao by remember { mutableStateOf(false) }
    var mostrarAvisoDemo by remember { mutableStateOf(false) }
    val senhaFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val contexto = LocalContext.current

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            Column(
                Modifier.widthIn(max = 460.dp).fillMaxWidth().align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Default.Devices, contentDescription = "Símbolo Empresta Aê", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(15.dp).size(38.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Empresta Aê", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Controle de equipamentos escolares", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Acesse sua conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Informe os dados fornecidos pela sua escola.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedTextField(
                            value = usuario, onValueChange = { usuario = it; erroUsuario = false; erroAcesso = false },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Usuário ou e-mail") }, placeholder = { Text("Seu usuário escolar") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }, singleLine = true,
                            isError = erroUsuario, supportingText = { if (erroUsuario) Text("Informe seu usuário ou e-mail.") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), keyboardActions = KeyboardActions(onNext = { senhaFocus.requestFocus() })
                        )
                        OutlinedTextField(
                            value = senha, onValueChange = { senha = it; erroSenha = false; erroAcesso = false },
                            modifier = Modifier.fillMaxWidth().focusRequester(senhaFocus), label = { Text("Senha") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }, singleLine = true,
                            visualTransformation = if (mostrarSenha) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = { IconButton(onClick = { mostrarSenha = !mostrarSenha }, modifier = Modifier.size(48.dp)) { Icon(if (mostrarSenha) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (mostrarSenha) "Ocultar senha" else "Mostrar senha") } },
                            isError = erroSenha, supportingText = { if (erroSenha) Text("Informe sua senha.") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, keyboardType = androidx.compose.ui.text.input.KeyboardType.Password),
                            keyboardActions = KeyboardActions(onDone = {
                                erroUsuario = usuario.isBlank(); erroSenha = senha.isBlank()
                                if (!erroUsuario && !erroSenha) { focusManager.clearFocus(); erroAcesso = true }
                            })
                        )
                        TextButton(onClick = { mostrarRecuperacao = true }, modifier = Modifier.align(Alignment.End)) { Text("Esqueci minha senha") }
                        if (erroAcesso) {
                            Text("O acesso da escola ainda não está conectado neste aplicativo. Suas credenciais não foram verificadas. Solicite ajuda à administração da escola.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            erroUsuario = usuario.isBlank(); erroSenha = senha.isBlank(); erroAcesso = false
                            if (!erroUsuario && !erroSenha) { focusManager.clearFocus(); erroAcesso = true }
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Entrar") }
                    }
                }
                Text("Ainda não tem acesso? Solicite suas credenciais à administração da escola.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text("Modo de demonstração", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Explore as funções com dados locais neste aparelho. Esta opção não autentica sua identidade.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { mostrarAvisoDemo = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Text("Continuar sem conta · demonstração") }
                Text("Empresta Aê · Controle de equipamentos escolares\nVersão ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
    if (mostrarRecuperacao) AlertDialog(onDismissRequest = { mostrarRecuperacao = false }, title = { Text("Recuperar acesso") }, text = { Text("A recuperação de senha ainda não está configurada. Entre em contato com a administração da sua escola para receber ajuda.") }, confirmButton = { TextButton(onClick = { mostrarRecuperacao = false }) { Text("Entendi") } })
    if (mostrarAvisoDemo) AlertDialog(onDismissRequest = { mostrarAvisoDemo = false }, title = { Text("Entrar na demonstração?") }, text = { Text("Esta opção não verifica sua identidade. Use somente para conhecer o aplicativo; registros e perfil ficam armazenados localmente neste celular.") }, confirmButton = { TextButton(onClick = { mostrarAvisoDemo = false; onContinuarDemonstracao() }) { Text("Entrar na demonstração") } }, dismissButton = { TextButton(onClick = { mostrarAvisoDemo = false }) { Text("Voltar") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmprestaApp(modoVisual: String, nomeProfessor: String, onModoVisual: (String) -> Unit, onNomeProfessor: (String) -> Unit, onSair: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("empresta-ae-local", 0) }
    var banco by remember { mutableStateOf(runCatching { prefs.getString(DB_KEY, null)?.let { json.decodeFromString<BancoLocal>(it) } ?: bancoInicial() }.getOrElse { bancoInicial() }) }
    fun salvar(novo: BancoLocal) { banco = novo; prefs.edit().putString(DB_KEY, json.encodeToString(novo)).apply() }

    var aba by remember { mutableStateOf("Início") }
    var pesquisa by remember { mutableStateOf("") }
    var filtroEquipamento by remember { mutableStateOf("Todos") }
    var filtroAtividade by remember { mutableStateOf("Todos") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var professor by remember { mutableStateOf(nomeProfessor) }
    LaunchedEffect(nomeProfessor) { professor = nomeProfessor }
    var sala by remember { mutableStateOf("") }
    val selecionados = remember { mutableStateListOf<String>() }
    var mostrarProblema by remember { mutableStateOf(false) }
    var problemaCodigo by remember { mutableStateOf("") }
    var problemaDescricao by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(mensagem) {
        mensagem?.let { snackbarHostState.showSnackbar(it); mensagem = null }
    }
    val exportarBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(json.encodeToString(banco).toByteArray(Charsets.UTF_8)) } ?: error("Não foi possível abrir o arquivo.") }
            .onSuccess { mensagem = "Backup exportado com sucesso." }.onFailure { mensagem = "Não foi possível exportar o backup: ${it.localizedMessage ?: "erro de gravação"}" }
    }
    val importarBackup = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) runCatching {
            val conteudo = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Arquivo vazio ou inacessível.")
            json.decodeFromString<BancoLocal>(conteudo).also { novo ->
                require(novo.equipamentos.isNotEmpty()) { "O backup não possui equipamentos." }
                require(novo.equipamentos.map { it.codigo.lowercase() }.distinct().size == novo.equipamentos.size) { "O backup contém códigos repetidos." }
                val codigos = novo.equipamentos.map { it.codigo }.toSet()
                require(novo.emprestimos.all { em -> em.codigos.all { it in codigos } }) { "Há empréstimos associados a códigos inexistentes." }
            }
        }.onSuccess { salvar(it); mensagem = "Backup restaurado com sucesso." }
            .onFailure { mensagem = "Backup inválido: ${it.localizedMessage ?: "não foi possível ler o arquivo"}" }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, topBar = {
        TopAppBar(title = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.Devices, contentDescription = "Empresta Aê", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(9.dp).size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("Empresta Aê", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                Text(if (nomeProfessor.isBlank()) "Professor · Demonstração local" else "$nomeProfessor · Demonstração local", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f))
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
                "Início" -> Inicio(banco.copy(emprestimos = banco.emprestimos.filter { nomeProfessor.isNotBlank() && it.professor.equals(nomeProfessor, true) }), nomeProfessor, onIr = { aba = it })
                "Pedir" -> Emprestar(banco, professor, { professor = it }, sala, { sala = it }, selecionados, onConfirmar = {
                    val nome = professor.trim(); val destino = sala.trim(); val escolhidos = selecionados.toList()
                    if (nome.isBlank() || destino.isBlank()) mensagem = "Preencha o nome do responsável e a sala."
                    else if (escolhidos.isEmpty()) mensagem = "Selecione pelo menos um equipamento."
                    else if (escolhidos.any { codigo -> banco.equipamentos.none { it.codigo == codigo && it.status == "Disponível" } }) mensagem = "Um dos equipamentos selecionados não está mais disponível. Atualize sua seleção."
                    else {
                        val novos = banco.equipamentos.map { if (it.codigo in escolhidos) it.copy(status = "Em uso") else it }
                        val novo = Emprestimo(UUID.randomUUID().toString(), nome, destino, escolhidos, agora())
                        salvar(banco.copy(equipamentos = novos, emprestimos = listOf(novo) + banco.emprestimos))
                        onNomeProfessor(nome); selecionados.clear(); professor = nome; sala = ""; mensagem = "Pedido registrado com sucesso."
                    }
                }, onProblema = { problemaCodigo = ""; mostrarProblema = true })
                "Equipamentos" -> Inventario(banco, pesquisa, { pesquisa = it }, filtroEquipamento, { filtroEquipamento = it }, onReportar = { codigo -> problemaCodigo = codigo; mostrarProblema = true })
                "Atividade" -> Historico(banco.copy(emprestimos = banco.emprestimos.filter { nomeProfessor.isNotBlank() && it.professor.equals(nomeProfessor, true) }), pesquisa, { pesquisa = it }, filtroAtividade, { filtroAtividade = it }, nomeProfessor, onDevolver = { em, codigo ->
                    val novosDevolvidos = (em.devolvidos + codigo).distinct()
                    val finalizado = em.codigos.all { it in novosDevolvidos }
                    salvar(banco.copy(emprestimos = banco.emprestimos.map { if (it.id == em.id) it.copy(devolucao = if (finalizado) agora() else null, devolvidoPor = nomeProfessor.ifBlank { "Professor" }, devolvidos = novosDevolvidos) else it }, equipamentos = banco.equipamentos.map { if (it.codigo == codigo) it.copy(status = if (banco.problemas.any { p -> p.codigo == codigo }) "Manutenção" else "Disponível") else it })); mensagem = "Devolução de $codigo registrada."
                })
                else -> Mais(banco, nomeProfessor, onNomeProfessor, modoVisual, onModoVisual, onProblema = { problemaCodigo = ""; mostrarProblema = true }, onExportar = { exportarBackup.launch("empresta-ae-backup.json") }, onImportar = { importarBackup.launch("application/json") }, onSair = onSair)
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
        else { val p = Problema(UUID.randomUUID().toString(), codigo, problemaDescricao.trim(), agora()); val emUso = banco.equipamentos.any { it.codigo.equals(codigo, true) && it.status == "Em uso" }; salvar(banco.copy(problemas = listOf(p) + banco.problemas, equipamentos = banco.equipamentos.map { if (it.codigo.equals(codigo, true) && it.status != "Em uso") it.copy(status = "Manutenção") else it })); mostrarProblema = false; problemaCodigo = ""; problemaDescricao = ""; mensagem = if (emUso) "Problema registrado. O equipamento será encaminhado à manutenção após a devolução." else "Problema registrado; equipamento marcado para manutenção." }
    }) { Text("Registrar") } }, dismissButton = { TextButton(onClick = { mostrarProblema = false }) { Text("Cancelar") } })

}

@Composable
private fun Inicio(banco: BancoLocal, nomeProfessor: String, onIr: (String) -> Unit) {
    val emUso = banco.equipamentos.count { it.status == "Em uso" }; val disponiveis = banco.equipamentos.count { it.status == "Disponível" }; val manutencao = banco.equipamentos.count { it.status == "Manutenção" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .12f)), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(22.dp)) }
            Text(if (nomeProfessor.isBlank()) "Olá, professor(a)!" else "Olá, ${nomeProfessor.substringBefore(' ')}!", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Acompanhe os equipamentos da escola e organize o que precisa para sua aula.", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onIr("Pedir") }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Solicitar equipamento") }
        } } }
        item { Text("Equipamentos da escola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResumoCard("Livres", disponiveis.toString(), MaterialTheme.colorScheme.secondary, Icons.Default.CheckCircle, Modifier.weight(1f))
            ResumoCard("Em uso", emUso.toString(), MaterialTheme.colorScheme.primary, Icons.Default.Devices, Modifier.weight(1f))
            ResumoCard("Reparo", manutencao.toString(), MaterialTheme.colorScheme.tertiary, Icons.Default.ReportProblem, Modifier.weight(1f))
        } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Seus empréstimos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = { onIr("Atividade") }) { Text("Ver atividade") }
        } }
        val ativos = banco.emprestimos.filter { it.devolucao == null }
        if (ativos.isEmpty()) item { EmptyCard("Tudo em dia", if (nomeProfessor.isBlank()) "Salve seu nome em Ajustes e solicite equipamentos quando precisar." else "Você não tem equipamento emprestado no momento.", Icons.Default.Inbox) }
        items(ativos.take(4), key = { it.id }) { EmprestimoCard(it, banco.equipamentos, onAbrir = { onIr("Atividade") }) }
        item { Text("Acesso rápido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { onIr("Equipamentos") }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Icon(Icons.Default.Devices, null); Spacer(Modifier.width(7.dp)); Text("Consultar itens") }
            OutlinedButton(onClick = { onIr("Atividade") }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Icon(Icons.AutoMirrored.Filled.Assignment, null); Spacer(Modifier.width(7.dp)); Text("Minhas atividades") }
        } }
    }
}

@Composable
private fun ResumoCard(titulo: String, valor: String, cor: androidx.compose.ui.graphics.Color, icone: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier, border = BorderStroke(1.dp, cor.copy(alpha = .18f)), colors = CardDefaults.cardColors(containerColor = cor.copy(alpha = .10f)), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icone, null, tint = cor, modifier = Modifier.size(18.dp))
                Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun Emprestar(banco: BancoLocal, professor: String, onProfessor: (String) -> Unit, sala: String, onSala: (String) -> Unit, selecionados: MutableList<String>, onConfirmar: () -> Unit, onProblema: () -> Unit) {
    val disponiveis = banco.equipamentos.filter { it.status == "Disponível" }
    var confirmarPedido by remember { mutableStateOf(false) }
    val podeEnviar = professor.isNotBlank() && sala.isNotBlank() && selecionados.isNotEmpty() && selecionados.all { codigo -> disponiveis.any { it.codigo == codigo } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TituloSecao("Pedir equipamento", "Escolha os itens que precisa para sua aula.") }
        item { OutlinedTextField(professor, onProfessor, Modifier.fillMaxWidth(), label = { Text("Seu nome") }, placeholder = { Text("Nome do professor") }, supportingText = { Text("Usado para localizar seus pedidos depois.") }, singleLine = true) }
        item { OutlinedTextField(sala, onSala, Modifier.fillMaxWidth(), label = { Text("Sala onde vai usar") }, placeholder = { Text("Ex.: Sala 12") }, singleLine = true) }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Escolha seus equipamentos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${selecionados.size} selecionado(s) • ${disponiveis.size} livres", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        if (disponiveis.isEmpty()) item { EmptyCard("Nenhum equipamento livre agora", "Confira novamente mais tarde ou informe um problema em Ajustes.") }
        items(disponiveis, key = { it.id }) { eq -> Card(Modifier.fillMaxWidth().clickable { if (eq.codigo in selecionados) selecionados.remove(eq.codigo) else selecionados.add(eq.codigo) }, border = BorderStroke(1.dp, if (eq.codigo in selecionados) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = if (eq.codigo in selecionados) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(eq.codigo in selecionados, { if (it) selecionados.add(eq.codigo) else selecionados.remove(eq.codigo) }); Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(22.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(eq.nome, fontWeight = FontWeight.SemiBold); Text("${eq.tipo}  •  ${eq.codigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("Livre", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
        } }
        if (!podeEnviar) item { Text("Preencha seu nome e a sala e selecione ao menos um equipamento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = { confirmarPedido = true }, Modifier.fillMaxWidth(), enabled = podeEnviar) { Icon(Icons.AutoMirrored.Filled.Assignment, null); Spacer(Modifier.width(8.dp)); Text("Revisar pedido") } }
        item { OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth()) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema em equipamento") } }
    }
    if (confirmarPedido) AlertDialog(
        onDismissRequest = { confirmarPedido = false },
        title = { Text("Confirme seu pedido") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Professor(a): $professor")
            Text("Sala: $sala")
            HorizontalDivider()
            Text("Equipamentos (${selecionados.size})", fontWeight = FontWeight.SemiBold)
            selecionados.forEach { codigo -> Text("• ${disponiveis.firstOrNull { it.codigo == codigo }?.nome ?: codigo} ($codigo)", style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { Button(onClick = { confirmarPedido = false; onConfirmar() }) { Text("Confirmar pedido") } },
        dismissButton = { TextButton(onClick = { confirmarPedido = false }) { Text("Voltar") } }
    )
}

@Composable
private fun Inventario(banco: BancoLocal, pesquisa: String, onPesquisa: (String) -> Unit, filtro: String, onFiltro: (String) -> Unit, onReportar: (String) -> Unit) {
    val lista = banco.equipamentos.filter { eq -> "${eq.nome} ${eq.codigo} ${eq.tipo} ${eq.status}".contains(pesquisa, true) && (filtro == "Todos" || eq.status == filtro) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSecao("Equipamentos", "Consulte o que está livre para uso.") }
        item { OutlinedTextField(pesquisa, onPesquisa, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar por nome ou código") }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("Todos", "Disponível", "Em uso", "Manutenção").forEach { estado ->
            val rótulo = when (estado) { "Disponível" -> "Livres"; "Manutenção" -> "Reparo"; else -> estado }
            FilterChip(selected = filtro == estado, onClick = { onFiltro(estado) }, label = { Text(rótulo, fontSize = 11.sp) }, modifier = Modifier.weight(1f))
        } } }
        item { Text("${lista.size} equipamento(s)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(lista, key = { it.id }) { eq -> Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(22.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(eq.nome, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge); Text("${eq.tipo}  •  ${eq.codigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                StatusPill(eq.status)
            }
            TextButton(onClick = { onReportar(eq.codigo) }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) { Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Informar problema") }
        } } }
        if (lista.isEmpty()) item { EmptyCard("Nenhum equipamento encontrado", "Altere o filtro ou tente outro nome ou código.") }
    }
}

@Composable
private fun Historico(banco: BancoLocal, pesquisa: String, onPesquisa: (String) -> Unit, filtro: String, onFiltro: (String) -> Unit, nomeProfessor: String, onDevolver: (Emprestimo, String) -> Unit) {
    val lista = banco.emprestimos.filter { em ->
        val nomesItens = em.codigos.mapNotNull { codigo -> banco.equipamentos.firstOrNull { it.codigo == codigo }?.nome }.joinToString(" ")
        "${em.sala} ${em.codigos.joinToString()} $nomesItens".contains(pesquisa, true) && when (filtro) { "Em aberto" -> em.devolucao == null; "Concluídos" -> em.devolucao != null; else -> true }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSecao("Minhas atividades", "Acompanhe pedidos, itens emprestados e devoluções.") }
        item { OutlinedTextField(pesquisa, onPesquisa, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar sala ou equipamento") }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Todos", "Em aberto", "Concluídos").forEach { estado -> FilterChip(selected = filtro == estado, onClick = { onFiltro(estado) }, label = { Text(estado) }) } } }
        if (lista.isEmpty()) item { EmptyActivityCard(if (nomeProfessor.isBlank()) "Identifique seu perfil" else "Nenhuma atividade ainda", if (nomeProfessor.isBlank()) "Informe seu nome em Ajustes para localizar seus pedidos." else "Seus pedidos e devoluções aparecerão aqui.") }
        items(lista, key = { it.id }) { em ->
            var codigoParaConfirmar by remember(em.id) { mutableStateOf<String?>(null) }
            Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("Pedido para ${em.sala}", fontWeight = FontWeight.Bold); Text("${em.inicio}  •  ${em.codigos.size} item(ns)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(if (em.devolucao == null) "Em aberto" else "Concluído") }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                em.codigos.forEach { codigo ->
                    val item = banco.equipamentos.firstOrNull { it.codigo == codigo }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) { Text(item?.nome ?: "Equipamento", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium); Text(codigo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        if (codigo in em.devolvidos) StatusPill("Devolvido") else TextButton(onClick = { codigoParaConfirmar = codigo }, enabled = em.devolucao == null) { Text("Devolver") }
                    }
                }
                if (em.devolucao != null) Text("Devolvido em ${em.devolucao} por ${em.devolvidoPor ?: "você"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
            codigoParaConfirmar?.let { codigo -> AlertDialog(onDismissRequest = { codigoParaConfirmar = null }, title = { Text("Confirmar devolução") }, text = { Text("Confirma a devolução de ${banco.equipamentos.firstOrNull { it.codigo == codigo }?.nome ?: codigo} ($codigo)?") }, confirmButton = { Button(onClick = { codigoParaConfirmar = null; onDevolver(em, codigo) }) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = { codigoParaConfirmar = null }) { Text("Voltar") } }) }
        }
        if (banco.problemas.isNotEmpty()) item { Text("Problemas informados (${banco.problemas.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        items(banco.problemas, key = { it.id }) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text("${p.codigo} • ${p.data}", fontWeight = FontWeight.SemiBold); Text(p.descricao) } } }
    }
}

@Composable
private fun Mais(
    banco: BancoLocal,
    nomeProfessor: String,
    onNomeProfessor: (String) -> Unit,
    modoVisual: String,
    onModoVisual: (String) -> Unit,
    onProblema: () -> Unit,
    onExportar: () -> Unit,
    onImportar: () -> Unit,
    onSair: () -> Unit
) {
    var nomeEditado by remember(nomeProfessor) { mutableStateOf(nomeProfessor) }
    var perfilSalvo by remember(nomeProfessor) { mutableStateOf(false) }
    var confirmarRestauracao by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TituloSecao("Ajustes", "Personalize o aplicativo para o seu uso.") }
        item { Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Seu perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Seu nome identifica seus pedidos e atividades.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(nomeEditado, { nomeEditado = it; perfilSalvo = false }, Modifier.fillMaxWidth(), label = { Text("Nome do professor") }, placeholder = { Text("Ex.: Maria Silva") }, singleLine = true)
            Button(onClick = { onNomeProfessor(nomeEditado.trim()); perfilSalvo = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = nomeEditado.isNotBlank()) { Text("Salvar meu nome") }
            if (perfilSalvo) Text("Nome salvo neste aparelho.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        } } }
        item { Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Escolha como prefere visualizar o aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Aparelho" to "Padrão", "Claro" to "Claro", "Escuro" to "Escuro").forEach { (valor, rotulo) ->
                    val icone = when (valor) { "Claro" -> Icons.Default.LightMode; "Escuro" -> Icons.Default.DarkMode; else -> Icons.Default.Settings }
                    FilterChip(selected = modoVisual == valor, onClick = { onModoVisual(valor) }, label = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) { Icon(icone, null, modifier = Modifier.size(16.dp)); Text(rotulo) } }, modifier = Modifier.weight(1f))
                }
            }
            if (modoVisual == "Aparelho") Text("O tema acompanha a configuração do seu celular.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        item { Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ajuda e dados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Relate um defeito ou proteja seus registros.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema") }
            FilledTonalButton(onClick = onExportar, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp)); Text("Salvar cópia dos dados") }
            OutlinedButton(onClick = { confirmarRestauracao = true }, Modifier.fillMaxWidth()) { Text("Restaurar cópia de segurança") }
        } } }
        item { Text("Os dados ficam salvos neste aparelho. Empréstimos e atividades são vinculados ao seu nome.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedButton(onClick = onSair, Modifier.fillMaxWidth()) { Text("Voltar à tela de acesso") } }
        item { Text("${banco.equipamentos.size} equipamentos disponíveis no catálogo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (confirmarRestauracao) AlertDialog(onDismissRequest = { confirmarRestauracao = false }, title = { Text("Restaurar cópia de segurança?") }, text = { Text("A restauração substituirá os dados salvos neste celular. Você poderá escolher o arquivo na próxima etapa.") }, confirmButton = { TextButton(onClick = { confirmarRestauracao = false; onImportar() }) { Text("Continuar") } }, dismissButton = { TextButton(onClick = { confirmarRestauracao = false }) { Text("Cancelar") } })
}

@Composable
private fun EmprestimoCard(em: Emprestimo, equipamentos: List<Equipamento>, onAbrir: () -> Unit) {
    Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text("Sala ${em.sala}", fontWeight = FontWeight.SemiBold); Text("Solicitado em ${em.inicio}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                StatusPill("Em uso")
            }
            em.codigos.forEach { codigo ->
                val item = equipamentos.firstOrNull { it.codigo == codigo }
                Text("${item?.nome ?: "Equipamento"} • $codigo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            TextButton(onClick = onAbrir, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) { Text("Acompanhar devolução") }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val cor = when (status) { "Disponível", "Devolvido", "Concluído" -> MaterialTheme.colorScheme.secondary; "Manutenção", "Em aberto" -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }
    Surface(color = cor.copy(alpha = .13f), shape = RoundedCornerShape(8.dp)) { Text(status, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = cor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TituloSecao(titulo: String, descricao: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun EmptyCard(titulo: String, detalhe: String, icone: ImageVector = Icons.Default.Inbox) {
    Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp)) { Icon(icone, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp).size(24.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(titulo, fontWeight = FontWeight.SemiBold); Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun EmptyActivityCard(titulo: String, detalhe: String) {
    Card(
        Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(20.dp).size(42.dp))
            }
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(detalhe, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
