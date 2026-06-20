package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.BackupRestoreHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InvoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = InvoiceDatabase.getDatabase(application)
    private val repository = InvoiceRepository(
        database.invoiceDao(),
        database.productDao(),
        database.customerDao(),
        database.businessProfileDao(),
        database.savedBusinessProfileDao()
    )

    init {
        viewModelScope.launch {
            val prof = repository.getBusinessProfileSync()
            if (prof == null) {
                // First boot placeholder seeding
                populateDummyData()
            }
        }
    }

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

    // Support generation: YYYY-MM-DD-0001
    fun generateNextInvoiceNumber(): String {
        val count = invoices.value.size
        val dateString = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return "$dateString-${String.format(java.util.Locale.US, "%04d", count + 1)}"
    }

    // ------------------ BUSINESS OPERATIONS ------------------
    fun saveBusinessProfile(name: String, address: String, phone: String, email: String, gstin: String, upiId: String, gmailId: String, shortIcon: String, logoUrl: String = "") {
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
                logoUrl = logoUrl.trim()
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
    fun saveProduct(id: Int, name: String, price: Double, tax: Double, unit: String, stock: Double, hsnSac: String = "") {
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
                _uiEvents.emit(UiEvent.ShowError("Please set a unit (e.g. pieces, hours)"))
                return@launch
            }

            val product = Product(
                id = id,
                name = name.trim(),
                price = price,
                taxRate = tax,
                unit = unit.trim(),
                stock = stock,
                hsnSac = hsnSac.trim()
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
    fun saveCustomer(id: Int, name: String, phone: String, email: String, address: String, gstin: String = "", placeOfSupply: String = "") {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiEvents.emit(UiEvent.ShowError("Customer name cannot be blank"))
                return@launch
            }
            val customer = Customer(
                id = id,
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim(),
                gstin = gstin.trim(),
                placeOfSupply = placeOfSupply.trim()
            )
            repository.insertCustomer(customer)
            _uiEvents.emit(UiEvent.ShowSuccess("Customer details saved!"))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _uiEvents.emit(UiEvent.ShowSuccess("Customer deleted"))
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
        placeOfSupply: String = ""
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

            val invoice = Invoice(
                id = id,
                invoiceNumber = invoiceNumber.trim(),
                customerId = customerId,
                status = status,
                notes = notes.trim(),
                vehicleNumber = vehicleNumber.trim(),
                brokerageBy = brokerageBy.trim(),
                placeOfSupply = placeOfSupply.trim()
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

    fun updateInvoiceStatus(invoiceId: Int, newStatus: String) {
        viewModelScope.launch {
            val scopeInvoices = invoices.value
            val match = scopeInvoices.find { it.invoice.id == invoiceId }
            if (match != null) {
                val updatedInvoice = match.invoice.copy(status = newStatus)
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

    fun populateDummyData() {
        viewModelScope.launch {
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
                    shortIcon = "⚡"
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
                    shortIcon = "⚡"
                )
                val saved2 = SavedBusinessProfile(
                    businessName = "Zenith Hardware & Spares",
                    address = "Block B, Industrial Area, Noida, 201301",
                    phone = "+91 96543 21098",
                    email = "orders@zenithhardware.com",
                    gstin = "09BBBBB2222B2Z2",
                    upiId = "zenith@okaxis",
                    shortIcon = "🛠️"
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
                // Invoice 1 - Paid
                val inv1_date = System.currentTimeMillis() - (3 * 24 * 3600 * 1000L) // 3 days ago
                val inv1 = Invoice(
                    id = 0,
                    invoiceNumber = "INV-2026-0001",
                    customerId = custId1,
                    dateTimestamp = inv1_date,
                    status = "Paid",
                    notes = "Thank you for shopping with Apex Tech!",
                    vehicleNumber = "DL-3C-AQ-1234",
                    placeOfSupply = "09-Uttar Pradesh"
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
                    invoiceNumber = "INV-2026-0002",
                    customerId = custId2,
                    dateTimestamp = inv2_date,
                    status = "Sent",
                    notes = "Due immediately upon receipt of invoice.",
                    brokerageBy = "Direct Sales Team",
                    placeOfSupply = "27-Maharashtra"
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
                    invoiceNumber = "INV-2026-0003",
                    customerId = custId3,
                    dateTimestamp = System.currentTimeMillis(),
                    status = "Draft",
                    notes = "Self pickup by client.",
                    placeOfSupply = "07-Delhi"
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

                _uiEvents.emit(UiEvent.ShowSuccess("Sample Demo Data Loaded successfully!"))
            } catch (e: Exception) {
                _uiEvents.emit(UiEvent.ShowError("Failed to seed data: ${e.message}"))
            }
        }
    }

    sealed interface UiEvent {
        data class ShowSuccess(val msg: String) : UiEvent
        data class ShowError(val msg: String) : UiEvent
    }
}
