package com.example.parachat.ui.feature.chat;

import androidx.lifecycle.SavedStateHandle;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<FirebaseAuthRepository> authRepositoryProvider;

  private final Provider<MessageRepository> messageRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public ChatViewModel_Factory(Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.messageRepositoryProvider = messageRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(authRepositoryProvider.get(), messageRepositoryProvider.get(), userRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static ChatViewModel_Factory create(
      Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<MessageRepository> messageRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new ChatViewModel_Factory(authRepositoryProvider, messageRepositoryProvider, userRepositoryProvider, savedStateHandleProvider);
  }

  public static ChatViewModel newInstance(FirebaseAuthRepository authRepository,
      MessageRepository messageRepository, UserRepository userRepository,
      SavedStateHandle savedStateHandle) {
    return new ChatViewModel(authRepository, messageRepository, userRepository, savedStateHandle);
  }
}
