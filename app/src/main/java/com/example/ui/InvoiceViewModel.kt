package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.BackupRestoreHelper
import com.example.util.UpdateHelper
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

@OptIn(kotlinx.coroutines.FlowPreview::class)
class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = InvoiceDatabase.getDatabase(application)
    private val repository = InvoiceRepository(
        database.invoiceDao(),
        database.productDao(),
        database.customerDao(),
        database.businessProfileDao(),
        database.savedBusinessProfileDao()
    )

    private val prefs = application.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)

    init {
        SupabaseClientManager.initialize(application)
    }

    // Low stock threshold
    private val _lowStockThreshold = MutableStateFlow(prefs.getFloat("low_stock_threshold", 5.0f))
    val lowStockThreshold: StateFlow<Float> = _lowStockThreshold

    fun updateLowStockThreshold(value: Float) {
        viewModelScope.launch {
            prefs.edit().putFloat("low_stock_threshold", value).apply()
            _lowStockThreshold.value = value
        }
    }

    // Update checker state
    private val _updateInfo = MutableStateFlow<UpdateHelper.UpdateInfo?>(null)
    val updateInfo: StateFlow<UpdateHelper.UpdateInfo?> = _updateInfo

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates

    fun checkForUpdates(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _isCheckingForUpdates.value = true
            val info = UpdateHelper.checkForUpdates()
            _updateInfo.value = info
            if (!silent) {
                _isCheckingForUpdates.value = false
                if (info != null && !info.isUpdateAvailable) {
                    _uiEvents.emit(UiEvent.ShowSuccess("You are on the latest version!"))
                } else if (info == null) {
                    _uiEvents.emit(UiEvent.ShowError("Failed to check for updates. Check your connection."))
                }
            }
        }
    }

    // Google Drive Sync states
    private val _googleDriveSyncEnabled = MutableStateFlow(false)
    val googleDriveSyncEnabled: StateFlow<Boolean> = _googleDriveSyncEnabled.asStateFlow()

    private val _isGoogleDriveSyncing = MutableStateFlow(false)
    val isGoogleDriveSyncing: StateFlow<Boolean> = _isGoogleDriveSyncing.asStateFlow()

    private val _googleDriveAccessToken = MutableStateFlow("")
    val googleDriveAccessToken: StateFlow<String> = _googleDriveAccessToken.asStateFlow()

    private val _googleDriveLastSyncTime = MutableStateFlow("Never")
    val googleDriveLastSyncTime: StateFlow<String> = _googleDriveLastSyncTime.asStateFlow()

    private val _googleDriveSyncMode = MutableStateFlow(prefs.getString("gd_sync_mode", "auto") ?: "auto")
    val googleDriveSyncMode: StateFlow<String> = _googleDriveSyncMode.asStateFlow()

    private val _googleDriveAccountEmail = MutableStateFlow(prefs.getString("gd_account_email", "") ?: "")
    val googleDriveAccountEmail: StateFlow<String> = _googleDriveAccountEmail.asStateFlow()

    fun setGoogleDriveSyncMode(mode: String) {
        val profileName = businessProfile.value?.businessName ?: "default"
        prefs.edit().putString("gd_sync_mode_$profileName", mode).apply()
        _googleDriveSyncMode.value = mode
    }

    // Update download states
    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    // Standard business alerts
    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents: SharedFlow<UiEvent> = _uiEvents

    // Reactive State Flows
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceWithDetails>> = repository.allInvoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val businessProfile: StateFlow<BusinessProfile?> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedBusinessProfiles: StateFlow<List<SavedBusinessProfile>> = repository.savedBusinessProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSales: StateFlow<Double?> = repository.totalSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val outstandingAmount: StateFlow<Double?> = repository.outstandingAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        val gdEnabled = prefs.getBoolean("gd_sync_enabled", false)
        val gdToken = prefs.getString("gd_access_token", "") ?: ""
        val gdLastSync = prefs.getString("gd_last_sync_time", "Never") ?: "Never"
        val gdEmail = prefs.getString("gd_account_email", "") ?: ""
        _googleDriveSyncEnabled.value = gdEnabled
        _googleDriveAccessToken.value = gdToken
        _googleDriveLastSyncTime.value = gdLastSync

        viewModelScope.launch {
            checkForUpdates(silent = true)
        }

        // Auto-authenticate when business profile is loaded or on startup if enabled
        viewModelScope.launch {
            if (gdEnabled && gdEmail.isNotEmpty()) {
                fetchGoogleDriveTokenAutomatically(application, gdEmail)
            }
            businessProfile.collect { profile ->
                if (profile != null && profile.gmailId.isNotEmpty()) {
                    fetchGoogleDriveTokenAutomatically(application, profile.gmailId)
                }
            }
        }

        // Synchronize googleDriveSyncMode state with the active profile
        viewModelScope.launch {
            repository.businessProfile.collect { profile ->
                val profileName = profile?.businessName ?: "default"
                _googleDriveSyncMode.value = prefs.getString("gd_sync_mode_$profileName", "auto") ?: "auto"
            }
        }

        // Auto-run Google Drive background sync when data changes and token is configured
        viewModelScope.launch {
            combine(
                repository.allProducts,
                repository.allCustomers,
                repository.allInvoices,
                repository.businessProfile,
                _googleDriveAccessToken
            ) { array: Array<Any?> ->
                array[4] as String
            }
            .debounce(3000)
            .collect { token ->
                val profileName = businessProfile.value?.businessName ?: "default"
                val mode = prefs.getString("gd_sync_mode_$profileName", "auto") ?: "auto"
                if (mode == "manual") {
                    android.util.Log.d("GoogleDriveAutoSync", "Auto-backup skipped: manual mode is active.")
                    return@collect
                }
                if (token.isNotEmpty()) {
                    if (mode == "hourly") {
                        val lastSync = prefs.getLong("gd_last_sync_timestamp_$profileName", 0L)
                        val elapsed = System.currentTimeMillis() - lastSync
                        if (elapsed < 3600 * 1000) {
                            android.util.Log.d("GoogleDriveAutoSync", "Auto-backup skipped: hourly mode active and less than 1 hour elapsed.")
                            return@collect
                        }
                    }
                    backupToGoogleDrive { success, msg ->
                        android.util.Log.d("GoogleDriveAutoSync", "Background auto-backup ($mode): $msg")
                    }
                }
            }
        }
    }

    // Support sequential generation: INV-YYYY-MMM-DD-XXXX with progressive suffix logic per active company
    fun generateNextInvoiceNumber(): String {
        val existingInvoices = invoices.value
        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val dateString = java.text.SimpleDateFormat("yyyy-MMM-dd", java.util.Locale.US).format(calendar.time).uppercase()
        val currentCompany = businessProfile.value?.businessName ?: ""
        
        var maxSuffix = 0
        val regex = Regex("\\d+$")
        for (inv in existingInvoices) {
            // Filter sequence to only include invoices belonging to the active company profile
            if (inv.invoice.businessName != currentCompany) {
                continue
            }
            // Filter to only include invoices created in the current calendar year
            val invCalendar = java.util.Calendar.getInstance().apply { timeInMillis = inv.invoice.dateTimestamp }
            val invYear = invCalendar.get(java.util.Calendar.YEAR)
            if (invYear != currentYear) {
                continue
            }
            
            val numStr = inv.invoice.invoiceNumber
            val matchResult = regex.find(numStr)
            if (matchResult != null) {
                try {
                    val suffixVal = matchResult.value.toInt()
                    if (suffixVal > maxSuffix) {
                        maxSuffix = suffixVal
                    }
                } catch (e: Exception) {
                    // Ignore parsing issues
                }
            }
        }
        val nextSuffix = maxSuffix + 1
        return "INV-$dateString-${String.format(java.util.Locale.US, "%04d", nextSuffix)}"
    }

    // ------------------ BUSINESS OPERATIONS ------------------
    fun saveBusinessProfile(
        name: String,
        address: String,
        phone: String,
        email: String,
        gstin: String,
        upiId: String,
        gmailId: String,
        shortIcon: String,
        logoUrl: String = "",
        bankAccountName: String = "",
        bankName: String = "",
        bankAccountNo: String = "",
        bankBranch: String = "",
        bankIfsc: String = ""
    ) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Business Name cannot be empty"))
                return@launch
            }
            val profile = BusinessProfile(
                id = 1,
                businessName = name.trim(),
                address = address.trim(),
                phone = phone.trim(),
                email = email.trim(),
                gstin = gstin.trim(),
                upiId = upiId.trim(),
                gmailId = gmailId.trim(),
                shortIcon = shortIcon.trim(),
                logoUrl = logoUrl.trim(),
                bankAccountName = bankAccountName.trim(),
                bankName = bankName.trim(),
                bankAccountNo = bankAccountNo.trim(),
                bankBranch = bankBranch.trim(),
                bankIfsc = bankIfsc.trim()
            )
            repository.saveBusinessProfile(profile)
            _uiEvents.emit(UiEvent.ShowSuccess("Business profile updated successfully!"))
        }
    }

    fun saveSavedBusinessProfile(profile: SavedBusinessProfile) {
        viewModelScope.launch {
            if (profile.businessName.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Profile name cannot be empty"))
                return@launch
            }
            repository.saveSavedBusinessProfile(profile)
            _uiEvents.emit(UiEvent.ShowSuccess("Business profile template saved to your list!"))
        }
    }

    fun deleteSavedBusinessProfile(id: Int) {
        viewModelScope.launch {
            repository.deleteSavedBusinessProfile(id)
            _uiEvents.emit(UiEvent.ShowSuccess("Removed from saved list"))
        }
    }

    // ------------------ PRODUCT STOCK OPERATIONS ------------------
    fun saveProduct(id: Int, name: String, price: Double, tax: Double, unit: String, stock: Double, hsnSac: String = "", attachmentPath: String = "") {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Product name cannot be blank"))
                return@launch
            }
            if (price < 0) {
                _uiEvents.emit(UiEvent.ShowError("Price cannot be negative"))
                return@launch
            }
            if (tax < 0 || tax > 100) {
                _uiEvents.emit(UiEvent.ShowError("Tax rate must be between 0% and 100%"))
                return@launch
            }
            if (unit.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Please set a unit (e.g. kg, bags, QT, box)"))
                return@launch
            }

            val product = Product(
                id = id,
                name = name.trim(),
                price = price,
                taxRate = tax,
                unit = unit.trim(),
                stock = stock,
                hsnSac = hsnSac.trim(),
                attachmentPath = attachmentPath.trim()
            )
            repository.insertProduct(product)
            _uiEvents.emit(UiEvent.ShowSuccess("Product stock saved successfully!"))
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
            _uiEvents.emit(UiEvent.ShowSuccess("Product deleted successfully"))
        }
    }

    // ------------------ CUSTOMER OPERATIONS ------------------
    fun saveCustomer(id: Int, name: String, companyName: String, phone: String, email: String, address: String, gstin: String = "", placeOfSupply: String = "", isClosed: Boolean = false) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Customer name cannot be blank"))
                return@launch
            }
            if (phone.isNotBlank()) {
                val digitsCount = phone.filter { it.isDigit() }.length
                if (digitsCount < 10 || digitsCount > 13) {
                    _uiEvents.emit(UiEvent.ShowError("Phone number must contain between 10 to 13 digits"))
                    return@launch
                }
            }
            val customer = Customer(
                id = id,
                name = name.trim(),
                companyName = companyName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                gstin = gstin.trim(),
                placeOfSupply = placeOfSupply.trim(),
                isClosed = isClosed
            )
            repository.insertCustomer(customer)
            _uiEvents.emit(UiEvent.ShowSuccess("Customer details saved!"))
        }
    }

    fun toggleCustomerClosedStatus(customer: Customer, isClosed: Boolean) {
        viewModelScope.launch {
            val updated = customer.copy(isClosed = isClosed)
            repository.insertCustomer(updated)
            val actionText = if (isClosed) "Closed (Inactive)" else "Active"
            _uiEvents.emit(UiEvent.ShowSuccess("Client '${customer.name}' marked as $actionText"))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val hasInvoices = invoices.value.any { it.invoice.customerId == customer.id }
            if (hasInvoices) {
                _uiEvents.emit(UiEvent.ShowError("Client has generated invoices. Cannot be deleted. Please mark them as Closed instead."))
            } else {
                repository.deleteCustomer(customer)
                _uiEvents.emit(UiEvent.ShowSuccess("Customer deleted"))
            }
        }
    }

    // ------------------ INVOICE CALCULATOR & RECORDERS ------------------
    fun saveInvoice(
        id: Int,
        invoiceNumber: String,
        customerId: Int,
        status: String,
        items: List<InvoiceLineItem>,
        notes: String,
        vehicleNumber: String = "",
        brokerageBy: String = "",
        placeOfSupply: String = "",
        dueDateTimestamp: Long = 0L,
        attachmentPath: String = "",
        paymentMethod: String = "",
        paymentNote: String = "",
        paymentAttachmentPath: String = "",
        closeReason: String = ""
    ) {
        viewModelScope.launch {
            if (invoiceNumber.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Invoice number is empty"))
                return@launch
            }
            if (customerId == 0) {
                _uiEvents.emit(UiEvent.ShowError("Please select/add a Customer"))
                return@launch
            }
            if (items.isEmpty()) {
                _uiEvents.emit(UiEvent.ShowError("Invoice must have at least one line item"))
                return@launch
            }

            val existingInv = if (id != 0) invoices.value.find { it.invoice.id == id }?.invoice else null
            val companyNameForInvoice = existingInv?.businessName?.takeIf { it.isNotEmpty() }
                ?: (businessProfile.value?.businessName ?: "")

            val invoice = Invoice(
                id = id,
                invoiceNumber = invoiceNumber.trim(),
                customerId = customerId,
                businessName = companyNameForInvoice,
                attachmentPath = attachmentPath.trim(),
                status = status,
                notes = notes.trim(),
                vehicleNumber = vehicleNumber.trim(),
                brokerageBy = brokerageBy.trim(),
                placeOfSupply = placeOfSupply.trim(),
                dueDateTimestamp = dueDateTimestamp,
                paymentMethod = paymentMethod,
                paymentNote = paymentNote,
                paymentAttachmentPath = paymentAttachmentPath,
                closeReason = closeReason
            )

            val invoiceId = repository.saveInvoice(invoice, items)
            _uiEvents.emit(UiEvent.ShowSuccess("Invoice #${invoiceNumber} saved! ID: $invoiceId"))
        }
    }

    fun deleteInvoice(invoiceId: Int) {
        viewModelScope.launch {
            repository.deleteInvoice(invoiceId)
            _uiEvents.emit(UiEvent.ShowSuccess("Invoice deleted successfully"))
        }
    }

    fun deleteInvoicesBulk(invoiceIds: List<Int>) {
        viewModelScope.launch {
            try {
                for (id in invoiceIds) {
                    repository.deleteInvoice(id)
                }
                _uiEvents.emit(UiEvent.ShowSuccess("Successfully deleted ${invoiceIds.size} invoices"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("Invoices bulk delete failed: ${e.message}"))
            }
        }
    }

    fun deleteCustomersBulk(customersList: List<Customer>) {
        viewModelScope.launch {
            try {
                var deleteCount = 0
                var skipCount = 0
                for (cust in customersList) {
                    val hasInvoices = invoices.value.any { it.invoice.customerId == cust.id }
                    if (hasInvoices) {
                        skipCount++
                    } else {
                        repository.deleteCustomer(cust)
                        deleteCount++
                    }
                }
                if (skipCount > 0) {
                    _uiEvents.emit(UiEvent.ShowError("Deleted $deleteCount clients. Skipped $skipCount clients with generated invoices."))
                } else {
                    _uiEvents.emit(UiEvent.ShowSuccess("Successfully deleted $deleteCount clients"))
                }
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("Clients bulk delete failed: ${e.message}"))
            }
        }
    }

    fun updateInvoiceStatus(
        invoiceId: Int,
        newStatus: String,
        paymentMethod: String = "",
        paymentNote: String = "",
        paymentAttachmentPath: String = "",
        closeReason: String = ""
    ) {
        viewModelScope.launch {
            val scopeInvoices = invoices.value
            val match = scopeInvoices.find { it.invoice.id == invoiceId }
            if (match != null) {
                val updatedInvoice = match.invoice.copy(
                    status = newStatus,
                    paymentMethod = paymentMethod,
                    paymentNote = paymentNote,
                    paymentAttachmentPath = paymentAttachmentPath,
                    closeReason = closeReason
                )
                repository.saveInvoice(updatedInvoice, match.lineItems)
                _uiEvents.emit(UiEvent.ShowSuccess("Invoice status updated to $newStatus"))
            }
        }
    }

    fun incrementDownloadCount(invoiceId: Int) {
        viewModelScope.launch {
            val scopeInvoices = invoices.value
            val match = scopeInvoices.find { it.invoice.id == invoiceId }
            if (match != null) {
                val updatedInvoice = match.invoice.copy(downloadCount = match.invoice.downloadCount + 1)
                repository.saveInvoice(updatedInvoice, match.lineItems)
            }
        }
    }

    fun restoreDatabaseBackup(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupData = BackupRestoreHelper.importFromJson(jsonString)
                repository.restoreData(
                    profile = backupData.profile,
                    savedProfiles = backupData.savedProfiles,
                    products = backupData.products,
                    customers = backupData.customers,
                    invoices = backupData.invoices,
                    lineItems = backupData.lineItems
                )
                onSuccess()
                _uiEvents.emit(UiEvent.ShowSuccess("Data backup restored successfully!"))
            } catch (e: Exception) {
                onError(e.message ?: "Invalid backup file or corrupt formatting")
                _uiEvents.emit(UiEvent.ShowError("Backup Restore Failed: ${e.message}"))
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            try {
                repository.restoreData(
                    profile = null,
                    savedProfiles = emptyList(),
                    products = emptyList(),
                    customers = emptyList(),
                    invoices = emptyList(),
                    lineItems = emptyList()
                )
                _uiEvents.emit(UiEvent.ShowSuccess("All database data cleared!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("Failed to clear data: ${e.message}"))
            }
        }
    }

    fun populateDummyData() {
        viewModelScope.launch {
            println("=== InvoiceViewModel: Seeding dummy data started ===")
            try {
                // Seed Business Profile
                val sampleProfile = BusinessProfile(
                    id = 1,
                    businessName = "Apex Tech Solutions",
                    address = "104 Nehru Place, New Delhi, Delhi, 110019",
                    phone = "+91 98765 43210",
                    email = "invoice@apextech.com",
                    gstin = "07AAAAA1111A1Z1",
                    upiId = "apextech@ybl",
                    gmailId = "apextech.solutions@gmail.com",
                    shortIcon = "⚡",
                    bankAccountName = "Apex Tech Solutions",
                    bankName = "ICICI Bank",
                    bankAccountNo = "123456789",
                    bankBranch = "Noida",
                    bankIfsc = "ICICI1234"
                )
                repository.saveBusinessProfile(sampleProfile)

                // Seed Saved Profiles templates
                val saved1 = SavedBusinessProfile(
                    businessName = "Apex Tech Solutions",
                    address = "104 Nehru Place, New Delhi, Delhi, 110019",
                    phone = "+91 98765 43210",
                    email = "invoice@apextech.com",
                    gstin = "07AAAAA1111A1Z1",
                    upiId = "apextech@ybl",
                    gmailId = "apextech.solutions@gmail.com",
                    shortIcon = "⚡",
                    bankAccountName = "Apex Tech Solutions",
                    bankName = "ICICI Bank",
                    bankAccountNo = "123456789",
                    bankBranch = "Noida",
                    bankIfsc = "ICICI1234"
                )
                val saved2 = SavedBusinessProfile(
                    businessName = "Zenith Hardware & Spares",
                    address = "Block B, Industrial Area, Noida, 201301",
                    phone = "+91 96543 21098",
                    email = "orders@zenithhardware.com",
                    gstin = "09BBBBB2222B2Z2",
                    upiId = "zenith@okaxis",
                    shortIcon = "🛠️",
                    bankAccountName = "Zenith Hardware Pvt Ltd",
                    bankName = "HDFC Bank",
                    bankAccountNo = "501004392120",
                    bankBranch = "New Delhi Okhla",
                    bankIfsc = "HDFC0000120"
                )
                repository.saveSavedBusinessProfile(saved1)
                repository.saveSavedBusinessProfile(saved2)

                // Seed Products
                val p1 = Product(name = "Premium Wireless Earbuds", price = 2499.0, taxRate = 18.0, unit = "pcs", stock = 120.0, hsnSac = "8518")
                val p2 = Product(name = "Ultra-thin Mechanical Keyboard", price = 3999.0, taxRate = 18.0, unit = "pcs", stock = 50.0, hsnSac = "8471")
                val p3 = Product(name = "Ergonomic Office Chair", price = 7999.0, taxRate = 12.0, unit = "pcs", stock = 25.0, hsnSac = "9403")
                val p4 = Product(name = "Software Development Services", price = 1500.0, taxRate = 18.0, unit = "hrs", stock = 900.0, hsnSac = "9983")
                val p5 = Product(name = "USB-C Fast Charging Adapter", price = 899.0, taxRate = 18.0, unit = "pcs", stock = 200.0, hsnSac = "8504")

                val id1 = repository.insertProduct(p1).toInt()
                val id2 = repository.insertProduct(p2).toInt()
                val id3 = repository.insertProduct(p3).toInt()
                val id4 = repository.insertProduct(p4).toInt()
                val id5 = repository.insertProduct(p5).toInt()

                // Seed Customers
                val c1 = Customer(name = "Aman Sharma", phone = "+91 99999 88888", email = "aman@gmail.com", address = "Sector 15, Noida, UP", gstin = "09AAAAA5555A2Z3", placeOfSupply = "09-Uttar Pradesh")
                val c2 = Customer(name = "Global Tech Ltd", phone = "+91 90000 11111", email = "accounts@globaltech.co", address = "BKC, Bandra, Mumbai, MH", gstin = "27BBBBB6666B1Z4", placeOfSupply = "27-Maharashtra")
                val c3 = Customer(name = "Rohan Mehra", phone = "+91 91111 22222", email = "rohan@yahoo.com", address = "Karol Bagh, New Delhi", placeOfSupply = "07-Delhi")

                val custId1 = repository.insertCustomer(c1).toInt()
                val custId2 = repository.insertCustomer(c2).toInt()
                val custId3 = repository.insertCustomer(c3).toInt()

                // Seed Invoices
                val fmt = java.text.SimpleDateFormat("yyyy-MMM-dd", java.util.Locale.US)
                // Invoice 1 - Paid
                val inv1_date = System.currentTimeMillis() - (3 * 24 * 3600 * 1000L) // 3 days ago
                val inv1 = Invoice(
                    id = 0,
                    invoiceNumber = "INV-${fmt.format(java.util.Date(inv1_date)).uppercase()}-0001",
                    customerId = custId1,
                    dateTimestamp = inv1_date,
                    status = "Paid",
                    notes = "Thank you for shopping with Apex Tech!",
                    vehicleNumber = "DL-3C-AQ-1234",
                    placeOfSupply = "09-Uttar Pradesh",
                    dueDateTimestamp = inv1_date + (5L * 24 * 3600 * 1000L)
                )
                val item1 = InvoiceLineItem(
                    id = 0,
                    invoiceId = 0,
                    productId = id1,
                    productName = "Premium Wireless Earbuds",
                    price = 2499.0,
                    quantity = 2.0,
                    taxRate = 18.0,
                    unit = "pcs",
                    subtotal = 4998.0,
                    tax = 899.64,
                    total = 5897.64,
                    hsnSac = "8518"
                )
                val item2 = InvoiceLineItem(
                    id = 0,
                    invoiceId = 0,
                    productId = id5,
                    productName = "USB-C Fast Charging Adapter",
                    price = 899.0,
                    quantity = 1.0,
                    taxRate = 18.0,
                    unit = "pcs",
                    subtotal = 899.0,
                    tax = 161.82,
                    total = 1060.82,
                    hsnSac = "8504"
                )
                repository.saveInvoice(inv1, listOf(item1, item2))

                // Invoice 2 - Sent (Outstanding)
                val inv2_date = System.currentTimeMillis() - (1 * 24 * 3600 * 1000L) // 1 day ago
                val inv2 = Invoice(
                    id = 0,
                    invoiceNumber = "INV-${fmt.format(java.util.Date(inv2_date)).uppercase()}-0002",
                    customerId = custId2,
                    dateTimestamp = inv2_date,
                    status = "Sent",
                    notes = "Due immediately upon receipt of invoice.",
                    brokerageBy = "Direct Sales Team",
                    placeOfSupply = "27-Maharashtra",
                    dueDateTimestamp = inv2_date + (10L * 24 * 3600 * 1000L)
                )
                val item3 = InvoiceLineItem(
                    id = 0,
                    invoiceId = 0,
                    productId = id4,
                    productName = "Software Development Services",
                    price = 1500.0,
                    quantity = 20.0,
                    taxRate = 18.0,
                    unit = "hrs",
                    subtotal = 30000.0,
                    tax = 5400.0,
                    total = 35400.0,
                    hsnSac = "9983"
                )
                repository.saveInvoice(inv2, listOf(item3))

                // Invoice 3 - Draft (Outstanding)
                val inv3 = Invoice(
                    id = 0,
                    invoiceNumber = "INV-${fmt.format(java.util.Date()).uppercase()}-0003",
                    customerId = custId3,
                    dateTimestamp = System.currentTimeMillis(),
                    status = "Draft",
                    notes = "Self pickup by client.",
                    placeOfSupply = "07-Delhi",
                    dueDateTimestamp = System.currentTimeMillis() + (7L * 24 * 3600 * 1000L)
                )
                val item4 = InvoiceLineItem(
                    id = 0,
                    invoiceId = 0,
                    productId = id2,
                    productName = "Ultra-thin Mechanical Keyboard",
                    price = 3999.0,
                    quantity = 1.0,
                    taxRate = 18.0,
                    unit = "pcs",
                    subtotal = 3999.0,
                    tax = 719.82,
                    total = 4718.82,
                    hsnSac = "8471"
                )
                val item5 = InvoiceLineItem(
                    id = 0,
                    invoiceId = 0,
                    productId = id3,
                    productName = "Ergonomic Office Chair",
                    price = 7999.0,
                    quantity = 1.0,
                    taxRate = 12.0,
                    unit = "pcs",
                    subtotal = 7999.0,
                    tax = 959.88,
                    total = 8958.88,
                    hsnSac = "9403"
                )
                repository.saveInvoice(inv3, listOf(item4, item5))

                println("=== InvoiceViewModel: Seeding dummy data completed successfully ===")
                _uiEvents.emit(UiEvent.ShowSuccess("Sample Demo Data Loaded successfully!"))
            } catch (e: Exception) {
                println("=== InvoiceViewModel: Seeding dummy data FAILED ===")
                e.printStackTrace()
                _uiEvents.emit(UiEvent.ShowError("Failed to seed data: ${e.message}"))
            }
        }
    }

    // ------------------ GOOGLE DRIVE OPERATIONS ------------------
    fun setGoogleDriveAccessToken(token: String) {
        val trimmed = token.trim()
        _googleDriveAccessToken.value = trimmed
        if (trimmed.isNotEmpty()) {
            _googleDriveSyncEnabled.value = true
            prefs.edit()
                .putBoolean("gd_sync_enabled", true)
                .putString("gd_access_token", trimmed)
                .apply()
        }
    }

    fun disableGoogleDriveSync() {
        _googleDriveSyncEnabled.value = false
        _googleDriveAccessToken.value = ""
        _googleDriveAccountEmail.value = ""
        prefs.edit()
            .putBoolean("gd_sync_enabled", false)
            .remove("gd_access_token")
            .remove("gd_account_email")
            .apply()
    }

    fun backupToGoogleDrive(onComplete: (Boolean, String) -> Unit) {
        val token = _googleDriveAccessToken.value
        if (token.isEmpty()) {
            onComplete(false, "Google Drive not authorized. Please configure access token.")
            return
        }

        viewModelScope.launch {
            _isGoogleDriveSyncing.value = true
            try {
                val json = exportAppDataToJSON()
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val existingFileId = com.example.data.GoogleDriveService.findBackupFile(token)
                        val success = com.example.data.GoogleDriveService.uploadBackupFile(token, json, existingFileId)
                        
                        withContext(Dispatchers.Main) {
                            _isGoogleDriveSyncing.value = false
                            if (success) {
                                val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                                _googleDriveLastSyncTime.value = nowStr
                                prefs.edit()
                                    .putString("gd_last_sync_time", nowStr)
                                    .putLong("gd_last_sync_timestamp", System.currentTimeMillis())
                                    .apply()
                                onComplete(true, "Successfully backed up data to Google Drive!")
                            } else {
                                com.example.data.GoogleDriveService.invalidateToken(getApplication(), token)
                                _googleDriveAccessToken.value = ""
                                prefs.edit().remove("gd_access_token").apply()
                                onComplete(false, "Drive upload failed. Please connect Google Drive again.")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            _isGoogleDriveSyncing.value = false
                            val errorMsg = e.message ?: ""
                            if (errorMsg.contains("HTTP 401") || errorMsg.contains("HTTP 403")) {
                                com.example.data.GoogleDriveService.invalidateToken(getApplication(), token)
                                _googleDriveAccessToken.value = ""
                                prefs.edit().remove("gd_access_token").apply()
                                onComplete(false, "Google Drive session expired. Please connect Google Drive again.")
                            } else {
                                onComplete(false, "Sync failed: ${e.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _isGoogleDriveSyncing.value = false
                onComplete(false, "Preparation error: ${e.message}")
            }
        }
    }

    fun restoreFromGoogleDrive(onComplete: (Boolean, String) -> Unit) {
        val token = _googleDriveAccessToken.value
        if (token.isEmpty()) {
            onComplete(false, "Google Drive not authorized. Please configure access token.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                _isGoogleDriveSyncing.value = true
            }

            try {
                val existingFileId = com.example.data.GoogleDriveService.findBackupFile(token)
                if (existingFileId == null) {
                    withContext(Dispatchers.Main) {
                        _isGoogleDriveSyncing.value = false
                        onComplete(false, "No 'invoice_app_backup.json' file is found on your Google Drive.")
                    }
                    return@launch
                }

                val jsonContent = com.example.data.GoogleDriveService.downloadBackupFile(token, existingFileId)
                withContext(Dispatchers.Main) {
                    if (jsonContent != null) {
                        restoreDatabaseBackup(
                            jsonString = jsonContent,
                            onSuccess = {
                                _isGoogleDriveSyncing.value = false
                                onComplete(true, "Successfully restored records from Google Drive!")
                            },
                            onError = { err ->
                                _isGoogleDriveSyncing.value = false
                                onComplete(false, "Restore error: $err")
                            }
                        )
                    } else {
                        _isGoogleDriveSyncing.value = false
                        onComplete(false, "Failed to download backup file from Google Drive.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isGoogleDriveSyncing.value = false
                    val errorMsg = e.message ?: ""
                    if (errorMsg.contains("HTTP 401") || errorMsg.contains("HTTP 403")) {
                        com.example.data.GoogleDriveService.invalidateToken(getApplication(), token)
                        _googleDriveAccessToken.value = ""
                        prefs.edit().remove("gd_access_token").apply()
                        onComplete(false, "Google Drive session expired. Please connect Google Drive again.")
                    } else {
                        onComplete(false, "Connection error: ${e.message}")
                    }
                }
            }
        }
    }

    fun fetchGoogleDriveTokenAutomatically(context: Context, email: String) {
        if (email.isEmpty()) return
        val activity = context as? android.app.Activity
        
        // Persistently save the email for auto-reauthentication on app launch
        prefs.edit().putString("gd_account_email", email).apply()
        _googleDriveAccountEmail.value = email
        
        val accountManager = android.accounts.AccountManager.get(context)
        val matchingAccount = android.accounts.Account(email, "com.google")
        accountManager.getAuthToken(
            matchingAccount,
            "oauth2:https://www.googleapis.com/auth/drive.file",
            null,
            activity,
            { future ->
                try {
                    val bundle = future.result
                    val token = bundle.getString(android.accounts.AccountManager.KEY_AUTHTOKEN)
                    if (!token.isNullOrEmpty()) {
                        viewModelScope.launch(Dispatchers.Main) {
                            setGoogleDriveAccessToken(token)
                            android.util.Log.d("GoogleDriveAutoSync", "Successfully auto-authenticated Google Drive for $email")

                            // Auto restore if database is empty
                            if (products.value.isEmpty() && invoices.value.isEmpty() && customers.value.isEmpty()) {
                                android.util.Log.d("GoogleDriveAutoSync", "Local records empty. Triggering automatic cloud restore...")
                                restoreFromGoogleDrive { success, msg ->
                                    android.util.Log.d("GoogleDriveAutoSync", "Auto-restore completed: success=$success, msg=$msg")
                                }
                            }
                        }
                    } else {
                        android.util.Log.e("GoogleDriveAutoSync", "Token was null or empty after consent flow")
                        if (activity != null) {
                            viewModelScope.launch(Dispatchers.Main) {
                                android.widget.Toast.makeText(context, "Google Drive: failed to get access token", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GoogleDriveAutoSync", "Failed to auto-authenticate Google Drive: ${e.message}")
                    if (activity != null) {
                        viewModelScope.launch(Dispatchers.Main) {
                            android.widget.Toast.makeText(context, "Failed to connect Google Drive: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            null
        )
    }

    fun exportAppDataToJSON(): String {
        return BackupRestoreHelper.exportToJson(
            profile = businessProfile.value,
            savedProfiles = savedBusinessProfiles.value,
            products = products.value,
            customers = customers.value,
            invoices = invoices.value
        )
    }

    // ------------------ IN-APP UPDATE OPERATIONS ------------------
    private var downloadJob: kotlinx.coroutines.Job? = null

    fun cancelApkDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _isDownloading.value = false
        _downloadStatus.value = null
        _downloadProgress.value = 0f
    }

    fun downloadAndInstallApk(apkUrl: String) {
        if (_isDownloading.value) return
        _isDownloading.value = true
        _downloadProgress.value = 0f
        _downloadStatus.value = "Starting download..."

        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val connection = getRedirectedConnection(apkUrl)
                connection.connect()

                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    throw java.io.IOException("Server returned HTTP " + connection.responseCode + " " + connection.responseMessage)
                }

                val fileLength = connection.contentLength
                val updateFile = java.io.File(context.cacheDir, "invoice_update.apk")
                if (updateFile.exists()) {
                    updateFile.delete()
                }

                connection.inputStream.use { input ->
                    java.io.FileOutputStream(updateFile).use { output ->
                        val buffer = ByteArray(4096)
                        var total: Long = 0
                        var count = 0
                        while (isActive && input.read(buffer).also { count = it } != -1) {
                            total += count
                            if (fileLength > 0) {
                                val progress = total.toFloat() / fileLength
                                withContext(Dispatchers.Main) {
                                    _downloadProgress.value = progress
                                    val mbDown = String.format("%.1f", total / 1_048_576.0)
                                    val mbTotal = String.format("%.1f", fileLength / 1_048_576.0)
                                    _downloadStatus.value = "Downloading... $mbDown / $mbTotal MB"
                                }
                            }
                            output.write(buffer, 0, count)
                        }
                        output.flush()
                    }
                }

                if (!isActive) return@launch

                withContext(Dispatchers.Main) {
                    _downloadProgress.value = 1f
                    _downloadStatus.value = "Download complete. Checking permissions..."
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    if (!context.packageManager.canRequestPackageInstalls()) {
                        val settingsIntent = android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = android.net.Uri.parse("package:" + context.packageName)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        withContext(Dispatchers.Main) {
                            try {
                                context.startActivity(settingsIntent)
                                _downloadStatus.value = "Please enable 'Install unknown apps' permission"
                            } catch (e: Exception) {
                                _downloadStatus.value = "Could not open settings: ${e.message}"
                            }
                        }
                        return@launch
                    }
                }

                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    updateFile
                )

                val installIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "application/vnd.android.package-archive")
                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                }

                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(installIntent)
                        _downloadStatus.value = "Install dialog opened"
                    } catch (e: Exception) {
                        _downloadStatus.value = "Could not open installer: ${e.message}"
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    withContext(Dispatchers.Main) {
                        _downloadStatus.value = "Error: ${e.localizedMessage ?: "Unknown error"}"
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isDownloading.value = false
                }
            }
        }
    }

    private fun getRedirectedConnection(urlStr: String): java.net.HttpURLConnection {
        var url = java.net.URL(urlStr)
        var connection = url.openConnection() as java.net.HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        
        var status = connection.responseCode
        var redirects = 0
        while (status == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
               status == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
               status == 307 || status == 308) {
            if (redirects > 5) break
            val newUrl = connection.getHeaderField("Location") ?: break
            connection.disconnect()
            url = java.net.URL(newUrl)
            connection = url.openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            status = connection.responseCode
            redirects++
        }
        return connection
    }

    // User Authentication States
    private val _isUserLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private fun checkAndClearOnAccountSwitch(newEmail: String) {
        val lastEmail = prefs.getString("last_logged_account_email", "") ?: ""
        if (lastEmail.isNotEmpty() && !lastEmail.equals(newEmail, ignoreCase = true)) {
            viewModelScope.launch(Dispatchers.IO) {
                database.clearAllTables()
            }
        }
        prefs.edit().putString("last_logged_account_email", newEmail).apply()
    }

    suspend fun loginUser(email: String, password: String): Boolean {
        if (SupabaseClientManager.isConfigured()) {
            val res = SupabaseClientManager.signInUser(getApplication(), email, password)
            if (res.isSuccess) {
                checkAndClearOnAccountSwitch(email)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putString("user_email", email)
                    .apply()
                _isUserLoggedIn.value = true
                _userEmail.value = email
                _uiEvents.emit(UiEvent.ShowSuccess("Welcome back, $email!"))
                return true
            } else {
                _uiEvents.emit(UiEvent.ShowError(res.exceptionOrNull()?.message ?: "Supabase auth failed"))
                return false
            }
        }

        val registeredPassword = prefs.getString("reg_pwd_$email", null)
        return if (registeredPassword != null && registeredPassword == password) {
            checkAndClearOnAccountSwitch(email)
            prefs.edit()
                .putBoolean("is_logged_in", true)
                .putString("user_email", email)
                .apply()
            _isUserLoggedIn.value = true
            _userEmail.value = email
            _uiEvents.emit(UiEvent.ShowSuccess("Welcome back, $email!"))
            true
        } else {
            _uiEvents.emit(UiEvent.ShowError("Invalid email or password"))
            false
        }
    }

    suspend fun registerUser(email: String, password: String): Boolean {
        if (SupabaseClientManager.isConfigured()) {
            val res = SupabaseClientManager.signUpUser(getApplication(), email, password)
            if (res.isSuccess) {
                val status = res.getOrNull()
                prefs.edit().putString("reg_pwd_$email", password).apply()

                if (status == "CONFIRMATION_REQUIRED") {
                    _uiEvents.emit(UiEvent.ShowSuccess("Registration successful! Please check your email inbox to confirm your account."))
                    return false
                } else {
                    checkAndClearOnAccountSwitch(email)
                    prefs.edit()
                        .putBoolean("is_logged_in", true)
                        .putString("user_email", email)
                        .apply()
                    _isUserLoggedIn.value = true
                    _userEmail.value = email
                    _uiEvents.emit(UiEvent.ShowSuccess("Account created in Supabase successfully!"))
                    return true
                }
            } else {
                _uiEvents.emit(UiEvent.ShowError(res.exceptionOrNull()?.message ?: "Supabase signup failed"))
                return false
            }
        }

        val existing = prefs.getString("reg_pwd_$email", null)
        return if (existing != null) {
            _uiEvents.emit(UiEvent.ShowError("User already exists with this email"))
            false
        } else {
            checkAndClearOnAccountSwitch(email)
            prefs.edit()
                .putString("reg_pwd_$email", password)
                .putBoolean("is_logged_in", true)
                .putString("user_email", email)
                .apply()
            _isUserLoggedIn.value = true
            _userEmail.value = email
            _uiEvents.emit(UiEvent.ShowSuccess("Account created successfully!"))
            true
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Boolean {
        if (SupabaseClientManager.isConfigured()) {
            val res = SupabaseClientManager.resetPassword(email)
            if (res.isSuccess) {
                _uiEvents.emit(UiEvent.ShowSuccess("Password reset link sent to $email! Please check your email inbox."))
                return true
            } else {
                _uiEvents.emit(UiEvent.ShowError(res.exceptionOrNull()?.message ?: "Failed to send password reset email"))
                return false
            }
        }
        _uiEvents.emit(UiEvent.ShowSuccess("Password reset link sent to $email!"))
        return true
    }

    fun logoutUser() {
        SupabaseClientManager.clearSession(getApplication())
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .putString("user_email", "")
            .apply()
        _isUserLoggedIn.value = false
        _userEmail.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            database.clearAllTables()
        }

        viewModelScope.launch {
            _uiEvents.emit(UiEvent.ShowSuccess("Logged out successfully"))
        }
    }

    sealed interface UiEvent {
        data class ShowSuccess(val msg: String) : UiEvent
        data class ShowError(val msg: String) : UiEvent
    }
}
