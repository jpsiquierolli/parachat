package com.example.parachat.ui.feature.home;

import com.example.parachat.auth.FirebaseAuthRepository;
import com.example.parachat.domain.UserRepository;
import com.example.parachat.domain.chat.MessageRepository;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<FirebaseAuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  public HomeViewModel_Factory(Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get(), messageRepositoryProvider.get());
  }

  public static HomeViewModel_Factory create(
      Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider) {
    return new HomeViewModel_Factory(authRepositoryProvider, userRepositoryProvider, messageRepositoryProvider);
  }

  public static HomeViewModel newInstance(FirebaseAuthRepository authRepository,
      UserRepository userRepository, MessageRepository messageRepository) {
    return new HomeViewModel(authRepository, userRepository, messageRepository);
  }
}
