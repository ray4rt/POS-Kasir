package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.model.ShopSettings
import com.example.data.model.TransactionEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.util.CurrencyFormatter
import com.example.util.PdfReportGenerator
import com.example.util.SalesReportData
import com.example.util.UnitConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class ReportPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("7 Hari"),
    MONTHLY("Bulanan"),
    ALL("Semua")
}

@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    settings: ShopSettings?,
    onShowMessage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.DAILY) }
    var dayOffset by remember { mutableIntStateOf(0) }     // 0 = today, -1 = yesterday, ...
    var monthOffset by remember { mutableIntStateOf(0) }   // 0 = current month, -1 = last month, ...
    var isExportingPdf by remember { mutableStateOf(false) }

    val periodWindow = remember(selectedPeriod, dayOffset, monthOffset) {
        val localeId = Locale("id", "ID")
        when (selectedPeriod) {
            ReportPeriod.DAILY -> {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis

                val displayFmt = SimpleDateFormat("dd MMM yyyy", localeId)
                val slugFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
                val dateLabel = displayFmt.format(cal.time)
                val suffix = if (dayOffset == 0) " (Hari Ini)" else if (dayOffset == -1) " (Kemarin)" else ""
                ReportWindow(
                    startMs = start,
                    endMs = end,
                    periodTypeLabel = "Penjualan Harian",
                    periodRangeLabel = "$dateLabel$suffix",
                    fileSlug = "Harian_${slugFmt.format(cal.time)}"
                )
            }
            ReportPeriod.WEEKLY -> {
                val cal = Calendar.getInstance()
                val end = cal.timeInMillis
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -6)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = startCal.timeInMillis
                val shortFmt = SimpleDateFormat("dd MMM", localeId)
                val fullFmt = SimpleDateFormat("dd MMM yyyy", localeId)
                val slugFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
                ReportWindow(
                    startMs = start,
                    endMs = end,
                    periodTypeLabel = "Penjualan 7 Hari Terakhir",
                    periodRangeLabel = "${shortFmt.format(startCal.time)} - ${fullFmt.format(cal.time)}",
                    fileSlug = "Mingguan_${slugFmt.format(cal.time)}"
                )
            }
            ReportPeriod.MONTHLY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    add(Calendar.MONTH, monthOffset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                val endCal = (cal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                    add(Calendar.MILLISECOND, -1)
                }
                val end = endCal.timeInMillis

                val monthFmt = SimpleDateFormat("MMMM yyyy", localeId)
                val slugFmt = SimpleDateFormat("yyyyMM", Locale.US)
                val monthLabel = monthFmt.format(cal.time).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(localeId) else it.toString()
                }
                ReportWindow(
                    startMs = start,
                    endMs = end,
                    periodTypeLabel = "Penjualan Bulanan",
                    periodRangeLabel = monthLabel,
                    fileSlug = "Bulanan_${slugFmt.format(cal.time)}"
                )
            }
            ReportPeriod.ALL -> {
                ReportWindow(
                    startMs = Long.MIN_VALUE,
                    endMs = Long.MAX_VALUE,
                    periodTypeLabel = "Penjualan Keseluruhan",
                    periodRangeLabel = "Semua Periode",
                    fileSlug = "Semua_${SimpleDateFormat("yyyyMMdd", Locale.US).format(System.currentTimeMillis())}"
                )
            }
        }
    }

    val filtered = remember(transactions, periodWindow) {
        transactions.filter { tx ->
            tx.status == "COMPLETED" &&
                tx.timestamp >= periodWindow.startMs &&
                tx.timestamp <= periodWindow.endMs
        }
    }

    val totalRevenue = filtered.sumOf { it.totalAmount }
    val totalCost = filtered.sumOf { it.totalCost }
    val totalProfit = filtered.sumOf { it.profit }
    val totalDiscount = filtered.sumOf { it.discountAmount }
    val totalTax = filtered.sumOf { it.taxAmount }
    val profitMarginPct = if (totalRevenue > 0.0) (totalProfit / totalRevenue) * 100.0 else 0.0
    val count = filtered.size

    val topProducts = remember(filtered) {
        val map = mutableMapOf<String, Pair<Double, Double>>()
        filtered.forEach { tx ->
            tx.getItems().forEach { item ->
                val prev = map[item.productName] ?: (0.0 to 0.0)
                map[item.productName] = (prev.first + item.quantity) to (prev.second + item.subtotal)
            }
        }
        map.entries.sortedByDescending { it.value.second }.take(5)
    }

    val reportData = remember(periodWindow, filtered, settings) {
        SalesReportData(
            periodTypeLabel = periodWindow.periodTypeLabel,
            periodRangeLabel = periodWindow.periodRangeLabel,
            fileSlug = periodWindow.fileSlug,
            transactions = filtered,
            settings = settings
        )
    }

    val reportSummaryText = remember(periodWindow, totalRevenue, totalCost, totalDiscount, totalTax, totalProfit, count, settings) {
        buildString {
            append("=== ${periodWindow.periodTypeLabel.uppercase()} ===\n")
            append("Toko: ${settings?.shopName ?: "Kasir Offline"}\n")
            append("Periode: ${periodWindow.periodRangeLabel}\n")
            append("Waktu Ekspor: ${CurrencyFormatter.formatDate(System.currentTimeMillis())}\n")
            append("--------------------------------------\n")
            append("Total Transaksi : $count\n")
            append("Total Omzet     : ${CurrencyFormatter.format(totalRevenue)}\n")
            append("Total Modal     : ${CurrencyFormatter.format(totalCost)}\n")
            append("Total Diskon    : ${CurrencyFormatter.format(totalDiscount)}\n")
            append("Total Pajak     : ${CurrencyFormatter.format(totalTax)}\n")
            append("Estimasi Laba   : ${CurrencyFormatter.format(totalProfit)}\n")
            append("======================================\n")
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            isExportingPdf = true
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    PdfReportGenerator.writePdfToUri(context, uri, reportData)
                }
                isExportingPdf = false
                if (success) {
                    onShowMessage("Laporan PDF berhasil disimpan.")
                } else {
                    onShowMessage("Gagal menyimpan laporan PDF.")
                }
            }
        }
    }

    fun exportAndShareOrViewPdf(viewOnly: Boolean) {
        if (isExportingPdf) return
        isExportingPdf = true
        scope.launch {
            val file = withContext(Dispatchers.IO) {
                PdfReportGenerator.generateCachedPdfFile(context, reportData)
            }
            isExportingPdf = false
            if (file == null) {
                onShowMessage("Gagal membuat file PDF.")
                return@launch
            }
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                if (viewOnly) {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(viewIntent, "Buka Laporan PDF"))
                } else {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "${periodWindow.periodTypeLabel} - ${periodWindow.periodRangeLabel}")
                        putExtra(Intent.EXTRA_TEXT, "Terlampir laporan ${periodWindow.periodTypeLabel} (${periodWindow.periodRangeLabel}).")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan PDF"))
                }
            } catch (_: Exception) {
                onShowMessage("Tidak ada aplikasi pembuka/berbagi PDF yang tersedia.")
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Laporan Penjualan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Pantau performa bisnis & ekspor laporan harian/bulanan ke PDF",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ReportPeriod.entries) { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("report_period_chip_${period.name.lowercase()}")
                        )
                    }
                }

                // Period Date/Month Navigator for Harian & Bulanan
                if (selectedPeriod == ReportPeriod.DAILY || selectedPeriod == ReportPeriod.MONTHLY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedPeriod == ReportPeriod.DAILY) dayOffset-- else monthOffset--
                                },
                                modifier = Modifier.testTag("report_period_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Periode Sebelumnya"
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (selectedPeriod == ReportPeriod.DAILY) Icons.Default.Today else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = periodWindow.periodRangeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            val canGoNext = if (selectedPeriod == ReportPeriod.DAILY) dayOffset < 0 else monthOffset < 0
                            IconButton(
                                onClick = {
                                    if (selectedPeriod == ReportPeriod.DAILY && dayOffset < 0) dayOffset++
                                    else if (selectedPeriod == ReportPeriod.MONTHLY && monthOffset < 0) monthOffset++
                                },
                                enabled = canGoNext,
                                modifier = Modifier.testTag("report_period_next_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Periode Berikutnya"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ringkasan Eksekutif", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))

                        ReportSummaryRow("Jumlah Transaksi:", "$count transaksi", boldValue = true)
                        ReportSummaryRow(
                            "Total Omzet (Penjualan):",
                            CurrencyFormatter.format(totalRevenue),
                            boldValue = true,
                            valueColor = EmeraldPrimary
                        )
                        ReportSummaryRow("Total Modal (HPP):", CurrencyFormatter.format(totalCost))
                        ReportSummaryRow("Total Diskon Toko:", CurrencyFormatter.format(totalDiscount))
                        ReportSummaryRow("Total Pajak Terkumpul:", CurrencyFormatter.format(totalTax))
                        ReportSummaryRow("Margin Keuntungan:", String.format(Locale.US, "%.1f%%", profitMarginPct))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("Estimasi Keuntungan Bersih:", fontWeight = FontWeight.Bold)
                            Text(
                                CurrencyFormatter.format(totalProfit),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Breakdown by Payment Method
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Penerimaan per Metode Bayar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val group = filtered.groupBy { it.paymentMethod }
                        if (group.isEmpty()) {
                            Text(
                                "Belum ada data pada periode ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            group.forEach { (m, list) ->
                                val sum = list.sumOf { it.totalAmount }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text("$m (${list.size}x)")
                                    Text(CurrencyFormatter.format(sum), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Top Products Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Produk Terlaris", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (topProducts.isEmpty()) {
                            Text(
                                "Belum ada produk terjual pada periode ini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            topProducts.forEachIndexed { idx, entry ->
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${entry.key} (${UnitConverter.formatQty(entry.value.first)})",
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    )
                                    Text(
                                        text = CurrencyFormatter.format(entry.value.second),
                                        fontWeight = FontWeight.SemiBold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // PDF Export Card
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ekspor Laporan PDF",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simpan atau bagikan laporan lengkap (${periodWindow.periodTypeLabel}) termasuk ringkasan omzet, laba, metode bayar, produk terlaris, dan rincian transaksi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isExportingPdf) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Menyiapkan dokumen PDF...", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    savePdfLauncher.launch("Laporan_Penjualan_${periodWindow.fileSlug}.pdf")
                                },
                                enabled = !isExportingPdf,
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("export_pdf_save_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Simpan PDF")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan PDF", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { exportAndShareOrViewPdf(viewOnly = false) },
                                enabled = !isExportingPdf,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("export_pdf_share_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Bagikan PDF")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bagikan PDF", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { exportAndShareOrViewPdf(viewOnly = true) },
                            enabled = !isExportingPdf,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("export_pdf_view_button")
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = "Buka Pratinjau PDF")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka / Pratinjau PDF")
                        }
                    }
                }
            }

            // Quick Text Export Actions
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clip.setPrimaryClip(ClipData.newPlainText("Laporan Penjualan", reportSummaryText))
                            onShowMessage("Ringkasan laporan disalin ke clipboard.")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_text_copy_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin Teks")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Teks")
                    }

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "${periodWindow.periodTypeLabel} - ${periodWindow.periodRangeLabel}")
                                putExtra(Intent.EXTRA_TEXT, reportSummaryText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Bagikan Ringkasan Teks"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("export_text_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Bagikan Teks")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bagikan Teks")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryRow(
    label: String,
    value: String,
    boldValue: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            fontWeight = if (boldValue) FontWeight.Bold else FontWeight.Normal,
            color = valueColor,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private data class ReportWindow(
    val startMs: Long,
    val endMs: Long,
    val periodTypeLabel: String,
    val periodRangeLabel: String,
    val fileSlug: String
)
