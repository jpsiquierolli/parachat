package parachat.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import parachat.data.room.user.UserDao
import parachat.data.room.user.UserEntity

import parachat.data.room.chat.MessageDao
import parachat.data.room.chat.MessageEntity

@Database(
    entities = [UserEntity::class, MessageEntity::class],
    version = 5
)
abstract class ParachatDatabase : RoomDatabase() {

    abstract val userDao: UserDao
    abstract val messageDao: MessageDao
    abstract val userDao: UserDao
    abstract val historyDao: HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: QuizAppDatabase? = null

        fun getInstance(context: Context): QuizAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuizAppDatabase::class.java,
                    "quiz-app"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object UserDatabaseProvider {

    @Volatile
    private var INSTANCE: ParachatDatabase? = null

    fun provide(context: Context): ParachatDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ParachatDatabase::class.java,
                "parachat-app"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}