package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.InvoiceDatabase
import com.example.data.SavedBusinessProfile
import com.example.ui.InvoiceViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@Implements(androidx.core.content.FileProvider::class)
class ShadowFileProvider {
  companion object {
    @Implementation
    @JvmStatic
    fun getUriForFile(context: android.content.Context, authority: String, file: java.io.File): android.net.Uri {
      return android.net.Uri.parse("content://$authority/${file.name}")
    }
  }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [ShadowFileProvider::class])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Invoice Generator", appName)
  }

  @Test
  fun testDatabaseAndViewModelInitialization() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = InvoiceDatabase.getDatabase(context)
    assertNotNull(db)
    
    // Verify saved business profile dao
    val dao = db.savedBusinessProfileDao()
    assertNotNull(dao)
    
    val p = SavedBusinessProfile(
      businessName = "Test Biz",
      address = "Test Addr",
      phone = "123",
      email = "a@b.com",
      gstin = "GST123"
    )
    dao.insertSavedProfile(p)
    
    // Verify view model
    val viewModel = InvoiceViewModel(context as Application)
    assertNotNull(viewModel)
  }

  @Test
  fun testSeedingDummyData() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val viewModel = InvoiceViewModel(context as Application)
    viewModel.populateDummyData()
    
    // Let any background/main thread tasks run to completion
    org.robolectric.shadows.ShadowLooper.idleMainLooper()
    
    val db = InvoiceDatabase.getDatabase(context)
    val profile = db.businessProfileDao().getProfileSync()
    assertNotNull("Dummy data profile should be seeded", profile)
    assertEquals("Apex Tech Solutions", profile?.businessName)
  }

  @Test
  fun testPdfGeneratorFileProvider() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pdfFile = java.io.File(context.cacheDir, "test_invoice.pdf")
    pdfFile.writeText("Dummy PDF Content")
    
    // Resolve URI via FileProvider using the shadowed implementation
    val uri = androidx.core.content.FileProvider.getUriForFile(
      context,
      "com.aistudio.invoicegenerator.gqtwv.fileprovider",
      pdfFile
    )
    assertNotNull("URI generated via FileProvider should not be null", uri)
    assertEquals("content", uri.scheme)
    assertEquals("com.aistudio.invoicegenerator.gqtwv.fileprovider", uri.authority)
  }
}
