package xyz.ludothegreat.audiobooktv.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class Credentials(
    val serverUrl: String,
    val username: String,
    val token: String,
    val userId: String,
    val pinnedCertSha256: String? = null,
)

/**
 * Abstracts persistence of [Credentials] so [SessionManager] can be unit
 * tested without an Android Context. The concrete production binding is
 * [CredentialStore] (EncryptedSharedPreferences); test fakes implement
 * this directly.
 */
interface CredentialStorage {
    fun load(): Credentials?

    fun save(creds: Credentials)

    fun clear()
}

@Singleton
class CredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) : CredentialStorage {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun load(): Credentials? {
        val url = prefs.getString(KEY_URL, null) ?: return null
        val user = prefs.getString(KEY_USER, null) ?: return null
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val pin = prefs.getString(KEY_PINNED_CERT, null)
        return Credentials(url, user, token, id, pin)
    }

    override fun save(creds: Credentials) {
        prefs.edit {
            putString(KEY_URL, creds.serverUrl)
            putString(KEY_USER, creds.username)
            putString(KEY_TOKEN, creds.token)
            putString(KEY_USER_ID, creds.userId)
            if (creds.pinnedCertSha256 != null) {
                putString(KEY_PINNED_CERT, creds.pinnedCertSha256)
            } else {
                remove(KEY_PINNED_CERT)
            }
        }
    }

    override fun clear() {
        prefs.edit { clear() }
    }

    companion object {
        private const val FILE_NAME = "audiobooktv-credentials"
        private const val KEY_URL = "server_url"
        private const val KEY_USER = "username"
        private const val KEY_TOKEN = "token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PINNED_CERT = "pinned_cert_sha256"
    }
}
