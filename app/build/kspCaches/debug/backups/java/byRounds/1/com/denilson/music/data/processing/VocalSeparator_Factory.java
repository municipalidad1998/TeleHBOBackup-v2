package com.denilson.music.data.processing;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
    "cast",
    "deprecation"
})
public final class VocalSeparator_Factory implements Factory<VocalSeparator> {
  private final Provider<Context> contextProvider;

  public VocalSeparator_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VocalSeparator get() {
    return newInstance(contextProvider.get());
  }

  public static VocalSeparator_Factory create(Provider<Context> contextProvider) {
    return new VocalSeparator_Factory(contextProvider);
  }

  public static VocalSeparator newInstance(Context context) {
    return new VocalSeparator(context);
  }
}
