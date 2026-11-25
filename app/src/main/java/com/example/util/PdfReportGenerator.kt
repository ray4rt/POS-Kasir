package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.model.ShopSettings
import com.example.data.model.TransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SalesReportData(
    val periodTypeLabel: String,     // e.g., "Harian", "Mingguan (7 Hari)", "Bulanan", "Semua Waktu"
    val periodRangeLabel: String,    // e.g., "25 November 2026" or "November 2026"
    val fileSlug: String,            // e.g., "Harian_20261125" or "Bulanan_202611"
    val transactions: List<TransactionEntity>,
    val settings: ShopSettings?
)

object PdfReportGenerator {
    private const val PAGE_WIDTH = 595   // A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842  // A4 height in points (72 dpi)
    private const val MARGIN_X = 36f
    private const val MARGIN_TOP = 36f
    private const val MARGIN_BOTTOM = 42f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2)

    private val EMERALD = Color.parseColor("#10A37F")
    private val EMERALD_DARK = Color.parseColor("#0E8C6D")
    private val EMERALD_LIGHT_BG = Color.parseColor("#ECFDF5")
    private val SLATE_900 = Color.parseColor("#0F172A")
    private val SLATE_700 = Color.parseColor("#334155")
    private val SLATE_500 = Color.parseColor("#64748B")
    private val BORDER_GRAY = Color.parseColor("#CBD5E1")
    private val ROW_ALT_BG = Color.parseColor("#F8FAFC")
    private val HEADER_GRAY_BG = Color.parseColor("#F1F5F9")

    fun writePdfToUri(context: Context, uri: Uri, data: SalesReportData): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                writePdfToStream(out, data)
            } ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun generateCachedPdfFile(context: Context, data: SalesReportData): File? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            } else {
                // Keep cache clean by removing older temporary report PDFs
                reportsDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".pdf")) {
                        file.delete()
                    }
                }
            }
            val file = File(reportsDir, "Laporan_Penjualan_${data.fileSlug}.pdf")
            FileOutputStream(file).use { out ->
                val ok = writePdfToStream(out, data)
                if (!ok) return null
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun writePdfToStream(outputStream: OutputStream, data: SalesReportData): Boolean {
        val document = PdfDocument()
        return try {
            val completed = data.transactions
                .filter { it.status == "COMPLETED" }
                .sortedByDescending { it.timestamp }

            val totalRevenue = completed.sumOf { it.totalAmount }
            val totalCost = completed.sumOf { it.totalCost }
            val totalDiscount = completed.sumOf { it.discountAmount }
            val totalTax = completed.sumOf { it.taxAmount }
            val totalProfit = completed.sumOf { it.profit }
            val profitMarginPct = if (totalRevenue > 0.0) (totalProfit / totalRevenue) * 100.0 else 0.0
            val avgTicket = if (completed.isNotEmpty()) totalRevenue / completed.size else 0.0

            val paymentGroups = completed.groupBy { it.paymentMethod }
                .map { (method, list) ->
                    Triple(method, list.size, list.sumOf { it.totalAmount })
                }
                .sortedByDescending { it.third }

            val topProductsMap = mutableMapOf<String, Pair<Double, Double>>() // name -> (qty, subtotal)
            completed.forEach { tx ->
                tx.getItems().forEach { item ->
                    val prev = topProductsMap[item.productName] ?: (0.0 to 0.0)
                    topProductsMap[item.productName] = (prev.first + item.quantity) to (prev.second + item.subtotal)
                }
            }
            val topProducts = topProductsMap.entries
                .sortedByDescending { it.value.second }
                .take(10)

            val shopName = data.settings?.shopName?.takeIf { it.isNotBlank() } ?: "POS Kasir Offline"
            val shopAddress = data.settings?.address?.takeIf { it.isNotBlank() } ?: ""
            val shopPhone = data.settings?.phone?.takeIf { it.isNotBlank() } ?: ""
            val exportedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

            // Reusable paints
            val titleWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#E2F8F1")
                textSize = 9.5f
            }
            val badgeWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = EMERALD_DARK
                textSize = 11.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_500
                textSize = 9f
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_900
                textSize = 9.5f
            }
            val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_900
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_900
                textSize = 9.5f
                textAlign = Paint.Align.RIGHT
            }
            val bodyRightBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_900
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val profitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = EMERALD_DARK
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_700
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val tableHeaderRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_700
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val smallMutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = SLATE_500
                textSize = 8f
            }
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
            }
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = BORDER_GRAY
                strokeWidth = 0.8f
            }

            var pageNumber = 1
            var currentPage = document.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            var canvas: Canvas = currentPage.canvas
            var y = MARGIN_TOP

            fun drawFooter(c: Canvas, pageNum: Int) {
                val footerY = PAGE_HEIGHT - 20f
                strokePaint.color = BORDER_GRAY
                c.drawLine(MARGIN_X, footerY - 12f, PAGE_WIDTH - MARGIN_X, footerY - 12f, strokePaint)
                c.drawText(
                    "$shopName • Dicetak pada $exportedAt",
                    MARGIN_X,
                    footerY,
                    smallMutedPaint
                )
                val pageStr = "Halaman $pageNum"
                val rightMuted = Paint(smallMutedPaint).apply { textAlign = Paint.Align.RIGHT }
                c.drawText(pageStr, PAGE_WIDTH - MARGIN_X, footerY, rightMuted)
            }

            fun startNewPage(onNewPageHeader: ((Canvas) -> Float)? = null) {
                drawFooter(canvas, pageNumber)
                document.finishPage(currentPage)
                pageNumber++
                currentPage = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = currentPage.canvas
                y = MARGIN_TOP
                if (onNewPageHeader != null) {
                    y = onNewPageHeader(canvas)
                }
            }

            fun ensureSpace(neededHeight: Float, onNewPageHeader: ((Canvas) -> Float)? = null) {
                if (y + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                    startNewPage(onNewPageHeader)
                }
            }

            // 1. Header Banner
            val bannerHeight = if (shopAddress.isNotBlank() || shopPhone.isNotBlank()) 82f else 68f
            val bannerRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + bannerHeight)
            fillPaint.color = EMERALD
            canvas.drawRoundRect(bannerRect, 10f, 10f, fillPaint)

            canvas.drawText(
                ellipsizeText(shopName, titleWhitePaint, CONTENT_WIDTH * 0.65f),
                MARGIN_X + 16f,
                y + 26f,
                titleWhitePaint
            )
            canvas.drawText(
                "LAPORAN ${data.periodTypeLabel.uppercase()}",
                PAGE_WIDTH - MARGIN_X - 16f,
                y + 24f,
                badgeWhitePaint
            )
            canvas.drawText(
                "Periode: ${data.periodRangeLabel}",
                PAGE_WIDTH - MARGIN_X - 16f,
                y + 40f,
                Paint(subWhitePaint).apply { textAlign = Paint.Align.RIGHT }
            )

            var infoY = y + 44f
            if (shopAddress.isNotBlank()) {
                canvas.drawText(
                    ellipsizeText(shopAddress, subWhitePaint, CONTENT_WIDTH * 0.58f),
                    MARGIN_X + 16f,
                    infoY,
                    subWhitePaint
                )
                infoY += 14f
            }
            if (shopPhone.isNotBlank()) {
                canvas.drawText(
                    "Telp: $shopPhone",
                    MARGIN_X + 16f,
                    infoY,
                    subWhitePaint
                )
            }
            canvas.drawText(
                "Ekspor: $exportedAt",
                PAGE_WIDTH - MARGIN_X - 16f,
                y + bannerHeight - 12f,
                Paint(subWhitePaint).apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f }
            )

            y += bannerHeight + 18f

            // 2. Ringkasan Eksekutif
            canvas.drawText("RINGKASAN EKSEKUTIF", MARGIN_X, y, sectionPaint)
            y += 8f

            val kpiCardHeight = 112f
            val kpiRect = RectF(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + kpiCardHeight)
            fillPaint.color = EMERALD_LIGHT_BG
            canvas.drawRoundRect(kpiRect, 8f, 8f, fillPaint)
            strokePaint.color = Color.parseColor("#A7F3D0")
            canvas.drawRoundRect(kpiRect, 8f, 8f, strokePaint)
            strokePaint.color = BORDER_GRAY

            val colLeftX = MARGIN_X + 14f
            val colLeftValX = MARGIN_X + (CONTENT_WIDTH / 2f) - 12f
            val colRightX = MARGIN_X + (CONTENT_WIDTH / 2f) + 12f
            val colRightValX = PAGE_WIDTH - MARGIN_X - 14f

            // Vertical divider inside KPI card
            canvas.drawLine(
                MARGIN_X + CONTENT_WIDTH / 2f,
                y + 12f,
                MARGIN_X + CONTENT_WIDTH / 2f,
                y + kpiCardHeight - 12f,
                strokePaint
            )

            var ky = y + 24f
            canvas.drawText("Jumlah Transaksi", colLeftX, ky, labelPaint)
            canvas.drawText("${completed.size} transaksi", colLeftValX, ky, bodyRightBoldPaint)

            canvas.drawText("Total Omzet", colRightX, ky, labelPaint)
            canvas.drawText(CurrencyFormatter.format(totalRevenue), colRightValX, ky, bodyRightBoldPaint)

            ky += 22f
            canvas.drawText("Rata-rata / Transaksi", colLeftX, ky, labelPaint)
            canvas.drawText(CurrencyFormatter.format(avgTicket), colLeftValX, ky, bodyRightPaint)

            canvas.drawText("Total Modal (HPP)", colRightX, ky, labelPaint)
            canvas.drawText(CurrencyFormatter.format(totalCost), colRightValX, ky, bodyRightPaint)

            ky += 22f
            canvas.drawText("Total Diskon", colLeftX, ky, labelPaint)
            canvas.drawText(CurrencyFormatter.format(totalDiscount), colLeftValX, ky, bodyRightPaint)

            canvas.drawText("Total Pajak", colRightX, ky, labelPaint)
            canvas.drawText(CurrencyFormatter.format(totalTax), colRightValX, ky, bodyRightPaint)

            ky += 24f
            canvas.drawText("Margin Laba", colLeftX, ky, bodyBoldPaint)
            canvas.drawText(String.format(Locale.US, "%.1f%%", profitMarginPct), colLeftValX, ky, bodyRightBoldPaint)

            canvas.drawText("Estimasi Laba Bersih", colRightX, ky, bodyBoldPaint)
            canvas.drawText(CurrencyFormatter.format(totalProfit), colRightValX, ky, profitPaint)

            y += kpiCardHeight + 18f

            // 3. Penerimaan per Metode Bayar
            ensureSpace(54f + (paymentGroups.size.coerceAtLeast(1) * 20f))
            canvas.drawText("PENERIMAAN PER METODE BAYAR", MARGIN_X, y, sectionPaint)
            y += 8f

            val pmCol1 = MARGIN_X + 10f
            val pmCol2 = MARGIN_X + CONTENT_WIDTH * 0.55f
            val pmCol3 = PAGE_WIDTH - MARGIN_X - 95f
            val pmCol4 = PAGE_WIDTH - MARGIN_X - 10f

            fillPaint.color = HEADER_GRAY_BG
            canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 20f, fillPaint)
            canvas.drawText("Metode Pembayaran", pmCol1, y + 13.5f, tableHeaderPaint)
            canvas.drawText("Transaksi", pmCol2, y + 13.5f, tableHeaderRightPaint)
            canvas.drawText("Porsi", pmCol3, y + 13.5f, tableHeaderRightPaint)
            canvas.drawText("Total Nominal", pmCol4, y + 13.5f, tableHeaderRightPaint)
            y += 20f

            if (paymentGroups.isEmpty()) {
                canvas.drawText("Tidak ada transaksi pada periode ini.", pmCol1, y + 14f, labelPaint)
                y += 22f
            } else {
                paymentGroups.forEachIndexed { idx, (method, txCount, sum) ->
                    ensureSpace(20f)
                    if (idx % 2 == 1) {
                        fillPaint.color = ROW_ALT_BG
                        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 19f, fillPaint)
                    }
                    val sharePct = if (totalRevenue > 0.0) (sum / totalRevenue) * 100.0 else 0.0
                    canvas.drawText(method, pmCol1, y + 13f, bodyPaint)
                    canvas.drawText("${txCount}x", pmCol2, y + 13f, bodyRightPaint)
                    canvas.drawText(String.format(Locale.US, "%.1f%%", sharePct), pmCol3, y + 13f, bodyRightPaint)
                    canvas.drawText(CurrencyFormatter.format(sum), pmCol4, y + 13f, bodyRightBoldPaint)
                    y += 19f
                }
            }
            canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, strokePaint)
            y += 18f

            // 4. Produk Terlaris (Top 10)
            if (topProducts.isNotEmpty()) {
                ensureSpace(48f + (topProducts.size * 19f).coerceAtMost(120f))
                canvas.drawText("PRODUK TERLARIS (TOP ${topProducts.size})", MARGIN_X, y, sectionPaint)
                y += 8f

                fun drawTopProductHeader(c: Canvas, startY: Float): Float {
                    fillPaint.color = HEADER_GRAY_BG
                    c.drawRect(MARGIN_X, startY, PAGE_WIDTH - MARGIN_X, startY + 20f, fillPaint)
                    c.drawText("No", MARGIN_X + 8f, startY + 13.5f, tableHeaderPaint)
                    c.drawText("Nama Produk", MARGIN_X + 34f, startY + 13.5f, tableHeaderPaint)
                    c.drawText("Qty Terjual", PAGE_WIDTH - MARGIN_X - 115f, startY + 13.5f, tableHeaderRightPaint)
                    c.drawText("Total Penjualan", PAGE_WIDTH - MARGIN_X - 10f, startY + 13.5f, tableHeaderRightPaint)
                    return startY + 20f
                }

                y = drawTopProductHeader(canvas, y)
                topProducts.forEachIndexed { idx, entry ->
                    ensureSpace(20f) { newCanvas ->
                        newCanvas.drawText("PRODUK TERLARIS (LANJUTAN)", MARGIN_X, y, sectionPaint)
                        y += 8f
                        drawTopProductHeader(newCanvas, y)
                    }
                    if (idx % 2 == 1) {
                        fillPaint.color = ROW_ALT_BG
                        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 19f, fillPaint)
                    }
                    canvas.drawText("${idx + 1}.", MARGIN_X + 8f, y + 13f, bodyPaint)
                    canvas.drawText(
                        ellipsizeText(entry.key, bodyPaint, CONTENT_WIDTH - 220f),
                        MARGIN_X + 34f,
                        y + 13f,
                        bodyPaint
                    )
                    canvas.drawText(
                        UnitConverter.formatQty(entry.value.first),
                        PAGE_WIDTH - MARGIN_X - 115f,
                        y + 13f,
                        bodyRightPaint
                    )
                    canvas.drawText(
                        CurrencyFormatter.format(entry.value.second),
                        PAGE_WIDTH - MARGIN_X - 10f,
                        y + 13f,
                        bodyRightBoldPaint
                    )
                    y += 19f
                }
                canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, strokePaint)
                y += 18f
            }

            // 5. Rincian Transaksi
            ensureSpace(64f)
            canvas.drawText("RINCIAN TRANSAKSI (${completed.size})", MARGIN_X, y, sectionPaint)
            y += 8f

            val txColNo = MARGIN_X + 6f
            val txColDate = MARGIN_X + 28f
            val txColInvoice = MARGIN_X + 104f
            val txColCustomer = MARGIN_X + 210f
            val txColMethod = MARGIN_X + 336f
            val txColTotal = PAGE_WIDTH - MARGIN_X - 84f
            val txColProfit = PAGE_WIDTH - MARGIN_X - 6f

            fun drawTransactionTableHeader(c: Canvas, startY: Float): Float {
                fillPaint.color = HEADER_GRAY_BG
                c.drawRect(MARGIN_X, startY, PAGE_WIDTH - MARGIN_X, startY + 20f, fillPaint)
                c.drawText("No", txColNo, startY + 13.5f, tableHeaderPaint)
                c.drawText("Tanggal", txColDate, startY + 13.5f, tableHeaderPaint)
                c.drawText("No. Invoice", txColInvoice, startY + 13.5f, tableHeaderPaint)
                c.drawText("Kasir / Pelanggan", txColCustomer, startY + 13.5f, tableHeaderPaint)
                c.drawText("Bayar", txColMethod, startY + 13.5f, tableHeaderPaint)
                c.drawText("Total", txColTotal, startY + 13.5f, tableHeaderRightPaint)
                c.drawText("Laba", txColProfit, startY + 13.5f, tableHeaderRightPaint)
                return startY + 20f
            }

            y = drawTransactionTableHeader(canvas, y)

            if (completed.isEmpty()) {
                canvas.drawText("Belum ada transaksi selesai pada periode ini.", txColNo, y + 15f, labelPaint)
                y += 24f
            } else {
                val txDateFmt = SimpleDateFormat("dd/MM HH:mm", Locale("id", "ID"))
                completed.forEachIndexed { idx, tx ->
                    ensureSpace(20f) { newCanvas ->
                        newCanvas.drawText("RINCIAN TRANSAKSI (LANJUTAN)", MARGIN_X, y, sectionPaint)
                        y += 8f
                        drawTransactionTableHeader(newCanvas, y)
                    }
                    if (idx % 2 == 1) {
                        fillPaint.color = ROW_ALT_BG
                        canvas.drawRect(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y + 19f, fillPaint)
                    }

                    val partyLabel = if (!tx.customerName.isNullOrBlank() && tx.customerName != "-") {
                        "${tx.cashierName} / ${tx.customerName}"
                    } else {
                        tx.cashierName
                    }

                    val rowPaint = Paint(bodyPaint).apply { textSize = 8.5f }
                    val rowRightPaint = Paint(bodyRightPaint).apply { textSize = 8.5f }
                    val rowRightBoldPaint = Paint(bodyRightBoldPaint).apply { textSize = 8.5f }

                    canvas.drawText("${idx + 1}", txColNo, y + 13f, rowPaint)
                    canvas.drawText(txDateFmt.format(Date(tx.timestamp)), txColDate, y + 13f, rowPaint)
                    canvas.drawText(ellipsizeText(tx.invoiceNumber, rowPaint, 100f), txColInvoice, y + 13f, rowPaint)
                    canvas.drawText(ellipsizeText(partyLabel, rowPaint, 120f), txColCustomer, y + 13f, rowPaint)
                    canvas.drawText(ellipsizeText(tx.paymentMethod, rowPaint, 52f), txColMethod, y + 13f, rowPaint)
                    canvas.drawText(CurrencyFormatter.format(tx.totalAmount), txColTotal, y + 13f, rowRightBoldPaint)
                    canvas.drawText(CurrencyFormatter.format(tx.profit), txColProfit, y + 13f, rowRightPaint)
                    y += 19f
                }
            }
            canvas.drawLine(MARGIN_X, y, PAGE_WIDTH - MARGIN_X, y, strokePaint)

            drawFooter(canvas, pageNumber)
            document.finishPage(currentPage)
            document.writeTo(outputStream)
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0 && paint.measureText(text.substring(0, end) + ellipsis) > maxWidth) {
            end--
        }
        return if (end <= 0) ellipsis else text.substring(0, end) + ellipsis
    }
}
