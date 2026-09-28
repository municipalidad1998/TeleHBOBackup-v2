package com.denilson.music.ui.viewmodels;

import com.denilson.music.data.repository.MusicRepository;
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<MusicRepository> repoProvider;

  private final Provider<PlayerConnection> playerProvider;

  public LibraryViewModel_Factory(Provider<MusicRepository> repoProvider,
      Provider<PlayerConnection> playerProvider) {
    this.repoProvider = repoProvider;
    this.playerProvider = playerProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(repoProvider.get(), playerProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<MusicRepository> repoProvider,
      Provider<PlayerConnection> playerProvider) {
    return new LibraryViewModel_Factory(repoProvider, playerProvider);
  }

  public static LibraryViewModel newInstance(MusicRepository repo, PlayerConnection player) {
    return new LibraryViewModel(repo, player);
  }
}
