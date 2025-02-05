package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Group
import org.springframework.data.jpa.repository.JpaRepository

interface GroupRepository : JpaRepository<Group, Long> {
}