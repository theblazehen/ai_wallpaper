package dev.blazelight.aiwallpaper.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.net.Uri
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.blazelight.aiwallpaper.MainActivity
import dev.blazelight.aiwallpaper.Utils
import kotlin.math.roundToInt

private fun getReadableImageDirectoryName(prefs: SharedPreferences): String {
    val savedUriString = prefs.getString("selectedImageDirectoryUri", null)
    return savedUriString?.let { uriString ->
        Uri.parse(uriString).path?.let { uriPath ->
            when {
                uriPath.startsWith("/tree/") -> uriPath.substringAfter("/tree/")
                else -> "Unknown Directory"
            }
        }
    } ?: "Directory Not Selected"
}
fun roundUpToNextMultipleOf64(value: Int): Int {
    return ((value + 63) / 64) * 64
}

private fun getDefaultResolution(context: Context): Pair<Int, Int> {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val size = Point()
    windowManager.defaultDisplay.getSize(size)

    return Pair(roundUpToNextMultipleOf64(size.x / 2), roundUpToNextMultipleOf64(size.y / 2))
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SettingsScreen(context: Context, prefs: SharedPreferences, utils: Utils) {
    val colors = MaterialTheme.colorScheme // Accessing the color scheme

    var apiKey by remember { mutableStateOf(prefs.getString("apiKey", "0000000") ?: "00000000") }
    var apiKeyVisible by remember { mutableStateOf(false) }

    var keepImageCount by remember { mutableStateOf(prefs.getInt("keepImageCount", 0).toString()) }
    var workModes = listOf("Set wallpaper", "Download only", "Muzei")
    var workMode by remember {
        mutableStateOf(
            prefs.getString("workMode", "Set wallpaper") ?: "Download only"
        )
    }
    var workModes_expanded by remember { mutableStateOf(false) }

    val refreshIntervalOptions = listOf(
        Pair(15, "15 minutes"),
        Pair(30, "30 minutes"),
        Pair(60, "1 hour"),
        Pair(120, "2 hours"),
        Pair(180, "3 hours"),
        Pair(360, "6 hours"),
        // Add more pairs as needed
    )
    val defaultRefreshInterval = 30
    var refreshInterval by remember {
        mutableStateOf(
            prefs.getInt(
                "refreshInterval",
                defaultRefreshInterval
            )
        )
    }
    var refreshMenuExpanded by remember { mutableStateOf(false) }

    val (defaultWidth, defaultHeight) = getDefaultResolution(context)

    var scale by remember { mutableStateOf(prefs.getFloat("scale", 1f).toString()) }
    var scaleFloat by remember { mutableStateOf(prefs.getFloat("scale", 1f)) }

    var width by remember { mutableStateOf(prefs.getInt("width", defaultWidth)) }
    var height by remember { mutableStateOf(prefs.getInt("height", defaultHeight)) }

    var upscale by remember { mutableStateOf(prefs.getBoolean("upscale", false)) }
    val models = listOf("stable_diffusion", "stable_diffusion_2.1", "Anything Diffusion", "ICBINP - I Can't Believe It's Not Photography", "SDXL 1.0")
    var model by remember {
        mutableStateOf(
            prefs.getString("model", "stable_diffusion") ?: "stable_diffusion"
        )
    }
    var models_expanded by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(prefs.getInt("steps", 30)) }
    var stepsFloat by remember { mutableStateOf(steps.toFloat()) }
    val savedPrompts = prefs.getStringSet("prompts", emptySet()) ?: emptySet()
    val promptTexts = remember { mutableStateListOf(*savedPrompts.toTypedArray()) }
    if (promptTexts.isEmpty() || promptTexts.last().isNotEmpty()) {
        promptTexts.add("") // Ensures there's always an empty prompt at the end
    }
    val imageDirectoryName = remember { mutableStateOf(getReadableImageDirectoryName(prefs)) }

    val buttonText = if (imageDirectoryName.value == "Directory Not Selected") {
        "Select Image Directory"
    } else {
        "Image Directory: ${imageDirectoryName.value}, Change?"
    }

    val selectDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? ->
            uri?.let {
                // Persist the URI permission and update SharedPreferences
                val takeFlags: Int =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                prefs.edit().putString("selectedImageDirectoryUri", uri.toString()).apply()

                // Update the image directory name state
                imageDirectoryName.value = getReadableImageDirectoryName(prefs)
            }
        }
    )


    fun saveSettings() {
        val filteredPrompts =
            promptTexts.filter { it.isNotBlank() }.toSet() // Filter out blank prompts

        with(prefs.edit()) {
            putString("apiKey", apiKey)
            putInt("keepImageCount", keepImageCount.toInt())
            putString("workMode", workMode)
            putFloat("scale", scale.toFloatOrNull() ?: 1f)
            putInt("width", width)
            putInt("height", height)
            putBoolean("upscale", upscale)
            putStringSet("prompts", filteredPrompts)
            putInt("steps", steps)
            putString("model", model)
            putInt("refreshInterval", refreshInterval)
            apply()
        }
        utils.startBackgroundWorker(context, prefs) // Pass necessary arguments
        Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
    }

    fun onPromptEdit(index: Int, updatedText: String) {
        promptTexts[index] = updatedText
        if (index == promptTexts.size - 1 && updatedText.isNotEmpty()) {
            promptTexts.add("")
        }
    }

    fun onDeletePrompt(index: Int) {
        if (promptTexts.size > 1) { // Prevent deleting the last prompt
            promptTexts.removeAt(index)
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {
                        context.startActivity(Intent(context, MainActivity::class.java))
                    }) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { saveSettings() }) {
                        Icon(imageVector = Icons.Filled.Save, contentDescription = "Save")
                    }
                },
                backgroundColor = colors.primaryContainer, // Set the AppBar's background color
                contentColor = colors.onPrimaryContainer // Set the AppBar's content color
            )
        },
        backgroundColor = colors.background // Set the Scaffold's background color
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text(
                    "Application settings",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface
                )
                Button(onClick = { selectDirectoryLauncher.launch(null) }) {
                    Text(buttonText)
                }
                OutlinedTextField(
                    value = keepImageCount,
                    onValueChange = {
                        keepImageCount = it
                    },
                    label = { Text("Keep image count (0 for infinite)") },
                    modifier = Modifier
                        .fillMaxWidth()
                    //  .padding(top = 8.dp)
                )
                // Display the current selected model and a button to show the dropdown
                ExposedDropdownMenuBox(
                    expanded = workModes_expanded,
                    onExpandedChange = {
                        workModes_expanded = !workModes_expanded
                    }
                ) {
                    // TextField acting as a dropdown trigger
                    TextField(
                        readOnly = true,
                        value = workMode,
                        onValueChange = { },
                        label = { Text("Set work mode", color = colors.onSurface) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                "Dropdown Arrow",
                                tint = colors.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            textColor = colors.onSurface

                        )
                    )

                    ExposedDropdownMenu(
                        expanded = workModes_expanded,
                        onDismissRequest = {
                            workModes_expanded = false
                        }
                    ) {
                        workModes.forEach { cur_mode ->
                            DropdownMenuItem(
                                onClick = {
                                    workMode = cur_mode
                                    workModes_expanded = false
                                }
                            ) {
                                Text(text = cur_mode)
                            }
                        }
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = refreshMenuExpanded,
                    onExpandedChange = {
                        refreshMenuExpanded = !refreshMenuExpanded
                    }

                ) {
                    TextField(
                        readOnly = true,
                        value = refreshIntervalOptions.first { it.first == refreshInterval }.second,
                        onValueChange = { },
                        label = { Text("Refresh Interval", color = colors.onSurface) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                "Dropdown Arrow",
                                tint = colors.onSurface
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            textColor = colors.onSurface

                        )
                    )
                    ExposedDropdownMenu(
                        expanded = refreshMenuExpanded,
                        onDismissRequest = { refreshMenuExpanded = false }
                    ) {
                        refreshIntervalOptions.forEach { (value, label) ->
                            DropdownMenuItem(onClick = {
                                refreshInterval = value
                                refreshMenuExpanded = false
                            }) {
                                Text(label)
                            }
                        }
                    }
                }


                Divider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                Text(
                    "Stable Horde",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image =
                            if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (apiKeyVisible) "Hide API Key" else "Show API Key"

                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description,
                                tint = colors.onSurface
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                    //  .padding(top = 8.dp)
                )
                Divider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                Text(
                    "Image generation",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface
                )
                /*Text(
                    "Scale Divisor: ${scale.format(1)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
                Slider(
                    value = scaleFloat,
                    onValueChange = { newValue ->
                        val stepValue = (newValue / 0.1f).roundToInt() * 0.1f
                        scaleFloat = stepValue.coerceIn(1f, 5f)
                        scale = scaleFloat.toString() // Update the scale string for saving later
                    },
                    valueRange = 1f..5.0f, // Set the range of the slider
                    steps = 19, // Number of steps between the start and end

                    onValueChangeFinished = {
                        // Update the scale string when the user stops dragging the slider
                        scale = scaleFloat.toString()
                    },
                    //modifier = Modifier.padding(top = 8.dp)
                )*/
                Text(
                    "Resolution (default half native)",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = width.toString(),
                        onValueChange = { width = roundUpToNextMultipleOf64(it.toIntOrNull() ?: 0) },
                        label = { Text("Width") },
                        modifier = Modifier
                            .weight(1f)
                        //  .padding(top = 8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = height.toString(),
                        onValueChange = { height = roundUpToNextMultipleOf64(it.toIntOrNull() ?: 0) },
                        label = { Text("Height") },
                        modifier = Modifier
                            .weight(1f)
                        //  .padding(top = 8.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Button to reset to default resolution
                    IconButton(onClick = {
                        width = defaultWidth
                        height = defaultHeight
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset",
                            tint = colors.onSurface
                        )
                    }


                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    //modifier = Modifier.padding(all = 8.dp) // Adjust padding as needed
                ) {
                    Checkbox(
                        checked = upscale,
                        onCheckedChange = { upscale = it }
                    )
                    Spacer(Modifier.width(8.dp)) // Space between checkbox and text
                    Text(
                        "Upscale",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface
                    )
                }
                // Display the current selected model and a button to show the dropdown
                ExposedDropdownMenuBox(
                    expanded = models_expanded,
                    onExpandedChange = {
                        models_expanded = !models_expanded
                    }
                ) {
                    // TextField acting as a dropdown trigger
                    TextField(
                        readOnly = true,
                        value = model,
                        onValueChange = { },
                        label = { Text("Select Model", color = colors.onSurface) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = models_expanded
                            )
                        },

                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            textColor = colors.onSurface

                        )
                    )

                    // Dropdown Menu with model options
                    ExposedDropdownMenu(
                        expanded = models_expanded,
                        onDismissRequest = {
                            models_expanded = false
                        }
                    ) {
                        models.forEach { cur_model ->
                            DropdownMenuItem(
                                onClick = {
                                    model = cur_model
                                    models_expanded = false
                                }
                            ) {
                                Text(text = cur_model)
                            }
                        }
                    }
                }
                Text(
                    "Steps: $steps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface
                )
                Slider(
                    value = stepsFloat,
                    onValueChange = { newValue ->
                        val stepValue = (newValue / 0.1f).roundToInt() * 0.1f
                        stepsFloat = newValue.coerceIn(10f, 100f)
                        steps = stepsFloat.toInt() // Update the scale string for saving later
                    },
                    valueRange = 10f..100.0f, // Set the range of the slider
                    steps = 99, // Number of steps between the start and end

                    onValueChangeFinished = {
                        // Update the scale string when the user stops dragging the slider
                        steps = stepsFloat.toInt()
                    },
                    //modifier = Modifier.padding(top = 8.dp)
                )

                Divider(
                    color = Color.Gray,
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                Text(
                    "Prompts",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface
                )
                promptTexts.forEachIndexed { index, text ->
                    OutlinedTextField(
                        value = text,
                        onValueChange = { updatedText -> onPromptEdit(index, updatedText) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        trailingIcon = {
                            if (index > 0 || promptTexts.size > 1) {
                                IconButton(onClick = { onDeletePrompt(index) }) {
                                    Icon(
                                        Icons.Filled.Delete, contentDescription = "Delete prompt",
                                        tint = colors.onSurface
                                    )
                                }
                            }
                        }
                    )
                }

            }
        }
    }
}
