package com.blitzkraig.thorientation.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.database.ContentObserver
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.databinding.ActivityMainBinding
import com.blitzkraig.thorientation.databinding.ItemDisplayCardBinding
import com.blitzkraig.thorientation.engine.OverlayEngine
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.engine.SettingsEngine
import com.blitzkraig.thorientation.engine.ThorSettingsEngine
import com.blitzkraig.thorientation.model.DualPreset
import com.blitzkraig.thorientation.model.PrivilegeStatus
import com.blitzkraig.thorientation.model.ScreenOrientation
import com.blitzkraig.thorientation.receiver.BootReceiver
import com.blitzkraig.thorientation.service.OrientationAccessibilityService
import com.blitzkraig.thorientation.service.PersistentNotificationHelper
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var engine: RotationEngine
    private var thorSettingsObserver: ContentObserver? = null
    private var displayListener: DisplayManager.DisplayListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        engine = RotationEngine.get(this)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        thorSettingsObserver = ThorSettingsEngine.registerObserver(this) {
            refreshUi()
        }
        val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        if (dm != null) {
            displayListener = object : DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) { refreshUi() }
                override fun onDisplayRemoved(displayId: Int) { refreshUi() }
                override fun onDisplayChanged(displayId: Int) { refreshUi() }
            }
            dm.registerDisplayListener(displayListener, null)
        }
        refreshUi()
    }

    override fun onPause() {
        super.onPause()
        thorSettingsObserver?.let {
            ThorSettingsEngine.unregisterObserver(this, it)
            thorSettingsObserver = null
        }
        displayListener?.let {
            val dm = getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            dm?.unregisterDisplayListener(it)
            displayListener = null
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshUi()
    }

    private fun setupListeners() {
        binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
            onMasterToggle(isChecked)
        }
        binding.cardMasterSwitch.setOnClickListener {
            binding.switchMaster.isChecked = !binding.switchMaster.isChecked
        }

        binding.btnRefresh.setOnClickListener {
            refreshUi()
            Toast.makeText(this, "Displays refreshed", Toast.LENGTH_SHORT).show()
        }
        binding.btnPresetLandscape.setOnClickListener {
            applyPreset(DualPreset.LANDSCAPE)
        }
        binding.btnPresetPortrait.setOnClickListener {
            applyPreset(DualPreset.PORTRAIT)
        }
        binding.btnPresetPortraitReverse.setOnClickListener {
            applyPreset(DualPreset.PORTRAIT_REVERSE)
        }
        binding.btnPresetAuto.setOnClickListener {
            applyPreset(DualPreset.AUTO)
        }

        binding.switchPersistentNotif.setOnCheckedChangeListener { _, isChecked ->
            onPersistentNotifToggle(isChecked)
        }

        binding.btnNotifTargetPresets.setOnClickListener {
            PersistentNotificationHelper.setMode(this, PersistentNotificationHelper.MODE_PRESETS)
            updateNotifTargetHighlights()
            Toast.makeText(this, "Notification buttons: Presets", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotifTargetTop.setOnClickListener {
            PersistentNotificationHelper.setMode(this, PersistentNotificationHelper.MODE_TOP_SCREEN)
            updateNotifTargetHighlights()
            Toast.makeText(this, "Notification buttons: Top Screen", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotifTargetBottom.setOnClickListener {
            PersistentNotificationHelper.setMode(this, PersistentNotificationHelper.MODE_BOTTOM_SCREEN)
            updateNotifTargetHighlights()
            Toast.makeText(this, "Notification buttons: Bottom Screen", Toast.LENGTH_SHORT).show()
        }

        binding.switchStartAtBoot.setOnCheckedChangeListener { _, isChecked ->
            BootReceiver.setStartAtBootEnabled(this, isChecked)
            Toast.makeText(
                this,
                if (isChecked) "Start at boot enabled" else "Start at boot disabled",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.switchPersistenceService.setOnCheckedChangeListener { _, isChecked ->
            onPersistenceServiceToggle(isChecked)
        }
        binding.cardPersistenceService.setOnClickListener {
            binding.switchPersistenceService.isChecked = !binding.switchPersistenceService.isChecked
        }
    }

    private fun onMasterToggle(isChecked: Boolean) {
        engine.setMasterEnabled(isChecked)
        Toast.makeText(
            this,
            if (isChecked) "Thorientation enabled" else "Thorientation disabled — screens released",
            Toast.LENGTH_SHORT
        ).show()
        refreshUi()
    }

    private fun onPersistenceServiceToggle(isChecked: Boolean) {
        if (!isChecked) {
            // Turning OFF: If the service is running, it can disable itself directly
            if (OrientationAccessibilityService.disable()) {
                binding.tvPersistenceStatus.text = getString(R.string.pref_persistence_service_desc)
                Toast.makeText(this, "Background persistence disabled", Toast.LENGTH_SHORT).show()
            } else {
                // If service instance was null or disableSelf failed, direct user to settings to turn it off
                openAccessibilitySettings(forEnabling = false)
            }
        } else {
            // Turning ON: Accessibility services cannot be enabled programmatically; guide user to settings
            openAccessibilitySettings(forEnabling = true)
        }
    }

    private fun openAccessibilitySettings(forEnabling: Boolean) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val message = if (forEnabling) {
            "Enable 'Thorientation' in Accessibility to keep orientations active when closing apps"
        } else {
            "Disable 'Thorientation' in Accessibility settings"
        }
        try {
            startActivity(intent)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Cannot open accessibility settings", t)
            revertPersistenceSwitchState()
        }
    }

    private fun revertPersistenceSwitchState() {
        val isEnabled = OrientationAccessibilityService.isEnabledInSettings(this)
        binding.switchPersistenceService.setOnCheckedChangeListener(null)
        binding.switchPersistenceService.isChecked = isEnabled
        binding.switchPersistenceService.setOnCheckedChangeListener { _, isChecked ->
            onPersistenceServiceToggle(isChecked)
        }
    }

    private fun onPersistentNotifToggle(isChecked: Boolean) {
        if (isChecked) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATIONS
                )
            } else {
                PersistentNotificationHelper.setEnabled(this, true)
                binding.layoutNotifOptions.visibility = View.VISIBLE
                Toast.makeText(this, "Persistent notification enabled", Toast.LENGTH_SHORT).show()
            }
        } else {
            PersistentNotificationHelper.setEnabled(this, false)
            binding.layoutNotifOptions.visibility = View.GONE
            Toast.makeText(this, "Persistent notification disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyPreset(preset: DualPreset) {
        if (!engine.isMasterEnabled()) {
            engine.setMasterEnabled(true)
        }
        val ok = engine.applyPreset(preset)
        if (ok) {
            Toast.makeText(this, "Applied preset: ${getString(preset.titleRes)}", Toast.LENGTH_SHORT).show()
            refreshUi()
        } else {
            Toast.makeText(this, "Failed to apply preset. Check engine permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun refreshUi() {
        updateMasterSwitchUi()
        updateForceLandscapeBanner()
        updateEngineBanner()
        renderDisplayCards()
        updatePresetHighlights()
        updatePersistentNotifUi()
        updateStartAtBootUi()
        updatePersistenceServiceUi()
    }

    private fun updateMasterSwitchUi() {
        val isMaster = engine.isMasterEnabled()
        if (binding.switchMaster.isChecked != isMaster) {
            binding.switchMaster.setOnCheckedChangeListener(null)
            binding.switchMaster.isChecked = isMaster
            binding.switchMaster.setOnCheckedChangeListener { _, isChecked ->
                onMasterToggle(isChecked)
            }
        }

        if (isMaster) {
            binding.cardMasterSwitch.setBackgroundResource(R.drawable.bg_card_selected)
            binding.ivMasterPower.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.brand_accent)
            )
            binding.tvMasterStatus.text = getString(R.string.pref_master_switch_desc_on)
            binding.tvMasterStatus.setTextColor(ContextCompat.getColor(this, R.color.badge_success_text))
            binding.tvHeaderSubtitle.text = getString(R.string.subtitle)
        } else {
            binding.cardMasterSwitch.setBackgroundResource(R.drawable.bg_card)
            binding.ivMasterPower.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.text_tertiary)
            )
            binding.tvMasterStatus.text = getString(R.string.pref_master_switch_desc_off)
            binding.tvMasterStatus.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            binding.tvHeaderSubtitle.text = "${getString(R.string.subtitle)} • Off"
        }

        val contentAlpha = if (isMaster) 1.0f else 0.45f
        binding.containerDisplayCards.alpha = contentAlpha
        binding.btnPresetLandscape.alpha = contentAlpha
        binding.btnPresetPortrait.alpha = contentAlpha
        binding.btnPresetPortraitReverse.alpha = contentAlpha
        binding.btnPresetAuto.alpha = contentAlpha
    }

    private fun updateStartAtBootUi() {
        val isEnabled = BootReceiver.isStartAtBootEnabled(this)
        if (binding.switchStartAtBoot.isChecked != isEnabled) {
            binding.switchStartAtBoot.setOnCheckedChangeListener(null)
            binding.switchStartAtBoot.isChecked = isEnabled
            binding.switchStartAtBoot.setOnCheckedChangeListener { _, isChecked ->
                BootReceiver.setStartAtBootEnabled(this, isChecked)
                Toast.makeText(
                    this,
                    if (isChecked) "Start at boot enabled" else "Start at boot disabled",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updatePersistenceServiceUi() {
        val isEnabled = OrientationAccessibilityService.isEnabledInSettings(this)
        if (binding.switchPersistenceService.isChecked != isEnabled) {
            binding.switchPersistenceService.setOnCheckedChangeListener(null)
            binding.switchPersistenceService.isChecked = isEnabled
            binding.switchPersistenceService.setOnCheckedChangeListener { _, isChecked ->
                onPersistenceServiceToggle(isChecked)
            }
        }
        binding.tvPersistenceStatus.text = if (isEnabled) {
            "Active — orientations survive 'Close All' in App Switcher"
        } else {
            getString(R.string.pref_persistence_service_desc)
        }
    }

    private fun updatePersistentNotifUi() {
        val notifEnabled = PersistentNotificationHelper.isEnabled(this)
        if (binding.switchPersistentNotif.isChecked != notifEnabled) {
            binding.switchPersistentNotif.setOnCheckedChangeListener(null)
            binding.switchPersistentNotif.isChecked = notifEnabled
            binding.switchPersistentNotif.setOnCheckedChangeListener { _, isChecked ->
                onPersistentNotifToggle(isChecked)
            }
        }
        binding.layoutNotifOptions.visibility = if (notifEnabled) View.VISIBLE else View.GONE
        updateNotifTargetHighlights()
    }

    private fun updateNotifTargetHighlights() {
        val mode = PersistentNotificationHelper.getMode(this)
        highlightPresetButton(binding.btnNotifTargetPresets, mode == PersistentNotificationHelper.MODE_PRESETS)
        highlightPresetButton(binding.btnNotifTargetTop, mode == PersistentNotificationHelper.MODE_TOP_SCREEN)
        highlightPresetButton(binding.btnNotifTargetBottom, mode == PersistentNotificationHelper.MODE_BOTTOM_SCREEN)
    }

    private fun updateForceLandscapeBanner() {
        val isForceLandscape = ThorSettingsEngine.isForceLandscapeEnabled(this)
        binding.bannerForceLandscape.visibility = if (isForceLandscape) View.VISIBLE else View.GONE
    }

    private fun updateEngineBanner() {
        val hasOverlay = OverlayEngine.isAvailable(this)
        val hasWriteSettings = SettingsEngine.getStatus(this) == PrivilegeStatus.READY
        val isReady = hasOverlay && hasWriteSettings
        val isMaster = engine.isMasterEnabled()

        binding.ivEngineIcon.setImageResource(R.drawable.ic_dual_screen)
        binding.ivEngineIcon.setColorFilter(
            ContextCompat.getColor(this, if (isReady) R.color.text_primary else R.color.brand_warning)
        )
        binding.tvEngineTitle.text = "Engine: Overlay & Settings"
        binding.tvEngineBadge.text = when {
            !isReady -> "Setup Needed"
            !isMaster -> "Paused"
            else -> "Ready"
        }
        binding.tvEngineBadge.setTextColor(
            ContextCompat.getColor(
                this,
                when {
                    !isReady -> R.color.badge_warning_text
                    !isMaster -> R.color.badge_warning_text
                    else -> R.color.badge_success_text
                }
            )
        )

        if (!hasOverlay) {
            binding.tvEngineDetails.text = "Display over other apps permission required to maintain screen rotation."
            binding.btnFixPermission.visibility = View.VISIBLE
            binding.btnFixPermission.text = "Grant Overlay Permission"
            binding.btnFixPermission.setOnClickListener { requestOverlayPermission() }
        } else if (!hasWriteSettings) {
            binding.tvEngineDetails.text = "Modify system settings permission required for primary display rotation."
            binding.btnFixPermission.visibility = View.VISIBLE
            binding.btnFixPermission.text = "Grant Modify Settings"
            binding.btnFixPermission.setOnClickListener { requestWriteSettingsPermission() }
        } else {
            binding.tvEngineDetails.text = "Overlay and system settings permissions granted and active."
            binding.btnFixPermission.visibility = View.GONE
        }
    }

    private fun renderDisplayCards() {
        binding.containerDisplayCards.removeAllViews()
        val displays = engine.getDisplays()
        val inflater = LayoutInflater.from(this)

        for (display in displays) {
            val cardBinding = ItemDisplayCardBinding.inflate(inflater, binding.containerDisplayCards, false)

            cardBinding.tvScreenName.text = display.name
            cardBinding.tvScreenResolution.text = display.resolutionFormatted

            val currentOrientation = display.currentRotation
            cardBinding.tvCurrentOrientationBadge.text = getString(currentOrientation.shortLabelRes)

            val isTop = display.isPrimary
            cardBinding.ivScreenIcon.setImageResource(if (isTop) R.drawable.ic_top_screen else R.drawable.ic_bottom_screen)
            cardBinding.ivScreenIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary))

            // Highlight current orientation button
            highlightButton(cardBinding.btnPortrait, currentOrientation == ScreenOrientation.PORTRAIT)
            highlightButton(cardBinding.btnLandscape, currentOrientation == ScreenOrientation.LANDSCAPE)
            highlightButton(cardBinding.btnRevPortrait, currentOrientation == ScreenOrientation.REVERSE_PORTRAIT)
            highlightButton(cardBinding.btnAuto, currentOrientation == ScreenOrientation.AUTO)

            // Button actions
            cardBinding.btnPortrait.setOnClickListener { onSelectOrientation(display.displayId, ScreenOrientation.PORTRAIT) }
            cardBinding.btnLandscape.setOnClickListener { onSelectOrientation(display.displayId, ScreenOrientation.LANDSCAPE) }
            cardBinding.btnRevPortrait.setOnClickListener { onSelectOrientation(display.displayId, ScreenOrientation.REVERSE_PORTRAIT) }
            cardBinding.btnAuto.setOnClickListener { onSelectOrientation(display.displayId, ScreenOrientation.AUTO) }

            binding.containerDisplayCards.addView(cardBinding.root)
        }
    }

    private fun highlightButton(button: MaterialButton, isActive: Boolean) {
        if (isActive) {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active_bg))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active_icon))
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_inactive_bg))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_inactive_icon))
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_inactive_stroke))
            button.strokeWidth = (1 * resources.displayMetrics.density).toInt()
        }
    }

    private fun updatePresetHighlights() {
        val displays = engine.getDisplays()
        val topRot = displays.firstOrNull { it.isPrimary }?.currentRotation ?: displays.firstOrNull()?.currentRotation
        val bottomRot = displays.firstOrNull { !it.isPrimary }?.currentRotation

        val activePreset = if (topRot != null && bottomRot != null && topRot == bottomRot) {
            when (topRot) {
                ScreenOrientation.LANDSCAPE -> DualPreset.LANDSCAPE
                ScreenOrientation.PORTRAIT -> DualPreset.PORTRAIT
                ScreenOrientation.REVERSE_PORTRAIT -> DualPreset.PORTRAIT_REVERSE
                ScreenOrientation.AUTO -> DualPreset.AUTO
                else -> null
            }
        } else {
            null
        }

        highlightPresetButton(binding.btnPresetLandscape, activePreset == DualPreset.LANDSCAPE)
        highlightPresetButton(binding.btnPresetPortrait, activePreset == DualPreset.PORTRAIT)
        highlightPresetButton(binding.btnPresetPortraitReverse, activePreset == DualPreset.PORTRAIT_REVERSE)
        highlightPresetButton(binding.btnPresetAuto, activePreset == DualPreset.AUTO)
    }

    private fun highlightPresetButton(button: MaterialButton, isActive: Boolean) {
        if (isActive) {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active_bg))
            button.setTextColor(ContextCompat.getColor(this, R.color.button_active_text))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.button_active_icon))
            button.strokeWidth = 0
        } else {
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_card))
            button.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            button.iconTint = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_primary))
            button.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.surface_card_border))
            button.strokeWidth = (1 * resources.displayMetrics.density).toInt()
        }
    }

    private fun onSelectOrientation(displayId: Int, orientation: ScreenOrientation) {
        if (!engine.isMasterEnabled()) {
            engine.setMasterEnabled(true)
        }
        val success = engine.setOrientation(displayId, orientation)
        if (success) {
            Toast.makeText(this, "Set Display $displayId to ${getString(orientation.shortLabelRes)}", Toast.LENGTH_SHORT).show()
            refreshUi()
        } else {
            Toast.makeText(this, "Failed to rotate. Please check Overlay and Settings permissions.", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PersistentNotificationHelper.setEnabled(this, true)
                binding.switchPersistentNotif.isChecked = true
                binding.layoutNotifOptions.visibility = View.VISIBLE
                Toast.makeText(this, "Persistent notification enabled", Toast.LENGTH_SHORT).show()
            } else {
                binding.switchPersistentNotif.isChecked = false
                binding.layoutNotifOptions.visibility = View.GONE
                PersistentNotificationHelper.setEnabled(this, false)
                Toast.makeText(this, getString(R.string.notif_permission_required), Toast.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_NOTIFICATIONS = 1010
    }
}
