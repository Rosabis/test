package dev.edge.apkrenamer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.concurrent.thread
import kotlin.io.path.createTempFile

class MainActivity : AppCompatActivity() {

    private val allowedExt = setOf("apk", "apks", "apkm", "xapk")
    private var selectedTreeUri: Uri? = null
    private var includeSubDirs = false
    private var useAppName = true
    private var usePackageName = false
    private var useVersionName = true
    private var useVersionCode = true
    private var selectedSeparator = "_"
    private var isScanning = false

    private val items = mutableListOf<ApkItem>()
    private val adapter: ApkAdapter by lazy {
        ApkAdapter {
            refreshPlannedNames()
            adapter.notifyDataSetChanged()
        }
    }
    private val renameHistory = mutableListOf<Pair<DocumentFile, String>>()

    private lateinit var tvDir: TextView

    private val dirPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                selectedTreeUri = uri
                tvDir.text = uri.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<MaterialToolbar>(R.id.toolbar).setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_scan -> {
                    val uri = selectedTreeUri
                    if (uri == null) {
                        toast(getString(R.string.error_no_dir))
                    } else {
                        scanFilesAsync(uri, includeSubDirs)
                    }
                    true
                }

                R.id.action_settings -> {
                    showSettingsDialog()
                    true
                }

                else -> false
            }
        }

        tvDir = findViewById(R.id.tvDir)
        tvDir.text = getString(R.string.no_directory_selected)

        findViewById<RecyclerView>(R.id.rvItems).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.btnRename).setOnClickListener {
            if (!hasAnyFieldSelected()) {
                toast(getString(R.string.error_no_fields))
                return@setOnClickListener
            }
            val renamedCount = renameFiles()
            toast(getString(R.string.rename_done, renamedCount))
            adapter.notifyDataSetChanged()
        }

        findViewById<Button>(R.id.btnRollback).setOnClickListener {
            val rollbackCount = rollbackNames()
            toast(getString(R.string.rollback_done, rollbackCount))
            adapter.notifyDataSetChanged()
        }
    }

    private fun scanFilesAsync(uri: Uri, recursive: Boolean) {
        if (isScanning) return
        isScanning = true
        toast("开始扫描...")

        thread(start = true) {
            val root = DocumentFile.fromTreeUri(this, uri)
            if (root == null) {
                runOnUiThread {
                    isScanning = false
                }
                return@thread
            }

            val candidates = mutableListOf<DocumentFile>()
            collectCandidates(root, recursive, candidates)

            val parsedItems = mutableListOf<ApkItem>()
            candidates.forEach { file ->
                val parsed = parseApkMeta(this, file)
                if (parsed != null) parsedItems.add(parsed)
            }

            runOnUiThread {
                isScanning = false
                items.clear()
                items.addAll(parsedItems)
                refreshPlannedNames()
                adapter.submit(items)
                toast(getString(R.string.scan_done, items.size))
            }
        }
    }

    private fun collectCandidates(root: DocumentFile, recursive: Boolean, out: MutableList<DocumentFile>) {
        root.listFiles().forEach { file ->
            when {
                file.isFile && isAllowed(file.name) -> out.add(file)
                recursive && file.isDirectory -> collectCandidates(file, true, out)
            }
        }
    }

    private fun isAllowed(name: String?): Boolean {
        if (name.isNullOrBlank() || !name.contains(".")) return false
        val ext = name.substringAfterLast('.').lowercase()
        return ext in allowedExt
    }

    private fun parseApkMeta(context: Context, file: DocumentFile): ApkItem? {
        val ext = file.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val sourceFile = when (ext) {
            "apk" -> copyUriToTempFile(context, file.uri, ".apk")
            "apks", "apkm", "xapk" -> extractOneApkFromArchive(context, file.uri)
            else -> null
        } ?: return null

        try {
            val pkgInfo = getPackageInfoFromArchive(packageManager, sourceFile.path) ?: return null
            val appInfo = pkgInfo.applicationInfo ?: return null
            appInfo.sourceDir = sourceFile.path
            appInfo.publicSourceDir = sourceFile.path

            val appName = packageManager.getApplicationLabel(appInfo).toString()
            val icon = packageManager.getApplicationIcon(appInfo)
            val packageName = pkgInfo.packageName.orEmpty()
            val versionName = pkgInfo.versionName.orEmpty()
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode.toString()
            }

            val currentName = file.name.orEmpty()
            val plannedName = currentName
            return ApkItem(file, icon, appName, packageName, versionName, versionCode, currentName, plannedName)
        } finally {
            sourceFile.delete()
        }
    }

    private fun getPackageInfoFromArchive(pm: PackageManager, path: String): PackageInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageArchiveInfo(path, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageArchiveInfo(path, 0)
        }
    }

    private fun extractOneApkFromArchive(context: Context, archiveUri: Uri): File? {
        val archive = copyUriToTempFile(context, archiveUri, ".zip")
        ZipFile(archive).use { zip ->
            val targetEntry = zip.entries().toList().firstOrNull { entry ->
                !entry.isDirectory && entry.name.lowercase().endsWith("base.apk")
            } ?: zip.entries().toList().firstOrNull { entry ->
                !entry.isDirectory && entry.name.lowercase().endsWith(".apk")
            }

            if (targetEntry != null) {
                val out = createTempFile(prefix = "inner_", suffix = ".apk").toFile()
                zip.getInputStream(targetEntry).use { input ->
                    FileOutputStream(out).use { output -> input.copyTo(output) }
                }
                return out
            }
        }
        return null
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, suffix: String): File? {
        val tmp = createTempFile(prefix = "apkmeta_", suffix = suffix).toFile()
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { out -> input.copyTo(out) }
        } ?: return null
        return tmp
    }

    private fun hasAnyFieldSelected(): Boolean {
        return useAppName || usePackageName || useVersionName || useVersionCode
    }

    private fun buildName(item: ApkItem, sep: String): String {
        val parts = mutableListOf<String>()
        if (useAppName) parts.add(item.appName)
        if (usePackageName) parts.add(item.packageName)
        if (useVersionName) parts.add(item.versionName)
        if (useVersionCode) parts.add(item.versionCode)
        return parts.joinToString(sep).sanitizeForFileName().ifBlank { "app" }
    }

    private fun renameFiles(): Int {
        val sep = selectedSeparator
        val used = mutableSetOf<String>()
        var renamed = 0

        items.forEach { item ->
            if (!item.isSelected) return@forEach
            val oldName = item.file.name ?: return@forEach
            val ext = oldName.substringAfterLast('.', "")
            val base = buildName(item, sep)
            val uniqueBase = makeUnique(base, used)
            val newName = "$uniqueBase.$ext"
            val success = item.file.renameTo(newName)
            if (success) {
                renamed++
                renameHistory.add(item.file to oldName)
                item.currentName = newName
                item.plannedName = newName
            }
        }
        refreshPlannedNames()
        return renamed
    }

    private fun rollbackNames(): Int {
        if (renameHistory.isEmpty()) return 0
        var rolledBack = 0
        for (i in renameHistory.size - 1 downTo 0) {
            val (file, oldName) = renameHistory[i]
            val success = file.renameTo(oldName)
            if (success) {
                rolledBack++
                renameHistory.removeAt(i)
            }
        }
        items.forEach { item ->
            item.currentName = item.file.name.orEmpty()
        }
        refreshPlannedNames()
        return rolledBack
    }

    private fun makeUnique(base: String, used: MutableSet<String>): String {
        if (used.add(base)) return base
        var index = 2
        while (true) {
            val candidate = "${base}($index)"
            if (used.add(candidate)) return candidate
            index++
        }
    }

    private fun String.sanitizeForFileName(): String {
        return replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    private fun showSettingsDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null, false)
        val cbRecursive = view.findViewById<CheckBox>(R.id.cbRecursive)
        val cbAppName = view.findViewById<CheckBox>(R.id.cbAppName)
        val cbPackage = view.findViewById<CheckBox>(R.id.cbPackage)
        val cbVersionName = view.findViewById<CheckBox>(R.id.cbVersionName)
        val cbVersionCode = view.findViewById<CheckBox>(R.id.cbVersionCode)
        val spSeparator = view.findViewById<Spinner>(R.id.spSeparator)
        val btnPickDir = view.findViewById<Button>(R.id.btnPickDir)

        val separators = listOf("_", " ", "-", "+", ".")
        spSeparator.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, separators)
        spSeparator.setSelection((separators.indexOf(selectedSeparator)).coerceAtLeast(0))
        cbRecursive.isChecked = includeSubDirs
        cbAppName.isChecked = useAppName
        cbPackage.isChecked = usePackageName
        cbVersionName.isChecked = useVersionName
        cbVersionCode.isChecked = useVersionCode

        btnPickDir.setOnClickListener { dirPicker.launch(null) }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setView(view)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                includeSubDirs = cbRecursive.isChecked
                useAppName = cbAppName.isChecked
                usePackageName = cbPackage.isChecked
                useVersionName = cbVersionName.isChecked
                useVersionCode = cbVersionCode.isChecked
                selectedSeparator = spSeparator.selectedItem?.toString() ?: "_"

                items.forEach { item -> item.currentName = item.file.name.orEmpty() }
                refreshPlannedNames()
                adapter.notifyDataSetChanged()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun refreshPlannedNames() {
        val used = mutableSetOf<String>()
        items.forEach { item ->
            if (!item.isSelected) {
                item.plannedName = item.currentName
                return@forEach
            }
            val ext = item.currentName.substringAfterLast('.', "")
            val base = buildName(item, selectedSeparator)
            val uniqueBase = makeUnique(base, used)
            item.plannedName = "$uniqueBase.$ext"
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
