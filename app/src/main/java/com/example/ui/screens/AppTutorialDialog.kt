package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

data class TutorialStep(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val bulletPoints: List<Pair<String, String>>
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AppTutorialDialog(
    onDismiss: () -> Unit
) {
    val tutorialSteps = remember {
        listOf(
            TutorialStep(
                title = "Welcome to Invoice Easy",
                subtitle = "Complete billing, GST invoices & inventory in seconds",
                description = "Everything you need to streamline sales, track client payments, and generate branded tax invoices.",
                icon = Icons.Default.RocketLaunch,
                iconBgColor = Color(0xFF6366F1),
                bulletPoints = listOf(
                    "Professional PDF Invoices" to "Generate tax-compliant GST & thermal receipts instantly.",
                    "Real-time Inventory" to "Track stock levels, low-stock alerts, and HSN/SAC codes.",
                    "Client Directory" to "Keep customer contacts, GSTIN, and ledger records in one place."
                )
            ),
            TutorialStep(
                title = "1. Setup Your Business",
                subtitle = "Customise invoices with your own branding",
                description = "Navigate to Profile to set your business profile, bank details, and branding.",
                icon = Icons.Default.Business,
                iconBgColor = Color(0xFF3B82F6),
                bulletPoints = listOf(
                    "Business Profile & Logo" to "Enter company name, address, GSTIN, and custom monogram or logo.",
                    "Bank Details & QR Code" to "Add account number and UPI ID to automatically print instant payment QR codes.",
                    "Digital Signature" to "Upload your authorized signature to automatically stamp invoices."
                )
            ),
            TutorialStep(
                title = "2. Add Products & Clients",
                subtitle = "Speed up billing with pre-saved catalogs",
                description = "Quickly add items and customers so you can bill them in one tap.",
                icon = Icons.Default.Inventory2,
                iconBgColor = Color(0xFF10B981),
                bulletPoints = listOf(
                    "Product Catalog" to "Save items with unit price, tax rate (GST), unit (pcs, kg, hrs), and stock.",
                    "Auto Stock Deduction" to "Creating an invoice automatically deducts product inventory.",
                    "Client Ledger" to "Store client phone, address, and place of supply for automatic interstate GST calculation."
                )
            ),
            TutorialStep(
                title = "3. Create Invoices & Estimates",
                subtitle = "Fast, flexible billing for every situation",
                description = "Tap '+ Generate Bill' from Dashboard or Invoices screen to start a new document.",
                icon = Icons.Default.ReceiptLong,
                iconBgColor = Color(0xFFF59E0B),
                bulletPoints = listOf(
                    "Tax Invoices & Estimates" to "Create final GST invoices or draft quotes/estimates with 1-click conversion.",
                    "Itemized Line Items" to "Add catalog items or custom entries with discounts and automatic CGST/SGST split.",
                    "Payment Tracking" to "Record partial or full payments via Cash, UPI, or Bank Transfer."
                )
            ),
            TutorialStep(
                title = "4. Share, Print & Backup",
                subtitle = "Multiple PDF models, thermal printing & cloud sync",
                description = "Export and protect your records seamlessly.",
                icon = Icons.Default.CloudSync,
                iconBgColor = Color(0xFF8B5CF6),
                bulletPoints = listOf(
                    "3 PDF Models" to "Choose between Classic Navy, Clean Modern, or 58mm/80mm Thermal Receipt styles.",
                    "WhatsApp & Print" to "Share invoice PDFs directly to clients via WhatsApp, Email, or WiFi printers.",
                    "Google Drive Backup" to "Secure auto-sync or manual backup of all invoices, clients & products to your Google Drive."
                )
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { tutorialSteps.size })
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Bar with Skip and Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Step Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Step ${pagerState.currentPage + 1} of ${tutorialSteps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Close / Skip Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Tutorial",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Page Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val step = tutorialSteps[page]
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Animated Icon Box
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            step.iconBgColor,
                                            step.iconBgColor.copy(alpha = 0.75f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = step.icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Feature Cards
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            step.bulletPoints.forEach { (pointTitle, pointDesc) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = step.iconBgColor,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .padding(top = 1.dp)
                                        )
                                        Column {
                                            Text(
                                                text = pointTitle,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = pointDesc,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Indicator and Navigation Buttons
                Spacer(modifier = Modifier.height(12.dp))

                // Dot Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(tutorialSteps.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(6.dp)
                                .width(if (isSelected) 22.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Actions: Back and Next/Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Back", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Button(
                        onClick = {
                            if (pagerState.currentPage < tutorialSteps.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            } else {
                                onDismiss()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage < tutorialSteps.size - 1) "Next" else "Get Started",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
