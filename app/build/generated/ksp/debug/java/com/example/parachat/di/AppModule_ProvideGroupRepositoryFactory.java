package com.example.parachat.di;

import com.example.parachat.domain.chat.GroupRepository;
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
public final class AppModule_ProvideGroupRepositoryFactory implements Factory<GroupRepository> {
  private final Provider<FirebaseDatabase> firebaseDatabaseProvider;

  public AppModule_ProvideGroupRepositoryFactory(
      Provider<FirebaseDatabase> firebaseDatabaseProvider) {
    this.firebaseDatabaseProvider = firebaseDatabaseProvider;
  }

  @Override
  public GroupRepository get() {
    return provideGroupRepository(firebaseDatabaseProvider.get());
  }

  public static AppModule_ProvideGroupRepositoryFactory create(
      Provider<FirebaseDatabase> firebaseDatabaseProvider) {
    return new AppModule_ProvideGroupRepositoryFactory(firebaseDatabaseProvider);
  }

  public static GroupRepository provideGroupRepository(FirebaseDatabase firebaseDatabase) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGroupRepository(firebaseDatabase));
  }
}
