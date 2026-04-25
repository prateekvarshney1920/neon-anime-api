@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.example.neonanime

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// Theme Colors
internal val CyberBlack = Color(0xFF0D0D12)
val NeonPink = Color(0xFFFF007F)
val NeonCyan = Color(0xFF00F0FF)
val InstaGradientEnd = Color(0xFFDD2A7B)
val SurfaceDark = Color(0xFF1A1A24)

private val CyberpunkTheme = darkColorScheme(
    background = CyberBlack,
    surface = SurfaceDark,
    primary = NeonCyan,
    secondary = NeonPink,
    onBackground = Color.White
)

class AnimeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AppState>(AppState.Idle)
    val uiState: StateFlow<AppState> = _uiState.asStateFlow()

    private val okHttpClient = okhttp3.OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val api: NeonAnimeApi = Retrofit.Builder()
        .baseUrl("https://neon-anime-backend.onrender.com/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NeonAnimeApi::class.java)

    fun processMedia(context: Context, uri: Uri) {
        _uiState.value = AppState.Processing

        viewModelScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                if (bytes == null) {
                    _uiState.value = AppState.Error("Failed to read image")
                    return@launch
                }

                val requestBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", "image.jpg", requestBody)

                // Call API (Ensure your NeonAnimeApi interface is defined elsewhere in your project)
                val response = withContext(Dispatchers.IO) { api.processMedia(part) }

                if (response.status == "success" && response.image_base64 != null) {
                    val decodedBytes = Base64.decode(response.image_base64, Base64.DEFAULT)

                    val tempFile = withContext(Dispatchers.IO) {
                        val file = File(context.cacheDir, "processed_${UUID.randomUUID()}.jpg")
                        FileOutputStream(file).use { it.write(decodedBytes) }
                        file
                    }

                    _uiState.value = AppState.Success(
                        processedFile = tempFile,
                        caption = response.caption ?: "Caught in the digital crossfire. ⚡️"
                    )
                } else {
                    _uiState.value = AppState.Error(response.error ?: "Server error")
                }
            } catch (e: Exception) {
                _uiState.value = AppState.Error("Connection error. Is the server waking up?")
            }
        }
    }

    fun reset() {
        _uiState.value = AppState.Idle
    }
}

sealed class AppState {
    data object Idle : AppState()
    data object Processing : AppState()
    data class Success(val processedFile: File, val caption: String) : AppState()
    data class Error(val message: String) : AppState()
}

fun shareToInstagramStory(context: Context, file: File) {
    try {
        // Fix: Use FileProvider instead of Uri.fromFile
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
            setDataAndType(contentUri, "image/jpeg")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra("source_application", context.packageName)
        }

        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Instagram app not found", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun NeonAnimeScreen(
    modifier: Modifier = Modifier,
    viewModel: AnimeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { viewModel.processMedia(context, it) } }
    )

    MaterialTheme(colorScheme = CyberpunkTheme) {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "NEON.ANIME",
                    color = NeonCyan,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    modifier = Modifier.padding(bottom = 48.dp)
                )

                when (val currentState = state) {
                    is AppState.Idle -> {
                        CyberButton("SELECT MEDIA") {
                            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }
                    is AppState.Processing -> {
                        CircularProgressIndicator(color = NeonPink)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SYNTHESIZING...", color = NeonCyan)
                    }
                    is AppState.Success -> {
                        AsyncImage(
                            model = currentState.processedFile,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, NeonPink, RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
                            Text(text = currentState.caption, color = Color.LightGray, modifier = Modifier.padding(16.dp))
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CyberButton("SHARE TO IG", color = InstaGradientEnd) {
                                shareToInstagramStory(context, currentState.processedFile)
                            }
                            CyberButton("NEW", color = NeonCyan) { viewModel.reset() }
                        }
                    }
                    is AppState.Error -> {
                        Text(currentState.message, color = Color.Red, modifier = Modifier.padding(bottom = 16.dp))
                        CyberButton("RETRY") { viewModel.reset() }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberButton(text: String, color: Color = NeonCyan, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeonAnimeScreen() }
    }
}