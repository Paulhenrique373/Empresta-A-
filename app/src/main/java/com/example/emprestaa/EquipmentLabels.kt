package com.example.emprestaa

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

internal enum class LabelFormat(val title: String) {
    QR("QR Code"), CODE_128("Código 128")
}

internal data class EquipmentLabel(val bitmap: Bitmap, val encodedValue: String, val format: LabelFormat)

internal object EquipmentLabelGenerator {
    fun valueFor(equipment: Equipamento, format: LabelFormat): String =
        if (format == LabelFormat.QR) equipment.id else equipment.codigo

    fun generate(equipment: Equipamento, format: LabelFormat): EquipmentLabel {
        val qr = format == LabelFormat.QR
        // QR labels encode the permanent internal ID; linear labels encode the equipment code.
        val value = valueFor(equipment, format)
        require(value.isNotBlank()) { "O equipamento não possui um identificador para gerar a etiqueta." }
        val width = if (qr) 620 else 1400
        val height = if (qr) 760 else 560
        val matrixWidth = if (qr) 460 else 1200
        val matrixHeight = if (qr) 460 else 280
        val hints = mapOf(
            EncodeHintType.MARGIN to if (qr) 3 else 18,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val matrix = MultiFormatWriter().encode(
            value,
            if (qr) BarcodeFormat.QR_CODE else BarcodeFormat.CODE_128,
            matrixWidth,
            matrixHeight,
            hints
        )
        val codeBitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            codeBitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        }

        val label = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(label)
        canvas.drawColor(Color.WHITE)
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 43, 99)
            textAlign = Paint.Align.CENTER
            textSize = if (qr) 38f else 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            textSize = if (qr) 30f else 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(equipment.nome.take(48), width / 2f, if (qr) 52f else 60f, titlePaint)
        val codeTop = if (qr) 82f else 92f
        val codeRect = if (qr) {
            RectF((width - 460f) / 2f, codeTop, (width + 460f) / 2f, codeTop + 460f)
        } else {
            RectF((width - 1200f) / 2f, codeTop, (width + 1200f) / 2f, codeTop + 280f)
        }
        canvas.drawBitmap(codeBitmap, null, codeRect, null)
        val details = listOfNotNull(equipment.codigo.takeIf { it.isNotBlank() }, equipment.tipo.takeIf { it.isNotBlank() })
            .joinToString("  •  ")
        canvas.drawText(details.take(64), width / 2f, if (qr) 590f else 430f, detailPaint)
        canvas.drawText("Empresta Aê", width / 2f, if (qr) 650f else 490f, detailPaint)
        codeBitmap.recycle()
        return EquipmentLabel(label, value, format)
    }
}

internal fun shareEquipmentLabel(context: Context, equipment: Equipamento, label: EquipmentLabel) {
    val directory = File(context.cacheDir, "labels").apply { mkdirs() }
    val safeName = equipment.codigo.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val file = File(directory, "empresta-ae-$safeName-${label.format.name.lowercase(Locale.ROOT)}.png")
    FileOutputStream(file).use { output ->
        check(label.bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Não foi possível gravar a etiqueta." }
    }
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "Etiqueta ${equipment.codigo} · Empresta Aê")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        clipData = android.content.ClipData.newUri(context.contentResolver, "Etiqueta ${equipment.codigo}", uri)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar etiqueta"))
}

internal fun printEquipmentLabel(context: Context, equipment: Equipamento, label: EquipmentLabel) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val title = "Etiqueta ${equipment.codigo}"
    printManager.print(title, EquipmentLabelPrintAdapter(equipment, label), PrintAttributes.Builder().build())
}

private class EquipmentLabelPrintAdapter(
    private val equipment: Equipamento,
    private val label: EquipmentLabel
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("etiqueta-${equipment.codigo}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build(),
            oldAttributes != newAttributes
        )
    }

    override fun onWrite(
        pages: Array<PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback
    ) {
        val document = PdfDocument()
        try {
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            if (pages.none { it.start <= 0 && it.end >= 0 }) {
                callback.onWriteFinished(emptyArray())
                return
            }
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            page.canvas.drawColor(Color.WHITE)
            val inset = 44f
            val availableWidth = 595f - inset * 2
            val availableHeight = 520f
            val ratio = minOf(availableWidth / label.bitmap.width, availableHeight / label.bitmap.height)
            val drawWidth = label.bitmap.width * ratio
            val drawHeight = label.bitmap.height * ratio
            val left = (595f - drawWidth) / 2
            val top = 90f
            page.canvas.drawBitmap(label.bitmap, null, RectF(left, top, left + drawWidth, top + drawHeight), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            document.finishPage(page)
            if (cancellationSignal.isCanceled) {
                callback.onWriteCancelled()
                return
            }
            FileOutputStream(destination.fileDescriptor).use(document::writeTo)
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
        } catch (error: Exception) {
            callback.onWriteFailed(error.localizedMessage ?: "Não foi possível preparar a impressão.")
        } finally {
            document.close()
        }
    }
}
