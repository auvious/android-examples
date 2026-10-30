package com.auvious.auviousproject

/**
 * Environment configuration for Auvious SDK
 * Pairs base URLs with their corresponding MQTT WebSocket endpoints
 */
enum class AuviousEnvironment(val title: String, val baseUrl: String, val mqttUrl: String) {
    PRODUCTION("Production", "https://auvious.video", "wss://auvious.video/ws"),
    US("US", "https://us.auvious.video", "wss://us.auvious.video/ws"),
    AUSTRALIA("Australia", "https://au.auvious.video", "wss://au.auvious.video/ws"),
    MIDDLE_EAST("Middle East", "https://me.auvious.video", "wss://me.auvious.video/ws"),
    INDIA("India", "https://in.auvious.video", "wss://in.auvious.video/ws"),
    UAE("UAE", "https://uae.auvious.video", "wss://uae.auvious.video/ws"),
    STAGING("Staging", "https://stg.auvious.video", "wss://stg.auvious.video/ws"),
    DEVELOPMENT("Development", "https://dev.auvious.video", "wss://dev.auvious.video/ws");

    companion object {
        fun fromName(name: String?): AuviousEnvironment {
            return entries.find { it.name == name } ?: PRODUCTION
        }
    }
}
