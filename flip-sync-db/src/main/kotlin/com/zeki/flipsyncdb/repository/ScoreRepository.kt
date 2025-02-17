package com.zeki.flipsyncdb.repository

import com.zeki.flipsyncdb.entity.Score
import org.springframework.data.jpa.repository.JpaRepository

interface ScoreRepository : JpaRepository<Score, Long> {
}