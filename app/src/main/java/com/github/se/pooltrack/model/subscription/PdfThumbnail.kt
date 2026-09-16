package com.github.se.pooltrack.model.subscription

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri

/**
 * Renders the first page of the PDF at [uri] to a [Bitmap], or `null` if it can't be opened or has
 * no pages.
 */
fun renderFirstPdfPage(context: Context, uri: Uri): Bitmap? {
  val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
  pfd.use {
    PdfRenderer(it).use { renderer ->
      if (renderer.pageCount == 0) return null
      renderer.openPage(0).use { page ->
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap
      }
    }
  }
}
