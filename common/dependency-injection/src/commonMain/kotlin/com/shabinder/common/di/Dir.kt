package com.shabinder.common.di

import androidx.compose.ui.graphics.ImageBitmap
import co.touchlab.kermit.Kermit
import com.shabinder.common.models.DownloadResult
import com.shabinder.common.models.TrackDetails
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

expect class Dir(
    logger: Kermit,
) {
    fun isPresent(path:String):Boolean
    fun fileSeparator(): String
    fun defaultDir(): String
    fun imageCacheDir(): String
    fun createDirectory(dirPath:String)
    suspend fun cacheImage(image: Any,path: String) // in Android = ImageBitmap, Desktop = BufferedImage
    suspend fun loadImage(url:String): ImageBitmap?
    suspend fun clearCache()
    suspend fun saveFileWithMetadata(mp3ByteArray: ByteArray, path: String, trackDetails: TrackDetails)
}

suspend fun downloadFile(url: String): Flow<DownloadResult> {
    return flow {
        val client = createHttpClient()
        val response = client.get<HttpStatement>(url).execute()
        val data = ByteArray(response.contentLength()!!.toInt())
        var offset = 0
        do {
            val currentRead = response.content.readAvailable(data, offset, data.size)
            offset += currentRead
            val progress = (offset * 100f / data.size).roundToInt()
            emit(DownloadResult.Progress(progress))
        } while (currentRead > 0)
        if (response.status.isSuccess()) {
            emit(DownloadResult.Success(data))
        } else {
            emit(DownloadResult.Error("File not downloaded"))
        }
        client.close()
    }
}
fun getNameURL(url: String): String {
    return url.substring(url.lastIndexOf('/') + 1, url.length)
}
/*
* Call this function at startup!
* */
fun Dir.createDirectories() {
    createDirectory(defaultDir())
    createDirectory(imageCacheDir())
    createDirectory(defaultDir() + "Tracks/")
    createDirectory(defaultDir() + "Albums/")
    createDirectory(defaultDir() + "Playlists/")
    createDirectory(defaultDir() + "YT_Downloads/")
}
fun Dir.finalOutputDir(itemName:String, type:String, subFolder:String, defaultDir:String, extension:String = ".mp3" ): String =
    defaultDir + removeIllegalChars(type) + this.fileSeparator() +
            if(subFolder.isEmpty())"" else { removeIllegalChars(subFolder) + this.fileSeparator()} +
            removeIllegalChars(itemName) + extension