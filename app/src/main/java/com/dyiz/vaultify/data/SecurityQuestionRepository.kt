package com.dyiz.vaultify.data

import com.dyiz.vaultify.Database.SecurityQuestionDao
import com.dyiz.vaultify.Database.SecurityQuestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityQuestionRepository @Inject constructor(
    private val securityQuestionDao: SecurityQuestionDao
) {

    suspend fun save(question: String, answer: String, hint: String) {
        securityQuestionDao.insert(
            SecurityQuestionEntity(
                id = 1,
                question = question,
                answer = answer,
                hint = hint
            )
        )
    }

    suspend fun get(): SecurityQuestionEntity? = securityQuestionDao.get()
}
