package com.dyiz.vaultify.data

import com.dyiz.vaultify.Database.PinDao
import com.dyiz.vaultify.Database.PinEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinRepository @Inject constructor(
    private val pinDao: PinDao
) {

    suspend fun savePin(pin: String) {
        pinDao.insertPin(PinEntity(id = 1, pin = pin))
    }

    suspend fun getPin(): PinEntity? = pinDao.getPin()

    suspend fun deletePin() {
        pinDao.deleteAll()
    }
}
