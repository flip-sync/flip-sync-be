package com.zeki.common.util

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException
import java.net.URI

object IPUtils {
    /**
     * 실제 Public IP 주소를 조회합니다.
     * @return String 형태의 IP 주소
     * @throws Exception IP 조회 실패시 예외 발생
     */
    fun getPublicIP(): String {
        val ipServices = listOf(
            "https://api.ipify.org",
            "https://checkip.amazonaws.com",
            "https://icanhazip.com"
        )

        for (service in ipServices) {
            try {
                val url = URI(service).toURL()
                return url.openStream().use { stream ->
                    BufferedReader(InputStreamReader(stream)).readLine().trim()
                }
            } catch (e: Exception) {
                continue
            }
        }
        throw Exception("공개 IP 주소를 가져오는데 실패했습니다.")
    }

    /**
     * 비동기로 Public IP 주소를 조회합니다.
     * @return IP 주소를 포함한 Result
     */
    suspend fun getPublicIPAsync(): Result<String> = runCatching {
        getPublicIP()
    }

    /**
     * 로컬 IP 주소를 조회합니다.
     * @return String 형태의 IP 주소
     * @throws Exception IP 조회 실패시 예외 발생
     */
    fun getLocalIP(): String {
        try {
            return NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .flatMap { it.inetAddresses.asSequence() }
                .filter { address ->
                    !address.isLoopbackAddress &&
                            !address.isLinkLocalAddress &&
                            !address.isMulticastAddress &&
                            address.hostAddress.contains('.')
                }
                .map { it.hostAddress }
                .firstOrNull() ?: throw Exception("유효한 IP 주소를 찾을 수 없습니다.")
        } catch (e: SocketException) {
            throw Exception("네트워크 인터페이스 조회 실패", e)
        }
    }

    /**
     * 호스트 이름을 조회합니다.
     * @return String 형태의 호스트 이름
     */
    fun getHostName(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }

    /**
     * IP가 사설 IP인지 확인합니다.
     * @param ip 확인할 IP 주소
     * @return Boolean 사설 IP 여부
     */
    fun isPrivateIP(ip: String): Boolean {
        return try {
            val parts = ip.split(".").map { it.toInt() }
            when {
                // 10.0.0.0 ~ 10.255.255.255
                parts[0] == 10 -> true
                // 172.16.0.0 ~ 172.31.255.255
                parts[0] == 172 && parts[1] in 16..31 -> true
                // 192.168.0.0 ~ 192.168.255.255
                parts[0] == 192 && parts[1] == 168 -> true
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 모든 네트워크 인터페이스의 IP 주소를 조회합니다.
     * @return Map<String, String> 형태의 인터페이스명과 IP 주소 쌍
     */
    fun getAllNetworkInterfaces(): Map<String, String> {
        return try {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { networkInterface ->
                    networkInterface.inetAddresses.asSequence()
                        .filter { address ->
                            !address.isLoopbackAddress &&
                                    !address.isLinkLocalAddress &&
                                    !address.isMulticastAddress &&
                                    address.hostAddress.contains('.')
                        }
                        .map { networkInterface.name to it.hostAddress }
                }
                .toMap()
        } catch (e: SocketException) {
            emptyMap()
        }
    }
}