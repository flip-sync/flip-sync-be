package com.zeki.flipsyncserver.domain.service

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.User
import com.zeki.flipsyncdb.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class GetUserEntityService(
    private val userRepository: UserRepository
) {

    fun getUser(username: String): User {
        return userRepository.findByUsername(username) ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND)
    }

    fun getUserNullable(username: String): User? {
        return userRepository.findByUsername(username)
    }

}
