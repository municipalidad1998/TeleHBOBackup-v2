package com.denilson.music.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AnalysisDao_Impl implements AnalysisDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  private final EntityUpsertionAdapter<AnalysisEntity> __upsertionAdapterOfAnalysisEntity;

  public AnalysisDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM analysis";
        return _query;
      }
    };
    this.__upsertionAdapterOfAnalysisEntity = new EntityUpsertionAdapter<AnalysisEntity>(new EntityInsertionAdapter<AnalysisEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `analysis` (`songId`,`gainDb`,`peakDb`,`loudnessLufs`,`silenceStartMs`,`endTrimMs`,`analyzedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AnalysisEntity entity) {
        statement.bindLong(1, entity.getSongId());
        statement.bindDouble(2, entity.getGainDb());
        statement.bindDouble(3, entity.getPeakDb());
        statement.bindDouble(4, entity.getLoudnessLufs());
        statement.bindLong(5, entity.getSilenceStartMs());
        statement.bindLong(6, entity.getEndTrimMs());
        statement.bindLong(7, entity.getAnalyzedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<AnalysisEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `analysis` SET `songId` = ?,`gainDb` = ?,`peakDb` = ?,`loudnessLufs` = ?,`silenceStartMs` = ?,`endTrimMs` = ?,`analyzedAt` = ? WHERE `songId` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AnalysisEntity entity) {
        statement.bindLong(1, entity.getSongId());
        statement.bindDouble(2, entity.getGainDb());
        statement.bindDouble(3, entity.getPeakDb());
        statement.bindDouble(4, entity.getLoudnessLufs());
        statement.bindLong(5, entity.getSilenceStartMs());
        statement.bindLong(6, entity.getEndTrimMs());
        statement.bindLong(7, entity.getAnalyzedAt());
        statement.bindLong(8, entity.getSongId());
      }
    });
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final AnalysisEntity entity, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfAnalysisEntity.upsert(entity);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getBySong(final long songId,
      final Continuation<? super AnalysisEntity> $completion) {
    final String _sql = "SELECT * FROM analysis WHERE songId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, songId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AnalysisEntity>() {
      @Override
      @Nullable
      public AnalysisEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfGainDb = CursorUtil.getColumnIndexOrThrow(_cursor, "gainDb");
          final int _cursorIndexOfPeakDb = CursorUtil.getColumnIndexOrThrow(_cursor, "peakDb");
          final int _cursorIndexOfLoudnessLufs = CursorUtil.getColumnIndexOrThrow(_cursor, "loudnessLufs");
          final int _cursorIndexOfSilenceStartMs = CursorUtil.getColumnIndexOrThrow(_cursor, "silenceStartMs");
          final int _cursorIndexOfEndTrimMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTrimMs");
          final int _cursorIndexOfAnalyzedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "analyzedAt");
          final AnalysisEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpSongId;
            _tmpSongId = _cursor.getLong(_cursorIndexOfSongId);
            final float _tmpGainDb;
            _tmpGainDb = _cursor.getFloat(_cursorIndexOfGainDb);
            final float _tmpPeakDb;
            _tmpPeakDb = _cursor.getFloat(_cursorIndexOfPeakDb);
            final float _tmpLoudnessLufs;
            _tmpLoudnessLufs = _cursor.getFloat(_cursorIndexOfLoudnessLufs);
            final long _tmpSilenceStartMs;
            _tmpSilenceStartMs = _cursor.getLong(_cursorIndexOfSilenceStartMs);
            final long _tmpEndTrimMs;
            _tmpEndTrimMs = _cursor.getLong(_cursorIndexOfEndTrimMs);
            final long _tmpAnalyzedAt;
            _tmpAnalyzedAt = _cursor.getLong(_cursorIndexOfAnalyzedAt);
            _result = new AnalysisEntity(_tmpSongId,_tmpGainDb,_tmpPeakDb,_tmpLoudnessLufs,_tmpSilenceStartMs,_tmpEndTrimMs,_tmpAnalyzedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByAlbum(final long albumId,
      final Continuation<? super List<AnalysisEntity>> $completion) {
    final String _sql = "SELECT a.* FROM analysis a INNER JOIN songs s ON a.songId = s.id WHERE s.albumId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, albumId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AnalysisEntity>>() {
      @Override
      @NonNull
      public List<AnalysisEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfGainDb = CursorUtil.getColumnIndexOrThrow(_cursor, "gainDb");
          final int _cursorIndexOfPeakDb = CursorUtil.getColumnIndexOrThrow(_cursor, "peakDb");
          final int _cursorIndexOfLoudnessLufs = CursorUtil.getColumnIndexOrThrow(_cursor, "loudnessLufs");
          final int _cursorIndexOfSilenceStartMs = CursorUtil.getColumnIndexOrThrow(_cursor, "silenceStartMs");
          final int _cursorIndexOfEndTrimMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTrimMs");
          final int _cursorIndexOfAnalyzedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "analyzedAt");
          final List<AnalysisEntity> _result = new ArrayList<AnalysisEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnalysisEntity _item;
            final long _tmpSongId;
            _tmpSongId = _cursor.getLong(_cursorIndexOfSongId);
            final float _tmpGainDb;
            _tmpGainDb = _cursor.getFloat(_cursorIndexOfGainDb);
            final float _tmpPeakDb;
            _tmpPeakDb = _cursor.getFloat(_cursorIndexOfPeakDb);
            final float _tmpLoudnessLufs;
            _tmpLoudnessLufs = _cursor.getFloat(_cursorIndexOfLoudnessLufs);
            final long _tmpSilenceStartMs;
            _tmpSilenceStartMs = _cursor.getLong(_cursorIndexOfSilenceStartMs);
            final long _tmpEndTrimMs;
            _tmpEndTrimMs = _cursor.getLong(_cursorIndexOfEndTrimMs);
            final long _tmpAnalyzedAt;
            _tmpAnalyzedAt = _cursor.getLong(_cursorIndexOfAnalyzedAt);
            _item = new AnalysisEntity(_tmpSongId,_tmpGainDb,_tmpPeakDb,_tmpLoudnessLufs,_tmpSilenceStartMs,_tmpEndTrimMs,_tmpAnalyzedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<AnalysisEntity>> $completion) {
    final String _sql = "SELECT * FROM analysis";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AnalysisEntity>>() {
      @Override
      @NonNull
      public List<AnalysisEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfSongId = CursorUtil.getColumnIndexOrThrow(_cursor, "songId");
          final int _cursorIndexOfGainDb = CursorUtil.getColumnIndexOrThrow(_cursor, "gainDb");
          final int _cursorIndexOfPeakDb = CursorUtil.getColumnIndexOrThrow(_cursor, "peakDb");
          final int _cursorIndexOfLoudnessLufs = CursorUtil.getColumnIndexOrThrow(_cursor, "loudnessLufs");
          final int _cursorIndexOfSilenceStartMs = CursorUtil.getColumnIndexOrThrow(_cursor, "silenceStartMs");
          final int _cursorIndexOfEndTrimMs = CursorUtil.getColumnIndexOrThrow(_cursor, "endTrimMs");
          final int _cursorIndexOfAnalyzedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "analyzedAt");
          final List<AnalysisEntity> _result = new ArrayList<AnalysisEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AnalysisEntity _item;
            final long _tmpSongId;
            _tmpSongId = _cursor.getLong(_cursorIndexOfSongId);
            final float _tmpGainDb;
            _tmpGainDb = _cursor.getFloat(_cursorIndexOfGainDb);
            final float _tmpPeakDb;
            _tmpPeakDb = _cursor.getFloat(_cursorIndexOfPeakDb);
            final float _tmpLoudnessLufs;
            _tmpLoudnessLufs = _cursor.getFloat(_cursorIndexOfLoudnessLufs);
            final long _tmpSilenceStartMs;
            _tmpSilenceStartMs = _cursor.getLong(_cursorIndexOfSilenceStartMs);
            final long _tmpEndTrimMs;
            _tmpEndTrimMs = _cursor.getLong(_cursorIndexOfEndTrimMs);
            final long _tmpAnalyzedAt;
            _tmpAnalyzedAt = _cursor.getLong(_cursorIndexOfAnalyzedAt);
            _item = new AnalysisEntity(_tmpSongId,_tmpGainDb,_tmpPeakDb,_tmpLoudnessLufs,_tmpSilenceStartMs,_tmpEndTrimMs,_tmpAnalyzedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM analysis";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
