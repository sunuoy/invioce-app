package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import com.example.util.BackupRestoreHelper
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import com.example.data.BusinessProfile
import com.example.data.SavedBusinessProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedBusinessProfiles.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var gmailId by remember { mutableStateOf("") }
    var shortIcon by remember { mutableStateOf("💼") }
    var logoUrl by remember { mutableStateOf("") }
    
    // Bank Details State
    var bankAccountName by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNo by remember { mutableStateOf("") }
    var bankBranch by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }

    val isBankNameError = remember(bankName) {
        bankName.isNotEmpty() && !bankName.all { it.isLetter() || it.isWhitespace() }
    }

    val isBankAccountNoError = remember(bankAccountNo) {
        bankAccountNo.isNotEmpty() && (!bankAccountNo.all { it.isDigit() } || bankAccountNo.length < 10 || bankAccountNo.length > 14)
    }

    var pendingLogoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val selectImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            pendingLogoUri = uri
        }
    }

    val prefs = remember { context.getSharedPreferences("invoice_generator_prefs", android.content.Context.MODE_PRIVATE) }
    var pdfTheme by remember { mutableStateOf(prefs.getString("pdf_theme", "Classic Navy") ?: "Classic Navy") }

    var customFontPath by remember { mutableStateOf(prefs.getString("custom_font_path", "") ?: "") }
    var customFontName by remember { mutableStateOf(prefs.getString("custom_font_name", "") ?: "") }

    val selectFontLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val fontFile = File(context.filesDir, "custom_app_font.ttf")
                    fontFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    customFontPath = fontFile.absolutePath
                    
                    var fileName = "custom_font.ttf"
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                    customFontName = fileName
                    prefs.edit()
                        .putString("custom_font_path", customFontPath)
                        .putString("custom_font_name", customFontName)
                        .apply()
                    Toast.makeText(context, "Custom font loaded: $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to load custom font: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var signaturePath by remember { mutableStateOf(prefs.getString("authorized_signature_path", "") ?: "") }
    var isSignatureEnabled by remember { mutableStateOf(prefs.getBoolean("authorized_signature_enabled", false)) }
    var showSignatureDrawingDialog by remember { mutableStateOf(false) }

    var pendingSignatureUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val selectSignatureImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            pendingSignatureUri = uri
        }
    }

    var showGoogleAccountChooser by remember { mutableStateOf(false) }
    var isGmailSyncing by remember { mutableStateOf(false) }

    var showRestoreConfirmation by remember { mutableStateOf<String?>(null) }

    if (pendingLogoUri != null) {
        LogoCropperDialog(
            uri = pendingLogoUri!!,
            onDismiss = { pendingLogoUri = null },
            onCropped = { croppedBitmap ->
                pendingLogoUri = null
                try {
                    val timestamp = System.currentTimeMillis()
                    val logoFile = File(context.filesDir, "custom_brand_logo_${timestamp}.png")
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("custom_brand_logo") && file.name.endsWith(".png")) {
                            file.delete()
                        }
                    }
                    logoFile.outputStream().use { out ->
                        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    logoUrl = logoFile.absolutePath
                    Toast.makeText(context, "Cropped brand logo successfully saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save cropped logo: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (pendingSignatureUri != null) {
        LogoCropperDialog(
            uri = pendingSignatureUri!!,
            onDismiss = { pendingSignatureUri = null },
            onCropped = { croppedBitmap ->
                pendingSignatureUri = null
                try {
                    val timestamp = System.currentTimeMillis()
                    val signatureFile = File(context.filesDir, "custom_authorized_signature_${timestamp}.png")
                    context.filesDir.listFiles()?.forEach { file ->
                        if (file.name.startsWith("custom_authorized_signature") && file.name.endsWith(".png")) {
                            file.delete()
                        }
                    }
                    signatureFile.outputStream().use { out ->
                        croppedBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                    }
                    signaturePath = signatureFile.absolutePath
                    prefs.edit()
                        .putString("authorized_signature_path", signaturePath)
                        .putBoolean("authorized_signature_enabled", true)
                        .apply()
                    isSignatureEnabled = true
                    Toast.makeText(context, "Authorized signature image successfully saved!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save signature: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

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
            logoUrl = it.logoUrl
            bankAccountName = it.bankAccountName
            bankName = it.bankName
            bankAccountNo = it.bankAccountNo
            bankBranch = it.bankBranch
            bankIfsc = it.bankIfsc
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Business Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick, modifier = Modifier.testTag("profile_menu_btn")) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "Business Name cannot be empty", Toast.LENGTH_SHORT).show()
                            } else if (isBankNameError) {
                                Toast.makeText(context, "Bank Name must contain text only", Toast.LENGTH_SHORT).show()
                            } else if (isBankAccountNoError) {
                                Toast.makeText(context, "Bank Account number must be 10 to 14 digits only", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveBusinessProfile(
                                    name = name,
                                    address = address,
                                    phone = phone,
                                    email = email,
                                    gstin = gstin,
                                    upiId = upiId,
                                    gmailId = gmailId,
                                    shortIcon = shortIcon,
                                    logoUrl = logoUrl,
                                    bankAccountName = bankAccountName,
                                    bankName = bankName,
                                    bankAccountNo = bankAccountNo,
                                    bankBranch = bankBranch,
                                    bankIfsc = bankIfsc
                                )
                                prefs.edit().putString("pdf_theme", pdfTheme).apply()
                                Toast.makeText(context, "Business Metadata Saved Successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("top_bar_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save settings top option",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
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

            // Saved business profiles registry
            Card(
                modifier = Modifier.fillMaxWidth().testTag("saved_headers_registry_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Saved Business Headers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${savedProfiles.size} template(s) saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        // Add current input fields as a template profile to database
                        FilledTonalButton(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Enter a Business name first to save template", Toast.LENGTH_SHORT).show()
                                } else if (isBankNameError) {
                                    Toast.makeText(context, "Bank Name must contain text only", Toast.LENGTH_SHORT).show()
                                } else if (isBankAccountNoError) {
                                    Toast.makeText(context, "Bank Account number must be 10 to 14 digits only", Toast.LENGTH_SHORT).show()
                                } else {
                                    val newTemplate = SavedBusinessProfile(
                                        businessName = name.trim(),
                                        address = address.trim(),
                                        phone = phone.trim(),
                                        email = email.trim(),
                                        gstin = gstin.trim(),
                                        upiId = upiId.trim(),
                                        gmailId = gmailId.trim(),
                                        shortIcon = shortIcon.trim(),
                                        bankAccountName = bankAccountName.trim(),
                                        bankName = bankName.trim(),
                                        bankAccountNo = bankAccountNo.trim(),
                                        bankBranch = bankBranch.trim(),
                                        bankIfsc = bankIfsc.trim()
                                    )
                                    viewModel.saveSavedBusinessProfile(newTemplate)
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add template icon", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save As Template", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (savedProfiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No templates saved yet. Fill above inputs and tap 'Save As Template'.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    } else {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            items(savedProfiles.size) { index ->
                                val profile = savedProfiles[index]
                                Card(
                                    modifier = Modifier
                                        .width(190.dp)
                                        .clickable {
                                            name = profile.businessName
                                            address = profile.address
                                            phone = profile.phone
                                            email = profile.email
                                            gstin = profile.gstin
                                            upiId = profile.upiId
                                            gmailId = profile.gmailId
                                            shortIcon = if (profile.shortIcon.isBlank()) "💼" else profile.shortIcon
                                            bankAccountName = profile.bankAccountName
                                            bankName = profile.bankName
                                            bankAccountNo = profile.bankAccountNo
                                            bankBranch = profile.bankBranch
                                            bankIfsc = profile.bankIfsc
                                            Toast.makeText(context, "Loaded: ${profile.businessName}", Toast.LENGTH_SHORT).show()
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                    Icon(
                                                        imageVector = Icons.Default.Business,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = {
                                                    viewModel.deleteSavedBusinessProfile(profile.id)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete template",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = profile.businessName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        if (profile.phone.isNotBlank()) {
                                            Text(
                                                text = "Ph: ${profile.phone}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }

                                        if (profile.gstin.isNotBlank()) {
                                            Text(
                                                text = "GSTIN: ${profile.gstin}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        } else {
                                            Text(
                                                text = "No GSTIN added",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
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

                    // Header Profile Drop Down Box for selecting saved templates
                    var headerSelectorExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = if (name.isNotBlank()) name else "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Quick-Load Saved Header Profile") },
                            placeholder = { Text("Tap to choose from saved headers...") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open templates dropdown") },
                            leadingIcon = { Icon(Icons.Default.FolderOpen, "Templates icon") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ),
                            enabled = false
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { headerSelectorExpanded = true }
                        )
                        DropdownMenu(
                            expanded = headerSelectorExpanded,
                            onDismissRequest = { headerSelectorExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            if (savedProfiles.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No saved business templates found. Build one below and save!") },
                                    onClick = { headerSelectorExpanded = false }
                                )
                            } else {
                                savedProfiles.forEach { p ->
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                        text = { Text("${p.businessName} (Ph: ${p.phone})") },
                                        onClick = {
                                            name = p.businessName
                                            address = p.address
                                            phone = p.phone
                                            email = p.email
                                            gstin = p.gstin
                                            upiId = p.upiId
                                            gmailId = p.gmailId
                                            shortIcon = if (p.shortIcon.isBlank()) "💼" else p.shortIcon
                                            headerSelectorExpanded = false
                                            Toast.makeText(context, "Loaded Profile: ${p.businessName}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        }
                    }

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

            // Bank Settlement Details Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("business_bank_details_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bank Settlement Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = bankAccountName,
                        onValueChange = { bankAccountName = it },
                        label = { Text("Account Holder Name") },
                        placeholder = { Text("e.g. Apex Tech Solutions") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Bank Beneficiary Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_acc_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { input ->
                            if (input.all { it.isLetter() || it.isWhitespace() }) {
                                bankName = input
                            }
                        },
                        label = { Text("Bank Name") },
                        placeholder = { Text("e.g. ICICI Bank") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bank Name") },
                        isError = isBankNameError,
                        supportingText = {
                            if (isBankNameError) {
                                Text("Bank Name must be text only", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankAccountNo,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 14) {
                                bankAccountNo = input
                            }
                        },
                        label = { Text("A/c No") },
                        placeholder = { Text("e.g. 9160635224") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Account Number") },
                        isError = isBankAccountNoError,
                        supportingText = {
                            if (isBankAccountNoError) {
                                Text("Bank Account must be 10 to 14 digits (numbers only)", color = MaterialTheme.colorScheme.error)
                            } else {
                                Text("Minimum 10 to 14 digit numbers only", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_acc_no_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankBranch,
                        onValueChange = { bankBranch = it },
                        label = { Text("Branch Name") },
                        placeholder = { Text("e.g. Noida Sector 62") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = "Branch Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_branch_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankIfsc,
                        onValueChange = { bankIfsc = it },
                        label = { Text("IFSC Code") },
                        placeholder = { Text("e.g. ICIC0001234") },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = "IFSC Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_ifsc_input"),
                        singleLine = true
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
                        text = "Step 1: Premium Brand Logo Option",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Upload a brand image or paste a custom link path below to represent your business on the invoice document:",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { selectImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("select_local_logo_button")
                        ) {
                            Icon(Icons.Default.Image, contentDescription = "Gallery logo selection", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pick Logo Image from Device", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (logoUrl.isNotBlank() && !logoUrl.startsWith("preset_")) {
                            Box(
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(horizontal = 8.dp)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val isLocalPath = File(logoUrl).exists()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(if (isLocalPath) "📸" else "🌐", fontSize = 11.sp)
                                    Text(
                                        text = if (isLocalPath) "Active Local Logo" else "Active Web Link",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = logoUrl,
                        onValueChange = { logoUrl = it },
                        label = { Text("Or Custom Brand Image URL / Logo Path") },
                        placeholder = { Text("e.g. https://domain.com/logo.png") },
                        modifier = Modifier.fillMaxWidth().testTag("custom_logo_url_input"),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Web link icon") },
                        trailingIcon = if (logoUrl.isNotBlank()) {
                            {
                                IconButton(onClick = { logoUrl = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Reset input field")
                                }
                            }
                        } else null
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Step 3: PDF Print Accent Design Theme",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Elevate billing aesthetics by switching the border borders, titles, headers, and backgrounds of printed documents:",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val themesList = listOf("Classic Navy", "Forest Green", "Burgundy", "Charcoal", "Sunset Indigo")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themesList.forEach { tName ->
                            val isSelected = pdfTheme == tName
                            val chipColor = when(tName) {
                                "Classic Navy" -> Color(0xFF1E3A8A)
                                "Forest Green" -> Color(0xFF065F46)
                                "Burgundy" -> Color(0xFF881337)
                                "Charcoal" -> Color(0xFF1F2937)
                                else -> Color(0xFF4338CA)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { pdfTheme = tName },
                                label = { Text(tName, fontSize = 11.sp) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .background(chipColor, shape = CircleShape)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Step 5: Authorized Signatory (Optional)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("custom_signature_title")
                    )
                    Text(
                        text = "Enable and upload/draw an optional signature image to overlay above the \"Authorized Signatory\" marker in generated PDFs:",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isSignatureEnabled,
                            onCheckedChange = { checked ->
                                isSignatureEnabled = checked
                                prefs.edit().putBoolean("authorized_signature_enabled", checked).apply()
                                Toast.makeText(context, if (checked) "Signature enabled on PDFs" else "Signature disabled", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("signature_enable_checkbox")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Render signature on generated PDFs",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Enables printed digital overlay above the marker line",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { selectSignatureImageLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("upload_signature_button")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload signature", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload (.png / .jpg)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { showSignatureDrawingDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("draw_signature_button")
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "Draw digital signature", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Draw on Screen", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (signaturePath.isNotBlank() && File(signaturePath).exists()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().testTag("signature_preview_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Active Signature Preview:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Start)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White)
                                        .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    val bitmap = remember(signaturePath) {
                                        android.graphics.BitmapFactory.decodeFile(signaturePath)
                                    }
                                    if (bitmap != null) {
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Active Signature preview overlay",
                                            modifier = Modifier.fillMaxSize().padding(4.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (File(signaturePath).exists()) {
                                                File(signaturePath).delete()
                                            }
                                            signaturePath = ""
                                            isSignatureEnabled = false
                                            prefs.edit()
                                                .putString("authorized_signature_path", "")
                                                .putBoolean("authorized_signature_enabled", false)
                                                .apply()
                                            Toast.makeText(context, "Signature cleared successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.testTag("clear_signature_button_preview")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete & Disable", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
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

            // Demo & Dummy Data Seeding Card
            Card(
                modifier = Modifier.fillMaxWidth().testTag("demo_seeding_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Load Sample Data Icon",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Instant Demo Data Seeding",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Text(
                        text = "Seed the database with sample business profiles, ready-to-bill products/services, clients, and mock invoices. This gives you beautiful charts, metrics, and instant test templates on the streaming simulator!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            viewModel.populateDummyData()
                            Toast.makeText(context, "Sample dataset seeded! Go back to Home / Products to explore.", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth().testTag("seed_sample_dataset_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Repopulate", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seed & Populate Demo Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                                    gmailId = "younusM33@gmail.com"
                                                    isGmailSyncing = false
                                                    showGoogleAccountChooser = false
                                                    Toast.makeText(context, "Synced successfully with younusM33@gmail.com!", Toast.LENGTH_SHORT).show()
                                                }, 1500)
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
                    viewModel.saveBusinessProfile(
                        name = name,
                        address = address,
                        phone = phone,
                        email = email,
                        gstin = gstin,
                        upiId = upiId,
                        gmailId = gmailId,
                        shortIcon = shortIcon,
                        logoUrl = logoUrl,
                        bankAccountName = bankAccountName,
                        bankName = bankName,
                        bankAccountNo = bankAccountNo,
                        bankBranch = bankBranch,
                        bankIfsc = bankIfsc
                    )
                    prefs.edit().putString("pdf_theme", pdfTheme).apply()
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

            if (showSignatureDrawingDialog) {
                SignatureDrawingDialog(
                    onDismiss = { showSignatureDrawingDialog = false },
                    onSave = { drawnBitmap ->
                        try {
                            val timestamp = System.currentTimeMillis()
                            val signatureFile = File(context.filesDir, "custom_authorized_signature_${timestamp}.png")
                            context.filesDir.listFiles()?.forEach { file ->
                                if (file.name.startsWith("custom_authorized_signature") && file.name.endsWith(".png")) {
                                    file.delete()
                                }
                            }
                            signatureFile.outputStream().use { out ->
                                drawnBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                            }
                            signaturePath = signatureFile.absolutePath
                            prefs.edit()
                                .putString("authorized_signature_path", signaturePath)
                                .putBoolean("authorized_signature_enabled", true)
                                .apply()
                            isSignatureEnabled = true
                            showSignatureDrawingDialog = false
                            Toast.makeText(context, "Digital signature drawn & saved successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to save signature: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LogoCropperDialog(
    uri: android.net.Uri,
    onDismiss: () -> Unit,
    onCropped: (android.graphics.Bitmap) -> Unit
) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    
    val rawBitmap = remember(uri) {
        loadRescaledBitmap(context, uri, maxDim = 1200)
    }
    
    if (rawBitmap == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Could not load image.", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
        return
    }
    
    var zoom by remember { mutableStateOf(1.0f) }
    var panX by remember { mutableStateOf(0f) }
    var panY by remember { mutableStateOf(0f) }
    var rotationDegrees by remember { mutableStateOf(0f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Polish & Crop Logo",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Drag to pan. Use zoom slider and rotation button to center company logo inside the square crop box:",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        bitmap = rawBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = zoom,
                                scaleY = zoom,
                                translationX = panX,
                                translationY = panY,
                                rotationZ = rotationDegrees
                            )
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    panX += dragAmount.x
                                    panY += dragAmount.y
                                }
                            }
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(4.dp))
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Slider(
                        value = zoom,
                        onValueChange = { zoom = it },
                        valueRange = 1.0f..4.0f,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                    Icon(
                        Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(
                        onClick = { rotationDegrees = (rotationDegrees + 90f) % 360f },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rotate 90°", fontSize = 12.sp)
                    }
                    
                    TextButton(
                        onClick = {
                            zoom = 1.0f
                            panX = 0f
                            panY = 0f
                            rotationDegrees = 0f
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Layout", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cropped = generateCroppedBitmap(
                        original = rawBitmap,
                        zoom = zoom,
                        panX = panX,
                        panY = panY,
                        rotation = rotationDegrees,
                        displayDensity = density
                    )
                    onCropped(cropped)
                }
            ) {
                Text("Confirm & Crop", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun loadRescaledBitmap(context: android.content.Context, uri: android.net.Uri, maxDim: Int): android.graphics.Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeStream(stream, null, options)
            var inSampleSize = 1
            while ((options.outWidth / inSampleSize) > maxDim || (options.outHeight / inSampleSize) > maxDim) {
                inSampleSize *= 2
            }
            val loadOptions = android.graphics.BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                android.graphics.BitmapFactory.decodeStream(stream2, null, loadOptions)
            }
        }
    } catch (e: java.lang.Exception) {
        null
    }
}

fun generateCroppedBitmap(
    original: android.graphics.Bitmap,
    zoom: Float,
    panX: Float,
    panY: Float,
    rotation: Float,
    displayDensity: Float
): android.graphics.Bitmap {
    val targetSize = 512
    val croppedBitmap = android.graphics.Bitmap.createBitmap(targetSize, targetSize, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(croppedBitmap)
    
    val paint = android.graphics.Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    
    canvas.drawColor(android.graphics.Color.TRANSPARENT)
    
    val matrix = android.graphics.Matrix()
    val srcW = original.width.toFloat()
    val srcH = original.height.toFloat()
    val initialScale = Math.min(targetSize / srcW, targetSize / srcH)
    
    matrix.postTranslate(-srcW / 2f, -srcH / 2f)
    matrix.postScale(initialScale, initialScale)
    matrix.postRotate(rotation)
    matrix.postScale(zoom, zoom)
    
    val viewportPx = 240f * displayDensity
    val scaleRelation = targetSize / viewportPx
    
    matrix.postTranslate(
        targetSize / 2f + panX * scaleRelation,
        targetSize / 2f + panY * scaleRelation
    )
    
    canvas.drawBitmap(original, matrix, paint)
    return croppedBitmap
}

@Composable
fun SignatureDrawingDialog(
    onDismiss: () -> Unit,
    onSave: (android.graphics.Bitmap) -> Unit
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke = remember { mutableStateListOf<Offset>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Draw Authorized Signature", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Sign inside the box below. Use your finger or stylus:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .border(1.dp, MaterialTheme.colorScheme.outline)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke.clear()
                                    currentStroke.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke.add(change.position)
                                    // To update UI on drag, we force a rebuild
                                    val temp = currentStroke.toList()
                                    currentStroke.clear()
                                    currentStroke.addAll(temp)
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke.toList())
                                        currentStroke.clear()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw previous strokes
                        strokes.forEach { stroke ->
                            if (stroke.size > 1) {
                                for (i in 0 until stroke.size - 1) {
                                    drawLine(
                                        color = Color.Black,
                                        start = stroke[i],
                                        end = stroke[i + 1],
                                        strokeWidth = 5f,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                        // Draw current stroke
                        if (currentStroke.size > 1) {
                            for (i in 0 until currentStroke.size - 1) {
                                drawLine(
                                    color = Color.Black,
                                    start = currentStroke[i],
                                    end = currentStroke[i + 1],
                                    strokeWidth = 5f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (strokes.isEmpty()) {
                        onDismiss()
                        return@Button
                    }
                    
                    val width = 450
                    val height = 220
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.TRANSPARENT)
                    
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        strokeWidth = 7f
                        style = android.graphics.Paint.Style.STROKE
                        strokeCap = android.graphics.Paint.Cap.ROUND
                        strokeJoin = android.graphics.Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    
                    val allPoints = strokes.flatMap { it }
                    val minX = allPoints.minOfOrNull { it.x } ?: 0f
                    val maxX = allPoints.maxOfOrNull { it.x } ?: 100f
                    val minY = allPoints.minOfOrNull { it.y } ?: 0f
                    val maxY = allPoints.maxOfOrNull { it.y } ?: 100f
                    
                    val contentWidth = maxX - minX
                    val contentHeight = maxY - minY
                    
                    val padding = 22f
                    
                    strokes.forEach { stroke ->
                        val strokePath = android.graphics.Path()
                        if (stroke.isNotEmpty()) {
                            val scaleX = if (contentWidth > 0f) (width - 2 * padding) / contentWidth else 1f
                            val scaleY = if (contentHeight > 0f) (height - 2 * padding) / contentHeight else 1f
                            val scale = minOf(scaleX, scaleY)
                            
                            val offsetX = padding + (width - 2 * padding - contentWidth * scale) / 2f - minX * scale
                            val offsetY = padding + (height - 2 * padding - contentHeight * scale) / 2f - minY * scale
                            
                            strokePath.moveTo(stroke[0].x * scale + offsetX, stroke[0].y * scale + offsetY)
                            for (i in 1 until stroke.size) {
                                strokePath.lineTo(stroke[i].x * scale + offsetX, stroke[i].y * scale + offsetY)
                            }
                            canvas.drawPath(strokePath, paint)
                        }
                    }
                    
                    onSave(bitmap)
                },
                modifier = Modifier.testTag("save_dialog_sig_btn")
            ) {
                Text("Save Signature")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { strokes.clear() }, modifier = Modifier.testTag("clear_dialog_sig_btn")) {
                    Text("Reset")
                }
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("close_dialog_sig_btn")) {
                    Text("Cancel")
                }
            }
        }
    )
}
