package dev.franco.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

const val SIMPLE_STORAGE = "simple_storage"

class StorageImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : Storage {
    private var sharedPreferences: SharedPreferences? = null

    init {
        sharedPreferences = context.getSharedPreferences(SIMPLE_STORAGE, Context.MODE_PRIVATE)
    }

    override fun getString(key: String): String? {
        return sharedPreferences?.getString(key, null)
    }

    override fun putString(key: String, value: String) {
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }
}
