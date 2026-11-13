@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.jcotters.watchlists.documents

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeTabSeparatedText
import platform.darwin.NSObject
import platform.posix.memcpy
import kotlin.uuid.Uuid

@Composable
internal actual fun rememberDocumentActions(
    onPicked: (PickedDocument) -> Unit,
    onResult: (DocumentResult) -> Unit
): DocumentActions {
    val scope = rememberCoroutineScope()
    val picked by rememberUpdatedState(onPicked)
    val result by rememberUpdatedState(onResult)
    val actions = remember(scope) { IosDocumentActions(scope, { picked(it) }, { result(it) }) }
    DisposableEffect(actions) { onDispose { actions.dispose() } }
    return actions
}

private class IosDocumentActions(
    private val scope: CoroutineScope,
    private val onPicked: (PickedDocument) -> Unit,
    private val onResult: (DocumentResult) -> Unit,
) : DocumentActions {
    private var controller: UIViewController? = null
    private var exported: NSURL? = null
    private var reading = false
    private var alive = true
    private val delegate = DocumentPickerDelegate(::picked, ::cancelled)

    override fun open() {
        if (controller != null) return
        reading = true
        present(
            UIDocumentPickerViewController(
                forOpeningContentTypes = listOf(
                    UTTypeCommaSeparatedText,
                    UTTypeTabSeparatedText,
                    UTTypePlainText,
                    UTTypeJSON
                ), asCopy = true
            ).also { it.delegate = delegate })
    }

    override fun save(document: ExportDocument) = export(document, share = false)
    override fun share(document: ExportDocument) = export(document, share = true)
    private fun export(document: ExportDocument, share: Boolean) {
        if (controller != null) return
        reading = false
        scope.launch {
            try {
                val url = withContext(Dispatchers.Default) {
                    val folder = NSTemporaryDirectory() + "movers-export-" + Uuid.random().toString()
                    check(NSFileManager.defaultManager.createDirectoryAtPath(folder, true, null, null))
                    val target = NSURL.fileURLWithPath("$folder/${document.name}")
                    val data = if (document.bytes.isEmpty()) NSData() else document.bytes.usePinned {
                        NSData.create(
                            bytes = it.addressOf(0), length = document.bytes.size.toULong()
                        )
                    }
                    check(data.writeToURL(target, atomically = true))
                    target
                }
                exported = url
                if (!alive) {
                    cleanup(); return@launch
                }
                if (share) {
                    val sheet = UIActivityViewController(activityItems = listOf(url), applicationActivities = null)
                    sheet.completionWithItemsHandler = { _, completed, _, _ ->
                        controller = null; cleanup()
                        if (alive) onResult(if (completed) DocumentResult.Shared else DocumentResult.Cancelled)
                    }
                    present(sheet)
                } else present(
                    UIDocumentPickerViewController(
                        forExportingURLs = listOf(url),
                        asCopy = true
                    ).also { it.delegate = delegate })
            } catch (e: CancellationException) {
                cleanup(); throw e
            } catch (_: Exception) {
                cleanup(); if (alive) onResult(DocumentResult.Failed("Couldn't export this file."))
            }
        }
    }

    private fun present(view: UIViewController) {
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (presenter?.presentedViewController != null) presenter = presenter.presentedViewController
        if (presenter == null) {
            cleanup(); onResult(DocumentResult.Failed("Couldn't open file actions.")); return
        }
        view.popoverPresentationController?.let {
            it.sourceView = presenter.view; it.sourceRect = CGRectMake(0.0, 0.0, 1.0, 1.0)
        }
        controller = view
        presenter.presentViewController(view, animated = true, completion = null)
    }

    private fun picked(didPickDocumentsAtURLs: List<*>) {
        this.controller = null
        if (!alive) return
        if (!reading) {
            cleanup(); onResult(DocumentResult.Saved); return
        }
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
            ?: run { onResult(DocumentResult.Failed("Couldn't open this file.")); return }
        scope.launch {
            try {
                val document = withContext(Dispatchers.Default) {
                    val path = checkNotNull(url.path)
                    val attributes = checkNotNull(NSFileManager.defaultManager.attributesOfItemAtPath(path, null))
                    val size = checkNotNull(attributes[NSFileSize] as? NSNumber).longLongValue
                    require(size in 0..MaxImportBytes.toLong())
                    val data = checkNotNull(NSData.create(contentsOfURL = url))
                    require(data.length <= MaxImportBytes.toULong())
                    val bytes = ByteArray(data.length.toInt())
                    if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
                    PickedDocument(url.lastPathComponent ?: "films.txt", bytes)
                }
                if (alive) onPicked(document)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (alive) onResult(DocumentResult.Failed("Choose a readable file up to 5 MB."))
            } finally {
                // asCopy imports belong to this app; never remove an external provider's source.
                url.path?.takeIf { it.startsWith(NSHomeDirectory() + "/") }
                    ?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
            }
        }
    }

    private fun cancelled() {
        this.controller = null; cleanup()
        if (alive) onResult(DocumentResult.Cancelled)
    }

    private fun cleanup() {
        exported?.path?.substringBeforeLast('/')?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        exported = null
    }

    fun dispose() {
        alive = false
        (controller as? UIDocumentPickerViewController)?.delegate = null
        controller?.dismissViewControllerAnimated(false, null)
        controller = null; cleanup()
    }
}

private class DocumentPickerDelegate(
    private val onPicked: (List<*>) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) =
        onPicked(didPickDocumentsAtURLs)

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) = onCancelled()
}

@Composable
internal actual fun rememberImportHelpPreference(): ImportHelpPreference = remember {
    object : ImportHelpPreference {
        override var seen by mutableStateOf(NSUserDefaults.standardUserDefaults.boolForKey("movers.import-help.seen"))
            private set

        override fun dismiss() {
            seen = true; NSUserDefaults.standardUserDefaults.setBool(true, "movers.import-help.seen")
        }
    }
}
