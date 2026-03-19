package com.example.parachat.ui.feature.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBackClick: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val viewModel = viewModel<CreateGroupViewModel>()
    val users by viewModel.users.collectAsState()
    val selectedUserIds by viewModel.selectedUserIds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Grupo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp))
                    } else {
                        IconButton(
                            onClick = {
                                viewModel.createGroup(groupName, groupDescription, onGroupCreated)
                            },
                            enabled = groupName.isNotBlank() && selectedUserIds.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Criar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Nome do Grupo") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = groupDescription,
                onValueChange = { groupDescription = it },
                label = { Text("Descrição") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Selecionar Integrantes", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(users) { user ->
                    val isSelected = selectedUserIds.contains(user.id)
                    ListItem(
                        headlineContent = { Text(user.username) },
                        supportingContent = { Text(user.email) },
                        leadingContent = {
                            Icon(Icons.Default.Group, contentDescription = null)
                        },
                        trailingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleUserSelection(user.id) }
                            )
                        },
                        modifier = Modifier.clickable { viewModel.toggleUserSelection(user.id) }
                    )
                }
            }
        }
    }
}
