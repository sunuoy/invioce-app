package com.example.data

import kotlinx.coroutines.flow.Flow

class InvoiceRepository(
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val businessProfileDao: BusinessProfileDao
) {
    // ------------------ PRODUCTS ------------------
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    
    suspend fun getProductById(id: Int): Product? = productDao.getProductById(id)
    
    suspend fun insertProduct(product: Product): Long = productDao.insertProduct(product)
    
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    
    suspend fun deleteProduct(product: Product) = productDao.deleteProduct(product)
    
    suspend fun deleteProductById(id: Int) = productDao.deleteProductById(id)

    // ------------------ CUSTOMERS ------------------
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    
    suspend fun getCustomerById(id: Int): Customer? = customerDao.getCustomerById(id)
    
    suspend fun insertCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    // ------------------ BUSINESS PROFILE ------------------
    val businessProfile: Flow<BusinessProfile?> = businessProfileDao.getProfile()
    
    suspend fun getBusinessProfileSync(): BusinessProfile? = businessProfileDao.getProfileSync()
    
    suspend fun saveBusinessProfile(profile: BusinessProfile) = businessProfileDao.insertOrUpdateProfile(profile)

    // ------------------ INVOICES ------------------
    val allInvoices: Flow<List<InvoiceWithDetails>> = invoiceDao.getAllInvoices()
    
    fun getInvoiceById(id: Int): Flow<InvoiceWithDetails?> = invoiceDao.getInvoiceById(id)

    suspend fun saveInvoice(invoice: Invoice, items: List<InvoiceLineItem>): Long {
        // Compute actual numbers from line items
        val subtotal = items.sumOf { it.subtotal }
        val taxTotal = items.sumOf { it.tax }
        val grandTotal = items.sumOf { it.total }

        val finalizedInvoice = invoice.copy(
            subtotal = subtotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal
        )

        val invoiceId = if (finalizedInvoice.id == 0) {
            invoiceDao.insertInvoice(finalizedInvoice).toInt()
        } else {
            invoiceDao.updateInvoice(finalizedInvoice)
            finalizedInvoice.id
        }

        // Drop current items and rewrite
        invoiceDao.deleteLineItemsByInvoiceId(invoiceId)
        val preparedItems = items.map { it.copy(invoiceId = invoiceId) }
        invoiceDao.insertLineItems(preparedItems)

        // Optional: Deduct stock automatically if invoice is marked "Sent" or "Paid"
        if (finalizedInvoice.status == "Paid" || finalizedInvoice.status == "Sent") {
            for (it in preparedItems) {
                if (it.productId != 0) {
                    val prod = productDao.getProductById(it.productId)
                    if (prod != null) {
                        val newStock = prod.stock - it.quantity
                        productDao.updateProduct(prod.copy(stock = newStock))
                    }
                }
            }
        }

        return invoiceId.toLong()
    }

    suspend fun deleteInvoice(invoiceId: Int) {
        invoiceDao.deleteInvoiceById(invoiceId)
        invoiceDao.deleteLineItemsByInvoiceId(invoiceId)
    }

    // ------------------ ANALYTICS ------------------
    val totalSales: Flow<Double?> = invoiceDao.getTotalSales()
    val outstandingAmount: Flow<Double?> = invoiceDao.getOutstandingAmount()

    // ------------------ BACKUP & RESTORE ------------------
    suspend fun restoreData(
        profile: BusinessProfile?,
        products: List<Product>,
        customers: List<Customer>,
        invoices: List<Invoice>,
        lineItems: List<InvoiceLineItem>
    ) {
        businessProfileDao.clearBusinessProfile()
        productDao.clearAllProducts()
        customerDao.clearAllCustomers()
        invoiceDao.clearAllInvoices()
        invoiceDao.clearAllLineItems()

        if (profile != null) {
            businessProfileDao.insertOrUpdateProfile(profile)
        }
        if (products.isNotEmpty()) {
            productDao.insertProductsBulk(products)
        }
        if (customers.isNotEmpty()) {
            customerDao.insertCustomersBulk(customers)
        }
        if (invoices.isNotEmpty()) {
            invoiceDao.insertInvoicesBulk(invoices)
        }
        if (lineItems.isNotEmpty()) {
            invoiceDao.insertLineItems(lineItems)
        }
    }
}
