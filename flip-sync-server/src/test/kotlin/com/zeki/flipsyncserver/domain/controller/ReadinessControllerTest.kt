package com.zeki.flipsyncserver.domain.controller

import com.zeki.flipsyncserver.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ReadinessControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `health returns up without database check`() {
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.database").value("NOT_CHECKED"))
    }

    @Test
    fun `ready returns up when database is reachable`() {
        mockMvc.perform(get("/ready"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("UP"))
            .andExpect(jsonPath("$.data.database").value("UP"))
    }
}
