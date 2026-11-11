package com.jcotters.watchlists.documents

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
internal actual fun rememberDocumentActions(onPicked: (PickedDocument) -> Unit, onResult: (DocumentResult) -> Unit): DocumentActions {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val result by rememberUpdatedState(onResult)
    var saving by remember { mutableStateOf<ExportDocument?>(null) }
    var alive by remember { mutableStateOf(true) }
    DisposableEffect(Unit) { onDispose { alive = false } }
    val opener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { result(DocumentResult.Cancelled); return@rememberLauncherForActivityResult }
        scope.launch {
            try {
                val document = withContext(Dispatchers.IO) {
                    val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    } ?: "films.txt"
                    val bytes = checkNotNull(context.contentResolver.openInputStream(uri)).use { input ->
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= MaxImportBytes) { "Choose a file up to 5 MB." }
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    PickedDocument(name, bytes)
                }
                if (alive) picked(document)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { if (alive) result(DocumentResult.Failed(if (e is IllegalArgumentException) "Choose a readable file up to 5 MB." else "Couldn't open this file.")) }
        }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { response ->
        val document = saving
        saving = null
        val uri = response.data?.data
        if (response.resultCode != android.app.Activity.RESULT_OK || uri == null || document == null) {
            result(DocumentResult.Cancelled); return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                withContext(Dispatchers.IO) { checkNotNull(context.contentResolver.openOutputStream(uri, "w")).use { it.write(document.bytes) } }
                if (alive) result(DocumentResult.Saved)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { if (alive) result(DocumentResult.Failed("Couldn't save this file.")) }
        }
    }
    return remember(context, opener, saver) {
        object : DocumentActions {
            override fun open() {
                try { opener.launch(arrayOf("text/*", "application/json", "application/csv", "application/vnd.ms-excel")) }
                catch (_: Exception) { result(DocumentResult.Failed("No file picker is available.")) }
            }
            override fun save(document: ExportDocument) {
                if (saving != null) return
                saving = document
                try {
                    saver.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType(document.mimeType).putExtra(Intent.EXTRA_TITLE, document.name))
                } catch (_: Exception) { saving = null; result(DocumentResult.Failed("Couldn't open file saving.")) }
            }
            override fun share(document: ExportDocument) {
                scope.launch {
                    try {
                        val uri = withContext(Dispatchers.IO) {
                            val root = File(context.cacheDir, "list-exports").apply { mkdirs() }
                            root.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86_400_000 }?.forEach { it.deleteRecursively() }
                            val folder = File(root, java.util.UUID.randomUUID().toString()).apply { mkdirs() }
                            val file = File(folder, document.name).apply { writeBytes(document.bytes) }
                            FileProvider.getUriForFile(context, "${context.packageName}.list-files", file)
                        }
                        val intent = Intent(Intent.ACTION_SEND).setType(document.mimeType)
                            .putExtra(Intent.EXTRA_STREAM, uri).apply { clipData = ClipData.newRawUri("List export", uri) }
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        context.startActivity(Intent.createChooser(intent, "Export list"))
                        if (alive) result(DocumentResult.Shared)
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { if (alive) result(DocumentResult.Failed("Couldn't share this file.")) }
                }
            }
        }
    }
}

@Composable
internal actual fun rememberImportHelpPreference(): ImportHelpPreference {
    val context = LocalContext.current
    return remember(context) {
        val preferences = context.getSharedPreferences("movers.import-help", Context.MODE_PRIVATE)
        object : ImportHelpPreference {
            override var seen by mutableStateOf(preferences.getBoolean("seen", false))
                private set
            override fun dismiss() { seen = true; preferences.edit().putBoolean("seen", true).apply() }
        }
    }
}
