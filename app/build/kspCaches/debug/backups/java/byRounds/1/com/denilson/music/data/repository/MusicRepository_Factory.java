package com.denilson.music.data.repository;

import com.denilson.music.data.datastore.SettingsRepository;
import com.denilson.music.data.db.AppDatabase;
import com.denilson.music.data.scanner.MediaStoreScanner;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
    "cast",
    "deprecation"
})
public final class MusicRepository_Factory implements Factory<MusicRepository> {
  private final Provider<AppDatabase> dbProvider;

  private final Provider<MediaStoreScanner> scannerProvider;

  private final Provider<SettingsRepository> settingsRepoProvider;

  public MusicRepository_Factory(Provider<AppDatabase> dbProvider,
      Provider<MediaStoreScanner> scannerProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    this.dbProvider = dbProvider;
    this.scannerProvider = scannerProvider;
    this.settingsRepoProvider = settingsRepoProvider;
  }

  @Override
  public MusicRepository get() {
    return newInstance(dbProvider.get(), scannerProvider.get(), settingsRepoProvider.get());
  }

  public static MusicRepository_Factory create(Provider<AppDatabase> dbProvider,
      Provider<MediaStoreScanner> scannerProvider,
      Provider<SettingsRepository> settingsRepoProvider) {
    return new MusicRepository_Factory(dbProvider, scannerProvider, settingsRepoProvider);
  }

  public static MusicRepository newInstance(AppDatabase db, MediaStoreScanner scanner,
      SettingsRepository settingsRepo) {
    return new MusicRepository(db, scanner, settingsRepo);
  }
}
