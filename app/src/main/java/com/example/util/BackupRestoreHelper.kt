package com.example.util

import com.example.data.*
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreHelper {

    fun exportToJson(
        profile: BusinessProfile?,
        products: List<Product>,
        customers: List<Customer>,
        invoices: List<InvoiceWithDetails>
    ): String {
        val root = JSONObject()

        // 1. Business Profile
        if (profile != null) {
            val profJson = JSONObject().apply {
                put("businessName", profile.businessName)
                put("address", profile.address)
                put("phone", profile.phone)
                put("email", profile.email)
                put("gstin", profile.gstin)
                put("logoUrl", profile.logoUrl)
                put("upiId", profile.upiId)
                put("gmailId", profile.gmailId)
                put("shortIcon", profile.shortIcon)
                put("bankAccountName", profile.bankAccountName)
                put("bankName", profile.bankName)
                put("bankAccountNo", profile.bankAccountNo)
                put("bankBranch", profile.bankBranch)
                put("bankIfsc", profile.bankIfsc)
            }
            root.put("businessProfile", profJson)
        }

        // 2. Products
        val productsArr = JSONArray()
        for (prod in products) {
            val prodJson = JSONObject().apply {
                put("id", prod.id)
                put("name", prod.name)
                put("price", prod.price)
                put("taxRate", prod.taxRate)
                put("unit", prod.unit)
                put("stock", prod.stock)
                put("hsnSac", prod.hsnSac)
            }
            productsArr.put(prodJson)
        }
        root.put("products", productsArr)

        // 3. Customers
        val customersArr = JSONArray()
        for (cust in customers) {
            val custJson = JSONObject().apply {
                put("id", cust.id)
                put("name", cust.name)
                put("phone", cust.phone)
                put("email", cust.email)
                put("address", cust.address)
                put("gstin", cust.gstin)
                put("placeOfSupply", cust.placeOfSupply)
            }
            customersArr.put(custJson)
        }
        root.put("customers", customersArr)

        // 4. Invoices and line items
        val invoicesArr = JSONArray()
        for (invWithDetail in invoices) {
            val inv = invWithDetail.invoice
            val invJson = JSONObject().apply {
                put("id", inv.id)
                put("invoiceNumber", inv.invoiceNumber)
                put("customerId", inv.customerId)
                put("dateTimestamp", inv.dateTimestamp)
                put("status", inv.status)
                put("subtotal", inv.subtotal)
                put("taxTotal", inv.taxTotal)
                put("grandTotal", inv.grandTotal)
                put("notes", inv.notes)
                put("vehicleNumber", inv.vehicleNumber)
                put("brokerageBy", inv.brokerageBy)
                put("placeOfSupply", inv.placeOfSupply)
                put("downloadCount", inv.downloadCount)
            }

            val itemsArr = JSONArray()
            for (item in invWithDetail.lineItems) {
                val itemJson = JSONObject().apply {
                    put("id", item.id)
                    put("invoiceId", item.invoiceId)
                    put("productId", item.productId)
                    put("productName", item.productName)
                    put("price", item.price)
                    put("quantity", item.quantity)
                    put("taxRate", item.taxRate)
                    put("unit", item.unit)
                    put("subtotal", item.subtotal)
                    put("tax", item.tax)
                    put("total", item.total)
                    put("hsnSac", item.hsnSac)
                }
                itemsArr.put(itemJson)
            }
            invJson.put("lineItems", itemsArr)
            invoicesArr.put(invJson)
        }
        root.put("invoices", invoicesArr)

        return root.toString(4)
    }

    fun importFromJson(jsonString: String): BackupData {
        val root = JSONObject(jsonString)

        // 1. Business Profile
        val profile = if (root.has("businessProfile")) {
            val profJson = root.getJSONObject("businessProfile")
            BusinessProfile(
                id = 1,
                businessName = profJson.optString("businessName", ""),
                address = profJson.optString("address", ""),
                phone = profJson.optString("phone", ""),
                email = profJson.optString("email", ""),
                gstin = profJson.optString("gstin", ""),
                logoUrl = profJson.optString("logoUrl", ""),
                upiId = profJson.optString("upiId", ""),
                gmailId = profJson.optString("gmailId", ""),
                shortIcon = profJson.optString("shortIcon", "💼"),
                bankAccountName = profJson.optString("bankAccountName", ""),
                bankName = profJson.optString("bankName", ""),
                bankAccountNo = profJson.optString("bankAccountNo", ""),
                bankBranch = profJson.optString("bankBranch", ""),
                bankIfsc = profJson.optString("bankIfsc", "")
            )
        } else null

        // 2. Products
        val products = mutableListOf<Product>()
        if (root.has("products")) {
            val arr = root.getJSONArray("products")
            for (i in 0 until arr.length()) {
                val prodJson = arr.getJSONObject(i)
                products.add(
                    Product(
                        id = prodJson.optInt("id", 0),
                        name = prodJson.optString("name", ""),
                        price = prodJson.optDouble("price", 0.0),
                        taxRate = prodJson.optDouble("taxRate", 0.0),
                        unit = prodJson.optString("unit", "pcs"),
                        stock = prodJson.optDouble("stock", 0.0),
                        hsnSac = prodJson.optString("hsnSac", "")
                    )
                )
            }
        }

        // 3. Customers
        val customers = mutableListOf<Customer>()
        if (root.has("customers")) {
            val arr = root.getJSONArray("customers")
            for (i in 0 until arr.length()) {
                val custJson = arr.getJSONObject(i)
                customers.add(
                    Customer(
                        id = custJson.optInt("id", 0),
                        name = custJson.optString("name", ""),
                        phone = custJson.optString("phone", ""),
                        email = custJson.optString("email", ""),
                        address = custJson.optString("address", ""),
                        gstin = custJson.optString("gstin", ""),
                        placeOfSupply = custJson.optString("placeOfSupply", "")
                    )
                )
            }
        }

        // 4. Invoices and line items
        val invoices = mutableListOf<Invoice>()
        val lineItems = mutableListOf<InvoiceLineItem>()
        if (root.has("invoices")) {
            val arr = root.getJSONArray("invoices")
            for (i in 0 until arr.length()) {
                val invJson = arr.getJSONObject(i)
                val invId = invJson.optInt("id", 0)
                invoices.add(
                    Invoice(
                        id = invId,
                        invoiceNumber = invJson.optString("invoiceNumber", ""),
                        customerId = invJson.optInt("customerId", 0),
                        dateTimestamp = invJson.optLong("dateTimestamp", System.currentTimeMillis()),
                        status = invJson.optString("status", "Paid"),
                        subtotal = invJson.optDouble("subtotal", 0.0),
                        taxTotal = invJson.optDouble("taxTotal", 0.0),
                        grandTotal = invJson.optDouble("grandTotal", 0.0),
                        notes = invJson.optString("notes", ""),
                        vehicleNumber = invJson.optString("vehicleNumber", ""),
                        brokerageBy = invJson.optString("brokerageBy", ""),
                        placeOfSupply = invJson.optString("placeOfSupply", ""),
                        downloadCount = invJson.optInt("downloadCount", 0)
                    )
                )

                if (invJson.has("lineItems")) {
                    val itemsArr = invJson.getJSONArray("lineItems")
                    for (j in 0 until itemsArr.length()) {
                        val itemJson = itemsArr.getJSONObject(j)
                        lineItems.add(
                            InvoiceLineItem(
                                id = itemJson.optInt("id", 0),
                                invoiceId = itemJson.optInt("invoiceId", invId),
                                productId = itemJson.optInt("productId", 0),
                                productName = itemJson.optString("productName", ""),
                                price = itemJson.optDouble("price", 0.0),
                                quantity = itemJson.optDouble("quantity", 0.0),
                                taxRate = itemJson.optDouble("taxRate", 0.0),
                                unit = itemJson.optString("unit", "pcs"),
                                subtotal = itemJson.optDouble("subtotal", 0.0),
                                tax = itemJson.optDouble("tax", 0.0),
                                total = itemJson.optDouble("total", 0.0),
                                hsnSac = itemJson.optString("hsnSac", "")
                            )
                        )
                    }
                }
            }
        }

        return BackupData(profile, products, customers, invoices, lineItems)
    }
}

data class BackupData(
    val profile: BusinessProfile?,
    val products: List<Product>,
    val customers: List<Customer>,
    val invoices: List<Invoice>,
    val lineItems: List<InvoiceLineItem>
)
