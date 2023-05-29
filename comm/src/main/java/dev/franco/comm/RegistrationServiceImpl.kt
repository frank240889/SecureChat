package dev.franco.comm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class RegistrationServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : RegistrationService {
    override fun startRegistration(serviceName: String) {
        RegisterService.startRegisterService(context, serviceName)
    }

    override fun stopRegistration() {
        RegisterService.stopRegisterService(context)
    }
}
