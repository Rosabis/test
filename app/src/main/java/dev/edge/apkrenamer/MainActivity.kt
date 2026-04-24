package dev.edge.apkrenamer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import miuix.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile
import kotlin.io.path.createTempFile

class MainActivity : AppCompatActivity() {

    private val allowedExt = setOf("apk", "apks", "apkm", "xapk")
    private var selectedTreeUri: Uri? = null
    private val items = mutableListOf<ApkItem>()
    private val adapter = ApkAdapter()

    private lateinit var tvDir: TextView
    private lateinit var cbRecursive: CheckBox
    private lateinit var cbAppName: CheckBox
    private lateinit var cbPackage: CheckBox
    private lateinit var cbVersionName: CheckBox
    private lateinit var cbVersionCode: CheckBox
    private lateinit var spSeparator: Spinner

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

        tvDir = findViewById(R.id.tvDir)
        cbRecursive = findViewById(R.id.cbRecursive)
        cbAppName = findViewById(R.id.cbAppName)
        cbPackage = findViewById(R.id.cbPackage)
        cbVersionName = findViewById(R.id.cbVersionName)
        cbVersionCode = findViewById(R.id.cbVersionCode)
        spSeparator = findViewById(R.id.spSeparator)

        val separators = listOf("_", " ", "-", "+", ".")
        spSeparator.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, separators)

        findViewById<RecyclerView>(R.id.rvItems).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        findViewById<Button>(R.id.btnPickDir).setOnClickListener {
            dirPicker.launch(null)
        }

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            val uri = selectedTreeUri
            if (uri == null) {
                toast(getString(R.string.error_no_dir))
                return@setOnClickListener
            }
            scanFiles(uri, cbRecursive.isChecked)
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
    }

    private fun scanFiles(uri: Uri, recursive: Boolean) {
        val root = DocumentFile.fromTreeUri(this, uri) ?: return
        val candidates = mutableListOf<DocumentFile>()
        collectCandidates(root, recursive, candidates)

        items.clear()
        candidates.forEach { file ->
            val parsed = parseApkMeta(this, file)
            if (parsed != null) items.add(parsed)
        }
        adapter.submit(items)
        toast(getString(R.string.scan_done, items.size))
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

        val pkgInfo = getPackageInfoFromArchive(packageManager, sourceFile.path) ?: return null
        val appInfo = pkgInfo.applicationInfo ?: return null
        appInfo.sourceDir = sourceFile.path
        appInfo.publicSourceDir = sourceFile.path

        val appName = packageManager.getApplicationLabel(appInfo).toString()
        val packageName = pkgInfo.packageName.orEmpty()
        val versionName = pkgInfo.versionName.orEmpty()
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toString()
        }

        return ApkItem(file, appName, packageName, versionName, versionCode)
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
        return cbAppName.isChecked || cbPackage.isChecked || cbVersionName.isChecked || cbVersionCode.isChecked
    }

    private fun buildName(item: ApkItem, sep: String): String {
        val parts = mutableListOf<String>()
        if (cbAppName.isChecked) parts.add(item.appName)
        if (cbPackage.isChecked) parts.add(item.packageName)
        if (cbVersionName.isChecked) parts.add(item.versionName)
        if (cbVersionCode.isChecked) parts.add(item.versionCode)
        return parts.joinToString(sep).sanitizeForFileName().ifBlank { "app" }
    }

    private fun renameFiles(): Int {
        val sep = spSeparator.selectedItem?.toString() ?: "_"
        val used = mutableSetOf<String>()
        var renamed = 0

        items.forEach { item ->
            val ext = item.file.name?.substringAfterLast('.', "") ?: return@forEach
            val base = buildName(item, sep)
            val uniqueBase = makeUnique(base, used)
            val newName = "$uniqueBase.$ext"
            val success = item.file.renameTo(newName)
            if (success) renamed++
        }
        return renamed
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

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
