import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Local benchmark/validation runs may explicitly avoid reading Release credentials.
val skipReleaseSigning = providers.gradleProperty("nova.skipReleaseSigning")
    .map(String::toBoolean).getOrElse(false)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (!skipReleaseSigning && keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { input ->
        keystoreProperties.load(input)
    }
}

// 07C-7C-3D: retain legacy files locally for the verified private recovery
// package, but exclude exactly these 116 unlicensed wallpaper assets from APK/AAB.
// New Orivolt PNGs are intentionally not covered by this list.
val retiredWallpaperAssets = listOf(
    "fondos/Paisajes/1.jpg",
    "fondos/Paisajes/2.jpg",
    "fondos/Paisajes/Default_Create_an_image_of_a_crystalclear_lake_nestled_in_the_3_e65f0c67-0196-4170-bded-6b3176048e3a_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_0_24540c1f-8cf0-4a4d-bd48-68aebb25aaf3_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_0_6608b132-b474-4182-8ec5-1642b755d482_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_1_3403ef18-a4d7-402e-8979-c1d186d8441d_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_2_329f9364-1789-481d-8027-f9495e7b6e4c_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_2_ec695310-58b2-443d-a9e3-af612e20238e_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_3_60ec000f-7fee-41d5-ba4d-db6d508682c6_1.jpg",
    "fondos/Paisajes/Default_Fantasy_landscape_alien_planet_tentacles_strange_fauna_3_8f0ca79d-4c33-4666-830d-4a2010745331_1.jpg",
    "fondos/Paisajes/Gemini_Generated_Image_lg0nlflg0nlflg0n.jpg",
    "fondos/Paisajes/Gemini_Generated_Image_n7wpzrn7wpzrn7wp.jpg",
    "fondos/Paisajes/Gemini_Generated_Image_qxotgrqxotgrqxot.jpg",
    "fondos/Paisajes/Gemini_Generated_Image_vozp88vozp88vozp.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_in_t_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_in_t_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_in_t_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_0 (2).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_0 (3).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_1 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_1 (2).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_2 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Create_an_image_of_a_crystalclear_lake_nest_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Heres_a_prompt_Imagine_yourself_standing_at_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Heres_a_prompt_Imagine_yourself_standing_at_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Heres_a_prompt_Imagine_yourself_standing_at_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_1 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_2 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_I_would_like_an_image_of_a_futuristic_city_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_1 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_3 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_a_futuristic_view_of_Earth_from_a_h_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_yourself_standing_at_the_edge_of_a_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Imagine_yourself_standing_at_the_edge_of_a_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Ligh_storm_concept_art_fantasy_art_beautifu_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_2 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_2.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_long_range_view_Beautiful_Japanese_flower_g_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_0 (2).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_1 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_3 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Spring_beautiful_valley_of_flowers_realisti_3.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_0 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_0.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_1 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_1.jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_3 (1).jpg",
    "fondos/Paisajes/Leonardo_Diffusion_Town_in_cave_lanterns_3.jpg",
    "fondos/Horizonte/18b8503acd425f14872f917a8d16742e.jpg",
    "fondos/Horizonte/2a56d88ddcab9f300439a45f2592f079.jpg",
    "fondos/Horizonte/2cf72c0b444c83af57a2b3f467f35dd7.jpg",
    "fondos/Horizonte/351a59ad7aff54b8e23965fc6db2e97f.jpg",
    "fondos/Horizonte/3fad250fd0c988027993d142426a5cd4.jpg",
    "fondos/Horizonte/52c5b68e72b5e836499ef5416a94dcd4.jpg",
    "fondos/Horizonte/7ec330935fe29e8e677b79799ee5fa34.jpg",
    "fondos/Horizonte/8823ae1fe9eb63078fe02e8d61c2f26c.jpg",
    "fondos/Horizonte/b9b2a2da3bedbab88b81b7fdcad715c8.jpg",
    "fondos/Horizonte/bdf844e048d208e88ad90b507fb73bee.jpg",
    "fondos/Horizonte/db91e982967aab2670b4f18a11b60f28.jpg",
    "fondos/Retratos/136d4d367b87f2a8fee34ab89da47072.jpg",
    "fondos/Retratos/179262-retrato-diseno_grafico-diseno-arte-ceja-500x.jpg",
    "fondos/Retratos/24e7684e2fe28c208545e84ef1d332c6.jpg",
    "fondos/Retratos/274310a7b301d2b58a04dd499c2035fe.jpg",
    "fondos/Retratos/2782daf56d709f8337044a93dfdbcbd6.jpg",
    "fondos/Retratos/2e23276f675377db803ce2104e95e395.jpg",
    "fondos/Retratos/349938957dce20567a8c38b07de306b6.jpg",
    "fondos/Retratos/40b03049bb7b4e63012172f7c0d64058.jpg",
    "fondos/Retratos/45d126a57361a3d42a14db1e8f795d8a.jpg",
    "fondos/Retratos/567dc30bbc9faa1bb342123178ac979f.jpg",
    "fondos/Retratos/618b083a3ab885a4343885ca5763ac92.jpg",
    "fondos/Retratos/68df44d1288453c9c34f7767673d0851.jpg",
    "fondos/Retratos/703e7a656ba4e6ff2cb922ff1a55005f.jpg",
    "fondos/Retratos/770432a79b6312391af1711b077c27bb.jpg",
    "fondos/Retratos/7c7a5112990496cec5cb1c983580857a.jpg",
    "fondos/Retratos/83587faa41a69e77bb2002088ad5e3de.jpg",
    "fondos/Retratos/86f1b4a50003438a880057d6617e4e06.jpg",
    "fondos/Retratos/8786cd0202a4c0535db1567324fc015e.jpg",
    "fondos/Retratos/91ee00ca272a89840b7765c2cb406748.jpg",
    "fondos/Retratos/9720cca105062f0b264f5f6ccf8f9009.jpg",
    "fondos/Retratos/b21b60ccf3a2b4ae898b5224d9d14d9b.jpg",
    "fondos/Retratos/be3182f03f5b36e1c3e13392a86630f8.jpg",
    "fondos/Retratos/c5e890eaab6a00650236b54325bc72e2.jpg",
    "fondos/Retratos/c7636ae2b9e1bb1614dbca87d81ced84.jpg",
    "fondos/Retratos/d123183d9e79f2421ea13069aa6d3c30.jpg",
    "fondos/Retratos/eacff82034856467ba2ec5109daee03b.jpg",
    "fondos/Retratos/eb95bbd605de583fddbe6a66c88a3abb.jpg",
    "fondos/Retratos/fb53304eb72dc7596a27717e3c1fffe5.jpg",
    "fondos/cool/1fb981a4c9ae95d5eb7ead2db0f714ca.jpg",
    "fondos/cool/9f811beb86afd76c4ecd2d3d36aa5564.jpg",
    "fondos/cool/af30ea8448a4d2227de13447980c50fe.jpg",
    "fondos/cool/images.jpg",
    "fondos/terror/1.jpg",
    "fondos/terror/136d4d367b87f2a8fee34ab89da47072.jpg",
    "fondos/terror/83615db3b8632bfc2fcc943f3e71f480.jpg",
    "fondos/terror/8f268e89be2ad6090b4f3de8e1144a75.jpg",
    "fondos/terror/a9a5097039cd3a70f39ea262f5b269d2.jpg",
    "fondos/terror/b242625bdcc9d07ca6a9630978d84fb2.jpg",
    "fondos/terror/descarga.png",
    "fondos/terror/e4a89a041082b1767cca442823655adb.jpg",
    "fondos/terror/f70ff32f315c18b53f47e26e97b7a0da.jpg",
)
android {
    namespace = "com.daybreak.animelauncher"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.daybreak.animelauncher"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "1.6.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    lint {
        // Ignoramos estos errores específicos porque son necesarios para un Launcher y Media3
        disable += "ProtectedPermissions"
        disable += "UnsafeOptInUsageError"
        baseline = file("lint-baseline.xml")
    }

    signingConfigs {
        create("release") {
            if (!skipReleaseSigning && keystorePropertiesFile.exists()) {
                val storeFilePath = keystoreProperties.getProperty("storeFile")
                if (!storeFilePath.isNullOrBlank()) {
                    storeFile = file(storeFilePath)
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (!skipReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmark") {
            initWith(buildTypes.getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
                "proguard-benchmark-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/res/raw/bg_view_one.mp4",
                "/res/raw/bg_view_two.mp4"
            )
        }
    }

    // 07C-7B-2: paths persisted by older installations remain recognizable at
    // runtime, but these third-party marks must no longer be packaged as assets.
    // aapt matches asset filenames rather than their directory paths. Retain
    // Android's defaults, the 30 literal retired logo names, and the basenames
    // of exactly the 116 paths enumerated above (no iconos/*.png wildcard).
    androidResources {
        ignoreAssetsPatterns.addAll(
            listOf(
                "!.svn", "!.git", "!.ds_store", "!*.scc", ".*", "<dir>_*",
                "!CVS", "!thumbs.db", "!picasa.ini", "!*~",
                "cromo.png", "cromo (1).png",
                "discordia.png", "discordia (1).png",
                "facebook.png", "facebook (1).png",
                "gmail.png", "gmail (1).png",
                "google.png", "google (1).png", "google (2).png",
                "google-play.png", "google-play (1).png", "google-play (2).png",
                "google-drive.png", "google-drive (1).png", "google-drive (2).png",
                "logotipo-de-instagram.png",
                "pinterest.png", "pinterest (1).png",
                "telegrama.png", "telegrama (1).png", "telegrama (2).png", "telegrama (3).png",
                "twitter.png", "twitter-forma-negra.png",
                "whatsapp.png", "whatsapp (1).png",
                "youtube.png", "youtube (1).png"
            ) + retiredWallpaperAssets.map { it.substringAfterLast('/') }
        )
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.biometric)
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.code.gson:gson:2.10.1")
    // PERF-OPT-3: instala el Baseline Profile embebido en el APK en el momento de instalar
    // (API 24-30 vía broadcast; 31+ lo consume ART directamente del APK).
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.palette:palette-ktx:1.0.0")
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    add("benchmarkImplementation", "androidx.profileinstaller:profileinstaller:1.4.1")
    add("benchmarkImplementation", "androidx.compose.runtime:runtime-tracing")
    add("benchmarkImplementation", "androidx.tracing:tracing-perfetto:1.0.0")
}
