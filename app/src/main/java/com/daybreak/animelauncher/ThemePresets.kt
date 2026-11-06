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
    // Bundled wallpapers are withheld until rights are documented for each asset.
    val defaultThemes: List<ThemeOption> = emptyList()
    val categories: List<CategoryConfig> = emptyList()
}

fun getAssetImages(context: android.content.Context, path: String): List<String> {
    return try {
        context.assets.list(path)?.map { "file:///android_asset/$path/$it" } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

