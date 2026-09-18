package com.shipescape.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

fun exportDatastore(context: Context, datastoreDir: File, targetUri: Uri) {
    context.contentResolver.openOutputStream(targetUri)?.let { ZipOutputStream(it) }?.use { zos ->
        datastoreDir.listFiles()?.filter { it.isFile }?.forEach { file ->
            zos.putNextEntry(java.util.zip.ZipEntry(file.name))
            file.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
        }
    }
}

fun importDatastore(context: Context, datastoreDir: File, targetUri: Uri): Boolean = runCatching {
    datastoreDir.mkdirs()
    var cnt = 0
    context.contentResolver.openInputStream(targetUri)?.let { ZipInputStream(it) }?.use { zis ->
        generateSequence { zis.nextEntry }.forEach { entry ->
            if (!entry.isDirectory) {
                File(datastoreDir, File(entry.name).name).outputStream().use { zis.copyTo(it) }
                cnt++
            }
            zis.closeEntry()
        }
    } ?: error("")

    check(cnt > 0) { "" }
}.isSuccess

// 重启应用以重新加载配置
fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
    exitProcess(0)
}