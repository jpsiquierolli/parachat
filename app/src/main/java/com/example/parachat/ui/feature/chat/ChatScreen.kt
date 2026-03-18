package com.example.parachat.ui.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onBackClick: () -> Unit
) {
    val viewModel = viewModel<ChatViewModel>()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current

    val pinnedMessage by viewModel.pinnedMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearchActive by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permissão de áudio concedida. Tente gravar novamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Permissão de áudio negada", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = readBytesFromUri(context, it)
            val mimeType = context.contentResolver.getType(it) ?: "application/octet-stream"
            val extension = if (mimeType.contains("video")) "mp4" else "jpg"
            val type = if (mimeType.contains("video")) MessageType.VIDEO else MessageType.IMAGE
            if (bytes != null) {
                viewModel.sendMedia(bytes, extension, mimeType, type)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val stream = ByteArrayOutputStream()
            it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, stream)
            val bytes = stream.toByteArray()
            viewModel.sendMedia(bytes, "jpg", "image/jpeg", MessageType.IMAGE)
        }
    }
    
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
             getCurrentLocation(context) { lat, long ->
                 val message = Message(
                     senderId = currentUserId,
                     receiverId = userId,
                     content = "Lat: $lat, Long: $long",
                     type = MessageType.LOCATION,
                     latitude = lat,
                     longitude = long,
                     timestamp = System.currentTimeMillis()
                 )
                 viewModel.sendLocationMessage(message)
             }
        } else {
            Toast.makeText(context, "Permissão de localização negada", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
        }
    }

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        val file = File(context.cacheDir, "audio_record.mp3")
        audioFile = file
        
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Toast.makeText(context, "Gravando...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        recorder = newRecorder
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            
            audioFile?.let { file ->
                val bytes = file.readBytes()
                viewModel.sendMedia(bytes, "mp3", "audio/mpeg", MessageType.AUDIO)
                Toast.makeText(context, "Áudio enviado!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.onSearchQueryChange(it) },
                            placeholder = { Text("Buscar na conversa...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar busca")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text(text = "Chat") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (showBottomSheet) {
                    MediaBottomSheet(
                        onDismiss = { showBottomSheet = false },
                        onGalleryClick = {
                            showBottomSheet = false
                            galleryLauncher.launch("image/*")
                        },
                        onCameraClick = {
                            showBottomSheet = false
                            cameraLauncher.launch(null)
                        },
                        onLocationClick = {
                            showBottomSheet = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                 getCurrentLocation(context) { lat, long ->
                                     val message = Message(
                                         senderId = currentUserId,
                                         receiverId = userId,
                                         content = "Lat: $lat, Long: $long",
                                         type = MessageType.LOCATION,
                                         latitude = lat,
                                         longitude = long,
                                         timestamp = System.currentTimeMillis()
                                     )
                                     viewModel.sendLocationMessage(message)
                                 }
                             } else {
                                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                             }
                        },
                        onAudioClick = {
                            showBottomSheet = false
                            startRecording()
                        }
                    )
                }

                // Recording Indicator / Input Area
                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Red.copy(alpha = 0.1f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Gravando áudio... ", color = Color.Red)
                        IconButton(onClick = { stopRecording() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Parar", tint = Color.Red)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Anexar")
                        }
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { viewModel.onMessageChange(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Mensagem") }
                        )
                        IconButton(onClick = { viewModel.sendMessage() }) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (pinnedMessage != null) {
                PinnedMessageBar(
                    message = pinnedMessage,
                    onUnpin = { viewModel.unpinMessage() }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageItem(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        highlight = searchQuery,
                        onLongClick = {
                            viewModel.pinMessage(message)
                            Toast.makeText(context, "Mensagem fixada!", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedMessageBar(
    message: Message?,
    onUnpin: () -> Unit
) {
    if (message == null) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PushPin, contentDescription = "Fixado", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Mensagem fixada", style = MaterialTheme.typography.labelSmall)
                Text(text = message.content.ifBlank { "Mídia" }, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onUnpin) {
                Icon(Icons.Default.Close, contentDescription = "Desfixar", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    highlight: String = "",
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isCurrentUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongClick() }
                    )
                }
                .padding(8.dp)
        ) {
            Text(
                text = highlightedText(message.content, highlight),
                color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Imagem enviada",
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                )
            } else if (message.type == MessageType.LOCATION) {
                Text(
                     text = "📍 Localização",
                     color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (message.type == MessageType.AUDIO) {
                 Row(verticalAlignment = Alignment.CenterVertically) {
                     Icon(Icons.Default.Mic, contentDescription = null, tint = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                     Text(" Áudio", color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                 }
            }
        }
    }
}

@Composable
fun MediaBottomSheet(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onLocationClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    // A simple row of options since standard BottomSheet needs more setup in M3
   Row(
       modifier = Modifier
           .fillMaxWidth()
           .background(MaterialTheme.colorScheme.surface)
           .padding(16.dp),
       horizontalArrangement = Arrangement.SpaceEvenly
   ) {
       MediaOption(icon = Icons.Default.Image, label = "Galeria", onClick = onGalleryClick)
       MediaOption(icon = Icons.Default.CameraAlt, label = "Câmera", onClick = onCameraClick)
       MediaOption(icon = Icons.Default.LocationOn, label = "Local", onClick = onLocationClick)
       MediaOption(icon = Icons.Default.Mic, label = "Áudio", onClick = onAudioClick)
   }
}

@Composable
fun MediaOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label)
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val byteBuffer = ByteArrayOutputStream()
            val bufferSize = 1024
            val buffer = ByteArray(bufferSize)
            var len = 0
            while (inputStream.read(buffer).also { len = it } != -1) {
                byteBuffer.write(buffer, 0, len)
            }
            byteBuffer.toByteArray()
        }
    } catch (e: Exception) {
        null
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, onLocation: (Double, Double) -> Unit) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                onLocation(location.latitude, location.longitude)
            } else {
                 Toast.makeText(context, "Localização não disponível", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}

@Composable
private fun highlightedText(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    if (query.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val lower = text.lowercase()
    val target = query.lowercase()
    val builder = androidx.compose.ui.text.buildAnnotatedString {
        var start = 0
        while (true) {
            val index = lower.indexOf(target, startIndex = start)
            if (index < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            pushStyle(androidx.compose.ui.text.SpanStyle(background = MaterialTheme.colorScheme.tertiaryContainer))
            append(text.substring(index, index + target.length))
            pop()
            start = index + target.length
        }
    }
    return builder
}
