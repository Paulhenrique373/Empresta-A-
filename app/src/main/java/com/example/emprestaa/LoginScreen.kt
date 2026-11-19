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
fun TelaLogin(authViewModel: AuthViewModel, onContinuarDemonstracao: (com.example.emprestaa.data.SessaoUsuario) -> Unit) {
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
    val authState by authViewModel.state.collectAsState()
    var recuperacaoEnviada by remember { mutableStateOf(false) }
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.SignedIn -> onContinuarDemonstracao(state.user)
            is AuthState.Error -> {
                if (mostrarRecuperacao && state.message.startsWith("Se o e-mail")) recuperacaoEnviada = true
                else erroAcesso = true
            }
            else -> Unit
        }
    }

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
                                if (!erroUsuario && !erroSenha) { focusManager.clearFocus(); authViewModel.login(usuario, senha) }
                            })
                        )
                        TextButton(onClick = { mostrarRecuperacao = true }, modifier = Modifier.align(Alignment.End)) { Text("Esqueci minha senha") }
                        if (erroAcesso) {
                            Text((authState as? AuthState.Error)?.message ?: "Não foi possível entrar.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = {
                            erroUsuario = usuario.isBlank(); erroSenha = senha.isBlank(); erroAcesso = false
                            if (!erroUsuario && !erroSenha && usuario.contains("@")) { focusManager.clearFocus(); authViewModel.login(usuario, senha) } else if (!erroUsuario) erroAcesso = true
                        }, enabled = authState !is AuthState.Loading, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { if (authState is AuthState.Loading) CircularProgressIndicator(Modifier.size(22.dp)) else Text("Entrar") }
                    }
                }
                Text("Ainda não tem acesso? Solicite suas credenciais à administração da escola.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text("Empresta Aê · Controle de equipamentos escolares\nVersão ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
    if (mostrarRecuperacao) AlertDialog(onDismissRequest = { mostrarRecuperacao = false }, title = { Text("Recuperar acesso") }, text = { Text("A recuperação de senha ainda não está configurada. Entre em contato com a administração da sua escola para receber ajuda.") }, confirmButton = { TextButton(onClick = { mostrarRecuperacao = false }) { Text("Entendi") } })
}
