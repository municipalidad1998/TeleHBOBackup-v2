package com.denilson.music.ui.viewmodels;

import com.denilson.music.data.datastore.SettingsRepository;
import com.denilson.music.media.PlayerConnection;
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
    "cast",
    "deprecation"
})
public final class MainViewModel_Factory implements Factory<MainViewModel> {
  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<PlayerConnection> playerProvider;

  public MainViewModel_Factory(Provider<SettingsRepository> settingsRepoProvider,
      Provider<PlayerConnection> playerProvider) {
    this.settingsRepoProvider = settingsRepoProvider;
    this.playerProvider = playerProvider;
  }

  @Override
  public MainViewModel get() {
    return newInstance(settingsRepoProvider.get(), playerProvider.get());
  }

  public static MainViewModel_Factory create(Provider<SettingsRepository> settingsRepoProvider,
      Provider<PlayerConnection> playerProvider) {
    return new MainViewModel_Factory(settingsRepoProvider, playerProvider);
  }

  public static MainViewModel newInstance(SettingsRepository settingsRepo,
      PlayerConnection player) {
    return new MainViewModel(settingsRepo, player);
  }
}
