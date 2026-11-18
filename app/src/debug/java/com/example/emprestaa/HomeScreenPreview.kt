package com.example.emprestaa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentReturn
import androidx.compose.material.icons.outlined.AddToQueue
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.AddToQueue
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class HomeIdentity { CIVIC, EDITORIAL }

private data class PreviewLoan(
    val kind: String,
    val model: String,
    val tag: String,
    val takenAt: String,
)

private val previewLoans = listOf(
    PreviewLoan("Notebook", "Positivo Motion Q", "NB-005", "Hoje, 08:10"),
    PreviewLoan("Tablet", "Lenovo Tab P11", "TB-005", "Hoje, 10:25"),
    PreviewLoan("Notebook", "Acer TravelMate P2", "NB-008", "14 nov., 13:40"),
)

private data class HomeTokens(
    val brand: Color,
    val background: Color,
    val surface: Color,
    val ink: Color,
    val muted: Color,
    val divider: Color,
    val font: FontFamily,
    val radius: Dp,
    val outlinedIcons: Boolean,
)

private fun tokens(identity: HomeIdentity) = when (identity) {
    HomeIdentity.CIVIC -> HomeTokens(
        brand = Color(0xFF145B5A), background = Color(0xFFF6F5EF), surface = Color.White,
        ink = Color(0xFF172625), muted = Color(0xFF4C625F), divider = Color(0xFFD5DEDA),
        font = FontFamily.SansSerif, radius = 7.dp, outlinedIcons = true,
    )
    HomeIdentity.EDITORIAL -> HomeTokens(
        brand = Color(0xFF394779), background = Color(0xFFF4F3EE), surface = Color.White,
        ink = Color(0xFF252A3D), muted = Color(0xFF5E6373), divider = Color(0xFFD7D7D1),
        font = FontFamily.Serif, radius = 8.dp, outlinedIcons = false,
    )
}

@Composable
private fun HomeIdentityTheme(identity: HomeIdentity, content: @Composable () -> Unit) {
    val t = tokens(identity)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = t.brand,
            onPrimary = Color.White,
            background = t.background,
            onBackground = t.ink,
            surface = t.surface,
            onSurface = t.ink,
            onSurfaceVariant = t.muted,
            outline = t.divider,
            outlineVariant = t.divider,
        ),
        content = content,
    )
}

@Composable
internal fun HomePreviewScreen(identity: HomeIdentity, activeItems: Int) {
    val t = tokens(identity)
    val rows = previewLoans.take(activeItems.coerceIn(0, previewLoans.size))
    HomeIdentityTheme(identity) {
        Surface(Modifier.fillMaxSize(), color = t.background) {
            Column(
                Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                HomeHeader(t)

                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Com você agora", fontFamily = t.font, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = t.ink, modifier = Modifier.weight(1f))
                        Text(rows.size.toString().padStart(2, '0'), fontFamily = t.font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.muted)
                    }
                    Spacer(Modifier.height(8.dp))

                    if (rows.isEmpty()) {
                        Text(
                            "Nenhum equipamento com você",
                            fontFamily = t.font,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = t.muted,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    } else {
                        rows.forEachIndexed { index, loan ->
                            LoanLine(loan, t)
                            if (index < rows.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(color = t.divider, thickness = 1.dp)
                            }
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    val availableCount = if (rows.size == 3) 16 else 17
                    Text("$availableCount livres de 20 equipamentos", fontFamily = t.font, fontSize = 14.sp, color = t.muted)
                }

                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).heightIn(min = 56.dp),
                    shape = RoundedCornerShape(t.radius),
                    colors = ButtonDefaults.buttonColors(containerColor = t.brand, contentColor = Color.White),
                ) {
                    Icon(QrIcon(t), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Pegar equipamento", fontFamily = t.font, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                PreviewNavigation(t)
            }
        }
    }
}

@Composable
private fun HomeHeader(t: HomeTokens) {
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Ana Silva", fontFamily = t.font, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = t.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Emprestaê Jovem", fontFamily = t.font, fontSize = 12.sp, color = t.muted)
        }
        Surface(
            modifier = Modifier.size(44.dp).clip(CircleShape).semantics { contentDescription = "Perfil de Ana Silva" },
            color = t.surface,
            border = BorderStroke(1.dp, t.divider),
            shape = CircleShape,
        ) {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text("AS", fontFamily = t.font, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = t.brand)
            }
        }
    }
    androidx.compose.material3.HorizontalDivider(color = t.divider)
}

@Composable
private fun LoanLine(loan: PreviewLoan, t: HomeTokens) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 92.dp).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (loan.kind == "Notebook") LaptopIcon(t) else TabletIcon(t),
            contentDescription = loan.kind,
            tint = t.ink,
            modifier = Modifier.size(24.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(loan.model, fontFamily = t.font, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = t.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(loan.tag, fontFamily = t.font, fontSize = 12.sp, color = t.muted)
            Text(loan.takenAt, fontFamily = t.font, fontSize = 12.sp, color = t.muted)
        }
        OutlinedButton(
            onClick = {},
            modifier = Modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(t.radius),
            border = BorderStroke(1.dp, t.brand),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
        ) {
            Icon(ReturnIcon(t), contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text("Devolver", fontFamily = t.font, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PreviewNavigation(t: HomeTokens) {
    NavigationBar(containerColor = t.surface, windowInsets = WindowInsets.navigationBars) {
        NavigationBarItem(selected = true, onClick = {}, icon = { Icon(HomeIcon(t), contentDescription = "Início") }, label = { Text("Início", fontFamily = t.font, fontSize = 12.sp) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(InventoryIcon(t), contentDescription = "Pegar") }, label = { Text("Pegar", fontFamily = t.font, fontSize = 12.sp) })
        NavigationBarItem(selected = false, onClick = {}, icon = { Icon(HistoryIcon(t), contentDescription = "Histórico") }, label = { Text("Histórico", fontFamily = t.font, fontSize = 12.sp) })
    }
}

private fun LaptopIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.Laptop else Icons.Rounded.Laptop
private fun TabletIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.TabletAndroid else Icons.Rounded.TabletAndroid
private fun ReturnIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.AssignmentReturn else Icons.Rounded.AssignmentReturn
private fun HomeIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.Home else Icons.Rounded.Home
private fun InventoryIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.AddToQueue else Icons.Rounded.AddToQueue
private fun HistoryIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.History else Icons.Rounded.History
private fun QrIcon(t: HomeTokens): ImageVector = if (t.outlinedIcons) Icons.Outlined.QrCodeScanner else Icons.Rounded.QrCodeScanner

@Preview(name = "Cívico · 0 itens", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewCivicZero() = HomePreviewScreen(HomeIdentity.CIVIC, 0)

@Preview(name = "Cívico · 1 item", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewCivicOne() = HomePreviewScreen(HomeIdentity.CIVIC, 1)

@Preview(name = "Cívico · 3 itens", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewCivicThree() = HomePreviewScreen(HomeIdentity.CIVIC, 3)

@Preview(name = "Editorial · 0 itens", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewEditorialZero() = HomePreviewScreen(HomeIdentity.EDITORIAL, 0)

@Preview(name = "Editorial · 1 item", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewEditorialOne() = HomePreviewScreen(HomeIdentity.EDITORIAL, 1)

@Preview(name = "Editorial · 3 itens", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PreviewEditorialThree() = HomePreviewScreen(HomeIdentity.EDITORIAL, 3)
