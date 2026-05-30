package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }

    // Prefill fields when profile in DB is loaded
    LaunchedEffect(businessProfile) {
        businessProfile?.let {
            name = it.businessName
            address = it.address
            phone = it.phone
            email = it.email
            gstin = it.gstin
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
                    .fillModifier()
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
                    viewModel.saveBusinessProfile(name, address, phone, email, gstin)
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

// Extension to avoid compilation issues in case of height/width modifiers
private fun Modifier.fillModifier(): Modifier = this
