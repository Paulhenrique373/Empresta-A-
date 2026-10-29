package com.example.emprestaa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val dark = modo == "Escuro" || (modo == "Aparelho" && isSystemInDarkTheme())
    EmprestaAêTheme(darkTheme = dark) {
        EmprestaApp(
            modoVisual = modo,
            nomeProfessor = nomeProfessor,
            onModoVisual = { modo = it; prefs.edit().putString("modo_visual", it).apply() },
            onNomeProfessor = { nomeProfessor = it; prefs.edit().putString("nome_professor", it).apply() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmprestaApp(modoVisual: String, nomeProfessor: String, onModoVisual: (String) -> Unit, onNomeProfessor: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("empresta-ae-local", 0) }
    var banco by remember { mutableStateOf(runCatching { prefs.getString(DB_KEY, null)?.let { json.decodeFromString<BancoLocal>(it) } ?: bancoInicial() }.getOrElse { bancoInicial() }) }
    fun salvar(novo: BancoLocal) { banco = novo; prefs.edit().putString(DB_KEY, json.encodeToString(novo)).apply() }

    var aba by remember { mutableStateOf("Início") }
    var pesquisa by remember { mutableStateOf("") }
    var filtroEquipamento by remember { mutableStateOf("Todos") }
    var mensagem by remember { mutableStateOf<String?>(null) }
    var professor by remember { mutableStateOf(nomeProfessor) }
    LaunchedEffect(nomeProfessor) { professor = nomeProfessor }
    var sala by remember { mutableStateOf("") }
    val selecionados = remember { mutableStateListOf<String>() }
    var mostrarProblema by remember { mutableStateOf(false) }
    var problemaCodigo by remember { mutableStateOf("") }
    var problemaDescricao by remember { mutableStateOf("") }
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

    Scaffold(topBar = {
        CenterAlignedTopAppBar(title = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Empresta Aê", fontWeight = FontWeight.Bold)
                        Text("Área do professor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } })
    }, bottomBar = {
        NavigationBar {
            listOf("Início" to "Início", "Pedir" to "Pedir", "Itens" to "Equipamentos", "Atividade" to "Atividade", "Ajustes" to "Ajustes").forEach { (rótulo, destino) ->
                val icon = when (destino) { "Início" -> Icons.Default.Home; "Pedir" -> Icons.Default.Assignment; "Equipamentos" -> Icons.Default.Devices; "Atividade" -> Icons.Default.Search; else -> Icons.Default.MoreHoriz }
                NavigationBarItem(selected = aba == destino, onClick = { aba = destino; pesquisa = "" }, icon = { Icon(icon, null) }, label = { Text(rótulo, fontSize = 10.sp) })
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (aba) {
                "Início" -> Inicio(banco.copy(emprestimos = banco.emprestimos.filter { nomeProfessor.isNotBlank() && it.professor.equals(nomeProfessor, true) }), nomeProfessor, onIr = { aba = it })
                "Pedir" -> Emprestar(banco, professor, { professor = it }, sala, { sala = it }, selecionados, onConfirmar = {
                    val nome = professor.trim(); val destino = sala.trim(); val escolhidos = selecionados.toList()
                    if (nome.isBlank() || destino.isBlank()) mensagem = "Preencha o nome do responsável e a sala."
                    else if (escolhidos.isEmpty()) mensagem = "Selecione pelo menos um equipamento."
                    else {
                        val novos = banco.equipamentos.map { if (it.codigo in escolhidos) it.copy(status = "Em uso") else it }
                        val novo = Emprestimo(UUID.randomUUID().toString(), nome, destino, escolhidos, agora())
                        salvar(banco.copy(equipamentos = novos, emprestimos = listOf(novo) + banco.emprestimos))
                        onNomeProfessor(nome); selecionados.clear(); professor = nome; sala = ""; mensagem = "Pedido registrado com sucesso."
                    }
                }, onProblema = { problemaCodigo = ""; mostrarProblema = true })
                "Equipamentos" -> Inventario(banco, pesquisa, { pesquisa = it }, filtroEquipamento, { filtroEquipamento = it }, onReportar = { codigo -> problemaCodigo = codigo; mostrarProblema = true })
                "Atividade" -> Historico(banco.copy(emprestimos = banco.emprestimos.filter { nomeProfessor.isNotBlank() && it.professor.equals(nomeProfessor, true) }), pesquisa, { pesquisa = it }, nomeProfessor, onDevolver = { em, codigo ->
                    val novosDevolvidos = (em.devolvidos + codigo).distinct()
                    val finalizado = em.codigos.all { it in novosDevolvidos }
                    salvar(banco.copy(emprestimos = banco.emprestimos.map { if (it.id == em.id) it.copy(devolucao = if (finalizado) agora() else null, devolvidoPor = nomeProfessor.ifBlank { "Professor" }, devolvidos = novosDevolvidos) else it }, equipamentos = banco.equipamentos.map { if (it.codigo == codigo) it.copy(status = if (banco.problemas.any { p -> p.codigo == codigo }) "Manutenção" else "Disponível") else it })); mensagem = "Devolução de $codigo registrada."
                })
                else -> Mais(banco, nomeProfessor, onNomeProfessor, modoVisual, onModoVisual, onProblema = { problemaCodigo = ""; mostrarProblema = true }, onExportar = { exportarBackup.launch("empresta-ae-backup.json") }, onImportar = { importarBackup.launch("application/json") })
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

    mensagem?.let { msg -> AlertDialog(onDismissRequest = { mensagem = null }, text = { Text(msg) }, confirmButton = { TextButton(onClick = { mensagem = null }) { Text("OK") } }) }
}

@Composable
private fun Inicio(banco: BancoLocal, nomeProfessor: String, onIr: (String) -> Unit) {
    val emUso = banco.equipamentos.count { it.status == "Em uso" }; val disponiveis = banco.equipamentos.count { it.status == "Disponível" }; val manutencao = banco.equipamentos.count { it.status == "Manutenção" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), shape = RoundedCornerShape(24.dp)) { Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (nomeProfessor.isBlank()) "Olá, professor(a)!" else "Olá, ${nomeProfessor.substringBefore(' ')}!", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Consulte equipamentos e acompanhe seus empréstimos.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .9f))
            Button(onClick = { onIr("Pedir") }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("Pedir equipamento") }
        } } }
        item { Text("Equipamentos da escola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { ResumoCard("Disponíveis", disponiveis.toString(), MaterialTheme.colorScheme.primary, Modifier.weight(1f)); ResumoCard("Em uso", emUso.toString(), MaterialTheme.colorScheme.secondary, Modifier.weight(1f)); ResumoCard("Manutenção", manutencao.toString(), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f)) } }
        item { Text("Seus empréstimos ativos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        val ativos = banco.emprestimos.filter { it.devolucao == null }
        if (ativos.isEmpty()) item { EmptyCard("Nenhum empréstimo ativo", if (nomeProfessor.isBlank()) "Informe seu nome em Ajustes para acompanhar seus pedidos." else "Seus próximos pedidos aparecerão aqui.") }
        items(ativos.take(4), key = { it.id }) { EmprestimoCard(it) }
        item { Text("Acesso rápido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { onIr("Equipamentos") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Devices, null); Spacer(Modifier.width(5.dp)); Text("Ver equipamentos") }; OutlinedButton(onClick = { onIr("Atividade") }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Assignment, null); Spacer(Modifier.width(5.dp)); Text("Minhas atividades") } } }
    }
}

@Composable
private fun ResumoCard(titulo: String, valor: String, cor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = cor); Text(titulo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 14.sp) } }
}

@Composable
private fun Emprestar(banco: BancoLocal, professor: String, onProfessor: (String) -> Unit, sala: String, onSala: (String) -> Unit, selecionados: MutableList<String>, onConfirmar: () -> Unit, onProblema: () -> Unit) {
    val disponiveis = banco.equipamentos.filter { it.status == "Disponível" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TituloSecao("Pedir equipamento", "Escolha os itens que precisa para sua aula.") }
        item { OutlinedTextField(professor, onProfessor, Modifier.fillMaxWidth(), label = { Text("Seu nome") }, placeholder = { Text("Nome do professor") }, singleLine = true) }
        item { OutlinedTextField(sala, onSala, Modifier.fillMaxWidth(), label = { Text("Sala onde vai usar") }, placeholder = { Text("Ex.: Sala 12") }, singleLine = true) }
        item { Text("Toque para selecionar (${disponiveis.size} livres)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
        if (disponiveis.isEmpty()) item { EmptyCard("Nenhum equipamento livre agora", "Confira novamente mais tarde ou informe um problema em Ajustes.") }
        items(disponiveis, key = { it.id }) { eq -> Card(Modifier.fillMaxWidth().clickable { if (eq.codigo in selecionados) selecionados.remove(eq.codigo) else selecionados.add(eq.codigo) }, colors = CardDefaults.cardColors(containerColor = if (eq.codigo in selecionados) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(eq.codigo in selecionados, { if (it) selecionados.add(eq.codigo) else selecionados.remove(eq.codigo) }); Column(Modifier.weight(1f)) { Text(eq.nome, fontWeight = FontWeight.SemiBold); Text("${eq.tipo} • ${eq.codigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        } }
        item { Button(onClick = onConfirmar, Modifier.fillMaxWidth(), enabled = disponiveis.isNotEmpty()) { Text("Enviar pedido (${selecionados.size})") } }
        item { OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth()) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema em equipamento") } }
    }
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
        items(lista, key = { it.id }) { eq -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(eq.nome, fontWeight = FontWeight.SemiBold); Text("${eq.tipo}  •  ${eq.codigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(eq.status) }
            TextButton(onClick = { onReportar(eq.codigo) }, contentPadding = PaddingValues(0.dp)) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(5.dp)); Text("Informar um problema") }
        } } }
        if (lista.isEmpty()) item { EmptyCard("Nenhum equipamento encontrado", "Altere o filtro ou tente outro nome ou código.") }
    }
}

@Composable
private fun Historico(banco: BancoLocal, pesquisa: String, onPesquisa: (String) -> Unit, nomeProfessor: String, onDevolver: (Emprestimo, String) -> Unit) {
    val lista = banco.emprestimos.filter { "${it.professor} ${it.sala} ${it.codigos.joinToString()}".contains(pesquisa, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSecao("Minhas atividades", "Acompanhe seus pedidos e registre devoluções.") }
        item { OutlinedTextField(pesquisa, onPesquisa, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar sala ou equipamento") }, singleLine = true) }
        if (lista.isEmpty()) item { EmptyCard(if (nomeProfessor.isBlank()) "Identifique seu perfil" else "Nenhuma atividade ainda", if (nomeProfessor.isBlank()) "Informe seu nome em Ajustes para ver seus empréstimos." else "Seus pedidos e devoluções aparecerão aqui.") }
        items(lista, key = { it.id }) { em -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(em.professor, fontWeight = FontWeight.Bold); Text("${em.sala} • ${em.inicio}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(if (em.devolucao == null) "Em uso" else "Devolvido") }
            em.codigos.forEach { codigo -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(codigo, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                if (codigo in em.devolvidos) StatusPill("Devolvido") else TextButton(onClick = { onDevolver(em, codigo) }, enabled = em.devolucao == null) { Text("Devolver") }
            } }
            if (em.devolucao != null) Text("Todos devolvidos em ${em.devolucao}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
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
    onImportar: () -> Unit
) {
    var nomeEditado by remember(nomeProfessor) { mutableStateOf(nomeProfessor) }
    var confirmarRestauracao by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TituloSecao("Ajustes", "Personalize o aplicativo para o seu uso.") }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Seu perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Seu nome identifica seus pedidos e atividades.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(nomeEditado, { nomeEditado = it }, Modifier.fillMaxWidth(), label = { Text("Nome do professor") }, placeholder = { Text("Ex.: Maria Silva") }, singleLine = true)
            Button(onClick = { onNomeProfessor(nomeEditado.trim()) }, Modifier.fillMaxWidth()) { Text("Salvar meu nome") }
        } } }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Aparência", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Escolha como prefere visualizar o aplicativo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Aparelho" to "Padrão", "Claro" to "Claro", "Escuro" to "Escuro").forEach { (valor, rotulo) ->
                    FilterChip(selected = modoVisual == valor, onClick = { onModoVisual(valor) }, label = { Text(rotulo) }, modifier = Modifier.weight(1f))
                }
            }
            if (modoVisual == "Aparelho") Text("O tema acompanha a configuração do seu celular.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } } }
        item { Text("Dados e ajuda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth()) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema em equipamento") } }
        item { Button(onClick = onExportar, Modifier.fillMaxWidth()) { Text("Salvar cópia dos meus dados") } }
        item { OutlinedButton(onClick = { confirmarRestauracao = true }, Modifier.fillMaxWidth()) { Text("Restaurar cópia de segurança") } }
        item { Text("Os dados ficam salvos neste aparelho. Empréstimos e atividades são vinculados ao seu nome.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Text("${banco.equipamentos.size} equipamentos disponíveis no catálogo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    if (confirmarRestauracao) AlertDialog(onDismissRequest = { confirmarRestauracao = false }, title = { Text("Restaurar cópia de segurança?") }, text = { Text("A restauração substituirá os dados salvos neste celular. Você poderá escolher o arquivo na próxima etapa.") }, confirmButton = { TextButton(onClick = { confirmarRestauracao = false; onImportar() }) { Text("Continuar") } }, dismissButton = { TextButton(onClick = { confirmarRestauracao = false }) { Text("Cancelar") } })
}

@Composable
private fun EmprestimoCard(em: Emprestimo) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(em.professor, fontWeight = FontWeight.SemiBold); Text("${em.sala} • ${em.codigos.size} equipamento(s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill("Em uso") } }
}

@Composable
private fun StatusPill(status: String) {
    val cor = when (status) { "Disponível", "Devolvido" -> MaterialTheme.colorScheme.primary; "Manutenção" -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.secondary }
    Surface(color = cor.copy(alpha = .12f), shape = RoundedCornerShape(50)) { Text(status, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = cor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TituloSecao(titulo: String, descricao: String) { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(titulo, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(descricao, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun EmptyCard(titulo: String, detalhe: String) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(titulo, fontWeight = FontWeight.SemiBold); Text(detalhe, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
