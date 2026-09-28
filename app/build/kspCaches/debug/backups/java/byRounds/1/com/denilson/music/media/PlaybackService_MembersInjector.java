package com.denilson.music.media;

import com.denilson.music.data.datastore.SettingsRepository;
import com.denilson.music.data.repository.MusicRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlaybackService_MembersInjector implements MembersInjector<PlaybackService> {
  private final Provider<SettingsRepository> settingsRepoProvider;

  private final Provider<MusicRepository> repositoryProvider;

  public PlaybackService_MembersInjector(Provider<SettingsRepository> settingsRepoProvider,
      Provider<MusicRepository> repositoryProvider) {
    this.settingsRepoProvider = settingsRepoProvider;
    this.repositoryProvider = repositoryProvider;
  }

  public static MembersInjector<PlaybackService> create(
      Provider<SettingsRepository> settingsRepoProvider,
      Provider<MusicRepository> repositoryProvider) {
    return new PlaybackService_MembersInjector(settingsRepoProvider, repositoryProvider);
  }

  @Override
  public void injectMembers(PlaybackService instance) {
    injectSettingsRepo(instance, settingsRepoProvider.get());
    injectRepository(instance, repositoryProvider.get());
  }

  @InjectedFieldSignature("com.denilson.music.media.PlaybackService.settingsRepo")
  public static void injectSettingsRepo(PlaybackService instance, SettingsRepository settingsRepo) {
    instance.settingsRepo = settingsRepo;
  }

  @InjectedFieldSignature("com.denilson.music.media.PlaybackService.repository")
  public static void injectRepository(PlaybackService instance, MusicRepository repository) {
    instance.repository = repository;
  }
}
