package com.example.uiedvideocompacter.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Library : Screen("library")
    object Preview : Screen("preview/{encodedUri}")
    object Queue : Screen("queue")
    object Progress : Screen("progress")
    object Result : Screen("result")
    object Settings : Screen("settings")

    fun buildPreviewRoute(uris: List<String>): String {
        val json = "[\"" + uris.joinToString("\",\"") + "\"]"
        val encoded = android.util.Base64.encodeToString(json.toByteArray(), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
        return "preview/$encoded"
    }
}
