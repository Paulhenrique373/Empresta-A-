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
internal fun EquipmentLabelDialog(
    equipment: Equipamento,
    onDismiss: () -> Unit,
    onShare: (EquipmentLabel) -> Unit,
    onPrint: (EquipmentLabel) -> Unit
) {
    var format by remember(equipment.id) { mutableStateOf(LabelFormat.QR) }
    val generated = remember(equipment.id, equipment.codigo, equipment.nome, equipment.tipo, format) {
        runCatching { EquipmentLabelGenerator.generate(equipment, format) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Etiqueta do equipamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(equipment.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("Código ${equipment.codigo} · ${equipment.tipo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(selected = format == LabelFormat.QR, onClick = { format = LabelFormat.QR }, label = { Text("QR Code") })
                    FilterChip(selected = format == LabelFormat.CODE_128, onClick = { format = LabelFormat.CODE_128 }, label = { Text("Código 128") })
                }
                generated.onSuccess { label ->
                    androidx.compose.foundation.Image(
                        bitmap = label.bitmap.asImageBitmap(),
                        contentDescription = "Prévia da etiqueta ${equipment.codigo} em ${format.title}",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                    Text(
                        if (format == LabelFormat.QR) "O QR identifica este item pelo seu ID interno permanente." else "O código de barras representa ${equipment.codigo}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }.onFailure { error ->
                    Text(error.message ?: "Não foi possível gerar esta etiqueta.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { generated.getOrNull()?.let(onShare) }, enabled = generated.isSuccess) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Compartilhar")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { generated.getOrNull()?.let(onPrint) }, enabled = generated.isSuccess) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Imprimir")
                }
                TextButton(onClick = onDismiss) { Text("Fechar") }
            }
        }
    )
}
