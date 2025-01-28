package com.zeki.flipsyncserver.config.security

import com.zeki.flipsyncserver.domain.service.GetUserEntityService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(
    private val getUserEntityService: GetUserEntityService
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = getUserEntityService.getUser(username)
        return UserDetailsImpl.create(user)
    }
}