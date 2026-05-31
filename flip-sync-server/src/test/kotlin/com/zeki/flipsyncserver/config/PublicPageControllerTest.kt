package com.zeki.flipsyncserver.config

import com.zeki.flipsyncserver.support.IntegrationTest
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class PublicPageControllerTest : IntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `assetlinks returns Android app links verification file`() {
        mockMvc.perform(get("/.well-known/assetlinks.json"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].relation[0]").value("delegate_permission/common.handle_all_urls"))
            .andExpect(jsonPath("$[0].target.namespace").value("android_app"))
            .andExpect(jsonPath("$[0].target.package_name").value("com.fliplyze.flipsync"))
            .andExpect(jsonPath("$[0].target.sha256_cert_fingerprints[0]").value("69:35:79:C5:7A:8F:5F:2D:9E:7F:B0:88:26:68:1C:84:23:CE:B4:17:64:40:E1:C5:15:12:0C:6F:C1:48:63:CA"))
    }

    @Test
    fun `invite index returns fallback page`() {
        mockMvc.perform(get("/mob/invite"))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(content().string(containsString("FlipSync 초대")))
    }
}
