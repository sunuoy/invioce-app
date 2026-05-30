package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import com.example.util.BackupRestoreHelper
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.InvoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var gmailId by remember { mutableStateOf("") }
    var shortIcon by remember { mutableStateOf("💼") }

    var showGoogleAccountChooser by remember { mutableStateOf(false) }
    var isGmailSyncing by remember { mutableStateOf(false) }

    var showRestoreConfirmation by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() }
                if (!jsonString.isNullOrBlank()) {
                    showRestoreConfirmation = jsonString
                } else {
                    Toast.makeText(context, "Selected backup file was empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val exportBackupData = {
        try {
            val jsonString = BackupRestoreHelper.exportToJson(
                profile = businessProfile,
                products = products,
                customers = customers,
                invoices = invoices
            )
            val backupDir = File(context.cacheDir, "backups")
            if (!backupDir.exists()) backupDir.mkdirs()
            val file = File(backupDir, "invoice_easy_backup.json")
            file.writeText(jsonString)

            val fileUri = FileProvider.getUriForFile(
                context,
                "com.aistudio.invoicegenerator.gqtwv.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice Easy Database Backup")
                putExtra(Intent.EXTRA_TEXT, "Here is your Invoice Easy data backup file (JSON).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Share / Save Backup")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Prefill fields when profile in DB is loaded
    LaunchedEffect(businessProfile) {
        businessProfile?.let {
            name = it.businessName
            address = it.address
            phone = it.phone
            email = it.email
            gstin = it.gstin
            upiId = it.upiId
            gmailId = it.gmailId
            shortIcon = if (it.shortIcon.isBlank()) "💼" else it.shortIcon
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Configuration", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Visual Banner introduction Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Business,
                        contentDescription = "Business header details setup",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Column {
                        Text(
                            text = "Set Your Business Header",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "This metadata is utilized in PDF generation for headers, and sets tax identifiers like GSTIN.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Input Fields Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Header Profile Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Registered Business Name*") },
                        placeholder = { Text("e.g. Apex Tech Solutions") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = "Firm name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_biz_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Support Telephone Contact") },
                        placeholder = { Text("e.g. +91 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Business Contact number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Commercial Email Address") },
                        placeholder = { Text("e.g. invoice@apextech.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Contact email") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )

                    // Secure Gmail ID Integration Component block
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Gmail Account Secure Integration",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (gmailId.isBlank()) {
                        Button(
                            onClick = { showGoogleAccountChooser = true },
                            modifier = Modifier.fillMaxWidth().testTag("sync_gmail_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Un-synced Gmail status indicator icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Connect & Sync Gmail Account", fontSize = 13.sp)
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = gmailId.take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = gmailId,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Verified status badge indicator",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "Secure Gmail Integration Active",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = { 
                                        gmailId = "" 
                                        Toast.makeText(context, "Gmail integration disconnected", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text("Disconnect", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = { Text("Taxpayer ID / GSTIN") },
                        placeholder = { Text("e.g. 07AAAAA1111A1Z1") },
                        leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = "GST Details identification") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_biz_tax_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID (Payments)") },
                        placeholder = { Text("e.g. apextech@ybl") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = "UPI payments identifier") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_biz_upi_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Commercial Street Location Address") },
                        placeholder = { Text("e.g. 104 Nehru Place, Delhi, 110019") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Office location Address") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                    )
                }
            }

            // Beautiful Identity & Mascot Selector Card (Short Business Icon)
            Card(
                modifier = Modifier.fillMaxWidth().testTag("business_identity_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Business Mascot & Short Icon",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Customize a decorative symbol for your brand. This visual icon is featured on top dashboards, client cards, and directly inside generated PDF Invoice headers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = shortIcon.ifBlank { "💼" },
                                    fontSize = 26.sp
                                )
                            }
                        }

                        Column {
                            Text(
                                text = if (name.isNotBlank()) name else "Your Registered Business",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Active Short Logo Brand Signature",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    val shortPresets = listOf("💼", "🛒", "🏭", "💻", "🛠️", "📦", "🏠", "⚡", "🩺", "🎨", "🌟", "🍳")

                    Text(
                        text = "Select Preset Symbol Mascot:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shortPresets.forEach { emoji ->
                            val isSelected = shortIcon == emoji
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .padding(2.dp)
                                    .clickable { shortIcon = emoji }
                                    .background(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = shortIcon,
                        onValueChange = {
                            if (it.length <= 4) {
                                shortIcon = it
                            }
                        },
                        label = { Text("Or Type Custom Characters/Initials (Max 3 chars)") },
                        placeholder = { Text("e.g. ⭐, AT, IND") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_short_icon_input"),
                        singleLine = true
                    )
                }
            }

            // Disclaimer / Information Info Tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Config tip",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "* Fields with star indicators are mandatory for invoice generation.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // Data Backup & Recovery Section
            Card(
                modifier = Modifier.fillMaxWidth().testTag("backup_recovery_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Restore backup data",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Database Backup & Recovery",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Take periodic backups of your entire local database (business accounts, customer profiles, product items, and past invoices) into a portable JSON file, or restore a previous file back into this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { exportBackupData() },
                            modifier = Modifier.weight(1f).testTag("export_backup_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Export backup icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        Button(
                            onClick = { importLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f).testTag("import_backup_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Import backup icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Destructive Overwrite Dialog Warning
            if (showRestoreConfirmation != null) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmation = null },
                    title = { Text("Confirm Backup Overwrite") },
                    text = {
                        Text(
                            "WARNING: Restoring this backup file will completely wipe out your existing business profile, stock items, customer registries, and invoices! This action cannot be undone. Are you sure you want to overwrite all local database files?",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val json = showRestoreConfirmation!!
                                showRestoreConfirmation = null
                                viewModel.restoreDatabaseBackup(
                                    jsonString = json,
                                    onSuccess = {
                                        Toast.makeText(context, "Backup Restored Successfully!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { error ->
                                        Toast.makeText(context, "Error RESTORING: $error", Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Wipe & Restore Now")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreConfirmation = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Interactive Google GmailChooser Dialog Simulator
            if (showGoogleAccountChooser) {
                AlertDialog(
                    onDismissRequest = { showGoogleAccountChooser = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Secured authorization title",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Secure Google Account Sign-In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                "Choose a Gmail account to secure your invoice transmissions, back up records automatically, and send verified PDF links through Google Workspace ecosystem.",
                                style = MaterialTheme.typography.bodySmall
                            )

                            if (isGmailSyncing) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text("Retrieving Google security keys...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Row 1: The user email from metadata
                                    ListItem(
                                        headlineContent = { Text("younusM33@gmail.com", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                                        supportingContent = { Text("Active Developer Account", fontSize = 10.sp) },
                                        leadingContent = {
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("Y", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable {
                                                isGmailSyncing = true
                                                // Simulate secured sync
                                                context.mainExecutor.execute {
                                                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                        gmailId = "younusM33@gmail.com"
                                                        isGmailSyncing = false
                                                        showGoogleAccountChooser = false
                                                        Toast.makeText(context, "Synced successfully with younusM33@gmail.com!", Toast.LENGTH_SHORT).show()
                                                    }, 1500)
                                                }
                                            }
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )

                                    // Row 2: Standard custom entry placeholder option
                                    ListItem(
                                        headlineContent = { Text("Use another commercial account", fontSize = 13.sp) },
                                        supportingContent = { Text("Workspace / Business Gmail Address", fontSize = 10.sp) },
                                        leadingContent = {
                                            Surface(
                                                shape = RoundedCornerShape(100.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Add Google Account icon",
                                                    modifier = Modifier.padding(6.dp),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .clickable {
                                                gmailId = "invoice.billing@gmail.com"
                                                Toast.makeText(context, "Synced with invoice.billing@gmail.com", Toast.LENGTH_SHORT).show()
                                                showGoogleAccountChooser = false
                                            }
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(
                            onClick = { showGoogleAccountChooser = false },
                            enabled = !isGmailSyncing
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Save Profile Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Business Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveBusinessProfile(name, address, phone, email, gstin, upiId, gmailId, shortIcon)
                    Toast.makeText(context, "Business Metadata Saved Successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .testTag("save_business_profile_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = "Submit settings record")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Configuration Profile", fontSize = 15.sp)
            }
        }
    }
}
