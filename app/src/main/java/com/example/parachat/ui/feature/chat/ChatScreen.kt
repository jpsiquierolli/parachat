package com.example.parachat.ui.feature.chat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.example.parachat.domain.chat.Message
import com.example.parachat.domain.chat.MessageStatus
import com.example.parachat.domain.chat.MessageType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String? = null,
    groupId: String? = null,
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
            val file = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.mp4")
            audioFile = file
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setOutputFile(file.absolutePath)
            try {
                newRecorder.prepare()
                newRecorder.start()
                recorder = newRecorder
                isRecording = true
                Toast.makeText(context, "Gravando...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                e.printStackTrace()
                newRecorder.release()
                Toast.makeText(context, "Erro ao iniciar gravação.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permissão de microfone negada.", Toast.LENGTH_SHORT).show()
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

    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let { uri ->
                val bytes = readBytesFromUri(context, uri)
                if (bytes != null && bytes.isNotEmpty()) {
                    viewModel.sendMedia(bytes, "jpg", "image/jpeg", MessageType.IMAGE)
                } else {
                    Toast.makeText(context, "Erro ao ler foto.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraImageUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao abrir câmera.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permissão de câmera negada.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        try {
            val file = File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            cameraImageUri = uri
            cameraLauncher.launch(uri)
        } catch (e: SecurityException) {
            // Permission was revoked (e.g. after reinstall) — request it again
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir câmera.", Toast.LENGTH_SHORT).show()
        }
    }
    
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
             getCurrentLocation(context) { lat, long ->
                 val message = Message(
                     senderId = currentUserId,
                     receiverId = userId ?: "",
                     groupId = groupId,
                     content = "Localização enviada",
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

        val file = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.mp4")
        audioFile = file

        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        newRecorder.setOutputFile(file.absolutePath)
        try {
            newRecorder.prepare()
            newRecorder.start()
            recorder = newRecorder
            isRecording = true
            Toast.makeText(context, "Gravando...", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            newRecorder.release()
            Toast.makeText(context, "Erro ao iniciar gravação.", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
            recorder = null
            isRecording = false
            val file = audioFile
            if (file != null && file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                viewModel.sendMedia(bytes, "mp4", "audio/mp4", MessageType.AUDIO)
            } else {
                Toast.makeText(context, "Arquivo de áudio inválido.", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isRecording = false
            recorder = null
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
                    title = { Text(text = if (groupId != null) "Grupo" else "Chat") },
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
                            launchCamera()
                        },
                        onLocationClick = {
                            showBottomSheet = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                 getCurrentLocation(context) { lat, long ->
                                     val message = Message(
                                         senderId = currentUserId,
                                         receiverId = userId ?: "",
                                         groupId = groupId,
                                         content = "Localização",
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

                if (isRecording) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Gravando áudio... ", color = MaterialTheme.colorScheme.onErrorContainer)
                        IconButton(onClick = { stopRecording() }) {
                            Icon(Icons.Default.Stop, contentDescription = "Parar", tint = MaterialTheme.colorScheme.error)
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

            val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val dateDisplayFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
            val groupedMessages = remember(messages) {
                messages
                    .sortedBy { it.timestamp }
                    .groupBy { dateKeyFormat.format(Date(it.timestamp)) }
            }

            val listState = rememberLazyListState()
            LaunchedEffect(messages.size) {
                if (messages.isNotEmpty()) {
                    val totalItems = messages.size + groupedMessages.size
                    listState.animateScrollToItem(totalItems - 1)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                state = listState
            ) {
                val dates = groupedMessages.keys.toList().sorted()
                dates.forEach { dateKey ->
                    item {
                        DateHeader(dateDisplayFormat.format(dateKeyFormat.parse(dateKey)!!))
                    }
                    val dayMessages = groupedMessages[dateKey] ?: emptyList()
                    items(dayMessages) { message ->
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
}

@Composable
fun DateHeader(date: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = date,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start) {
             if (!isCurrentUser && message.groupId != null) {
                 Text(
                     text = message.senderId,
                     style = MaterialTheme.typography.labelSmall,
                     modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                     color = MaterialTheme.colorScheme.primary
                 )
             }
             Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (isCurrentUser) 12.dp else 0.dp,
                        bottomEnd = if (isCurrentUser) 0.dp else 12.dp
                    ))
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
                    .widthIn(max = 280.dp)
            ) {
                Column {
                    if (message.content.isNotBlank()) {
                         Text(
                            text = highlightedText(message.content, highlight),
                            color = if (isCurrentUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (message.type == MessageType.IMAGE && message.mediaUrl != null) {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Imagem",
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    } else if (message.type == MessageType.LOCATION && message.latitude != null && message.longitude != null) {
                        val lat = message.latitude
                        val lon = message.longitude
                        val mapUrl = "https://staticmap.openstreetmap.de/staticmap.php?center=$lat,$lon&zoom=15&size=256x160&markers=$lat,$lon,red-pushpin"
                        Column(modifier = Modifier.padding(top = 4.dp)) {
                            AsyncImage(
                                model = mapUrl,
                                contentDescription = "Mapa",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        try {
                                            context.startActivity(mapIntent)
                                        } catch (e: Exception) {
                                            val browserUri = Uri.parse("https://www.google.com/maps?q=$lat,$lon")
                                            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                                        }
                                    }
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isCurrentUser) Color.White else Color.Unspecified)
                                Text(" Toque para abrir no mapa", style = MaterialTheme.typography.labelSmall, color = if (isCurrentUser) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else if (message.type == MessageType.LOCATION) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = if (isCurrentUser) Color.White else Color.Unspecified)
                            Text(" Localização", style = MaterialTheme.typography.bodySmall, color = if (isCurrentUser) Color.White else Color.Unspecified)
                        }
                    } else if (message.type == MessageType.AUDIO) {
                        AudioPlayerItem(
                            url = message.mediaUrl,
                            isCurrentUser = isCurrentUser
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = time,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = (if (isCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val statusIcon = when (message.status) {
                                MessageStatus.SENT -> Icons.Default.Check
                                MessageStatus.DELIVERED -> Icons.Default.DoneAll
                                MessageStatus.READ -> Icons.Default.DoneAll
                            }
                            val statusTint = if (message.status == MessageStatus.READ) Color.Cyan else Color.White.copy(alpha = 0.7f)
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = statusTint
                            )
                        }
                    }
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

@Composable
fun AudioPlayerItem(url: String?, isCurrentUser: Boolean) {
    if (url == null) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = if (isCurrentUser) Color.White else Color.Unspecified)
            Text(" Áudio indisponível", style = MaterialTheme.typography.bodySmall,
                color = if (isCurrentUser) Color.White else Color.Unspecified)
        }
        return
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableIntStateOf(0) }
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup when composable leaves composition
    DisposableEffect(url) {
        onDispose {
            mediaPlayer.value?.release()
            mediaPlayer.value = null
        }
    }

    // Update progress while playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                val mp = mediaPlayer.value
                if (mp != null && mp.isPlaying) {
                    progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                }
                kotlinx.coroutines.delay(200)
            }
        }
    }

    fun togglePlayback() {
        val mp = mediaPlayer.value
        if (mp == null) {
            // First play — create and prepare asynchronously
            val newMp = MediaPlayer()
            newMp.setDataSource(url)
            newMp.setOnPreparedListener { prepared ->
                durationMs = prepared.duration
                prepared.start()
                isPlaying = true
            }
            newMp.setOnCompletionListener {
                isPlaying = false
                progress = 0f
            }
            newMp.prepareAsync()
            mediaPlayer.value = newMp
        } else if (mp.isPlaying) {
            mp.pause()
            isPlaying = false
        } else {
            mp.start()
            isPlaying = true
        }
    }

    val contentColor = if (isCurrentUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val totalSecs = durationMs / 1000
    val currentSecs = (progress * totalSecs).toInt()
    val timeLabel = "%d:%02d / %d:%02d".format(currentSecs / 60, currentSecs % 60, totalSecs / 60, totalSecs % 60)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 4.dp)
            .widthIn(min = 180.dp, max = 260.dp)
    ) {
        IconButton(onClick = { togglePlayback() }, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproduzir",
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Slider(
                value = progress,
                onValueChange = { newVal ->
                    progress = newVal
                    mediaPlayer.value?.seekTo((newVal * (mediaPlayer.value?.duration ?: 0)).toInt())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = contentColor,
                    activeTrackColor = contentColor,
                    inactiveTrackColor = contentColor.copy(alpha = 0.3f)
                )
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = contentColor.copy(alpha = 0.8f)
            )
        }
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
            pushStyle(androidx.compose.ui.text.SpanStyle(background = Color.Yellow.copy(alpha = 0.5f), fontWeight = FontWeight.Bold))
            append(text.substring(index, index + target.length))
            pop()
            start = index + target.length
        }
    }
    return builder
}
