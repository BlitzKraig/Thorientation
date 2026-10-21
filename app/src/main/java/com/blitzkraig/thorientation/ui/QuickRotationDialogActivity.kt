package com.blitzkraig.thorientation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blitzkraig.thorientation.R
import com.blitzkraig.thorientation.databinding.ActivityQuickRotationBinding
import com.blitzkraig.thorientation.databinding.ItemRotationOptionBinding
import com.blitzkraig.thorientation.engine.RotationEngine
import com.blitzkraig.thorientation.model.ScreenOrientation

class QuickRotationDialogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuickRotationBinding
    private lateinit var engine: RotationEngine
    private var displayId: Int = 0
    private var displayName: String = "Screen"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuickRotationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Make dialog window wrap content and center
        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        engine = RotationEngine.get(this)
        displayId = intent.getIntExtra(EXTRA_DISPLAY_ID, 0)
        displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME)
            ?: if (displayId == 0) getString(R.string.tile_top_screen) else getString(R.string.tile_bottom_screen)

        setupHeader()
        populateOptions()

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun setupHeader() {
        binding.tvDialogTitle.text = displayName
        val isTop = (displayId == 0)
        binding.ivDialogIcon.setImageResource(if (isTop) R.drawable.ic_top_screen else R.drawable.ic_bottom_screen)
        binding.ivDialogIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_primary))

        if (com.blitzkraig.thorientation.engine.ThorSettingsEngine.isForceLandscapeEnabled(this)) {
            binding.tvDialogSubtitle.text = "Warning: Thor Force Landscape is ON"
            binding.tvDialogSubtitle.setTextColor(ContextCompat.getColor(this, R.color.brand_warning))
        }
    }

    private fun populateOptions() {
        binding.containerOptions.removeAllViews()

        val currentRotation = engine.getSavedOrientation(displayId)
            ?: engine.getDisplays().firstOrNull { it.displayId == displayId }?.currentRotation
            ?: ScreenOrientation.PORTRAIT

        val options = listOf(
            ScreenOrientation.PORTRAIT,
            ScreenOrientation.LANDSCAPE,
            ScreenOrientation.REVERSE_PORTRAIT,
            ScreenOrientation.AUTO
        )

        val inflater = LayoutInflater.from(this)

        for (option in options) {
            val itemBinding = ItemRotationOptionBinding.inflate(inflater, binding.containerOptions, false)
            itemBinding.tvOptionTitle.setText(option.labelRes)
            itemBinding.ivOptionIcon.setImageResource(option.iconRes)

            val isSelected = (option == currentRotation)
            if (isSelected) {
                itemBinding.optionRoot.setBackgroundResource(R.drawable.bg_card_selected)
                itemBinding.ivCheck.visibility = View.VISIBLE
                itemBinding.tvOptionTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_primary))
                itemBinding.ivOptionIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary))
            } else {
                itemBinding.optionRoot.setBackgroundResource(R.drawable.bg_card)
                itemBinding.ivCheck.visibility = View.GONE
                itemBinding.tvOptionTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                itemBinding.ivOptionIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary))
            }

            itemBinding.optionRoot.setOnClickListener {
                val success = engine.setOrientation(displayId, option)
                if (success) {
                    Toast.makeText(
                        this,
                        "$displayName: ${getString(option.shortLabelRes)} applied",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to rotate. Check engine permissions.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }

            binding.containerOptions.addView(itemBinding.root)
        }
    }

    companion object {
        const val EXTRA_DISPLAY_ID = "extra_display_id"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
    }
}
