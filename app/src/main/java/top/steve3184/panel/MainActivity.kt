package top.steve3184.panel

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import top.steve3184.panel.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var setServerFragment: SetServerFragment? = null
    private var aboutFragment: AboutFragment? = null
    private val isSettingsFragmentVisible: Boolean
        get() = binding.settingsFragmentContainer.isVisible
    private val isAboutFragmentVisible: Boolean
        get() = binding.aboutFragmentContainer.isVisible

    private var uploadMessage: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val PREFS_NAME = "PanelAppPrefs"
        const val KEY_BASE_URL = "baseUrl"
    }

    private fun getLocalServerUrl(): String {
        val server = (application as PanelApplication).proxyServer
        val port = server?.port ?: 0
        return "http://127.0.0.1:$port"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setupOnBackPressed()

        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (uploadMessage == null) return@registerForActivityResult
            var results: Array<Uri>? = null
            if (result.resultCode == RESULT_OK) {
                result.data?.dataString?.let {
                    results = arrayOf(Uri.parse(it))
                }
            }
            uploadMessage!!.onReceiveValue(results)
            uploadMessage = null
        }

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baseUrl = sharedPreferences.getString(KEY_BASE_URL, null)

        if (baseUrl == null) {
            navigateToWelcomeAndFinish()
            return
        }

        (application as PanelApplication).startProxyServer(baseUrl)

        setupWebView()
        showWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webview.apply {
            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true

            webViewClient = WebViewClient()
            webChromeClient = object : WebChromeClient() {
                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!isSettingsFragmentVisible && !isAboutFragmentVisible) {
                        supportActionBar?.title = title ?: "Panel"
                    }
                }

                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>,
                    fileChooserParams: FileChooserParams
                ): Boolean {
                    uploadMessage?.onReceiveValue(null)
                    uploadMessage = filePathCallback

                    val intent = fileChooserParams.createIntent()
                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        uploadMessage = null
                        return false
                    }
                    return true
                }
            }
        }
    }

    private fun showWebView() {
        binding.settingsFragmentContainer.visibility = View.GONE
        binding.aboutFragmentContainer.visibility = View.GONE
        binding.webview.visibility = View.VISIBLE

        supportActionBar?.title = binding.webview.title ?: "Panel"
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        setServerFragment?.let { if (it.isAdded) supportFragmentManager.beginTransaction().hide(it).commit() }
        aboutFragment?.let { if (it.isAdded) supportFragmentManager.beginTransaction().hide(it).commit() }

        if (binding.webview.url.isNullOrEmpty()) {
            val localUrl = getLocalServerUrl()
            Log.d("MainActivity", "Loading WebView with URL: $localUrl")
            binding.webview.loadUrl(localUrl)
        }
    }

    private fun showServerSettingsFragment() {
        if (isSettingsFragmentVisible) return

        binding.webview.visibility = View.GONE
        binding.settingsFragmentContainer.visibility = View.VISIBLE
        supportActionBar?.title = getString(R.string.menu_set_server)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (setServerFragment == null) {
            setServerFragment = SetServerFragment().apply {
                onServerAddressSaved = { newBaseUrl ->
                    (application as PanelApplication).proxyServer?.updateRealBaseUrl(newBaseUrl)

                    showWebView()
                    binding.webview.reload()
                }
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.settings_fragment_container, setServerFragment!!)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(setServerFragment!!)
                .commit()
        }
    }

    private fun showAboutFragment() {
        if (isAboutFragmentVisible) return

        showWebView()

        binding.webview.visibility = View.GONE
        binding.aboutFragmentContainer.visibility = View.VISIBLE
        supportActionBar?.title = getString(R.string.menu_about)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (aboutFragment == null) {
            aboutFragment = AboutFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.about_fragment_container, aboutFragment!!)
                .commit()
        } else {
            supportFragmentManager.beginTransaction()
                .show(aboutFragment!!)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_refresh -> {
                if (!isSettingsFragmentVisible && !isAboutFragmentVisible) {
                    binding.webview.reload()
                }
                true
            }
            R.id.action_set_server -> {
                showServerSettingsFragment()
                true
            }
            R.id.action_about -> {
                showAboutFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSettingsFragmentVisible || isAboutFragmentVisible) {
                    showWebView()
                } else if (binding.webview.canGoBack()) {
                    binding.webview.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun navigateToWelcomeAndFinish() {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}