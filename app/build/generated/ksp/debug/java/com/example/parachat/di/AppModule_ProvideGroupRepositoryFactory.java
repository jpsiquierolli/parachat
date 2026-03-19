package com.example.parachat.di;

import com.example.parachat.domain.chat.GroupRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
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
  private final Provider<SupabaseClient> supabaseClientProvider;

  public AppModule_ProvideGroupRepositoryFactory(Provider<SupabaseClient> supabaseClientProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
  }

  @Override
  public GroupRepository get() {
    return provideGroupRepository(supabaseClientProvider.get());
  }

  public static AppModule_ProvideGroupRepositoryFactory create(
      Provider<SupabaseClient> supabaseClientProvider) {
    return new AppModule_ProvideGroupRepositoryFactory(supabaseClientProvider);
  }

  public static GroupRepository provideGroupRepository(SupabaseClient supabaseClient) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGroupRepository(supabaseClient));
  }
}
