package com.example.parachat.di;

import com.example.parachat.data.room.ParachatDatabase;
import com.example.parachat.domain.chat.MessageRepository;
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
public final class AppModule_ProvideMessageRepositoryFactory implements Factory<MessageRepository> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  private final Provider<ParachatDatabase> localDbProvider;

  public AppModule_ProvideMessageRepositoryFactory(Provider<SupabaseClient> supabaseClientProvider,
      Provider<ParachatDatabase> localDbProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
    this.localDbProvider = localDbProvider;
  }

  @Override
  public MessageRepository get() {
    return provideMessageRepository(supabaseClientProvider.get(), localDbProvider.get());
  }

  public static AppModule_ProvideMessageRepositoryFactory create(
      Provider<SupabaseClient> supabaseClientProvider, Provider<ParachatDatabase> localDbProvider) {
    return new AppModule_ProvideMessageRepositoryFactory(supabaseClientProvider, localDbProvider);
  }

  public static MessageRepository provideMessageRepository(SupabaseClient supabaseClient,
      ParachatDatabase localDb) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageRepository(supabaseClient, localDb));
  }
}
