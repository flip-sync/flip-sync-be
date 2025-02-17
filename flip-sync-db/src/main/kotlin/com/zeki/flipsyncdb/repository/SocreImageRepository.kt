package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.SocreImage
import org.springframework.data.jpa.repository.JpaRepository

interface SocreImageRepository : JpaRepository<SocreImage, Long> {
}