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
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

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

    // MIGRATION: Firebase is now the primary data source for users, messages, and groups
    // Supabase is used ONLY for media storage (images, videos, PDFs, audio, files)

    @Provides
    @Singleton
    fun provideUserRepository(database: FirebaseDatabase, localDb: ParachatDatabase): UserRepository {
        return FirebaseUserRepository(database, localDb)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(
        database: FirebaseDatabase,
        localDb: ParachatDatabase
    ): MessageRepository {
        return FirebaseMessageRepository(database, localDb)
    }

    @Provides
    @Singleton
    fun provideGroupRepository(database: FirebaseDatabase): GroupRepository {
        return FirebaseGroupRepository(database)
    }

    // Supabase is kept for media storage only (see MediaStorageRepository)
    // All chat data (users, messages, groups, conversations) is now stored in Firebase
}
