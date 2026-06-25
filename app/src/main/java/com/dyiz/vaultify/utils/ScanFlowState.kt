package com.dyiz.vaultify.utils

import android.graphics.Bitmap
import android.net.Uri

/**
 * Transient, in-memory state shared across the OCR flow screens (Docs → ExtractImage →
 * CropImage → ExtractedText). Kept as a process-lifetime singleton instead of navigation args
 * because the cropped [Bitmap] can be large and the [Uri] is only meaningful until the user
 * leaves the flow.
 */
object ScanFlowState {
    var imageUri: Uri? = null
    var croppedBitmap: Bitmap? = null
    var extractedText: String = ""

    /**
     * Id of the most recently saved vault item from the OCR flow. [DocsScreen] reads this on its
     * next composition so it can scroll to / visually flag the freshly-saved PDF, then clears
     * the field so subsequent compositions don't keep re-flagging the same item.
     */
    var lastSavedItemId: Long? = null

    fun setImage(uri: Uri?) {
        imageUri = uri
        croppedBitmap = null
    }

    fun clear() {
        imageUri = null
        croppedBitmap = null
        extractedText = ""
        // lastSavedItemId intentionally NOT cleared here — [DocsScreen] owns its lifecycle so
        // the highlight survives the navigation that follows [ExtractedTextScreen]'s back pop.
    }
}
