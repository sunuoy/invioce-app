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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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
}
