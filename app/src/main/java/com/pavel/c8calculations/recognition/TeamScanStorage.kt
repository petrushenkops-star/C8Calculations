package com.pavel.c8calculations.recognition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

object TeamScanStorage {
    private const val FILE_NAME = "team_structure_scan.png"

    fun save(context: Context, bitmap: Bitmap): Boolean = runCatching {
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }.getOrDefault(false)

    fun load(context: Context): Bitmap? = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) null else BitmapFactory.decodeFile(file.absolutePath)
    }.getOrNull()

    fun exists(context: Context): Boolean = File(context.filesDir, FILE_NAME).exists()
}
