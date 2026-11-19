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
fun Inicio(banco: BancoLocal, nomeProfessor: String, onIr: (String) -> Unit) {
    val emUso = banco.equipamentos.count { it.status == "Em uso" }; val disponiveis = banco.equipamentos.count { it.status == "Disponível" }; val manutencao = banco.equipamentos.count { it.status == "Manutenção" }
    val notebooks = banco.equipamentos.count { it.tipo.contains("notebook", ignoreCase = true) }
    val tablets = banco.equipamentos.count { it.tipo.contains("tablet", ignoreCase = true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .12f)), shape = RoundedCornerShape(22.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = .10f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp).size(22.dp)) }
            Text(if (nomeProfessor.isBlank()) "Olá, professor(a)!" else "Olá, ${nomeProfessor.substringBefore(' ')}!", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Acompanhe os equipamentos da escola e organize o que precisa para sua aula.", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .82f), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onIr("Pedir") }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) { Icon(Icons.Default.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Solicitar equipamento") }
        } } }
        item { Text("Equipamentos da escola", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResumoCard("Notebooks", notebooks.toString(), MaterialTheme.colorScheme.primary, Icons.Default.Devices, Modifier.weight(1f))
            ResumoCard("Tablets", tablets.toString(), MaterialTheme.colorScheme.primary, Icons.Default.Devices, Modifier.weight(1f))
        } }
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
        if (banco.equipamentos.isEmpty()) item { EmptyCard("Catálogo vazio", "Solicite à administração da escola o cadastro dos equipamentos.", Icons.Default.Devices) }
        if (ativos.isEmpty()) item { EmptyCard("Tudo em dia", if (nomeProfessor.isBlank()) "Solicite equipamentos quando precisar." else "Você não tem equipamento emprestado no momento.", Icons.Default.Inbox) }
        items(ativos.take(4), key = { it.id }) { EmprestimoCard(it, banco.equipamentos, onAbrir = { onIr("Atividade") }) }
        item { Text("Acesso rápido", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilledTonalButton(onClick = { onIr("Equipamentos") }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Icon(Icons.Default.Devices, null); Spacer(Modifier.width(7.dp)); Text("Consultar itens") }
            OutlinedButton(onClick = { onIr("Atividade") }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Icon(Icons.AutoMirrored.Filled.Assignment, null); Spacer(Modifier.width(7.dp)); Text("Minhas atividades") }
        } }
    }
}
