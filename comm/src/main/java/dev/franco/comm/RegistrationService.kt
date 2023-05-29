package dev.franco.comm

interface RegistrationService {
    fun startRegistration(serviceName: String)
    fun stopRegistration()
}
