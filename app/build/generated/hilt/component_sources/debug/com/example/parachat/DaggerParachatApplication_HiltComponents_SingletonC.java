package com.example.parachat;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.example.parachat.auth.FirebaseAuthRepository;
import com.example.parachat.data.room.ParachatDatabase;
import com.example.parachat.di.AppModule_ProvideAuthRepositoryFactory;
import com.example.parachat.di.AppModule_ProvideFirebaseAuthFactory;
import com.example.parachat.di.AppModule_ProvideFirebaseDatabaseFactory;
import com.example.parachat.di.AppModule_ProvideGroupRepositoryFactory;
import com.example.parachat.di.AppModule_ProvideMessageRepositoryFactory;
import com.example.parachat.di.AppModule_ProvideParachatDatabaseFactory;
import com.example.parachat.di.AppModule_ProvideUserRepositoryFactory;
import com.example.parachat.domain.UserRepository;
import com.example.parachat.domain.chat.GroupRepository;
import com.example.parachat.domain.chat.MessageRepository;
import com.example.parachat.ui.feature.chat.ChatViewModel;
import com.example.parachat.ui.feature.chat.ChatViewModel_HiltModules;
import com.example.parachat.ui.feature.chat.CreateGroupViewModel;
import com.example.parachat.ui.feature.chat.CreateGroupViewModel_HiltModules;
import com.example.parachat.ui.feature.chat.GroupManagementViewModel;
import com.example.parachat.ui.feature.chat.GroupManagementViewModel_HiltModules;
import com.example.parachat.ui.feature.chat.GroupsViewModel;
import com.example.parachat.ui.feature.chat.GroupsViewModel_HiltModules;
import com.example.parachat.ui.feature.home.HomeViewModel;
import com.example.parachat.ui.feature.home.HomeViewModel_HiltModules;
import com.example.parachat.ui.feature.login.LoginViewModel;
import com.example.parachat.ui.feature.login.LoginViewModel_HiltModules;
import com.example.parachat.ui.feature.profile.ProfileViewModel;
import com.example.parachat.ui.feature.profile.ProfileViewModel_HiltModules;
import com.example.parachat.ui.feature.signup.SignupViewModel;
import com.example.parachat.ui.feature.signup.SignupViewModel_HiltModules;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerParachatApplication_HiltComponents_SingletonC {
  private DaggerParachatApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public ParachatApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements ParachatApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements ParachatApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements ParachatApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements ParachatApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements ParachatApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements ParachatApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements ParachatApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public ParachatApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends ParachatApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends ParachatApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends ParachatApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends ParachatApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(8).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_ChatViewModel, ChatViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_CreateGroupViewModel, CreateGroupViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_GroupManagementViewModel, GroupManagementViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_GroupsViewModel, GroupsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_login_LoginViewModel, LoginViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_profile_ProfileViewModel, ProfileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_example_parachat_ui_feature_signup_SignupViewModel, SignupViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_parachat_ui_feature_profile_ProfileViewModel = "com.example.parachat.ui.feature.profile.ProfileViewModel";

      static String com_example_parachat_ui_feature_chat_CreateGroupViewModel = "com.example.parachat.ui.feature.chat.CreateGroupViewModel";

      static String com_example_parachat_ui_feature_chat_GroupManagementViewModel = "com.example.parachat.ui.feature.chat.GroupManagementViewModel";

      static String com_example_parachat_ui_feature_signup_SignupViewModel = "com.example.parachat.ui.feature.signup.SignupViewModel";

      static String com_example_parachat_ui_feature_chat_ChatViewModel = "com.example.parachat.ui.feature.chat.ChatViewModel";

      static String com_example_parachat_ui_feature_chat_GroupsViewModel = "com.example.parachat.ui.feature.chat.GroupsViewModel";

      static String com_example_parachat_ui_feature_login_LoginViewModel = "com.example.parachat.ui.feature.login.LoginViewModel";

      static String com_example_parachat_ui_feature_home_HomeViewModel = "com.example.parachat.ui.feature.home.HomeViewModel";

      @KeepFieldType
      ProfileViewModel com_example_parachat_ui_feature_profile_ProfileViewModel2;

      @KeepFieldType
      CreateGroupViewModel com_example_parachat_ui_feature_chat_CreateGroupViewModel2;

      @KeepFieldType
      GroupManagementViewModel com_example_parachat_ui_feature_chat_GroupManagementViewModel2;

      @KeepFieldType
      SignupViewModel com_example_parachat_ui_feature_signup_SignupViewModel2;

      @KeepFieldType
      ChatViewModel com_example_parachat_ui_feature_chat_ChatViewModel2;

      @KeepFieldType
      GroupsViewModel com_example_parachat_ui_feature_chat_GroupsViewModel2;

      @KeepFieldType
      LoginViewModel com_example_parachat_ui_feature_login_LoginViewModel2;

      @KeepFieldType
      HomeViewModel com_example_parachat_ui_feature_home_HomeViewModel2;
    }
  }

  private static final class ViewModelCImpl extends ParachatApplication_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<CreateGroupViewModel> createGroupViewModelProvider;

    private Provider<GroupManagementViewModel> groupManagementViewModelProvider;

    private Provider<GroupsViewModel> groupsViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<SignupViewModel> signupViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.createGroupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.groupManagementViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.groupsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.signupViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(8).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_ChatViewModel, ((Provider) chatViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_CreateGroupViewModel, ((Provider) createGroupViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_GroupManagementViewModel, ((Provider) groupManagementViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_chat_GroupsViewModel, ((Provider) groupsViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_login_LoginViewModel, ((Provider) loginViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_profile_ProfileViewModel, ((Provider) profileViewModelProvider)).put(LazyClassKeyProvider.com_example_parachat_ui_feature_signup_SignupViewModel, ((Provider) signupViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_example_parachat_ui_feature_chat_GroupsViewModel = "com.example.parachat.ui.feature.chat.GroupsViewModel";

      static String com_example_parachat_ui_feature_login_LoginViewModel = "com.example.parachat.ui.feature.login.LoginViewModel";

      static String com_example_parachat_ui_feature_signup_SignupViewModel = "com.example.parachat.ui.feature.signup.SignupViewModel";

      static String com_example_parachat_ui_feature_home_HomeViewModel = "com.example.parachat.ui.feature.home.HomeViewModel";

      static String com_example_parachat_ui_feature_chat_CreateGroupViewModel = "com.example.parachat.ui.feature.chat.CreateGroupViewModel";

      static String com_example_parachat_ui_feature_chat_GroupManagementViewModel = "com.example.parachat.ui.feature.chat.GroupManagementViewModel";

      static String com_example_parachat_ui_feature_chat_ChatViewModel = "com.example.parachat.ui.feature.chat.ChatViewModel";

      static String com_example_parachat_ui_feature_profile_ProfileViewModel = "com.example.parachat.ui.feature.profile.ProfileViewModel";

      @KeepFieldType
      GroupsViewModel com_example_parachat_ui_feature_chat_GroupsViewModel2;

      @KeepFieldType
      LoginViewModel com_example_parachat_ui_feature_login_LoginViewModel2;

      @KeepFieldType
      SignupViewModel com_example_parachat_ui_feature_signup_SignupViewModel2;

      @KeepFieldType
      HomeViewModel com_example_parachat_ui_feature_home_HomeViewModel2;

      @KeepFieldType
      CreateGroupViewModel com_example_parachat_ui_feature_chat_CreateGroupViewModel2;

      @KeepFieldType
      GroupManagementViewModel com_example_parachat_ui_feature_chat_GroupManagementViewModel2;

      @KeepFieldType
      ChatViewModel com_example_parachat_ui_feature_chat_ChatViewModel2;

      @KeepFieldType
      ProfileViewModel com_example_parachat_ui_feature_profile_ProfileViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.parachat.ui.feature.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideMessageRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), viewModelCImpl.savedStateHandle);

          case 1: // com.example.parachat.ui.feature.chat.CreateGroupViewModel 
          return (T) new CreateGroupViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideGroupRepositoryProvider.get());

          case 2: // com.example.parachat.ui.feature.chat.GroupManagementViewModel 
          return (T) new GroupManagementViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideGroupRepositoryProvider.get());

          case 3: // com.example.parachat.ui.feature.chat.GroupsViewModel 
          return (T) new GroupsViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideGroupRepositoryProvider.get());

          case 4: // com.example.parachat.ui.feature.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get(), singletonCImpl.provideMessageRepositoryProvider.get(), singletonCImpl.provideParachatDatabaseProvider.get());

          case 5: // com.example.parachat.ui.feature.login.LoginViewModel 
          return (T) new LoginViewModel(singletonCImpl.provideAuthRepositoryProvider.get());

          case 6: // com.example.parachat.ui.feature.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          case 7: // com.example.parachat.ui.feature.signup.SignupViewModel 
          return (T) new SignupViewModel(singletonCImpl.provideAuthRepositoryProvider.get(), singletonCImpl.provideUserRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends ParachatApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends ParachatApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends ParachatApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<FirebaseAuth> provideFirebaseAuthProvider;

    private Provider<FirebaseAuthRepository> provideAuthRepositoryProvider;

    private Provider<FirebaseDatabase> provideFirebaseDatabaseProvider;

    private Provider<ParachatDatabase> provideParachatDatabaseProvider;

    private Provider<MessageRepository> provideMessageRepositoryProvider;

    private Provider<UserRepository> provideUserRepositoryProvider;

    private Provider<GroupRepository> provideGroupRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 1));
      this.provideAuthRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuthRepository>(singletonCImpl, 0));
      this.provideFirebaseDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseDatabase>(singletonCImpl, 3));
      this.provideParachatDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<ParachatDatabase>(singletonCImpl, 4));
      this.provideMessageRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<MessageRepository>(singletonCImpl, 2));
      this.provideUserRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserRepository>(singletonCImpl, 5));
      this.provideGroupRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<GroupRepository>(singletonCImpl, 6));
    }

    @Override
    public void injectParachatApplication(ParachatApplication parachatApplication) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.example.parachat.auth.FirebaseAuthRepository 
          return (T) AppModule_ProvideAuthRepositoryFactory.provideAuthRepository(singletonCImpl.provideFirebaseAuthProvider.get());

          case 1: // com.google.firebase.auth.FirebaseAuth 
          return (T) AppModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 2: // com.example.parachat.domain.chat.MessageRepository 
          return (T) AppModule_ProvideMessageRepositoryFactory.provideMessageRepository(singletonCImpl.provideFirebaseDatabaseProvider.get(), singletonCImpl.provideParachatDatabaseProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // com.google.firebase.database.FirebaseDatabase 
          return (T) AppModule_ProvideFirebaseDatabaseFactory.provideFirebaseDatabase();

          case 4: // com.example.parachat.data.room.ParachatDatabase 
          return (T) AppModule_ProvideParachatDatabaseFactory.provideParachatDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 5: // com.example.parachat.domain.UserRepository 
          return (T) AppModule_ProvideUserRepositoryFactory.provideUserRepository(singletonCImpl.provideFirebaseDatabaseProvider.get(), singletonCImpl.provideParachatDatabaseProvider.get());

          case 6: // com.example.parachat.domain.chat.GroupRepository 
          return (T) AppModule_ProvideGroupRepositoryFactory.provideGroupRepository(singletonCImpl.provideFirebaseDatabaseProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
