package dev.edge.apkrenamer

import androidx.documentfile.provider.DocumentFile

data class ApkItem(
    val file: DocumentFile,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: String
)
