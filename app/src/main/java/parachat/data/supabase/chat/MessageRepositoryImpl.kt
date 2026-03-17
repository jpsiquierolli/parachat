package parachat.data.supabase.chat

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import parachat.domain.chat.Message
import parachat.domain.chat.MessageRepository

class MessageRepositoryImpl(
    private val client: SupabaseClient
) : MessageRepository {

    override suspend fun sendMessage(message: Message) {
        client.from("messages").insert(message)
    }

    override fun getMessages(currentUserId: String, otherUserId: String): Flow<List<Message>> = channelFlow {
        // Initial fetch
        try {
            send(fetchMessages(currentUserId, otherUserId))
        } catch (e: Exception) {
            e.printStackTrace()
             send(emptyList())
        }

        val channel = client.channel("messages-realtime")
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "messages"
        }

        launch {
            changeFlow.collect {
                // Refresh on change. Optimized approach would filter the Change payload.
                try {
                    send(fetchMessages(currentUserId, otherUserId))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        channel.subscribe()
        
        awaitClose {
            launch { channel.unsubscribe() }
        }
    }

    private suspend fun fetchMessages(u1: String, u2: String): List<Message> {
        return client.from("messages").select {
            filter {
                or {
                    and {
                        eq("senderId", u1)
                        eq("receiverId", u2)
                    }
                    and {
                        eq("senderId", u2)
                        eq("receiverId", u1)
                    }
                }
            }
            order("timestamp", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
        }.decodeList()
    }
}

