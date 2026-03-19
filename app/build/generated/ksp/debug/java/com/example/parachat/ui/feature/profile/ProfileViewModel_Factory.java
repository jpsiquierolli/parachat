package com.example.parachat.ui.feature.profile;

import com.example.parachat.auth.FirebaseAuthRepository;
import com.example.parachat.domain.UserRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ProfileViewModel_Factory implements Factory<ProfileViewModel> {
  private final Provider<FirebaseAuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public ProfileViewModel_Factory(Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public ProfileViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static ProfileViewModel_Factory create(
      Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new ProfileViewModel_Factory(authRepositoryProvider, userRepositoryProvider);
  }

  public static ProfileViewModel newInstance(FirebaseAuthRepository authRepository,
      UserRepository userRepository) {
    return new ProfileViewModel(authRepository, userRepository);
  }
}
