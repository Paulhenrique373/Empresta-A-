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
fun Mais(
    banco: BancoLocal,
    nomeProfessor: String,
    modoVisual: String,
    onModoVisual: (String) -> Unit,
    onProblema: () -> Unit,
    onSair: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { TituloSecao("Ajustes", "Personalize o aplicativo para o seu uso.") }
        item { Card(Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Seu perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (nomeProfessor.isBlank()) "Professor" else nomeProfessor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text("Nome vindo do cadastro da escola. Para alterar, procure a gestão.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text("Relate um defeito em algum equipamento.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onProblema, Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Default.ReportProblem, null); Spacer(Modifier.width(8.dp)); Text("Informar problema") }
        } } }
        item { OutlinedButton(onClick = onSair, Modifier.fillMaxWidth()) { Text("Sair da conta") } }
        item { Text("${banco.equipamentos.size} equipamentos disponíveis no catálogo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
