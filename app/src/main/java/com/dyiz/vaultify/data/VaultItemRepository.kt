package com.dyiz.vaultify.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import com.dyiz.vaultify.Database.VaultItemDao
import com.dyiz.vaultify.Database.VaultItemEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultItemRepository @Inject constructor(
    private val vaultItemDao: VaultItemDao,
    @ApplicationContext private val context: Context
) {
    private val vaultDir: File
        get() {
            val dir = File(context.filesDir, "vault").apply { mkdirs() }
            return dir
        }
    suspend fun getItemsByType(type: String): List<VaultItemEntity> = withContext(Dispatchers.IO) {
        vaultItemDao.getAllByType(type)
    }

    suspend fun getCountByType(type: String): Int = withContext(Dispatchers.IO) {
        vaultItemDao.countByType(type)
    }
    /**
     * Persists in-memory [bytes] as a new vault item under [type]. This is the in-app ingestion
     * path used by things we generate ourselves (e.g. the OCR screen's "Save as PDF") where
     * there's no external [Uri] to copy from and nothing to unhide on the device afterwards.
     *
     * Filename sanitising matches [saveFromUri] so a user-supplied "My notes.pdf" is stored as
     * "<epoch>_My_notes.pdf" and survives round-tripping back out to the gallery.
     */
    suspend fun saveFromBytes(
        bytes: ByteArray,
        type: String,
        displayName: String,
        mimeType: String?
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val extension = displayName.substringAfterLast('.', "")
                .takeIf { it.length in 1..5 } ?: extensionFromMime(mimeType, type)
            val safeBase = displayName.substringBeforeLast('.').ifBlank { "document" }
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val safeName = "$safeBase.${if (extension.isNotEmpty()) extension else "bin"}"
            val typeDir = File(vaultDir, type).apply { mkdirs() }
            val destFile = File(typeDir, "${System.currentTimeMillis()}_$safeName")
            FileOutputStream(destFile).use { it.write(bytes) }
            val entity = VaultItemEntity(
                type = type,
                localFilePath = destFile.absolutePath,
                displayName = safeName,
                mimeType = mimeType,
                originalRelativePath = null
            )
            val id = vaultItemDao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveFromUri(uri: Uri, type: String, displayName: String?, mimeType: String?): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contentResolver: ContentResolver = context.contentResolver
            val name = displayName ?: getDisplayNameFromUri(contentResolver, uri) ?: "item_${System.currentTimeMillis()}"
            val extension = name.substringAfterLast('.', "").takeIf { it.length in 1..5 } ?: extensionFromMime(mimeType, type)
            val safeName = name.substringBeforeLast('.').replace(Regex("[^a-zA-Z0-9._-]"), "_") + ".${if (extension.isNotEmpty()) extension else "bin"}"
            val typeDir = File(vaultDir, type).apply { mkdirs() }
            val destFile = File(typeDir, "${System.currentTimeMillis()}_$safeName")
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }             ?: return@withContext Result.failure(Exception("Could not open stream"))
            val originalRelativePath = getOriginalRelativePath(contentResolver, uri)
                ?: getOriginalRelativePathFromDocumentUri(uri)
            val entity = VaultItemEntity(
                type = type,
                localFilePath = destFile.absolutePath,
                displayName = safeName,
                mimeType = mimeType,
                originalRelativePath = originalRelativePath
            )
            val id = vaultItemDao.insert(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extensionFromMime(mime: String?, type: String): String = when {
        mime != null -> when {
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("png") -> "png"
            mime.contains("gif") -> "gif"
            mime.contains("webp") -> "webp"
            mime.contains("mp4") || mime.contains("video") -> "mp4"
            mime.contains("mpeg") || mime.contains("audio") -> "mp3"
            mime.contains("pdf") -> "pdf"
            else -> "bin"
        }
        else -> when (type) {
            "IMAGE" -> "jpg"
            "VIDEO" -> "mp4"
            "AUDIO" -> "mp3"
            else -> "bin"
        }
    }

/*    suspend fun unhideAndDelete(entities: List<VaultItemEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            for (entity in entities) {
                val file = File(entity.localFilePath)
                if (!file.exists()) {
                    vaultItemDao.deleteById(entity.id)
                    continue
                }
                val publicDir = when (entity.type) {
                    "IMAGE" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    "VIDEO" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    "AUDIO" -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                    else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                }
                if (!publicDir.exists()) publicDir.mkdirs()
                val restoreFile = File(publicDir, entity.displayName)
                var copyTo = restoreFile
                var counter = 1
                while (copyTo.exists()) {
                    copyTo = File(publicDir, "${entity.displayName.substringBeforeLast(".")}_$counter.${entity.displayName.substringAfterLast(".")}")
                    counter++
                }
                file.copyTo(copyTo, overwrite = false)
                file.delete()
                vaultItemDao.delete(entity)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }*/
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun unhideAndDelete(entities: List<VaultItemEntity>): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        val resolver = context.contentResolver
        for (entity in entities) {
            try {
                val file = File(entity.localFilePath)
                if (!file.exists()) {
                    vaultItemDao.delete(entity)
                    continue
                }

                // 1. Restore to original folder if we saved it (e.g. Pictures/Wallpapers), else default by type
                val defaultRelativePath = when (entity.type) {
                    "IMAGE" -> Environment.DIRECTORY_PICTURES
                    "VIDEO" -> Environment.DIRECTORY_MOVIES
                    "AUDIO" -> Environment.DIRECTORY_MUSIC
                    else -> Environment.DIRECTORY_DOWNLOADS
                }
                val rawRelativePath = entity.originalRelativePath?.takeIf { it.isNotBlank() } ?: defaultRelativePath
                // Each MediaStore collection only accepts a fixed set of top-level directories.
                // If the stored path's first segment isn't on the allow-list, we MUST fall back to
                // the type's default — otherwise resolver.insert() throws "Primary directory X not
                // allowed for content://...". This is the defensive safety net that keeps restore
                // working even if some legacy row stored a bogus path.
                val relativePath = sanitizeRelativePath(entity.type, rawRelativePath, defaultRelativePath)
                val collectionUri = when (entity.type) {
                    "IMAGE" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "VIDEO" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "AUDIO" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }
                val relativePathForMediaStore = when (entity.type) {
                    "IMAGE", "VIDEO", "AUDIO" -> if (relativePath.isNotBlank()) relativePath.trimEnd('/') + "/" else relativePath
                    else -> relativePath
                }
                val fileMimeType = entity.mimeType?.takeIf { it.isNotBlank() } ?: when (entity.type) {
                    "IMAGE" -> "image/jpeg"
                    "VIDEO" -> "video/mp4"
                    "AUDIO" -> "audio/mpeg"
                    else -> "application/octet-stream"
                }
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, entity.displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, fileMimeType)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (entity.type == "FILE") {
                            if (relativePathForMediaStore.isNotBlank()) put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathForMediaStore)
                        } else {
                            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePathForMediaStore)
                        }
                    }
                }

                // 3. MediaStore mein entry create karein aur data copy karein
                val newUri = resolver.insert(collectionUri, contentValues)
                    ?: throw Exception("Could not create MediaStore entry for ${entity.displayName}")
                resolver.openOutputStream(newUri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw Exception("Could not write content for ${entity.displayName}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && entity.type != "FILE") {
                    try {
                        resolver.update(newUri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                    } catch (_: Exception) { }
                }

                // 4. Restore hone ke baad internal vault file delete kar dein
                file.delete()
                vaultItemDao.delete(entity)
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
        Result.success(Unit)
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}
    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        val entity = vaultItemDao.getById(id)
        entity?.let {
            File(it.localFilePath).takeIf { it.exists() }?.delete()
            vaultItemDao.delete(it)
        }
    }

    /**
     * Permanently removes the given vault entries and their on-disk files.
     *
     * This is distinct from [unhideAndDelete] — nothing is restored to the gallery or public
     * storage. Returns the count of items that were removed from the vault so the UI can report
     * it to the user.
     */
    suspend fun deletePermanently(entities: List<VaultItemEntity>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                var removed = 0
                for (entity in entities) {
                    try {
                        File(entity.localFilePath).takeIf { it.exists() }?.delete()
                    } catch (_: Exception) {
                        // Swallow per-file failures so one bad entry doesn't stop the rest.
                    }
                    vaultItemDao.delete(entity)
                    removed++
                }
                Result.success(removed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun getDisplayNameFromUri(contentResolver: ContentResolver, uri: Uri): String? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            } ?: null
        } catch (_: Exception) {
            null
        }
    }

    /** Reads RELATIVE_PATH from MediaStore URI (API 29+) so we can restore to same folder on unhide. */
    private fun getOriginalRelativePath(contentResolver: ContentResolver, uri: Uri): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
                val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                if (pathIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(pathIndex)?.takeIf { it.isNotBlank() }?.trim()
                } else null
            } ?: null
        } catch (_: Exception) {
            null
        }
    }

    /** For document URIs (e.g. from OPEN_DOCUMENT), get path so we restore to same folder on unhide. */
    private fun getOriginalRelativePathFromDocumentUri(uri: Uri): String? {
        if (!DocumentsContract.isDocumentUri(context, uri)) return null
        return try {
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    resolver.query(uri, arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)?.use { cursor ->
                        val idx = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                        if (idx >= 0 && cursor.moveToFirst()) {
                            cursor.getString(idx)?.trim()?.takeIf { it.isNotBlank() }?.trimEnd('/')
                        } else null
                    }
                } catch (_: Exception) {
                    null
                } ?: parseDocIdFallback(uri)
            } else {
                parseDocIdFallback(uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Validates that [raw] starts with a directory allowed for the given [type]'s MediaStore
     * collection. If not, returns [defaultPath]. This prevents "Primary directory X not allowed"
     * failures at restore time caused by stray / legacy values (e.g. the numeric MediaStore _ID
     * older builds wrote when we tried to parse a doc id like `image:12345`).
     *
     * Allow-lists follow AOSP:
     *   - Images: DCIM, Pictures
     *   - Video:  DCIM, Movies, Pictures
     *   - Audio:  Music, Alarms, Audiobooks, Notifications, Podcasts, Recordings, Ringtones
     *   - File:   empty → goes to Downloads
     */
    private fun sanitizeRelativePath(type: String, raw: String, defaultPath: String): String {
        val v = raw.trim().trimEnd('/')
        if (v.isBlank()) return defaultPath
        val firstSegment = v.substringBefore('/').trim()
        val allowedPrimaryDirs: List<String> = when (type) {
            "IMAGE" -> listOf(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_PICTURES)
            "VIDEO" -> listOf(Environment.DIRECTORY_DCIM, Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES)
            "AUDIO" -> listOf(
                Environment.DIRECTORY_MUSIC,
                "Alarms",
                "Audiobooks",
                Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_PODCASTS,
                "Recordings",
                Environment.DIRECTORY_RINGTONES,
            )
            "FILE" -> return ""
            else -> return defaultPath
        }
        return if (allowedPrimaryDirs.any { it.equals(firstSegment, ignoreCase = true) }) v
        else defaultPath
    }

    private fun parseDocIdFallback(uri: Uri): String? {
        return try {
            val docId = DocumentsContract.getDocumentId(uri) ?: return null
            val kind = when (val colon = docId.indexOf(':')) {
                -1 -> ""
                else -> docId.substring(0, colon).lowercase()
            }
            // Media-provider doc IDs (image:, video:, audio:) contain ONLY the MediaStore _ID
            // after the colon — no directory at all. Returning that numeric id would poison the
            // RELATIVE_PATH on restore ("Primary directory 12345 not allowed for content://..."),
            // so bail and let the restore path apply the correct default instead.
            if (kind == "image" || kind == "video" || kind == "audio") return null
            val pathPart = when (val colon = docId.indexOf(':')) {
                -1 -> docId
                else -> docId.substring(colon + 1)
            }.trimStart('/')
            if (pathPart.isBlank()) return ""
            val fullDir = pathPart.substringBeforeLast("/", pathPart).trimEnd('/')
            when {
                fullDir.isBlank() -> ""
                fullDir.equals(Environment.DIRECTORY_DOWNLOADS, ignoreCase = true) || fullDir.equals("Downloads", ignoreCase = true) -> Environment.DIRECTORY_DOWNLOADS
                fullDir.regionMatches(0, Environment.DIRECTORY_DOWNLOADS, 0, Environment.DIRECTORY_DOWNLOADS.length, ignoreCase = true) ->
                    Environment.DIRECTORY_DOWNLOADS + "/" + fullDir.substring(Environment.DIRECTORY_DOWNLOADS.length).trimStart('/')
                fullDir.regionMatches(0, "Downloads", 0, "Downloads".length, ignoreCase = true) ->
                    "Download/" + fullDir.substring("Downloads".length).trimStart('/')
                else -> fullDir
            }
        } catch (_: Exception) {
            null
        }
    }
}
