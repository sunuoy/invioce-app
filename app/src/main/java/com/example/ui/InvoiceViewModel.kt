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
        database.businessProfileDao()
    )

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

    val totalSales: StateFlow<Double?> = repository.totalSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val outstandingAmount: StateFlow<Double?> = repository.outstandingAmount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Support generation: INV-YEAR-0001
    fun generateNextInvoiceNumber(): String {
        val count = invoices.value.size
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        return "INV-$year-${String.format("%04d", count + 1)}"
    }

    // ------------------ BUSINESS OPERATIONS ------------------
    fun saveBusinessProfile(name: String, address: String, phone: String, email: String, gstin: String, upiId: String, gmailId: String, shortIcon: String) {
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
                shortIcon = shortIcon.trim()
            )
            repository.saveBusinessProfile(profile)
            _uiEvents.emit(UiEvent.ShowSuccess("Business profile updated successfully!"))
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

    sealed interface UiEvent {
        data class ShowSuccess(val msg: String) : UiEvent
        data class ShowError(val msg: String) : UiEvent
    }
}
