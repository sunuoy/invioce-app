# ⚡ Invoice Easy Pro — Professional Billing Hub

A premium, offline-first Android application designed to streamline billing, invoicing, client directory management, and stock tracking. Built entirely with Kotlin, Jetpack Compose, Material 3, and Room Database, the app features dynamic, beautiful PDF invoice generation with payment QR codes, multiple visual themes, and robust backup capabilities.

---

## 🚀 Key Features

### 1. Interactive Dashboard & Analytics
* **Real-time Metrics:** Keep track of **Total Sales** (from Paid invoices) and **Outstanding Balances** (from Draft/Sent invoices) at a glance.
* **Invoice Summary:** Quick access to recently created invoices with color-coded status badges (Draft, Sent, Paid).
* **Shortcut Actions:** Jump instantly into creating a new invoice or viewing key sections from the dashboard.

### 2. Smart Billing & Invoicing
* **Sequential Invoice Numbers:** Automatically generates clean, sequential invoice numbers using customizable templates (e.g., `INV-YYYY-MMM-DD-XXXX`).
* **Line Item Calculator:** Add multiple items with real-time tax (CGST/SGST split), quantity, custom units, and discount adjustments.
* **Status Tracking:** Progress invoices from **Draft** to **Sent** to **Paid** to maintain accurate financial ledgers.
* **Bulk Management:** Select and delete multiple invoices simultaneously.

### 3. Native & Dynamic PDF Generator
* **Professional Layouts:** Generates fully standard-compliant **Tax Invoices** and **Bills of Supply** drawn directly onto a PDF canvas.
* **Live UPI QR Codes:** Encodes the exact invoice total, payee name, and UPI ID into a QR code embedded directly in the PDF for instant scan-and-pay.
* **Premium Color Themes:** Match your business branding with built-in styles:
  * *Classic Navy*, *Forest Green*, *Burgundy*, *Charcoal*, *Sunset Indigo*, *Burnt Sienna*, and *Blooming Romance*.
* **Smart Watermarks:** Automatically labels duplicate downloads with a "DUPLICATE COPY" watermark (from the 3rd download onwards).
* **Words Converter:** Converts the final numeric amount automatically to English words (e.g., *One Thousand Two Hundred Rupees and Fifty Paise Only*).
* **Digital Signatures:** Supports adding and scaling an authorized signatory signature image in the footer.

### 4. Inventory & Stock Tracking
* **Product Catalog:** Manage catalog items with unit prices, custom units (e.g., *pcs*, *kg*, *hrs*, *box*), and HSN/SAC tags.
* **Low Stock Alerts:** Get visual warnings when items fall below a custom threshold.
* **Auto-population:** Search and import products directly into new invoices to save time.

### 5. Client Directory
* **Client Profiles:** Store client names, mobile numbers, email addresses, physical addresses, place of supply, and GSTINs.
* **Tax Automation:** Auto-selects tax jurisdictions and pre-populates customer data on new invoices.

### 6. Data Portability & Settings
* **JSON Backup & Restore:** Complete export/import of all database items (business profile, products, clients, invoices, line items) in one click.
* **Demo Data Seeding:** Instantly populate the app with rich sample profiles, inventory, and transaction history for demo purposes.
* **Custom Settings:** Configure low-stock thresholds, PDF colors, and toggle digital signatures.

---

## 🛠️ Technology Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose & Material 3
* **Navigation:** Compose Navigation with animated slide & fade transitions
* **Local Database:** Room Database (SQLite) with multi-table relations and destructive migrations
* **PDF Utility:** Android Native `PdfDocument` & custom canvas graphics drawing
* **QR Codes:** ZXing (Zebra Crossing) core library
* **Dependency Injection & Async:** Kotlin Coroutines & Flow API for reactive UI states

---

## ⚙️ How to Run Locally

### Prerequisites
* [Android Studio (Koala or later)](https://developer.android.com/studio)
* Android SDK (API Level 24 minimum, API Level 36 target)

### Step-by-Step Setup

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/sunuoy/invioce-app.git
   cd invioce-app
   ```

2. **Open in Android Studio:**
   Select **File -> Open** and choose the directory containing the project. Let Gradle build and resolve dependencies.

3. **Configure Environment Variables:**
   Create a file named `.env` in the root project directory (using `.env.example` as a template) if you plan to integrate Gemini API features:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

4. **Run the App:**
   Select a physical device or emulator running Android 7.0 (API 24) or above and click the **Run** button (or press `Shift + F10`).

5. **Load Demo Data (Optional):**
   To preview the app with pre-filled mock invoices, products, and customers, navigate to the **Application Settings** side menu and click **Seed Demo Data**.
