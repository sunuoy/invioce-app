package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File
import com.example.util.BackupRestoreHelper
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.foundation.text.KeyboardOptions
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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val userAccounts by viewModel.allUserAccounts.collectAsStateWithLifecycle()

    // Google Drive States
    val gdAccessToken by viewModel.googleDriveAccessToken.collectAsStateWithLifecycle()
    val gdLastSyncTime by viewModel.googleDriveLastSyncTime.collectAsStateWithLifecycle()
    val gdSyncing by viewModel.isGoogleDriveSyncing.collectAsStateWithLifecycle()
    val gdSyncMode by viewModel.googleDriveSyncMode.collectAsStateWithLifecycle()
    val gdAccountEmail by viewModel.googleDriveAccountEmail.collectAsStateWithLifecycle()

    val googleAccountPickerLauncherForDrive = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                val accountName = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
                if (!accountName.isNullOrEmpty()) {
                    viewModel.fetchGoogleDriveTokenAutomatically(context, accountName)
                }
            }
        }
    }

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
        bankAccountNo.isNotEmpty() && (!bankAccountNo.all { it.isDigit() } || bankAccountNo.length < 9 || bankAccountNo.length > 16)
    }

    val isPhoneError = remember(phone) {
        phone.isNotEmpty() && (phone.filter { it.isDigit() }.length < 10 || phone.filter { it.isDigit() }.length > 13)
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
    var showTaxSummary by remember { mutableStateOf(prefs.getBoolean("show_tax_summary", true)) }
    var showSalesTrend by remember { mutableStateOf(prefs.getBoolean("show_sales_trend", true)) }

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

    var isProfilePrefilled by remember { mutableStateOf(false) }

    // Prefill fields when profile in DB is loaded
    LaunchedEffect(businessProfile) {
        if (!isProfilePrefilled && businessProfile != null) {
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
            isProfilePrefilled = true
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
                            } else if (isPhoneError) {
                                Toast.makeText(context, "Phone number must be 10 to 13 digits only", Toast.LENGTH_SHORT).show()
                            } else if (isBankNameError) {
                                Toast.makeText(context, "Bank Name must contain text only", Toast.LENGTH_SHORT).show()
                            } else if (isBankAccountNoError) {
                                Toast.makeText(context, "Bank Account number must be 9 to 16 digits only", Toast.LENGTH_SHORT).show()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF6366F1).copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .drawBehind {
                    // Orb 1 (Top Left) - Indigo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f)
                    )
                    // Orb 2 (Bottom Right) - Hot Pink
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f),
                            radius = size.maxDimension * 0.4f
                        ),
                        radius = size.maxDimension * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
                    )
                    // Orb 3 (Center Right) - Amber
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.45f),
                            radius = size.maxDimension * 0.35f
                        ),
                        radius = size.maxDimension * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.45f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // User Session Profile Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .floating3D(rotationX = 2.5f, rotationY = -3f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val avatarChar = currentUser?.username?.firstOrNull()?.uppercase() ?: "?"
                            val avatarBgColor = if (currentUser?.role == "Admin") Color(0xFF6366F1) else Color(0xFF0D9488)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(avatarBgColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = avatarChar,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Column {
                                Text(
                                    text = currentUser?.username ?: "Guest User",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (currentUser?.role == "Admin") Color(0xFF6366F1).copy(alpha = 0.15f) else Color(0xFF0D9488).copy(alpha = 0.15f),
                                    contentColor = if (currentUser?.role == "Admin") Color(0xFF6366F1) else Color(0xFF0D9488)
                                ) {
                                    Text(
                                        text = currentUser?.role ?: "User",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("logout_button")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout icon", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Logout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (currentUser?.role == "Admin") {
                    // Visual Banner introduction Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .floating3D(rotationX = 2.5f, rotationY = -3f)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF3B82F6).copy(alpha = 0.05f),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                        )
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(4.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_headers_registry_card")
                    .floating3D(rotationX = 2.5f, rotationY = -3f)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.03f),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(4.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
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
                                    Toast.makeText(context, "Bank Account number must be 9 to 16 digits only", Toast.LENGTH_SHORT).show()
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
                modifier = Modifier
                    .fillMaxWidth()
                    .floating3D(rotationX = 2.5f, rotationY = -3f)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF6366F1).copy(alpha = 0.03f),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
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
                        label = { Text("Phone") },
                        placeholder = { Text("e.g. 9876543210") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Business Contact number", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isPhoneError,
                        supportingText = {
                            if (isPhoneError) {
                                Text("Phone number must be 10 to 13 digits only")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        placeholder = { Text("e.g. invoice...") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Contact email", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = gmailId,
                        onValueChange = { gmailId = it },
                        label = { Text("Backup Gmail ID") },
                        placeholder = { Text("e.g. business@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Backup Gmail ID", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier.fillMaxWidth().testTag("setting_biz_gmail_input"),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    OutlinedTextField(
                        value = gstin,
                        onValueChange = { gstin = it },
                        label = { Text("GSTIN") },
                        placeholder = { Text("e.g. 07...") },
                        leadingIcon = { Icon(Icons.Default.AssignmentInd, contentDescription = "GST Details identification", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_biz_tax_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = { Text("UPI ID") },
                        placeholder = { Text("e.g. apex...") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = "UPI payments identifier", modifier = Modifier.size(20.dp)) },
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
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank Name") },
                        placeholder = { Text("e.g. ICICI") },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = "Bank Name", modifier = Modifier.size(20.dp)) },
                        isError = isBankNameError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankAccountName,
                        onValueChange = { bankAccountName = it },
                        label = { Text("Holder Name") },
                        placeholder = { Text("e.g. Apex Tech") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Bank Beneficiary Name", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_acc_name_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankAccountNo,
                        onValueChange = { bankAccountNo = it },
                        label = { Text("A/c No") },
                        placeholder = { Text("e.g. 12345...") },
                        leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = "Account Number", modifier = Modifier.size(20.dp)) },
                        isError = isBankAccountNoError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_acc_no_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = bankIfsc,
                        onValueChange = { bankIfsc = it },
                        label = { Text("IFSC Code") },
                        placeholder = { Text("e.g. ICIC0001234") },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = "IFSC Code", modifier = Modifier.size(20.dp)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setting_bank_ifsc_input"),
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
                        text = "Step 2: PDF Print Accent Design Theme",
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

                    val themesList = listOf("Classic Navy", "Forest Green", "Burgundy", "Charcoal", "Sunset Indigo", "Burnt sienna", "Blooming romance")
                    
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
                                "Burnt sienna" -> Color(0xFFE35336)
                                "Blooming romance" -> Color(0xFF660033)
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
                        text = "Step 3: Authorized Signatory (Optional)",
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

            // Dashboard Configuration Preferences Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth().testTag("dashboard_settings_card")
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
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Dashboard settings icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Dashboard Configuration",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Toggle visibility of elements displayed on the main report overview and dashboard home page:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GST Fiscal Tax Summary",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Show/Hide interactive tax calculation widgets on dashboard.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showTaxSummary,
                            onCheckedChange = { isChecked ->
                                showTaxSummary = isChecked
                                prefs.edit().putBoolean("show_tax_summary", isChecked).apply()
                                Toast.makeText(context, if (isChecked) "GST Fiscal Tax Summary Enabled!" else "GST Fiscal Tax Summary Disabled!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("tax_summary_toggle_settings")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sales Trend Projection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Show/Hide revenue projection charts on dashboard home page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = showSalesTrend,
                            onCheckedChange = { isChecked ->
                                showSalesTrend = isChecked
                                prefs.edit().putBoolean("show_sales_trend", isChecked).apply()
                                Toast.makeText(context, if (isChecked) "Sales Trend Projection Enabled!" else "Sales Trend Projection Disabled!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("sales_trend_toggle_settings")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Google Drive Cloud Sync & Backup Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floating3D(rotationX = 2.5f, rotationY = -3f)
                    .drawBehind {
                        drawCircle(
                            color = Color(0xFF3B82F6).copy(alpha = 0.03f),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(4.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
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
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud backup",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Google Drive Cloud Backup",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Auto-save your business profile, stock items, customer data, and invoices to your personal Google Drive account in the background.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gdAccessToken.isNotEmpty()) {
                                if (gdAccountEmail.isNotEmpty()) "Status: Connected to $gdAccountEmail" else "Status: Connected (Auto-sync Active)"
                            } else "Status: Disconnected",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (gdAccessToken.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )

                        if (gdAccessToken.isNotEmpty()) {
                            TextButton(
                                onClick = { viewModel.disableGoogleDriveSync() },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Disconnect", fontSize = 11.sp)
                            }
                        }
                    }

                    Text(
                        text = "Last Synced: $gdLastSyncTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (gdAccessToken.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Sync Frequency",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("auto" to "Automatic", "hourly" to "Hourly", "manual" to "Manual").forEach { (value, label) ->
                                val selected = gdSyncMode == value
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setGoogleDriveSyncMode(value) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (gdAccessToken.isEmpty()) {
                        Button(
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            onClick = {
                                val intent = android.accounts.AccountManager.newChooseAccountIntent(
                                    null, null, arrayOf("com.google"), null, null, null, null
                                )
                                googleAccountPickerLauncherForDrive.launch(intent)
                            }
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = "Connect Google Drive", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect Google Drive", fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !gdSyncing,
                                onClick = {
                                    viewModel.backupToGoogleDrive { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            ) {
                                if (gdSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup now", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now", fontSize = 11.sp)
                                }
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f).height(42.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !gdSyncing,
                                onClick = {
                                    viewModel.restoreFromGoogleDrive { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            ) {
                                if (gdSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudDownload, contentDescription = "Restore now", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore Now", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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

            // Save Profile Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Business Name cannot be empty", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isPhoneError) {
                        Toast.makeText(context, "Phone number must be 10 to 13 digits only", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isBankNameError) {
                        Toast.makeText(context, "Bank Name must contain text only", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isBankAccountNoError) {
                        Toast.makeText(context, "Bank Account number must be 9 to 16 digits only", Toast.LENGTH_SHORT).show()
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

            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .floating3D(rotationX = 2.5f, rotationY = -3f)
                    .testTag("admin_user_management_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(4.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.4f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    )
                )
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
                            imageVector = Icons.Default.People,
                            contentDescription = "User Accounts Management",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "User Accounts Management",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Create and manage passcode-protected accounts for this device. Users are restricted from administrative controls and deletion capabilities.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Form to add a user
                    var newUsername by remember { mutableStateOf("") }
                    var newPasscode by remember { mutableStateOf("") }
                    var newRole by remember { mutableStateOf("User") } // Default to "User"

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username") },
                        placeholder = { Text("e.g. billing_staff") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_username_input")
                    )

                    OutlinedTextField(
                        value = newPasscode,
                        onValueChange = { newPasscode = it },
                        label = { Text("Passcode") },
                        placeholder = { Text("e.g. 5678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("new_passcode_input")
                    )

                    // Role Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Role: ", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        listOf("Admin", "User").forEach { roleOption ->
                            val isSelected = newRole == roleOption
                            InputChip(
                                selected = isSelected,
                                onClick = { newRole = roleOption },
                                label = { Text(roleOption) }
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newUsername.isBlank() || newPasscode.isBlank()) {
                                Toast.makeText(context, "Username and Passcode are required!", Toast.LENGTH_SHORT).show()
                            } else if (userAccounts.any { it.username.equals(newUsername.trim(), ignoreCase = true) }) {
                                Toast.makeText(context, "Username already exists!", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.createUserAccount(newUsername.trim(), newPasscode.trim(), newRole)
                                newUsername = ""
                                newPasscode = ""
                                Toast.makeText(context, "Account successfully created!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(38.dp).testTag("create_account_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create User Account", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Registered Accounts (${userAccounts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        userAccounts.forEach { account ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(if (account.role == "Admin") Color(0xFF6366F1) else Color(0xFF0D9488), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (account.username.firstOrNull()?.uppercase() ?: "?"),
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Column {
                                        Text(account.username, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        Text("Role: ${account.role}", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }

                                // Do not allow deleting themselves
                                if (currentUser?.id != account.id) {
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteUserAccount(account.id)
                                            Toast.makeText(context, "Account deleted!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp).testTag("delete_account_btn_${account.username}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete account",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
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

@Composable
fun ThemeMiniPreview(themeName: String) {
    val primaryColor = when(themeName) {
        "Classic Navy" -> Color(0xFF1E3A8A)
        "Forest Green" -> Color(0xFF065F46)
        "Burgundy" -> Color(0xFF881337)
        "Burnt sienna" -> Color(0xFFE35336)
        "Modern Minimalist" -> Color(0xFF0F172A)
        "Royal Violet" -> Color(0xFF4C1D95)
        else -> Color(0xFF1E3A8A)
    }
    val secondaryColor = when(themeName) {
        "Classic Navy" -> Color(0xFF2563EB)
        "Forest Green" -> Color(0xFF10B981)
        "Burgundy" -> Color(0xFFE11D48)
        "Burnt sienna" -> Color(0xFFF4A460)
        "Modern Minimalist" -> Color(0xFF64748B)
        "Royal Violet" -> Color(0xFFA78BFA)
        else -> Color(0xFF2563EB)
    }
    val themeBg = when(themeName) {
        "Classic Navy" -> Color(0xFFF1F5F9)
        "Forest Green" -> Color(0xFFF0FDF4)
        "Burgundy" -> Color(0xFFFFF1F2)
        "Burnt sienna" -> Color(0xFFF5F5DC)
        "Modern Minimalist" -> Color(0xFFF1F5F9)
        "Royal Violet" -> Color(0xFFF5F3FF)
        else -> Color(0xFFF1F5F9)
    }
    val dividerColor = when(themeName) {
        "Classic Navy" -> Color(0xFF94A3B8)
        "Forest Green" -> Color(0xFF059669)
        "Burgundy" -> Color(0xFFFDA4AF)
        "Burnt sienna" -> Color(0xFFA0522D)
        "Modern Minimalist" -> Color(0xFFCBD5E1)
        "Royal Violet" -> Color(0xFF7C3AED)
        else -> Color(0xFF94A3B8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, dividerColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Mini Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(primaryColor, RoundedCornerShape(4.dp))
            )
            
            // Sender & Receiver block
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(secondaryColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.width(80.dp).height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.width(40.dp).height(8.dp).background(secondaryColor.copy(alpha = 0.7f), RoundedCornerShape(2.dp)))
                    Box(modifier = Modifier.width(50.dp).height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)))
                }
            }
            
            // Table Mock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(0.5.dp, dividerColor.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
            ) {
                // Table header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(primaryColor)
                )
                // Row 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(themeBg)
                )
                // Row 2
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .background(MaterialTheme.colorScheme.surface)
                )
            }
            
            // Grand Total Mock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .background(themeBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(20.dp).height(6.dp).background(primaryColor.copy(alpha = 0.7f), RoundedCornerShape(1.dp)))
                    Box(modifier = Modifier.width(30.dp).height(6.dp).background(primaryColor, RoundedCornerShape(1.dp)))
                }
            }
        }
    }
}
