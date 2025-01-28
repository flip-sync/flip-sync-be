package com.zeki.flipsyncserver.config.security

import com.zeki.common.exception.ApiException
import com.zeki.common.exception.ResponseCode
import com.zeki.flipsyncdb.entity.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails


class UserDetailsImpl private constructor(
    userPk: Long,
    email: String,
    password: String,
    name: String,
    role: User.UserRole
) : UserDetails {
    val userPk: Long = userPk
    val email: String = email
    val _password: String = password
    val name: String = name
    val role: User.UserRole = role

    companion object {
        fun create(user: User) = UserDetailsImpl(
            userPk = user.id ?: throw ApiException(ResponseCode.RESOURCE_NOT_FOUND),
            email = user.username,
            password = user.password,
            name = user.name,
            role = user.role
        )
    }


    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority(role.name))
    }

    override fun getPassword(): String {
        return _password
    }

    override fun getUsername(): String {
        return email
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }
}