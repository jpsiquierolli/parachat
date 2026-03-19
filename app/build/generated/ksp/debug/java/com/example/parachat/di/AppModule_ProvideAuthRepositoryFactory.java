package com.example.parachat.di;

import com.example.parachat.auth.FirebaseAuthRepository;
import com.google.firebase.auth.FirebaseAuth;
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<FirebaseAuthRepository> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<FirebaseAuth> firebaseAuthProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public FirebaseAuthRepository get() {
    return provideAuthRepository(firebaseAuthProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(
      Provider<FirebaseAuth> firebaseAuthProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(firebaseAuthProvider);
  }

  public static FirebaseAuthRepository provideAuthRepository(FirebaseAuth firebaseAuth) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(firebaseAuth));
  }
}
