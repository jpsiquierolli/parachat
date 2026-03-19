package com.example.parachat.ui.feature.signup;

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
public final class SignupViewModel_Factory implements Factory<SignupViewModel> {
  private final Provider<FirebaseAuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  public SignupViewModel_Factory(Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
  }

  @Override
  public SignupViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get());
  }

  public static SignupViewModel_Factory create(
      Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider) {
    return new SignupViewModel_Factory(authRepositoryProvider, userRepositoryProvider);
  }

  public static SignupViewModel newInstance(FirebaseAuthRepository authRepository,
      UserRepository userRepository) {
    return new SignupViewModel(authRepository, userRepository);
  }
}
