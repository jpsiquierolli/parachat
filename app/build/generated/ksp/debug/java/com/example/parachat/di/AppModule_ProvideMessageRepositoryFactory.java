package com.example.parachat.di;

import android.content.Context;
import com.example.parachat.data.room.ParachatDatabase;
import com.example.parachat.domain.chat.MessageRepository;
import com.google.firebase.database.FirebaseDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class AppModule_ProvideMessageRepositoryFactory implements Factory<MessageRepository> {
  private final Provider<FirebaseDatabase> databaseProvider;

  private final Provider<ParachatDatabase> localDbProvider;

  private final Provider<Context> contextProvider;

  public AppModule_ProvideMessageRepositoryFactory(Provider<FirebaseDatabase> databaseProvider,
      Provider<ParachatDatabase> localDbProvider, Provider<Context> contextProvider) {
    this.databaseProvider = databaseProvider;
    this.localDbProvider = localDbProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public MessageRepository get() {
    return provideMessageRepository(databaseProvider.get(), localDbProvider.get(), contextProvider.get());
  }

  public static AppModule_ProvideMessageRepositoryFactory create(
      Provider<FirebaseDatabase> databaseProvider, Provider<ParachatDatabase> localDbProvider,
      Provider<Context> contextProvider) {
    return new AppModule_ProvideMessageRepositoryFactory(databaseProvider, localDbProvider, contextProvider);
  }

  public static MessageRepository provideMessageRepository(FirebaseDatabase database,
      ParachatDatabase localDb, Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageRepository(database, localDb, context));
  }
}
