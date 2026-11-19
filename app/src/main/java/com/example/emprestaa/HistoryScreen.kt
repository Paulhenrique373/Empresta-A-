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


@Composable
fun Historico(banco: BancoLocal, pesquisa: String, onPesquisa: (String) -> Unit, filtro: String, onFiltro: (String) -> Unit, nomeProfessor: String, onDevolver: (Emprestimo, String) -> Unit) {
    val lista = banco.emprestimos.filter { em ->
        val nomesItens = em.codigos.mapNotNull { codigo -> banco.equipamentos.firstOrNull { it.codigo == codigo }?.nome }.joinToString(" ")
        "${em.codigos.joinToString()} $nomesItens".contains(pesquisa, true) && when (filtro) { "Em aberto" -> em.devolucao == null; "Concluídos" -> em.devolucao != null; else -> true }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSecao("Minhas atividades", "Acompanhe pedidos, itens emprestados e devoluções.") }
        item { OutlinedTextField(pesquisa, onPesquisa, Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar equipamento") }, singleLine = true) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Todos", "Em aberto", "Concluídos").forEach { estado -> FilterChip(selected = filtro == estado, onClick = { onFiltro(estado) }, label = { Text(estado) }) } } }
        if (lista.isEmpty()) item { EmptyActivityCard("Nenhuma atividade ainda", "Seus pedidos e devoluções aparecerão aqui.") }
        items(lista, key = { it.id }) { em ->
            var codigoParaConfirmar by remember(em.id) { mutableStateOf<String?>(null) }
            Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text("Empréstimo #${em.id}", fontWeight = FontWeight.Bold); Text("${em.inicio}  •  ${em.codigos.size} item(ns)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; StatusPill(if (em.devolucao == null) "Em aberto" else "Concluído") }
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
