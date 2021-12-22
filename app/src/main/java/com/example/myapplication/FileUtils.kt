package com.example.myapplication

import android.graphics.Bitmap.CompressFormat
import java.io.File

object FileUtils {
    fun getImageFormat(file: File): String {
        val fileName = file.name.lowercase()
        if (fileName.endsWith(".png")) {
            return CompressFormat.PNG.toString().lowercase()
        } else if (fileName.endsWith(".webp")) {
            return CompressFormat.WEBP.toString().lowercase()
        }
        return CompressFormat.JPEG.toString().lowercase()
    }

    fun createDirs(dir: File) {
        if (!dir.isDirectory) {
            dir.delete()
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
}