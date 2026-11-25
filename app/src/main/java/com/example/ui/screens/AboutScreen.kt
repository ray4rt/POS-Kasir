package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark

private const val DEVELOPER_WHATSAPP = "6285150872460"
private const val DEVELOPER_TIKTOK_URL = "https://www.tiktok.com/@ai.agentz"
private const val DEVELOPER_TIKTOK_HANDLE = "@ai.agentz"

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak dapat membuka tautan.", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // App Info Hero Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            EmeraldPrimary,
                                            EmeraldPrimaryDark
                                        )
                                    )
                                )
                                .padding(vertical = 24.dp, horizontal = 16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PointOfSale,
                                            contentDescription = "Ikon Aplikasi POS Kasir Offline",
                                            tint = Color.White,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "POS Kasir Offline",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Versi 1.0.0 • 100% Offline",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Tentang Aplikasi",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Aplikasi Point of Sale (POS) berbasis Android yang dirancang untuk membantu UMKM, toko kelontong, dan usaha retail mencatat transaksi secara cepat tanpa bergantung pada koneksi internet. Semua data produk, stok, pelanggan, dan laporan tersimpan aman secara lokal di perangkat Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }

            // Developer Info & Social Links Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary.copy(alpha = 0.14f))
                                    .border(1.5.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = "Ikon Developer",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rayden",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Developer",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Developer Aplikasi • $DEVELOPER_TIKTOK_HANDLE",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Dikembangkan dengan fokus pada kemudahan penggunaan, kecepatan transaksi, serta keandalan pencatatan stok dan keuangan untuk mendukung operasional usaha Anda sehari-hari.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 19.sp
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp))

                        Text(
                            text = "Hubungi & Ikuti Developer",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        // TikTok Link Button
                        OutlinedButton(
                            onClick = { openExternalUrl(DEVELOPER_TIKTOK_URL) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("tiktok_developer_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircle,
                                contentDescription = "TikTok Developer",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TikTok $DEVELOPER_TIKTOK_HANDLE (Rayden)",
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // WhatsApp Developer Button
                        Button(
                            onClick = {
                                openExternalUrl(
                                    "https://wa.me/$DEVELOPER_WHATSAPP?text=Halo%20Rayden%2C%20saya%20ingin%20bertanya%20mengenai%20aplikasi%20POS%20Kasir%20Offline."
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("whatsapp_developer_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "WhatsApp Developer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chat Developer via WhatsApp",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // App Features Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Keunggulan Aplikasi",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val features = listOf(
                            "100% Offline, data tersimpan aman di HP/Tablet Anda",
                            "Katalog produk dilengkapi foto, SKU, dan barcode",
                            "Mendukung penjualan timbangan/desimal (kg, gram, liter)",
                            "Kalkulasi harga decimal-safe tanpa selisih pembulatan",
                            "Dukungan cetak struk printer thermal 58mm & 80mm",
                            "Fitur simpan transaksi sementara (Hold Cart)",
                            "Backup & Restore data mudah dalam format JSON",
                            "Pemberitahuan stok menipis dan audit mutasi stok"
                        )

                        features.forEach { feat ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feat,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
