package dev.franco.security

interface Storage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}
