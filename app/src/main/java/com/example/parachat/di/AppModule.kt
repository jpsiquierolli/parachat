package com.example.parachat.di

import android.content.Context
import com.example.parachat.auth.FirebaseAuthRepository
import com.example.parachat.data.SupabaseProvider
import com.example.parachat.data.room.ParachatDatabase
import com.example.parachat.data.supabase.chat.SupabaseGroupRepository
import com.example.parachat.data.supabase.chat.SupabaseMessageRepository
import com.example.parachat.data.supabase.user.SupabaseUserRepository
import com.example.parachat.domain.UserRepository
import com.example.parachat.domain.chat.GroupRepository
import com.example.parachat.domain.chat.MessageRepository
import com.google.firebase.auth.FirebaseAuth
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
    fun provideUserRepository(supabaseClient: SupabaseClient, localDb: ParachatDatabase): UserRepository {
        return SupabaseUserRepository(supabaseClient, localDb)
    }

    @Provides
    @Singleton
    fun provideMessageRepository(supabaseClient: SupabaseClient, localDb: ParachatDatabase): MessageRepository {
        return SupabaseMessageRepository(supabaseClient, localDb)
    }

    @Provides
    @Singleton
    fun provideGroupRepository(supabaseClient: SupabaseClient): GroupRepository {
        return SupabaseGroupRepository(supabaseClient)
    }
}
