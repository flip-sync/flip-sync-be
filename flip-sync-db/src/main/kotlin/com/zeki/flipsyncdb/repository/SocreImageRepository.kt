package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.ScoreImage
import org.springframework.data.jpa.repository.JpaRepository

interface SocreImageRepository : JpaRepository<ScoreImage, Long> {
}