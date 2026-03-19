package com.example.parachat.ui.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.parachat.domain.User
import com.example.parachat.domain.displayName
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.ui.theme.ParachatTheme

@Composable
fun HomeScreen(
    onUserClick: (String, Boolean, String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val users by viewModel.users.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val contactIds by viewModel.contactIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (emails, names) = readDeviceContacts(context)
            viewModel.importDeviceContacts(emails, names)
        }
    }

    HomeContent(
        users = users,
        conversations = conversations,
        contactIds = contactIds,
        currentUser = currentUser,
        searchQuery = searchQuery,
        isLoading = isLoading,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onToggleContact = { userId, isContact ->
            if (isContact) viewModel.removeContact(userId) else viewModel.addContact(userId)
        },
        onImportContacts = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                val (emails, names) = readDeviceContacts(context)
                viewModel.importDeviceContacts(emails, names)
            } else {
                contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        },
        onUserClick = onUserClick,
        onCreateGroupClick = onCreateGroupClick,
        onProfileClick = onProfileClick,
        onGroupsClick = onGroupsClick,
        onSignOut = {
            viewModel.signOut(onComplete = onSignOut)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    users: List<User>,
    conversations: List<Conversation>,
    contactIds: Set<String>,
    currentUser: User?,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleContact: (String, Boolean) -> Unit,
    onImportContacts: () -> Unit,
    onUserClick: (String, Boolean, String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onSignOut: () -> Unit
) {
    var showUserSearch by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Parachat") },
                actions = {
                    androidx.compose.material3.TextButton(onClick = onImportContacts) {
                        Text("Importar")
                    }
                    IconButton(onClick = onGroupsClick) {
                        Icon(Icons.Default.Group, contentDescription = "Grupos")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Perfil")
                    }
                    IconButton(onClick = onSignOut) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!showUserSearch) {
                    androidx.compose.material3.SmallFloatingActionButton(
                        onClick = onCreateGroupClick,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Novo Grupo")
                    }
                }
                androidx.compose.material3.FloatingActionButton(onClick = { showUserSearch = !showUserSearch }) {
                    Icon(if (showUserSearch) Icons.Default.Close else Icons.Default.Search, contentDescription = "Novo Chat")
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (currentUser != null) {
                Text(
                    text = "Logado como: ${currentUser.displayName()}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Search Bar always visible
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar contatos e conversas...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                singleLine = true
            )

            // Tabs or Section Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Determine active tab based on showing user search or not
                
                androidx.compose.material3.TextButton(
                    onClick = { showUserSearch = false },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (!showUserSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Conversas")
                }
                
                androidx.compose.material3.TextButton(
                    onClick = { showUserSearch = true },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (showUserSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Contatos")
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            } else {
                val usersById = users.associateBy { it.id }

                // Show Users List if: Explicitly requested OR Searching (ViewModel filters users) OR No conversations yet
                if (showUserSearch || searchQuery.isNotBlank() || conversations.isEmpty()) {
                    // Show Users List
                    if (users.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isNotBlank()) "Nenhum usuário encontrado." else "Nenhum contato encontrado.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(users) { user ->
                                UserItem(
                                    user = user,
                                    isContact = contactIds.contains(user.id),
                                    onClick = {
                                        onUserClick(user.id, false, user.displayName())
                                        onSearchQueryChange("")
                                        showUserSearch = false
                                    },
                                    onToggleContact = { onToggleContact(user.id, contactIds.contains(user.id)) }
                                )
                            }
                        }
                    }
                } else {
                    // Show Conversations List
                    if (conversations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Nenhuma conversa iniciada.")
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    photoUrl = usersById[conversation.otherUserId]?.photoUrl,
                                    onClick = {
                                        onUserClick(
                                            conversation.otherUserId,
                                            conversation.isGroup,
                                            conversation.title
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Debug Text (Temporary)
            if (users.isEmpty() && conversations.isEmpty()) {
                Text(
                    text = "Debug: Nenhuma conta encontrada no banco de dados. Crie um novo usuário para testar.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
fun ConversationItem(conversation: Conversation, photoUrl: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            if (!photoUrl.isNullOrBlank() && !conversation.isGroup) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (conversation.isGroup) Icons.Default.Group else Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (conversation.unreadCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                ) {}
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = conversation.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = conversation.lastMessagePreview,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        if (conversation.unreadCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}


@Composable
fun UserItem(user: User, isContact: Boolean, onClick: () -> Unit, onToggleContact: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!user.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column {
            Text(
                text = user.displayName(),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        androidx.compose.material3.TextButton(onClick = onToggleContact) {
            Text(if (isContact) "Remover" else "Adicionar")
        }
    }
}

private fun readDeviceContacts(context: Context): Pair<List<String>, List<String>> {
    val emails = mutableSetOf<String>()
    val names = mutableSetOf<String>()

    val emailCursor = context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Email.ADDRESS,
            ContactsContract.CommonDataKinds.Email.DISPLAY_NAME
        ),
        null,
        null,
        null
    )

    emailCursor?.use { cursor ->
        val addressIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
        while (cursor.moveToNext()) {
            if (addressIdx >= 0) {
                cursor.getString(addressIdx)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { emails.add(it) }
            }
            if (nameIdx >= 0) {
                cursor.getString(nameIdx)?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { names.add(it) }
            }
        }
    }

    return emails.toList() to names.toList()
}

@Preview
@Composable
fun HomeContentPreview() {
    ParachatTheme {
        HomeContent(
            users = listOf(
                User(id = "1", username = "Alice", email = "alice@example.com"),
                User(id = "2", username = "Bob", email = "bob@example.com")
            ),
            conversations = emptyList(),
            contactIds = emptySet(),
            currentUser = User(id = "3", username = "Me", email = "me@example.com"),
            searchQuery = "",
            isLoading = false,
            onSearchQueryChange = {},
            onToggleContact = { _, _ -> },
            onImportContacts = {},
            onUserClick = { _, _, _ -> },
            onCreateGroupClick = {},
            onProfileClick = {},
            onGroupsClick = {},
            onSignOut = {}
        )
    }
}
