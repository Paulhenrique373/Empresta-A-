package com.example.emprestaa

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class LoginDirection { HERITAGE, SIGNAL }

@Composable
internal fun LoginDirectionPreview(direction: String, hasError: Boolean) {
    LoginDirectionScreen(
        direction = if (direction.equals("B", ignoreCase = true)) LoginDirection.SIGNAL else LoginDirection.HERITAGE,
        hasError = hasError,
    )
}

private data class LoginPalette(
    val bg: Color, val ink: Color, val muted: Color, val brand: Color, val accent: Color,
    val surface: Color, val field: Color, val edge: Color, val error: Color,
    val font: FontFamily,
)

private fun palette(direction: LoginDirection) = when (direction) {
    LoginDirection.HERITAGE -> LoginPalette(
        bg = Color(0xFFF8F3EF), ink = Color(0xFF2B1B2C), muted = Color(0xFF635563),
        brand = Color(0xFF572A58), accent = Color(0xFFC65383), surface = Color(0xFFEADDE5),
        field = Color(0xFFFFFCFA), edge = Color(0xFF766575), error = Color(0xFFB3261E),
        font = FontFamily.Serif,
    )
    LoginDirection.SIGNAL -> LoginPalette(
        bg = Color(0xFFF1F4F8), ink = Color(0xFF171D2B), muted = Color(0xFF4E596B),
        brand = Color(0xFF402777), accent = Color(0xFF007C98), surface = Color(0xFF252039),
        field = Color(0xFFFFFFFF), edge = Color(0xFF627086), error = Color(0xFFB3261E),
        font = FontFamily.SansSerif,
    )
}

@Preview(name = "A · vazia", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HeritageLoginEmptyPreview() = LoginDirectionScreen(LoginDirection.HERITAGE, hasError = false)

@Preview(name = "A · erro", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun HeritageLoginErrorPreview() = LoginDirectionScreen(LoginDirection.HERITAGE, hasError = true)

@Preview(name = "B · vazia", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SignalLoginEmptyPreview() = LoginDirectionScreen(LoginDirection.SIGNAL, hasError = false)

@Preview(name = "B · erro", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SignalLoginErrorPreview() = LoginDirectionScreen(LoginDirection.SIGNAL, hasError = true)

@Composable
private fun LoginDirectionScreen(direction: LoginDirection, hasError: Boolean) {
    val c = palette(direction)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = c.brand, onPrimary = Color.White,
            background = c.bg, onBackground = c.ink,
            surface = c.field, onSurface = c.ink,
            onSurfaceVariant = c.muted, outline = c.edge,
            error = c.error, onError = Color.White,
        ),
    ) {
        Surface(Modifier.fillMaxSize(), color = c.bg) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compact = maxHeight < 690.dp || maxWidth < 340.dp
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
                    BrandIllustration(direction, Modifier.fillMaxWidth().height(if (compact) 136.dp else 190.dp))
                    Spacer(Modifier.height(if (compact) 14.dp else 22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.width(5.dp).height(32.dp).clip(RoundedCornerShape(3.dp)).background(c.accent))
                        Column {
                            Text("Empresta Aê", fontFamily = c.font, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = c.ink)
                            Text("CONTROLE DE EQUIPAMENTOS ESCOLARES", fontFamily = c.font, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, color = c.muted, letterSpacing = 1.1.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("Acesse sua conta", fontFamily = c.font, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    Text("Use o e-mail da sua escola.", fontFamily = c.font, fontSize = 14.sp, lineHeight = 20.sp, color = c.muted, modifier = Modifier.padding(top = 3.dp, bottom = 18.dp))
                    PreviewField("E-mail", Icons.Outlined.Email, "", c)
                    Spacer(Modifier.height(12.dp))
                    PreviewField("Senha", Icons.Outlined.Lock, "", c, isPassword = true)
                    if (hasError) {
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("!", color = c.error, fontFamily = c.font, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Não foi possível entrar. Confira seu e-mail e senha e tente novamente.", color = c.error, fontFamily = c.font, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 4.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Text("Esqueci minha senha", color = c.brand, fontFamily = c.font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {}, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = c.brand),
                    ) { Text("Entrar", fontFamily = c.font, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(18.dp))
                    Text("Ainda não tem acesso? Solicite suas credenciais à administração da escola.", fontFamily = c.font, fontSize = 12.sp, lineHeight = 18.sp, color = c.muted)
                    Spacer(Modifier.height(18.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.edge.copy(alpha = .38f)))
                    Text("EMPRESTA AÊ  ·  ESCOLA E TECNOLOGIA", fontFamily = c.font, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = c.muted, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp), textAlign = TextAlign.Center, letterSpacing = .5.sp)
                }
            }
        }
    }
}

@Composable
private fun PreviewField(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, c: LoginPalette, isPassword: Boolean = false) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = c.ink, fontFamily = c.font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(8.dp))
                .background(c.field).border(1.dp, c.edge, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            if (value.isNotEmpty()) Text(value, color = c.ink, fontFamily = c.font, fontSize = 14.sp)
            else Text(if (isPassword) "Sua senha" else "nome@escola.edu.br", color = c.muted, fontFamily = c.font, fontSize = 14.sp, modifier = Modifier.weight(1f))
            if (isPassword) Icon(Icons.Outlined.Visibility, contentDescription = "Mostrar senha", tint = c.muted, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun BrandIllustration(direction: LoginDirection, modifier: Modifier = Modifier) {
    val c = palette(direction)
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (direction == LoginDirection.HERITAGE) {
            // Offset paper field: two devices and a small QR signature, drawn as the brand mark.
            drawRoundRect(c.surface, Offset(w * .12f, h * .08f), Size(w * .76f, h * .82f), androidx.compose.ui.geometry.CornerRadius(26.dp.toPx()))
            drawRoundRect(c.brand, Offset(w * .23f, h * .22f), Size(w * .39f, h * .40f), androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()))
            drawRoundRect(c.bg, Offset(w * .255f, h * .255f), Size(w * .34f, h * .32f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawLine(c.brand, Offset(w * .205f, h * .65f), Offset(w * .645f, h * .65f), 6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
            drawRoundRect(c.accent, Offset(w * .67f, h * .30f), Size(w * .14f, h * .34f), androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()))
            drawRoundRect(c.bg, Offset(w * .692f, h * .325f), Size(w * .096f, h * .20f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
            drawRect(c.accent, Offset(w * .70f, h * .555f), Size(w * .08f, h * .045f))
            drawQr(this, Offset(w * .48f, h * .30f), w * .09f, c.ink)
            drawCircle(c.accent, radius = 5.dp.toPx(), center = Offset(w * .80f, h * .22f))
            drawLine(c.brand.copy(alpha = .35f), Offset(w * .14f, h * .78f), Offset(w * .28f, h * .78f), 2.dp.toPx())
        } else {
            // Signal-like blocks reference a shelf, device screens and a QR label.
            drawRect(c.surface, Offset(w * .08f, h * .13f), Size(w * .84f, h * .70f))
            drawRect(c.accent, Offset(w * .08f, h * .13f), Size(w * .12f, h * .70f))
            drawRoundRect(c.bg, Offset(w * .25f, h * .25f), Size(w * .38f, h * .41f), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            drawRect(c.accent, Offset(w * .29f, h * .30f), Size(w * .30f, h * .025f))
            drawRect(c.brand, Offset(w * .29f, h * .36f), Size(w * .22f, h * .025f))
            drawLine(c.bg, Offset(w * .24f, h * .71f), Offset(w * .64f, h * .71f), 5.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Square)
            drawRoundRect(c.accent, Offset(w * .69f, h * .31f), Size(w * .15f, h * .34f), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
            drawRoundRect(c.bg, Offset(w * .714f, h * .34f), Size(w * .102f, h * .23f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            drawQr(this, Offset(w * .73f, h * .40f), w * .07f, c.brand)
            drawRect(c.brand, Offset(w * .73f, h * .59f), Size(w * .07f, h * .025f))
            drawRect(c.bg.copy(alpha = .65f), Offset(w * .23f, h * .77f), Size(w * .52f, 2.dp.toPx()))
        }
    }
}

private fun drawQr(scope: androidx.compose.ui.graphics.drawscope.DrawScope, topLeft: Offset, cell: Float, color: Color) {
    val modules = listOf(
        1 to 0, 2 to 0, 3 to 0, 0 to 1, 2 to 1, 4 to 1,
        0 to 2, 1 to 2, 3 to 2, 4 to 2, 1 to 3, 3 to 3, 4 to 4,
    )
    modules.forEach { (x, y) -> scope.drawRect(color, Offset(topLeft.x + x * cell / 5f, topLeft.y + y * cell / 5f), Size(cell / 5.8f, cell / 5.8f)) }
}
