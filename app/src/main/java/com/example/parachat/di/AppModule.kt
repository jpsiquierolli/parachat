package com.example.parachat.di

import android.content.Context
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.SupabaseProvider
import com.example.parachat.data.firebase.chat.FirebaseGroupRepository
import com.example.parachat.data.firebase.chat.FirebaseMessageRepository
import com.example.parachat.data.firebase.user.FirebaseUserRepository
import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.chat.GroupRepository
import com.example.parachat.domain.chat.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        // Force specific URL as google-services.json seems to be missing it or pointing to legacy
        return try {
            FirebaseDatabase.getInstance("https://parachat-50788-default-rtdb.firebaseio.com/")
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = SupabaseProvider.client

    @Provides
    @Singleton
    fun provideParachatDatabase(@ApplicationContext context: Context): ParachatDatabase {
        return ParachatDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(firebaseAuth: FirebaseAuth): FirebaseAuthRepository {
        return FirebaseAuthRepository(firebaseAuth)
    }

    @Provides
    @Singleton
    fun provideUserRepository(firebaseDatabase: FirebaseDatabase, localDb: ParachatDatabase): UserRepository {
        return FirebaseUserRepository(firebaseDatabase, localDb)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(firebaseDatabase: FirebaseDatabase, localDb: ParachatDatabase): MessageRepository {
        return FirebaseMessageRepository(firebaseDatabase, localDb)
    }

    @Provides
    @Singleton
    fun provideGroupRepository(firebaseDatabase: FirebaseDatabase): GroupRepository {
        return FirebaseGroupRepository(firebaseDatabase)
    }
}
