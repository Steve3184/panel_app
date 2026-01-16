package top.steve3184.panel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import top.steve3184.panel.databinding.ActivityWelcomeBinding
import java.io.IOException

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        if (sharedPreferences.contains(MainActivity.KEY_BASE_URL)) {
            navigateToMain()
            return
        }

        binding.buttonConnect.setOnClickListener {
            val url = binding.editTextUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                validateAndSaveServerUrl(url)
            } else {
                Toast.makeText(this, getString(R.string.prompt_enter_server_address), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setUiLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.buttonConnect.isEnabled = !isLoading
        binding.editTextUrl.isEnabled = !isLoading
    }

    private fun validateAndSaveServerUrl(url: String) {
        setUiLoading(true)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val cleanUrl = url.removeSuffix("/")
                val request = Request.Builder()
                    .url("$cleanUrl/api/panel-settings")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException(getString(R.string.toast_validation_failed, getString(R.string.invaild_server)))

                    val responseBody = response.body?.string() ?: ""
                    if (isValidPanelSettings(responseBody)) {
                        withContext(Dispatchers.Main) {
                            getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit()
                                .putString(MainActivity.KEY_BASE_URL, cleanUrl)
                                .apply()
                            Toast.makeText(this@WelcomeActivity, getString(R.string.toast_connection_successful), Toast.LENGTH_SHORT).show()
                            navigateToMain()
                        }
                    } else {
                        throw IOException(getString(R.string.toast_validation_failed, getString(R.string.invaild_server)))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setUiLoading(false)
                    val errorMessage = getString(R.string.toast_validation_failed, e.message ?: getString(R.string.unknown_error))
                    Toast.makeText(this@WelcomeActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun isValidPanelSettings(jsonString: String): Boolean {
        try {
            val json = JSONObject(jsonString)
            return json.has("panelName") || json.has("panelLogo") || json.has("gradioTunnel") ||
                    json.has("panelPort") || json.has("gradioTunnelUrl")
        } catch (e: JSONException) {
            return false
        }
    }
}