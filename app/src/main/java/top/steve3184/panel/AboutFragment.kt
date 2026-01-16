package top.steve3184.panel

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        val contentTextView: TextView = view.findViewById(R.id.textViewAboutContent)
        val versionTextView: TextView = view.findViewById(R.id.textViewPackageName) // 我们将重用这个 TextView

        contentTextView.text = getString(R.string.about_content)

        try {
            val context = requireContext()
            val packageManager: PackageManager = context.packageManager
            val packageName: String = context.packageName

            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }

            val versionName = packageInfo.versionName
            versionTextView.text = getString(R.string.version_info_format, versionName)

        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            versionTextView.text = getString(R.string.version_info_unavailable)
        }

        return view
    }
}