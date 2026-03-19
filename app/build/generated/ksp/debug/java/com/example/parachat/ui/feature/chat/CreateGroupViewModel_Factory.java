package com.example.parachat.ui.feature.chat;

import com.example.parachat.auth.FirebaseAuthRepository;
import com.example.parachat.domain.UserRepository;
import com.example.parachat.domain.chat.GroupRepository;
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
public final class CreateGroupViewModel_Factory implements Factory<CreateGroupViewModel> {
  private final Provider<FirebaseAuthRepository> authRepositoryProvider;

  private final Provider<UserRepository> userRepositoryProvider;

  private final Provider<GroupRepository> groupRepositoryProvider;

  public CreateGroupViewModel_Factory(Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<GroupRepository> groupRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.userRepositoryProvider = userRepositoryProvider;
    this.groupRepositoryProvider = groupRepositoryProvider;
  }

  @Override
  public CreateGroupViewModel get() {
    return newInstance(authRepositoryProvider.get(), userRepositoryProvider.get(), groupRepositoryProvider.get());
  }

  public static CreateGroupViewModel_Factory create(
      Provider<FirebaseAuthRepository> authRepositoryProvider,
      Provider<UserRepository> userRepositoryProvider,
      Provider<GroupRepository> groupRepositoryProvider) {
    return new CreateGroupViewModel_Factory(authRepositoryProvider, userRepositoryProvider, groupRepositoryProvider);
  }

  public static CreateGroupViewModel newInstance(FirebaseAuthRepository authRepository,
      UserRepository userRepository, GroupRepository groupRepository) {
    return new CreateGroupViewModel(authRepository, userRepository, groupRepository);
  }
}
