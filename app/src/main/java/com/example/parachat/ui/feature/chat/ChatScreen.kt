package com.example.parachat.ui.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageType
import com.example.parachat.domain.displayName
import com.example.parachat.domain.displayNameFromParts
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    onBackClick: () -> Unit
) {
    val viewModel = hiltViewModel<ChatViewModel>()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val otherUser by viewModel.otherUser.collectAsState()
    val pinnedMessage by viewModel.pinnedMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentUserId = viewModel.currentUserId
    val context = LocalContext.current

    var isSearchActive by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder: MediaRecorder? by remember { mutableStateOf(null) }
    var audioFile: File? by remember { mutableStateOf(null) }
    var showMediaOptions by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permissão de áudio negada", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bytes = readBytesFromUri(context, it)
            if (bytes != null) {
                val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                val extension = if (mimeType.contains("video")) "mp4" else "jpg"
                val type = if (mimeType.contains("video")) MessageType.VIDEO else MessageType.IMAGE
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

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            getCurrentLocation(context) { lat, long ->
                val message = Message(
                    senderId = currentUserId,
                    receiverId = userId,
                    content = "Localização: $lat, $long",
                    type = MessageType.LOCATION,
                    latitude = lat,
                    longitude = long,
                    timestamp = System.currentTimeMillis()
                )
                viewModel.sendLocationMessage(message)
            }
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

        val file = File(context.cacheDir, "audio_record.m4a")
        audioFile = file
        
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            
            audioFile?.let { file ->
                val bytes = file.readBytes()
                viewModel.sendMedia(bytes, "m4a", "audio/mp4", MessageType.AUDIO)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = otherUser?.displayName() ?: displayNameFromParts(username = null, email = "", id = userId))
                        otherUser?.let {
                            Text(
                                text = it.status,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (it.status == "ONLINE") Color(0xFF4CAF50) else Color.Gray
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    placeholder = { Text("Buscar mensagens...") },
                    trailingIcon = {
                        IconButton(onClick = { 
                            isSearchActive = false
                            viewModel.onSearchQueryChange("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar")
                        }
                    },
                    singleLine = true
                )
            }

            pinnedMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (msg.type == MessageType.TEXT) msg.content else "[Mídia]",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        IconButton(onClick = { viewModel.unpinMessage() }) {
                            Icon(Icons.Default.Close, contentDescription = "Desafixar", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        onLongClick = { viewModel.pinMessage(message) },
                        searchQuery = searchQuery
                    )
                }
            }

            if (showMediaOptions) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Galeria")
                    }
                    IconButton(onClick = { cameraLauncher.launch(null) }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Câmera")
                    }
                    IconButton(onClick = { 
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            getCurrentLocation(context) { lat, long ->
                                val message = Message(
                                    senderId = currentUserId,
                                    receiverId = userId,
                                    content = "Localização: $lat, $long",
                                    type = MessageType.LOCATION,
                                    latitude = lat,
                                    longitude = long,
                                    timestamp = System.currentTimeMillis()
                                )
                                viewModel.sendLocationMessage(message)
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }) {
                        Icon(Icons.Default.LocationOn, contentDescription = "Localização")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showMediaOptions = !showMediaOptions }) {
                    Icon(if (showMediaOptions) Icons.Default.Close else Icons.Default.Add, contentDescription = "Opções")
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = viewModel::onMessageChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mensagem") },
                    maxLines = 4
                )
                if (messageText.isBlank()) {
                    IconButton(onClick = { if (isRecording) stopRecording() else startRecording() }) {
                        Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, 
                             contentDescription = "Áudio",
                             tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary)
                    }
                } else {
                    IconButton(onClick = { viewModel.sendMessage() }) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onLongClick: () -> Unit,
    searchQuery: String
) {
    val alignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer

    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = alignment) {
        Card(
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongClick() })
            },
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                when (message.type) {
                    MessageType.TEXT -> {
                        val annotatedText = if (searchQuery.isNotBlank()) {
                            buildAnnotatedString {
                                val content = message.content
                                var lastIndex = 0
                                val query = searchQuery.lowercase()
                                val lowerContent = content.lowercase()
                                var index = lowerContent.indexOf(query, lastIndex)
                                while (index != -1) {
                                    append(content.substring(lastIndex, index))
                                    withStyle(style = SpanStyle(background = Color.Yellow, fontWeight = FontWeight.Bold)) {
                                        append(content.substring(index, index + query.length))
                                    }
                                    lastIndex = index + query.length
                                    index = lowerContent.indexOf(query, lastIndex)
                                }
                                if (lastIndex < content.length) {
                                    append(content.substring(lastIndex))
                                }
                            }
                        } else {
                            AnnotatedString(message.content)
                        }
                        Text(text = annotatedText)
                    }
                    MessageType.IMAGE -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                    MessageType.VIDEO -> {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = null,
                                modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = 0.3f))
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Vídeo",
                                modifier = Modifier.size(48.dp),
                                tint = Color.White
                            )
                        }
                    }
                    MessageType.LOCATION -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Localização", color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { 
                                // Action to open map could be added here
                            })
                        }
                    }
                    MessageType.AUDIO -> {
                        AudioMessagePlayer(message.mediaUrl)
                    }
                    else -> Text(text = "[Mídia]")
                }
                Spacer(modifier = Modifier.size(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End)) {
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusIcon = when (message.status) {
                            com.example.parachat.domain.chat.MessageStatus.SENT -> "✓"
                            com.example.parachat.domain.chat.MessageStatus.DELIVERED -> "✓✓"
                            com.example.parachat.domain.chat.MessageStatus.READ -> "✓✓"
                        }
                        val statusColor = if (message.status == com.example.parachat.domain.chat.MessageStatus.READ) Color.Blue else Color.Gray
                        Text(text = statusIcon, color = statusColor, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioMessagePlayer(mediaUrl: String?) {
    val context = LocalContext.current
    var player: MediaPlayer? by remember { mutableStateOf(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = {
                val url = mediaUrl.orEmpty()
                if (url.isBlank()) return@IconButton

                if (isPlaying) {
                    player?.pause()
                    isPlaying = false
                } else {
                    try {
                        if (player == null) {
                            player = MediaPlayer().apply {
                                setDataSource(url)
                                setOnCompletionListener { isPlaying = false }
                                setOnPreparedListener {
                                    it.start()
                                    isPlaying = true
                                }
                                prepareAsync()
                            }
                        } else {
                            player?.start()
                            isPlaying = true
                        }
                    } catch (_: Exception) {
                        isPlaying = false
                    }
                }
            }
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar áudio" else "Reproduzir áudio"
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = "Mensagem de Voz")
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
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    }
}
