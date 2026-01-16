package top.steve3184.panel

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class SetServerFragment : Fragment() {

    private lateinit var editTextServerUrl: EditText
    private lateinit var buttonSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences
    private val client = OkHttpClient()

    var onServerAddressSaved: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_set_server, container, false)

        editTextServerUrl = view.findViewById(R.id.editTextServerUrl)
        buttonSave = view.findViewById(R.id.buttonSave)
        progressBar = view.findViewById(R.id.progressBar)

        sharedPreferences = requireActivity().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

        buttonSave.setOnClickListener {
            val url = editTextServerUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                validateAndSaveServerUrl(url)
            } else {
                Toast.makeText(requireContext(), getString(R.string.prompt_enter_server_address), Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        setUiLoading(false)

        val savedUrl = sharedPreferences.getString(MainActivity.KEY_BASE_URL, "")
        editTextServerUrl.setText(savedUrl)
    }

    private fun setUiLoading(isLoading: Boolean) {
        activity?.runOnUiThread {
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            buttonSave.isEnabled = !isLoading
            editTextServerUrl.isEnabled = !isLoading
        }
    }

    private fun validateAndSaveServerUrl(url: String) {
        setUiLoading(true)
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
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
                            sharedPreferences.edit()
                                .putString(MainActivity.KEY_BASE_URL, cleanUrl)
                                .apply()

                            Toast.makeText(requireContext(), getString(R.string.toast_connection_successful), Toast.LENGTH_SHORT).show()

                            setUiLoading(false)
                            onServerAddressSaved?.invoke(cleanUrl)
                        }
                    } else {
                        throw IOException(getString(R.string.toast_validation_failed, getString(R.string.invaild_server)))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setUiLoading(false)
                    val errorMessage = getString(R.string.toast_validation_failed, e.message ?: getString(R.string.unknown_error))
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
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