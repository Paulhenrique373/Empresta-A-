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
fun Emprestar(banco: BancoLocal, selecionados: MutableList<String>, onConfirmar: () -> Unit, onProblema: () -> Unit, onEscanear: () -> Unit) {
    val disponiveis = banco.equipamentos.filter { it.status == "Disponível" }
    var confirmarPedido by remember { mutableStateOf(false) }
    val podeEnviar = selecionados.isNotEmpty() && selecionados.all { codigo -> disponiveis.any { it.codigo == codigo } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TituloSecao("Pedir equipamento", "Escolha os itens que precisa para sua aula.") }
        item { OutlinedButton(onClick = onEscanear, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.Search, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Escanear equipamento") } }
        item { Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Escolha seus equipamentos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("${selecionados.size} selecionado(s) • ${disponiveis.size} livres", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } }
        if (disponiveis.isEmpty()) item { EmptyCard("Nenhum equipamento livre agora", "Confira novamente mais tarde ou informe um problema em Ajustes.") }
        items(disponiveis, key = { it.id }) { eq -> Card(Modifier.fillMaxWidth().clickable { if (eq.codigo in selecionados) selecionados.remove(eq.codigo) else selecionados.add(eq.codigo) }, border = BorderStroke(1.dp, if (eq.codigo in selecionados) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), colors = CardDefaults.cardColors(containerColor = if (eq.codigo in selecionados) MaterialTheme.colorScheme.primaryContainer.copy(alpha = .65f) else MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(eq.codigo in selecionados, { if (it) selecionados.add(eq.codigo) else selecionados.remove(eq.codigo) }); Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(22.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(eq.nome, fontWeight = FontWeight.SemiBold); Text("${eq.tipo}  •  ${eq.codigo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("Livre", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
        } }
        if (!podeEnviar) item { Text("Selecione ao menos um equipamento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Button(onClick = { confirmarPedido = true }, Modifier.fillMaxWidth(), enabled = podeEnviar) { Icon(Icons.AutoMirrored.Filled.Assignment, null); Spacer(Modifier.width(8.dp)); Text("Revisar pedido") } }
        item { OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth()) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema em equipamento") } }
    }
    if (confirmarPedido) AlertDialog(
        onDismissRequest = { confirmarPedido = false },
        title = { Text("Confirme seu pedido") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Equipamentos (${selecionados.size})", fontWeight = FontWeight.SemiBold)
            selecionados.forEach { codigo -> Text("• ${disponiveis.firstOrNull { it.codigo == codigo }?.nome ?: codigo} ($codigo)", style = MaterialTheme.typography.bodySmall) }
        } },
        confirmButton = { Button(onClick = { confirmarPedido = false; onConfirmar() }) { Text("Confirmar pedido") } },
        dismissButton = { TextButton(onClick = { confirmarPedido = false }) { Text("Voltar") } }
    )
}
