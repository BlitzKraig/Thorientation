package com.blitzkraig.thorientation.ui

import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blitzkraig.thorientation.databinding.ActivityAppLaunchPickerBinding
import com.blitzkraig.thorientation.databinding.ItemAppBinding
import com.blitzkraig.thorientation.engine.RotationEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppItem(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val launchIntent: Intent
)

class AppLaunchPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppLaunchPickerBinding
    private var targetDisplayId: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppLaunchPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val engine = RotationEngine.get(this)
        val bottomDisplay = engine.getDisplays().firstOrNull { !it.isPrimary }
        targetDisplayId = bottomDisplay?.displayId ?: 1

        binding.toolbar.title = "Launch App on Bottom Screen ($targetDisplayId)"
        binding.toolbar.setNavigationIcon(android.R.drawable.ic_menu_revert)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvApps.layoutManager = LinearLayoutManager(this)

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            val pm = packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
            val appList = mutableListOf<AppItem>()

            for (info in resolveInfos) {
                if (info.activityInfo.packageName == packageName) continue
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName) ?: continue
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                appList.add(AppItem(label, info.activityInfo.packageName, icon, launchIntent))
            }

            appList.sortBy { it.label.lowercase() }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.rvApps.adapter = AppAdapter(appList) { selectedApp ->
                    launchOnDisplay(selectedApp)
                }
            }
        }
    }

    private fun launchOnDisplay(app: AppItem) {
        try {
            val options = ActivityOptions.makeBasic()
            options.launchDisplayId = targetDisplayId

            app.launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            startActivity(app.launchIntent, options.toBundle())
            Toast.makeText(
                this,
                "Launching ${app.label} on Display $targetDisplayId",
                Toast.LENGTH_SHORT
            ).show()
            finish()
        } catch (e: Throwable) {
            Toast.makeText(
                this,
                "Failed to launch on Display $targetDisplayId: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    class AppAdapter(
        private val items: List<AppItem>,
        private val onClick: (AppItem) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.tvAppName.text = item.label
            holder.binding.tvPackageName.text = item.packageName
            holder.binding.ivAppIcon.setImageDrawable(item.icon)
            holder.binding.root.setOnClickListener { onClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
