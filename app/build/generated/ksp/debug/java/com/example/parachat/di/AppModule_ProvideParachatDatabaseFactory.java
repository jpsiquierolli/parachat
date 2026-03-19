package com.example.parachat.di;

import android.content.Context;
import com.example.parachat.data.room.ParachatDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideParachatDatabaseFactory implements Factory<ParachatDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideParachatDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ParachatDatabase get() {
    return provideParachatDatabase(contextProvider.get());
  }

  public static AppModule_ProvideParachatDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideParachatDatabaseFactory(contextProvider);
  }

  public static ParachatDatabase provideParachatDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideParachatDatabase(context));
  }
}
