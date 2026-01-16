package top.steve3184.panel

import android.app.Application
import android.util.Log

class PanelApplication : Application() {

    var proxyServer: KtorProxyServer? = null
        private set

    override fun onCreate() {
        super.onCreate()
        Log.d("PanelApplication", "Application onCreate")
    }

    fun startProxyServer(baseUrl: String) {
        if (proxyServer == null) {
            try {
                Log.d("PanelApplication", "Starting KtorProxyServer...")
                proxyServer = KtorProxyServer(this, baseUrl)
                proxyServer?.start()
            } catch (e: Exception) {
                Log.e("PanelApplication", "Failed to start KtorProxyServer on Application level", e)
            }
        }
    }

    fun stopProxyServer() {
        Log.d("PanelApplication", "Stopping KtorProxyServer...")
        proxyServer?.stop()
        proxyServer = null
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d("PanelApplication", "Application onTerminate")
        stopProxyServer()
    }
}