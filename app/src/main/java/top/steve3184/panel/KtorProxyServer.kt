package top.steve3184.panel

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

class KtorProxyServer(
    private val context: Context,
    initialBaseUrl: String
) {
    private var realBaseUrl: String = initialBaseUrl.removeSuffix("/")
    private val cleanRealBaseUrl: String
        get() = realBaseUrl
    var port: Int = 0
        private set
    private val proxyClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    private var server: ApplicationEngine? = null

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }


    fun start() {
        if (server != null) return
        Log.d("KtorProxyServer", "Server starting...")
        port = findFreePort()
        server = embeddedServer(Netty, port = port, host = "127.0.0.1") {
            install(WebSockets)

            routing {
                route("{...}") {
                    handle {
                        val uri = call.request.path()
                        Log.d("KtorProxyServer", "Serving [${call.request.httpMethod.value}] $uri")

                        when {
                            uri.startsWith("/api/") -> proxyApiRequest(call)
                            else -> serveStaticAsset(call, uri)
                        }
                    }
                }
                webSocket("/ws/{...}") {
                    proxyWebSocketRequest(this)
                }
            }
        }.start(wait = false)
        Log.d("KtorProxyServer", "Server started at http://127.0.0.1:$port")
    }

    fun stop() {
        Log.d("KtorProxyServer", "Server stopping...")
        server?.stop(1_000, 2_000)
        server = null
        Log.d("KtorProxyServer", "Server stopped.")
    }

    fun updateRealBaseUrl(newUrl: String) {
        Log.d("KtorProxyServer", "Updating realBaseUrl to: $newUrl")
        this.realBaseUrl = newUrl.removeSuffix("/")
    }

    private suspend fun proxyWebSocketRequest(clientSession: DefaultWebSocketServerSession) {
        val targetWsUrl = cleanRealBaseUrl.replaceFirst("http", "ws") + clientSession.call.request.uri
        Log.d("KtorProxyServer", "Attempting to proxy WebSocket connection to: $targetWsUrl")

        val requestBuilder = Request.Builder().url(targetWsUrl)

        val ignoredHeaders = setOf(
            "Host", "Connection", "Upgrade",
            "Sec-WebSocket-Key", "Sec-WebSocket-Version",
            "Sec-WebSocket-Extensions", "Sec-WebSocket-Accept"
        )

        clientSession.call.request.headers.forEach { key, values ->
            if (!ignoredHeaders.any { it.equals(key, ignoreCase = true) }) {
                values.forEach { value ->
                    requestBuilder.addHeader(key, value)
                }
            }
        }
        val request = requestBuilder.build()
        var backendSocket: WebSocket? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("KtorProxyServer", "Successfully connected to backend WebSocket.")
                backendSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                clientSession.launch {
                    try {
                        clientSession.send(Frame.Text(text))
                    } catch (e: Exception) {
                        Log.e("KtorProxyServer", "Error sending text message to client", e)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                clientSession.launch {
                    try {
                        clientSession.send(Frame.Binary(fin = true, data = bytes.toByteArray()))
                    } catch (e: Exception) {
                        Log.e("KtorProxyServer", "Error sending binary message to client", e)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("KtorProxyServer", "Backend WebSocket is closing: $code / $reason")
                clientSession.launch {
                    clientSession.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR,"Backend closed connection"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("KtorProxyServer", "Failed to connect or maintain backend WebSocket connection", t)
                clientSession.launch {
                    clientSession.close(CloseReason(CloseReason.Codes.INTERNAL_ERROR,"Backend connection failed"))
                }
            }
        }

        proxyClient.newWebSocket(request, listener)

        try {
            for (frame in clientSession.incoming) {
                when (frame) {
                    is Frame.Text -> {
                        backendSocket?.send(frame.readText())
                    }
                    is Frame.Binary -> {
                        backendSocket?.send(ByteString.of(*frame.readBytes()))
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            Log.w("KtorProxyServer", "Client WebSocket connection closed or failed.", e)
        } finally {
            Log.d("KtorProxyServer", "Client connection ended. Closing backend connection.")
            backendSocket?.close(1000, "Client disconnected")
        }
    }

    private suspend fun serveStaticAsset(call: ApplicationCall, uri: String) {
        val assetPath = uri.removePrefix("/").ifEmpty { "index.html" }

        try {
            val assetStream = context.assets.open(assetPath)
            val mimeType = getMimeType(assetPath)
            Log.d("KtorProxyServer", "Serving asset: $assetPath with MIME type: $mimeType")
            call.respondBytes(assetStream.readBytes(), ContentType.parse(mimeType))

        } catch (e: IOException) {
            Log.w("KtorProxyServer", "Asset not found at path: '$assetPath'. Deciding on SPA fallback.")
            val isLikelyFileAsset = assetPath.contains('.') && assetPath.substringAfterLast('.').length in 2..5
            if (!isLikelyFileAsset) {
                try {
                    Log.d("KtorProxyServer", "Path '$assetPath' looks like a page route. Attempting SPA fallback to index.html.")
                    val indexStream = context.assets.open("index.html")
                    call.respondBytes(indexStream.readBytes(), ContentType.Text.Html)
                } catch (e2: IOException) {
                    Log.e("KtorProxyServer", "CRITICAL: SPA fallback failed, index.html is missing from assets.", e2)
                    call.respond(HttpStatusCode.NotFound, "Not Found: index.html is missing.")
                }
            } else {
                Log.w("KtorProxyServer", "Asset '$assetPath' was not found and is a file type. Responding with 404.")
                call.respond(HttpStatusCode.NotFound, "Not Found: $assetPath")
            }
        }
    }

    private suspend fun proxyApiRequest(call: ApplicationCall) {
        try {
            val targetUrl = cleanRealBaseUrl + call.request.uri
            val requestBuilder = Request.Builder().url(targetUrl)

            call.request.headers.forEach { key, values ->
                if (!key.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                    !key.equals(HttpHeaders.Host, ignoreCase = true)
                ) {
                    values.forEach { value -> requestBuilder.addHeader(key, value) }
                }
            }

            val ktorMethod = call.request.httpMethod
            val body = if (ktorMethod == HttpMethod.Post || ktorMethod == HttpMethod.Put || ktorMethod == HttpMethod.Patch) {
                val requestBodyBytes = call.receive<ByteArray>()

                val payloadAsString = String(requestBodyBytes, Charsets.UTF_8)
                Log.d("KtorProxyServer", ">>> Request Payload (size: ${requestBodyBytes.size}): $payloadAsString")

                requestBodyBytes.toRequestBody(call.request.contentType().toString().toMediaTypeOrNull())
            } else {
                null
            }

            requestBuilder.method(ktorMethod.value, body)

            val response = proxyClient.newCall(requestBuilder.build()).execute()

            val responseBody = response.body
            val responseHeaders = response.headers
            val responseStatus = HttpStatusCode.fromValue(response.code)

            val headersBuilder = Headers.build {
                responseHeaders.forEach { (key, value) ->
                    if (!key.equals(HttpHeaders.ContentLength, ignoreCase = true) &&
                        !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)) {
                        append(key, value)
                    }
                }
            }

            if (responseBody != null) {
                call.respond(object : OutgoingContent.ReadChannelContent() {
                    override val contentLength: Long = responseBody.contentLength()
                    override val contentType: ContentType? = responseBody.contentType()?.toString()?.let { ContentType.parse(it) }
                    override val headers: Headers = headersBuilder
                    override val status: HttpStatusCode = responseStatus
                    override fun readFrom(): ByteReadChannel = responseBody.source().inputStream().toByteReadChannel()
                })
            } else {
                call.respond(responseStatus)
            }

        } catch (e: Exception) {
            Log.e("KtorProxyServer", "Ktor Proxy error", e)
            call.respond(HttpStatusCode.InternalServerError, "Proxy error: ${e.message}")
        }
    }

    private fun getMimeType(path: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(path)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }
}