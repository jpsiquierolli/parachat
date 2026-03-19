package com.example.parachat.di;

import com.example.parachat.data.room.ParachatDatabase;
import com.example.parachat.domain.UserRepository;
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
public final class AppModule_ProvideUserRepositoryFactory implements Factory<UserRepository> {
  private final Provider<FirebaseDatabase> firebaseDatabaseProvider;

  private final Provider<ParachatDatabase> localDbProvider;

  public AppModule_ProvideUserRepositoryFactory(Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<ParachatDatabase> localDbProvider) {
    this.firebaseDatabaseProvider = firebaseDatabaseProvider;
    this.localDbProvider = localDbProvider;
  }

  @Override
  public UserRepository get() {
    return provideUserRepository(firebaseDatabaseProvider.get(), localDbProvider.get());
  }

  public static AppModule_ProvideUserRepositoryFactory create(
      Provider<FirebaseDatabase> firebaseDatabaseProvider,
      Provider<ParachatDatabase> localDbProvider) {
    return new AppModule_ProvideUserRepositoryFactory(firebaseDatabaseProvider, localDbProvider);
  }

  public static UserRepository provideUserRepository(FirebaseDatabase firebaseDatabase,
      ParachatDatabase localDb) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideUserRepository(firebaseDatabase, localDb));
  }
}
