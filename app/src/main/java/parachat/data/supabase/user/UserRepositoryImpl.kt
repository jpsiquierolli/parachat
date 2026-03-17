package parachat.data.supabase.user

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
// import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import parachat.domain.User
import parachat.domain.UserRepository
import parachat.domain.UserStatus

class UserRepositoryImpl(
    private val client: SupabaseClient
) : UserRepository {

    override suspend fun insert(user: User) {
        client.from("users").upsert(user) {
            select(Columns.list("id")) // Optional: select returned columns
        }
    }

    override fun getAll(): Flow<List<User>> = channelFlow {
        // Initial fetch
        send(fetchUsers())

        val channel = client.channel("users-realtime")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
        }

        launch {
            changeFlow.collect {
                // Determine if we need to refresh. For simplicity, refresh on any change to 'users' table.
                // In a real app, we might merge changes locally.
                send(fetchUsers())
            }
        }
        
        channel.subscribe()
        
        awaitClose {
            launch { channel.unsubscribe() }
        }
    }

    override fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val channel = client.channel("users-observe-$userId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "users"
            filter("id", FilterOperator.EQ, userId)
        }
        
        launch {
            send(fetchUsers().firstOrNull { it.id == userId })
            changeFlow.collect {
                send(fetchUsers().firstOrNull { it.id == userId })
            }
        }

        channel.subscribe()

        awaitClose {
            launch { channel.unsubscribe() }
        }
    }

    override suspend fun updateStatus(userId: String, status: UserStatus) {
        client.from("users").update({
            set("status", status.name)
            set("lastSeen", System.currentTimeMillis())
        }) {
            filter { eq("id", userId) }
        }
    }

    private suspend fun fetchUsers(): List<User> {
        return client.from("users").select().decodeList()
    }
}
