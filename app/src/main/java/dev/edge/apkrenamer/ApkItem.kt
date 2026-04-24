package dev.edge.apkrenamer

import android.graphics.drawable.Drawable
import androidx.documentfile.provider.DocumentFile

data class ApkItem(
    val file: DocumentFile,
    val icon: Drawable?,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String,
    var currentName: String,
    var plannedName: String,
    var isSelected: Boolean = true
)
