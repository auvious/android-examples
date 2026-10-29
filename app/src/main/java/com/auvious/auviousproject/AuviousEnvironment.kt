package com.auvious.auviousproject

/**
 * Environment configuration for Auvious SDK
 * Pairs base URLs with their corresponding MQTT WebSocket endpoints
 */
enum class AuviousEnvironment(val baseUrl: String, val mqttUrl: String) {
    PRODUCTION("https://auvious.video", "wss://auvious.video/ws"),
    STAGING("https://stg.auvious.video", "wss://stg.auvious.video/ws"),
    DEVELOPMENT("https://dev.auvious.video", "wss://dev.auvious.video/ws");

    companion object {
        fun fromName(name: String?): AuviousEnvironment {
            return entries.find { it.name == name } ?: PRODUCTION
        }
    }
}