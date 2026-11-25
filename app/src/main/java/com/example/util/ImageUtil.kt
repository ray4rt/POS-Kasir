package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageUtil {
    /**
     * Copies an image from a URI to the app's internal storage and returns the absolute path.
     */
    fun saveImageToInternalStorage(context: Context, uriString: String?): String? {
        if (uriString == null) return null
        
        // If it's already an internal path (doesn't start with content:// or file://), return it
        if (!uriString.startsWith("content://") && !uriString.startsWith("file://")) {
            return uriString
        }

        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // Create a unique filename
            val fileName = "product_img_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, fileName)
            
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Deletes an image from internal storage given its path.
     */
    fun deleteImage(path: String?) {
        if (path == null) return
        try {
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
