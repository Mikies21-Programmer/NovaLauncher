package com.daybreak.animelauncher

data class ThemeOption(
    val name: String,
    val backgroundUri: String,
    val styleConfig: AdvancedStyleConfig
)

data class CategoryConfig(
    val name: String,
    val folderPath: String
)

object ThemePresets {
    val defaultThemes = listOf(
        ThemeOption(
            name = "Retrato 1",
            backgroundUri = "file:///android_asset/fondos/Retratos/136d4d367b87f2a8fee34ab89da47072.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 2",
            backgroundUri = "file:///android_asset/fondos/Retratos/179262-retrato-diseno_grafico-diseno-arte-ceja-500x.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 3",
            backgroundUri = "file:///android_asset/fondos/Retratos/24e7684e2fe28c208545e84ef1d332c6.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 4",
            backgroundUri = "file:///android_asset/fondos/Retratos/274310a7b301d2b58a04dd499c2035fe.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 5",
            backgroundUri = "file:///android_asset/fondos/Retratos/2782daf56d709f8337044a93dfdbcbd6.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 6",
            backgroundUri = "file:///android_asset/fondos/Retratos/2e23276f675377db803ce2104e95e395.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 7",
            backgroundUri = "file:///android_asset/fondos/Retratos/349938957dce20567a8c38b07de306b6.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 8",
            backgroundUri = "file:///android_asset/fondos/Retratos/40b03049bb7b4e63012172f7c0d64058.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 9",
            backgroundUri = "file:///android_asset/fondos/Retratos/45d126a57361a3d42a14db1e8f795d8a.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        ),
        ThemeOption(
            name = "Retrato 10",
            backgroundUri = "file:///android_asset/fondos/Retratos/567dc30bbc9faa1bb342123178ac979f.jpg",
            styleConfig = AdvancedStyleConfig(miButtonColor = "#000000", miTextColor = "#FFFFFF", clockColor = "#000000", batteryColor = "#000000", messagesColor = "#000000", dateColor = "#000000", appDrawerTextColor = "#FFFFFF", sidebarColor = "#FFFFFF", sidebarOpacity = 1.0f, diagonalBarColor = "#000000", diagonalBarOpacity = 0.80f, triangleColor = "#FFFFFF", triangleOpacity = 0.60f)
        )
    )

    val categories = listOf(
        CategoryConfig("Paisajes", "fondos/Paisajes"),
        CategoryConfig("Horizonte", "fondos/Horizonte"),
        CategoryConfig("Retratos", "fondos/Retratos"),
        CategoryConfig("Cool", "fondos/cool"),
        CategoryConfig("Terror", "fondos/terror")
    )
}

fun getAssetImages(context: android.content.Context, path: String): List<String> {
    return try {
        context.assets.list(path)?.map { "file:///android_asset/$path/$it" } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

