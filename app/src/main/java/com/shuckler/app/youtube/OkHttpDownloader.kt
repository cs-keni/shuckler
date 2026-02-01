package com.shuckler.app.youtube

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

/**
 * OkHttp-based Downloader for NewPipe Extractor. Required for search and stream extraction.
 */
class OkHttpDownloader(private val client: OkHttpClient) : Downloader() {

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val data = request.dataToSend()
        val requestBody: RequestBody? = data?.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        val okRequest = okhttp3.Request.Builder()
            .method(request.httpMethod(), requestBody)
            .url(request.url())
            .addHeader("User-Agent", USER_AGENT)
        for ((name, values) in request.headers()) {
            if (values.size == 1) {
                okRequest.header(name, values[0])
            } else {
                okRequest.removeHeader(name)
                for (value in values) {
                    okRequest.addHeader(name, value)
                }
            }
        }
        val response = client.newCall(okRequest.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }
        val body = response.body?.string()
        val headersMap = mutableMapOf<String, MutableList<String>>()
        val headers = response.headers
        for (i in 0 until headers.size) {
            headersMap.getOrPut(headers.name(i)) { mutableListOf() }.add(headers.value(i))
        }
        return Response(
            response.code,
            response.message,
            headersMap,
            body,
            response.request.url.toString()
        )
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; rv:78.0) Gecko/20100101 Firefox/78.0"
    }
}
