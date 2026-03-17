package parachat.data.firebase.user

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseContactRepository(
    private val database: FirebaseDatabase
) {
    private val contactsRef = database.getReference("contacts")

    fun observeContacts(userId: String): Flow<List<String>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                trySend(ids)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        contactsRef.child(userId).addValueEventListener(listener)
        awaitClose { contactsRef.child(userId).removeEventListener(listener) }
    }

    suspend fun addContact(userId: String, contactId: String) {
        contactsRef.child(userId).child(contactId).setValue(true).await()
        contactsRef.child(contactId).child(userId).setValue(true).await()
    }

    suspend fun removeContact(userId: String, contactId: String) {
        contactsRef.child(userId).child(contactId).removeValue().await()
        contactsRef.child(contactId).child(userId).removeValue().await()
    }
}

