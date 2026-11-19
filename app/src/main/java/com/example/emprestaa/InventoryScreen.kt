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
fun Inventario(banco: BancoLocal, pesquisa: String, onPesquisa: (String) -> Unit, filtro: String, onFiltro: (String) -> Unit, onReportar: (String) -> Unit, onEscanear: () -> Unit, onEtiqueta: (Equipamento) -> Unit) {
    val lista = banco.equipamentos.filter { eq -> "${eq.nome} ${eq.codigo} ${eq.tipo} ${eq.status}".contains(pesquisa, true) && (filtro == "Todos" || eq.status == filtro) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSecao("Equipamentos", "Consulte o que está livre para uso.") }
        item { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(pesquisa, onPesquisa, Modifier.weight(1f), leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("Buscar por nome ou código") }, singleLine = true)
            IconButton(onClick = onEscanear, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.Devices, contentDescription = "Escanear equipamento", tint = MaterialTheme.colorScheme.primary) }
        } }
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onReportar(eq.codigo) }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)) { Icon(Icons.Default.ReportProblem, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Informar problema") }
                FilledTonalButton(onClick = { onEtiqueta(eq) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Etiqueta") }
            }
        } } }
        if (lista.isEmpty()) item { EmptyCard("Nenhum equipamento encontrado", "Altere o filtro ou tente outro nome ou código.") }
        if (banco.problemas.isNotEmpty()) {
            item { Text("Problemas relatados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            items(banco.problemas, key = { "problem-${it.id}" }) { problem ->
                val equipment = banco.equipamentos.firstOrNull { it.codigo == problem.codigo }
                Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(equipment?.nome ?: "Equipamento ${problem.codigo}", fontWeight = FontWeight.SemiBold)
                                Text("${problem.codigo} · Relatado ${problem.data}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            equipment?.let { StatusPill(it.status) }
                        }
                        Text(problem.descricao, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
