package com.example.parachat.ui.feature.home

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.parachat.domain.User
import com.example.parachat.domain.displayName
import com.example.parachat.domain.chat.Conversation
import com.example.parachat.ui.theme.ParachatTheme

@Composable
fun HomeScreen(
    onUserClick: (String) -> Unit,
    onCreateGroupClick: () -> Unit,
    onProfileClick: () -> Unit,
    onGroupsClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val viewModel = hiltViewModel<HomeViewModel>()
    val users by viewModel.users.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    HomeContent(
        users = users,
        conversations = conversations,
        currentUser = currentUser,
        searchQuery = searchQuery,
        isLoading = isLoading,
        onSearchQueryChange = viewModel::onSearchQueryChange,
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
    currentUser: User?,
    searchQuery: String,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onUserClick: (String) -> Unit,
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
                                UserItem(user = user, onClick = { 
                                    onUserClick(user.id)
                                    onSearchQueryChange("")
                                    showUserSearch = false
                                })
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
                                ConversationItem(conversation = conversation, onClick = { onUserClick(conversation.otherUserId) })
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
fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(28.dp))
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
fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
    }
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
            currentUser = User(id = "3", username = "Me", email = "me@example.com"),
            searchQuery = "",
            isLoading = false,
            onSearchQueryChange = {},
            onUserClick = {},
            onCreateGroupClick = {},
            onProfileClick = {},
            onGroupsClick = {},
            onSignOut = {}
        )
    }
}
