package com.example.parachat.di;

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
@QualifierMetadata
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
  private final Provider<FirebaseDatabase> firebaseDatabaseProvider;

  private final Provider<ParachatDatabase> localDbProvider;

  public AppModule_ProvideMessageRepositoryFactory(
      Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<ParachatDatabase> localDbProvider) {
    this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    this.localDbProvider = localDbProvider;
  }

  @Override
  public MessageRepository get() {
    return provideMessageRepository(firebaseDatabaseProvider.get(), localDbProvider.get());
  }

  public static AppModule_ProvideMessageRepositoryFactory create(
      Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<ParachatDatabase> localDbProvider) {
    return new AppModule_ProvideMessageRepositoryFactory(firebaseDatabaseProvider, localDbProvider);
  }

  public static MessageRepository provideMessageRepository(FirebaseDatabase firebaseDatabase,
      ParachatDatabase localDb) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageRepository(firebaseDatabase, localDb));
  }
}
