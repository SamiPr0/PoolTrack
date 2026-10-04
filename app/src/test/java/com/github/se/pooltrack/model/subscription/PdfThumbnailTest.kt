package com.github.se.pooltrack.model.subscription

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import java.io.FileNotFoundException
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PdfThumbnailTest {

  private val uri = Uri.parse("content://pass/1")

  private fun contextFailingWith(error: Throwable): Context {
    val resolver = mockk<ContentResolver>()
    every { resolver.openFileDescriptor(uri, "r") } throws error
    return mockk { every { contentResolver } returns resolver }
  }

  @Test
  fun returnsNullWhenUriIsNotFound() {
    assertNull(renderFirstPdfPage(contextFailingWith(FileNotFoundException("gone")), uri))
  }

  @Test
  fun returnsNullWhenUriPermissionIsRevoked() {
    assertNull(renderFirstPdfPage(contextFailingWith(SecurityException("revoked")), uri))
  }

  @Test
  fun returnsNullWhenProviderReturnsNoDescriptor() {
    val resolver = mockk<ContentResolver>()
    every { resolver.openFileDescriptor(uri, "r") } returns null
    val context = mockk<Context> { every { contentResolver } returns resolver }
    assertNull(renderFirstPdfPage(context, uri))
  }
}
