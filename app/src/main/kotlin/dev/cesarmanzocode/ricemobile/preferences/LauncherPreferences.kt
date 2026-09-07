package dev.cesarmanzocode.ricemobile.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.cesarmanzocode.ricemobile.apps.AppKey
import dev.cesarmanzocode.ricemobile.rice.RiceId
import dev.cesarmanzocode.ricemobile.wallpaper.WallpaperMarkerPolicy
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Single Preferences DataStore instance for the process (contract §11), created outside composition. */
val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

private val RICE = stringPreferencesKey("rice_id")
private val FAVORITES = stringPreferencesKey("favorites_v1")
private val WALLPAPER = stringPreferencesKey("wallpaper_applied")

data class LauncherPreferences(
    val rice: RiceId = RiceId.Default,
    val favorites: List<AppKey> = emptyList(),
    val appliedWallpaper: String? = null, // "id:assetRevision"
)

/**
 * A read of the DataStore flow: [Ready] once it has loaded (rice/favorites unknown values are
 * already normalized), or [Unavailable] on a genuine read `IOException` (contract §11: that is
 * not proof of corruption, so it must not overwrite what is on disk). [Unavailable] carries
 * in-memory-only defaults; recovery is automatic once the flow can read again.
 */
sealed interface PreferencesSnapshot {
    val preferences: LauncherPreferences

    data class Ready(override val preferences: LauncherPreferences) : PreferencesSnapshot
    data class Unavailable(override val preferences: LauncherPreferences) : PreferencesSnapshot
}

/**
 * DataStore-backed repository (contract §11). Every write goes through `edit { }` so the
 * "limit reached"/toggle outcome is decided inside the same transaction, never against stale
 * in-memory state; concurrent calls are naturally serialized by DataStore's own transactions.
 */
class PreferencesRepository(private val store: DataStore<Preferences>) {

    val snapshots: Flow<PreferencesSnapshot> = store.data
        .map<Preferences, PreferencesSnapshot> { prefs -> PreferencesSnapshot.Ready(prefs.toLauncherPreferences()) }
        .catch { error ->
            if (error is IOException) {
                emit(PreferencesSnapshot.Unavailable(LauncherPreferences()))
            } else {
                throw error
            }
        }

    suspend fun selectRice(id: RiceId) {
        store.edit { prefs -> prefs[RICE] = id.persisted }
    }

    suspend fun toggleFavorite(key: AppKey): FavoriteToggleResult {
        var outcome = FavoriteToggleResult.Removed
        store.edit { prefs ->
            val current = FavoriteCodec.decode(prefs[FAVORITES])
            val (next, result) = FavoriteRules.toggle(current, key)
            outcome = result
            prefs[FAVORITES] = FavoriteCodec.encode(next)
        }
        return outcome
    }

    suspend fun removeFavorite(key: AppKey) {
        store.edit { prefs ->
            val current = FavoriteCodec.decode(prefs[FAVORITES])
            prefs[FAVORITES] = FavoriteCodec.encode(FavoriteRules.remove(current, key))
        }
    }

    /** Only commits the marker if [riceId] is still the persisted rice (contract §8). */
    suspend fun markWallpaperApplied(riceId: RiceId, marker: String) {
        store.edit { prefs ->
            val persistedRice = RiceId.fromPersisted(prefs[RICE])
            if (WallpaperMarkerPolicy.shouldPersist(persistedRice, riceId)) {
                prefs[WALLPAPER] = marker
            }
        }
    }

    private fun Preferences.toLauncherPreferences(): LauncherPreferences = LauncherPreferences(
        rice = RiceId.fromPersisted(this[RICE]),
        favorites = FavoriteCodec.decode(this[FAVORITES]),
        appliedWallpaper = this[WALLPAPER],
    )
}
