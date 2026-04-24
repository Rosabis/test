package dev.edge.apkrenamer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import java.io.File
import java.io.FileOutputStream
import java.util.Enumeration
import java.util.zip.ZipFile
import kotlin.concurrent.thread
import kotlin.io.path.createTempFile

class MainActivity : AppCompatActivity() {
    companion object {
        private const val PREFS_NAME = "app_settings"
        private const val KEY_TREE_URI = "tree_uri"
        private const val KEY_INCLUDE_SUB_DIRS = "include_sub_dirs"
        private const val KEY_SEPARATOR = "separator"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FIELD_ORDER = "field_order"
        private const val KEY_IGNORE_EXT = "ignore_ext"
        private const val KEY_USE_APP_NAME = "use_app_name"
        private const val KEY_USE_PACKAGE = "use_package"
        private const val KEY_USE_VERSION_NAME = "use_version_name"
        private const val KEY_USE_VERSION_CODE = "use_version_code"

        private const val FIELD_APP_NAME = "appName"
        private const val FIELD_PACKAGE = "package"
        private const val FIELD_VERSION_NAME = "versionName"
        private const val FIELD_VERSION_CODE = "versionCode"
    }

    private val allowedExt = setOf("apk", "apks", "apkm", "xapk")
    private var selectedTreeUri: Uri? = null
    private var includeSubDirs = false
    private var ignoreFileExt = false
    private var useAppName = true
    private var usePackageName = false
    private var useVersionName = true
    private var useVersionCode = true
    private var selectedSeparator = "_"
    private var fieldOrder = mutableListOf(FIELD_APP_NAME, FIELD_PACKAGE, FIELD_VERSION_NAME, FIELD_VERSION_CODE)
    private var isScanning = false
    private var themeMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

    private val items = mutableListOf<ApkItem>()
    private val adapter: ApkAdapter by lazy {
        ApkAdapter {
            refreshPlannedNames()
            adapter.notifyDataSetChanged()
        }
    }
    private val renameHistory = mutableListOf<Pair<DocumentFile, String>>()
    private lateinit var prefs: SharedPreferences

    private lateinit var tvDir: TextView
    private lateinit var tvScanProgress: TextView
    private lateinit var pbScan: ProgressBar

    private val dirPicker =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                selectedTreeUri = uri
                prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
                tvDir.text = uri.toString()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        loadSettings()
        applyThemeMode(themeMode)
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
        tvScanProgress = findViewById(R.id.tvScanProgress)
        pbScan = findViewById(R.id.pbScan)
        tvDir.text = selectedTreeUri?.toString() ?: getString(R.string.no_directory_selected)

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
        runOnUiThread {
            tvScanProgress.visibility = TextView.VISIBLE
            pbScan.visibility = ProgressBar.VISIBLE
            pbScan.isIndeterminate = true
            pbScan.progress = 0
            tvScanProgress.text = getString(R.string.scan_progress_preparing)
        }

        thread(start = true) {
            val root = DocumentFile.fromTreeUri(this, uri)
            if (root == null) {
                runOnUiThread {
                    isScanning = false
                    hideScanProgress()
                }
                return@thread
            }

            val candidates = mutableListOf<DocumentFile>()
            collectCandidates(root, recursive, candidates)
            val total = candidates.size
            runOnUiThread {
                pbScan.isIndeterminate = false
                pbScan.max = total.coerceAtLeast(1)
                pbScan.progress = 0
                tvScanProgress.text = getString(R.string.scan_progress_value, 0, total)
            }

            val parsedItems = mutableListOf<ApkItem>()
            candidates.forEachIndexed { index, file ->
                val parsed = try {
                    parseApkMeta(this, file)
                } catch (_: Exception) {
                    null
                }
                if (parsed != null) parsedItems.add(parsed)
                val progress = index + 1
                runOnUiThread {
                    pbScan.progress = progress
                    tvScanProgress.text = getString(R.string.scan_progress_value, progress, total)
                }
            }

            runOnUiThread {
                isScanning = false
                items.clear()
                items.addAll(parsedItems)
                refreshPlannedNames()
                adapter.submit(items)
                toast(getString(R.string.scan_done, items.size))
                hideScanProgress()
            }
        }
    }

    private fun collectCandidates(root: DocumentFile, recursive: Boolean, out: MutableList<DocumentFile>) {
        root.listFiles().forEach { file ->
            when {
                file.isFile && isAllowedExt(file.name) -> out.add(file)
                file.isFile && ignoreFileExt && !isAllowedExt(file.name) -> {
                    if (hasZipHeader(file)) out.add(file)
                }
                recursive && file.isDirectory -> collectCandidates(file, true, out)
            }
        }
    }

    private fun isAllowedExt(name: String?): Boolean {
        if (name.isNullOrBlank() || !name.contains(".")) return false
        val ext = name.substringAfterLast('.').lowercase()
        return ext in allowedExt
    }

    private fun hasZipHeader(file: DocumentFile): Boolean {
        return try {
            contentResolver.openInputStream(file.uri)?.use { input ->
                val header = ByteArray(4)
                val bytesRead = input.read(header)
                if (bytesRead >= 4) {
                    // ZIP magic number: 0x504B0304
                    return header[0] == 0x50.toByte() &&
                            header[1] == 0x4B.toByte() &&
                            header[2] == 0x03.toByte() &&
                            header[3] == 0x04.toByte()
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun parseApkMeta(context: Context, file: DocumentFile): ApkItem? {
        val ext = file.name?.substringAfterLast('.', "")?.lowercase().orEmpty()
        val sourceFile = when (ext) {
            "apk" -> copyUriToTempFile(context, file.uri, ".apk")
            "apks", "apkm", "xapk" -> extractOneApkFromArchive(context, file.uri)
            else -> {
                // Non-standard extension: if ignoreFileExt is enabled and file is ZIP, treat as archive
                if (ignoreFileExt && hasZipHeader(file)) {
                    extractOneApkFromArchive(context, file.uri)
                } else null
            }
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
        val archive = copyUriToTempFile(context, archiveUri, ".zip") ?: return null
        try {
            ZipFile(archive).use { zip ->
                val entries = zip.entries().asList()
                val targetEntry = entries.firstOrNull { entry ->
                    !entry.isDirectory && entry.name.lowercase().endsWith("base.apk")
                } ?: entries.firstOrNull { entry ->
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
        } catch (_: Exception) {
            return null
        } finally {
            archive.delete()
        }
        return null
    }

    private fun <T> Enumeration<T>.asList(): List<T> {
        val list = mutableListOf<T>()
        while (hasMoreElements()) list.add(nextElement())
        return list
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
        val enabled = mapOf(
            FIELD_APP_NAME to useAppName,
            FIELD_PACKAGE to usePackageName,
            FIELD_VERSION_NAME to useVersionName,
            FIELD_VERSION_CODE to useVersionCode
        )
        val parts = mutableListOf<String>()
        fieldOrder.forEach { key ->
            if (enabled[key] != true) return@forEach
            when (key) {
                FIELD_APP_NAME -> parts.add(item.appName)
                FIELD_PACKAGE -> parts.add(item.packageName)
                FIELD_VERSION_NAME -> parts.add(item.versionName)
                FIELD_VERSION_CODE -> parts.add(item.versionCode)
            }
        }
        return parts.joinToString(sep).sanitizeForFileName().ifBlank { "app" }
    }

    private fun renameFiles(): Int {
        var renamed = 0

        items.forEach { item ->
            if (!item.isSelected) return@forEach
            val oldName = item.file.name ?: return@forEach
            val newName = item.plannedName
            if (newName == oldName) return@forEach
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
        val cbIgnoreExt = view.findViewById<CheckBox>(R.id.cbIgnoreExt)
        val spSeparator = view.findViewById<Spinner>(R.id.spSeparator)
        val btnPickDir = view.findViewById<Button>(R.id.btnPickDir)
        val rgTheme = view.findViewById<RadioGroup>(R.id.rgTheme)
        val rvFieldOrder = view.findViewById<RecyclerView>(R.id.rvFieldOrder)

        val workingOrder = fieldOrder.toMutableList()
        val selectedMap = mutableMapOf(
            FIELD_APP_NAME to useAppName,
            FIELD_PACKAGE to usePackageName,
            FIELD_VERSION_NAME to useVersionName,
            FIELD_VERSION_CODE to useVersionCode
        )

        val separators = listOf("_", " ", "-", "+", ".")
        spSeparator.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, separators)
        spSeparator.setSelection((separators.indexOf(selectedSeparator)).coerceAtLeast(0))
        cbRecursive.isChecked = includeSubDirs
        cbIgnoreExt.isChecked = ignoreFileExt

        btnPickDir.setOnClickListener { dirPicker.launch(null) }

        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> rgTheme.check(R.id.rbThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> rgTheme.check(R.id.rbThemeDark)
            else -> rgTheme.check(R.id.rbThemeSystem)
        }

        fun labelForField(field: String): String = when (field) {
            FIELD_APP_NAME -> getString(R.string.field_app_name)
            FIELD_PACKAGE -> getString(R.string.field_package_name)
            FIELD_VERSION_NAME -> getString(R.string.field_version_name)
            FIELD_VERSION_CODE -> getString(R.string.field_version_code)
            else -> field
        }

        val dragItems = workingOrder.map { FieldOrderItem(it, selectedMap[it] == true) }.toMutableList()

        val orderAdapter = FieldOrderAdapter(
            items = dragItems,
            labelProvider = ::labelForField,
            onCheckedChanged = { key, isChecked -> selectedMap[key] = isChecked }
        )
        rvFieldOrder.layoutManager = LinearLayoutManager(this)
        rvFieldOrder.adapter = orderAdapter

        val touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                val moving = dragItems.removeAt(from)
                dragItems.add(to, moving)
                orderAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
        })
        touchHelper.attachToRecyclerView(rvFieldOrder)

        orderAdapter.onStartDrag = { holder ->
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                touchHelper.startDrag(holder)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings))
            .setView(view)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                includeSubDirs = cbRecursive.isChecked
                ignoreFileExt = cbIgnoreExt.isChecked
                fieldOrder = dragItems.map { it.fieldKey }.toMutableList()
                useAppName = selectedMap[FIELD_APP_NAME] == true
                usePackageName = selectedMap[FIELD_PACKAGE] == true
                useVersionName = selectedMap[FIELD_VERSION_NAME] == true
                useVersionCode = selectedMap[FIELD_VERSION_CODE] == true
                selectedSeparator = spSeparator.selectedItem?.toString() ?: "_"
                themeMode = when (rgTheme.checkedRadioButtonId) {
                    R.id.rbThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.rbThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }

                items.forEach { item -> item.currentName = item.file.name.orEmpty() }
                refreshPlannedNames()
                adapter.notifyDataSetChanged()
                saveSettings()
                applyThemeMode(themeMode)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun loadSettings() {
        includeSubDirs = prefs.getBoolean(KEY_INCLUDE_SUB_DIRS, false)
        ignoreFileExt = prefs.getBoolean(KEY_IGNORE_EXT, false)
        selectedSeparator = prefs.getString(KEY_SEPARATOR, "_") ?: "_"
        useAppName = prefs.getBoolean(KEY_USE_APP_NAME, true)
        usePackageName = prefs.getBoolean(KEY_USE_PACKAGE, false)
        useVersionName = prefs.getBoolean(KEY_USE_VERSION_NAME, true)
        useVersionCode = prefs.getBoolean(KEY_USE_VERSION_CODE, true)
        themeMode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        val orderString = prefs.getString(KEY_FIELD_ORDER, null)
        if (!orderString.isNullOrBlank()) {
            val parsed = orderString.split(",").filter { it.isNotBlank() }.toMutableList()
            if (parsed.size == 4 && parsed.toSet().size == 4) {
                fieldOrder = parsed
            }
        }
        selectedTreeUri = prefs.getString(KEY_TREE_URI, null)?.let { Uri.parse(it) }
    }

    private fun saveSettings() {
        prefs.edit()
            .putBoolean(KEY_INCLUDE_SUB_DIRS, includeSubDirs)
            .putBoolean(KEY_IGNORE_EXT, ignoreFileExt)
            .putString(KEY_SEPARATOR, selectedSeparator)
            .putBoolean(KEY_USE_APP_NAME, useAppName)
            .putBoolean(KEY_USE_PACKAGE, usePackageName)
            .putBoolean(KEY_USE_VERSION_NAME, useVersionName)
            .putBoolean(KEY_USE_VERSION_CODE, useVersionCode)
            .putInt(KEY_THEME_MODE, themeMode)
            .putString(KEY_FIELD_ORDER, fieldOrder.joinToString(","))
            .apply()
    }

    private fun applyThemeMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun refreshPlannedNames() {
        val byParent = items.groupBy { it.file.parentFile?.uri?.toString().orEmpty() }
        byParent.values.forEach { group ->
            val occupiedNames = mutableSetOf<String>()
            group.firstOrNull()?.file?.parentFile?.listFiles()?.forEach { sibling ->
                sibling.name?.let { occupiedNames.add(it) }
            }

            group.forEach { item ->
                val current = item.currentName
                if (!item.isSelected) {
                    item.plannedName = current
                    return@forEach
                }

                val ext = current.substringAfterLast('.', "")
                val base = buildName(item, selectedSeparator)
                var candidate = "$base.$ext"

                if (candidate != current && candidate in occupiedNames) {
                    var index = 2
                    while (true) {
                        val next = "${base}($index).$ext"
                        if (next == current || next !in occupiedNames) {
                            candidate = next
                            break
                        }
                        index++
                    }
                }

                item.plannedName = candidate
                occupiedNames.remove(current)
                occupiedNames.add(candidate)
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun hideScanProgress() {
        tvScanProgress.visibility = TextView.GONE
        pbScan.visibility = ProgressBar.GONE
        pbScan.isIndeterminate = false
    }

    private data class FieldOrderItem(var fieldKey: String, var enabled: Boolean)

    private class FieldOrderAdapter(
        private val items: List<FieldOrderItem>,
        private val labelProvider: (String) -> String,
        private val onCheckedChanged: (String, Boolean) -> Unit
    ) : RecyclerView.Adapter<FieldOrderAdapter.FieldOrderHolder>() {

        var onStartDrag: ((RecyclerView.ViewHolder) -> Unit)? = null

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FieldOrderHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_field_order, parent, false)
            return FieldOrderHolder(view)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: FieldOrderHolder, position: Int) {
            holder.bind(items[position], labelProvider, onCheckedChanged, onStartDrag)
        }

        class FieldOrderHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
            private val cbEnabled = itemView.findViewById<CheckBox>(R.id.cbEnabled)
            private val tvDragHandle = itemView.findViewById<TextView>(R.id.tvDragHandle)

            fun bind(
                item: FieldOrderItem,
                labelProvider: (String) -> String,
                onCheckedChanged: (String, Boolean) -> Unit,
                onStartDrag: ((RecyclerView.ViewHolder) -> Unit)?
            ) {
                cbEnabled.setOnCheckedChangeListener(null)
                cbEnabled.text = labelProvider(item.fieldKey)
                cbEnabled.isChecked = item.enabled
                cbEnabled.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    item.enabled = isChecked
                    onCheckedChanged(item.fieldKey, isChecked)
                }
                tvDragHandle.setOnLongClickListener {
                    onStartDrag?.invoke(this)
                    true
                }
            }
        }
    }
}
