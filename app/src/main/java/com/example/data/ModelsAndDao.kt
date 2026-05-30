package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ------------------ ENTITIES ------------------

@Entity(tableName = "business_profile")
data class BusinessProfile(
    @PrimaryKey val id: Int = 1,
    val businessName: String = "",
    val address: String = "",
    val phone: String = "",
    val email: String = "",
    val gstin: String = "",
    val logoUrl: String = "",
    val upiId: String = "",
    val gmailId: String = "",
    val shortIcon: String = "💼"
)

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val taxRate: Double, // Percentage e.g. 18.0 for 18% GST
    val unit: String,    // "pcs", "kg", "hrs", "box", etc.
    val stock: Double = 0.0, // stock quantity
    val hsnSac: String = ""
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstin: String = "",
    val placeOfSupply: String = ""
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String, // e.g., "INV-2026-0001"
    val customerId: Int,       // FK (conceptually) to Customer
    val dateTimestamp: Long = System.currentTimeMillis(),
    val status: String,        // "Draft", "Sent", "Paid"
    val subtotal: Double = 0.0,
    val taxTotal: Double = 0.0,
    val grandTotal: Double = 0.0,
    val notes: String = "",
    val vehicleNumber: String = "",
    val brokerageBy: String = "",
    val placeOfSupply: String = ""
)

@Entity(tableName = "invoice_line_items")
data class InvoiceLineItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceId: Int, // FK to Invoice
    val productId: Int, // reference to Product (0 if custom item)
    val productName: String,
    val price: Double,
    val quantity: Double,
    val taxRate: Double, // Percentage e.g. 18.0
    val unit: String,    // e.g. "pcs", "kg", "hrs"
    val subtotal: Double, // quantity * price
    val tax: Double,      // (subtotal * taxRate) / 100.0
    val total: Double,     // subtotal + tax
    val hsnSac: String = ""
)

// ------------------ RELATIONS ------------------

data class InvoiceWithDetails(
    @Embedded val invoice: Invoice,
    @Relation(
        parentColumn = "customerId",
        entityColumn = "id"
    )
    val customer: Customer?,
    @Relation(
        parentColumn = "id",
        entityColumn = "invoiceId"
    )
    val lineItems: List<InvoiceLineItem>
)

// ------------------ DAOs ------------------

@Dao
interface InvoiceDao {
    @Transaction
    @Query("SELECT * FROM invoices ORDER BY dateTimestamp DESC")
    fun getAllInvoices(): Flow<List<InvoiceWithDetails>>

    @Transaction
    @Query("SELECT * FROM invoices WHERE id = :id")
    fun getInvoiceById(id: Int): Flow<InvoiceWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoice(invoice: Invoice): Long

    @Update
    suspend fun updateInvoice(invoice: Invoice)

    @Delete
    suspend fun deleteInvoice(invoice: Invoice)

    @Query("DELETE FROM invoices WHERE id = :id")
    suspend fun deleteInvoiceById(id: Int)

    // Lines items queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineItems(items: List<InvoiceLineItem>)

    @Query("DELETE FROM invoice_line_items WHERE invoiceId = :invoiceId")
    suspend fun deleteLineItemsByInvoiceId(invoiceId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvoicesBulk(invoices: List<Invoice>)

    @Query("DELETE FROM invoices")
    suspend fun clearAllInvoices()

    @Query("DELETE FROM invoice_line_items")
    suspend fun clearAllLineItems()

    // Simple analytics queries (Paid only counts towards sales, Draft/Sent can be tracked in code)
    @Query("SELECT SUM(grandTotal) FROM invoices WHERE status = 'Paid'")
    fun getTotalSales(): Flow<Double?>

    @Query("SELECT SUM(grandTotal) FROM invoices WHERE status != 'Paid'")
    fun getOutstandingAmount(): Flow<Double?>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductsBulk(products: List<Product>)

    @Query("DELETE FROM products")
    suspend fun clearAllProducts()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomersBulk(customers: List<Customer>)

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()
}

@Dao
interface BusinessProfileDao {
    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    fun getProfile(): Flow<BusinessProfile?>

    @Query("SELECT * FROM business_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileSync(): BusinessProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: BusinessProfile)

    @Query("DELETE FROM business_profile")
    suspend fun clearBusinessProfile()
}

// ------------------ DATABASE ------------------

@Database(
    entities = [
        BusinessProfile::class,
        Product::class,
        Customer::class,
        Invoice::class,
        InvoiceLineItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class InvoiceDatabase : RoomDatabase() {
    abstract fun invoiceDao(): InvoiceDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun businessProfileDao(): BusinessProfileDao

    companion object {
        @Volatile
        private var INSTANCE: InvoiceDatabase? = null

        fun getDatabase(context: Context): InvoiceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InvoiceDatabase::class.java,
                    "invoice_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
