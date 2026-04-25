@file:Suppress("PreviewAnnotationInFunctionWithParameters")

package com.example.neonanime
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.lifecycle.viewModelScope
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch




internal val CyberBlack = Color(0xFF0D0D12)
val NeonPink = Color(0xFFFF007F)
val NeonCyan = Color(0xFF00F0FF)
val InstaGradientStart = Color(0xFFF58529)
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

    fun processMedia(uri: Uri) {
        _uiState.value = AppState.Processing


        viewModelScope.launch {
            delay(3000)


            val generatedCaption = "Lost in the digital ether. 🌃✨ #Cyberpunk #AnimeArt #NeonDreams"
            _uiState.value = AppState.Success(
                originalUri = uri,
                processedUri = uri,
                caption = generatedCaption
            )
        }
    }

    fun reset() {
        _uiState.value = AppState.Idle
    }
}

sealed class AppState {
    data object Idle : AppState()
    data object Processing : AppState()
    data class Success(val originalUri: Uri, val processedUri: Uri, val caption: String) : AppState()
    data class Error(val message: String) : AppState()
}


fun shareToInstagramStory(context: Context, mediaUri: Uri) {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        setDataAndType(mediaUri, "image/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        putExtra("source_application", "NeonAnimeApp")
    }

    try {
        context.startActivity(intent)
    } catch (e: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "Instagram app not found", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
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
        onResult = { uri ->
            if (uri != null) {
                viewModel.processMedia(uri)
            }
        }
    )

    MaterialTheme(colorScheme = CyberpunkTheme) {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                            mediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    }
                    is AppState.Processing -> {
                        CircularProgressIndicator(color = NeonPink)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("SYNTHESIZING...", color = NeonCyan, letterSpacing = 2.sp)
                    }
                    is AppState.Success -> {

                        AsyncImage(
                            model = currentState.processedUri,
                            contentDescription = "Processed Media",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .border(2.dp, NeonPink, RoundedCornerShape(16.dp))
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = currentState.caption,
                                color = Color.LightGray,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))


                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            CyberButton("SHARE TO IG", color = InstaGradientEnd) {
                                shareToInstagramStory(context, currentState.processedUri)
                            }
                            CyberButton("NEW", color = NeonCyan) {
                                viewModel.reset()
                            }
                        }
                    }
                    is AppState.Error -> {
                        Text(currentState.message, color = Color.Red)
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CyberButtonPreview() {
    MaterialTheme(colorScheme = CyberpunkTheme) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CyberButton(text = "PREVIEW BUTTON") {}
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NeonAnimeScreen()
        }
    }
}