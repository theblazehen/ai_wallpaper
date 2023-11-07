package dev.blazelight.aiwallpaper

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.core.view.children
import android.content.SharedPreferences

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val spinner: Spinner = findViewById(R.id.refreshIntervalSpinner)
        val adapter = ArrayAdapter.createFromResource(this,
            R.array.refresh_intervals, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val apiKeyEditText: EditText = findViewById(R.id.apiKeyEditText)
        val dynamicEditTextContainer: LinearLayout = findViewById(R.id.dynamicEditTextContainer)
        val addEditTextButton: Button = findViewById(R.id.addEditTextButton)
        val saveButton: Button = findViewById(R.id.saveButton)


        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedIntervalPosition = prefs.getInt("refreshIntervalPosition", 0)
        val savedApiKey = prefs.getString("apiKey", "0000000000")
        val savedPrompts = prefs.getStringSet("prompts", emptySet())

        val widthEditText: EditText = findViewById(R.id.widthEditText)
        val heightEditText: EditText = findViewById(R.id.heightEditText)
        val scaleEditText: EditText = findViewById(R.id.scaleEditText)

        val savedWidth = prefs.getInt("width", calculateDeviceWidth())
        val savedHeight = prefs.getInt("height", calculateDeviceHeight())
        val savedScale = prefs.getFloat("scale", 1f) // Default scale is 1 (no scaling)

        widthEditText.setText(savedWidth.toString())
        heightEditText.setText(savedHeight.toString())
        scaleEditText.setText(savedScale.toString())

        spinner.setSelection(savedIntervalPosition)
        apiKeyEditText.setText(savedApiKey)
        savedPrompts?.forEach { addPromptRow(dynamicEditTextContainer, it) }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("refreshIntervalPosition", position).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        addEditTextButton.setOnClickListener {
            addPromptRow(dynamicEditTextContainer, "")
        }

        saveButton.setOnClickListener {
            val editor = prefs.edit()
            editor.putString("apiKey", apiKeyEditText.text.toString())

            val widthValue = widthEditText.text.toString().toIntOrNull() ?: calculateDeviceWidth()
            val heightValue = heightEditText.text.toString().toIntOrNull() ?: calculateDeviceHeight()

            editor.putInt("width", widthValue)
            editor.putInt("height", heightValue)

            val prompts = dynamicEditTextContainer.children.map {
                val editText = it.findViewById<EditText>(R.id.promptEditText)
                editText.text.toString()
            }.toSet()

            // Get the scale value and ensure it's a positive float
            val scaleValue = scaleEditText.text.toString().toFloatOrNull() ?: 1f

            editor.putFloat("scale", scaleValue)

            editor.putStringSet("prompts", prompts)
            editor.apply()

            finish()
        }
    }

    private fun addPromptRow(container: LinearLayout, text: String) {
        val promptRow = LayoutInflater.from(this).inflate(R.layout.prompt_row, container, false)
        val editText: EditText = promptRow.findViewById(R.id.promptEditText)
        val removeButton: ImageButton = promptRow.findViewById(R.id.removeButton)

        editText.setText(text)
        removeButton.setOnClickListener { container.removeView(promptRow) }

        container.addView(promptRow)
    }

    // Calculate the device width rounded up to the next multiple of 64
    fun calculateDeviceWidth(): Int {
        val screenWidth = resources.displayMetrics.widthPixels
        return roundUpToNextMultipleOf64(screenWidth)
    }

    // Calculate the device height rounded up to the next multiple of 64
    fun calculateDeviceHeight(): Int {
        val screenHeight = resources.displayMetrics.heightPixels
        return roundUpToNextMultipleOf64(screenHeight)
    }

}