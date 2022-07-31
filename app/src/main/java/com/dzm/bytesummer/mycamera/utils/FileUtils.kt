package com.dzm.bytesummer.mycamera.utils

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.loader.content.CursorLoader
import java.io.File

class FileUtils {
    companion object {
        const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        fun uriToPath(context: Context, uri: Uri?): String? {
            if (uri == null) return null
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            val loader = CursorLoader(context, uri, proj, null, null, null)
            val cursor = loader.loadInBackground()!!
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            return cursor.getString(columnIndex)
        }
    }
}