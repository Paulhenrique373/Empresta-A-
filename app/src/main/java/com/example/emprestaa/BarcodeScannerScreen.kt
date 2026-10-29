package com.example.emprestaa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun BarcodeScannerScreen(onClose: () -> Unit, onCodeRead: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnCodeRead by rememberUpdatedState(onCodeRead)
    var permissionGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        permissionDenied = !granted
    }

    DisposableEffect(scanner, analyzerExecutor) {
        onDispose {
            cameraProvider?.unbindAll()
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Surface(Modifier.fillMaxSize(), color = Color(0xFF080B12)) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Escanear equipamento", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("QR Code ou código de barras", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .75f))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar leitor", tint = Color.White)
                }
            }

            if (permissionGranted) {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { viewContext ->
                            PreviewView(viewContext).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                val previewView = this
                                val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                                providerFuture.addListener({
                                    try {
                                        val provider = providerFuture.get()
                                        cameraProvider = provider
                                        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                                        val processing = AtomicBoolean(false)
                                        val delivered = AtomicBoolean(false)
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                        analysis.setAnalyzer(analyzerExecutor) { proxy ->
                                            val image = proxy.image
                                            if (image == null || delivered.get() || !processing.compareAndSet(false, true)) {
                                                proxy.close()
                                            } else {
                                                scanner.process(InputImage.fromMediaImage(image, proxy.imageInfo.rotationDegrees))
                                                    .addOnSuccessListener { barcodes ->
                                                        val raw = barcodes.firstNotNullOfOrNull { it.rawValue?.trim()?.takeIf(String::isNotEmpty) }
                                                        if (raw != null && delivered.compareAndSet(false, true)) latestOnCodeRead(raw)
                                                    }
                                                    .addOnFailureListener { cameraError = "Não foi possível ler este código. Tente novamente." }
                                                    .addOnCompleteListener {
                                                        proxy.close()
                                                        processing.set(false)
                                                    }
                                            }
                                        }
                                        provider.unbindAll()
                                        boundCamera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                                    } catch (_: Exception) {
                                        cameraError = "Não foi possível iniciar a câmera. Verifique se ela está disponível e tente novamente."
                                    }
                                }, ContextCompat.getMainExecutor(viewContext))
                            }
                        }
                    )

                    val transition = rememberInfiniteTransition(label = "scan-line")
                    val linePosition by transition.animateFloat(
                        initialValue = .15f,
                        targetValue = .85f,
                        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
                        label = "scan-line-position"
                    )
                    Box(Modifier.fillMaxWidth(.82f).height(220.dp)) {
                        Surface(Modifier.fillMaxSize(), color = Color.Transparent, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF80A9FF))) {}
                        Box(
                            Modifier.fillMaxWidth().padding(horizontal = 10.dp).offset(y = (linePosition * 190).dp)
                                .height(2.dp).align(Alignment.TopCenter).background(Color(0xFF5B91FF))
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Posicione o código dentro da área de leitura", color = Color.White, style = MaterialTheme.typography.bodyLarge)
                Text("Mantenha o celular firme e aproxime o código", color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                if (cameraError != null) Text(cameraError!!, color = Color(0xFFFFB4AB), modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    val next = !torchOn
                    boundCamera?.cameraControl?.enableTorch(next)
                    torchOn = next
                }, enabled = boundCamera?.cameraInfo?.hasFlashUnit() == true, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(if (torchOn) Icons.Default.FlashOff else Icons.Default.FlashOn, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (torchOn) "Desligar lanterna" else "Ligar lanterna")
                }
            } else {
                Spacer(Modifier.weight(1f))
                Surface(color = Color(0xFF191F33), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(if (permissionDenied) "A câmera é necessária para escanear" else "Permita o acesso à câmera", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("Você também pode fechar esta tela e pesquisar o código manualmente no catálogo.", color = Color.White.copy(alpha = .75f), style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { permissionRequest.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Permitir câmera") }
                        if (permissionDenied) TextButton(onClick = {
                            val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(settingsIntent)
                        }) { Text("Abrir configurações do aplicativo") }
                        OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Voltar") }
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
