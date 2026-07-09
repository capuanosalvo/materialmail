import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("com.google.gms.google-services")
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}
val googleClientId = secrets.getProperty("GOOGLE_CLIENT_ID") ?: ""
val pushBackendUrl = secrets.getProperty("PUSH_BACKEND_URL") ?: "https://monomail-push.yourdomain.workers.dev"

val keystoreFile = rootProject.file("keystore.properties")
val keystoreProps = Properties()
if (keystoreFile.exists()) {
    keystoreProps.load(FileInputStream(keystoreFile))
}

android {
    namespace = "com.shrivatsav.monomail"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.shrivatsav.monomail"
        minSdk = 26
        targetSdk = 35
        versionCode = 61
        versionName = "1.7.37-md3e.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "distribution"

    productFlavors {
        create("github") {
            dimension = "distribution"
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"\"")
            buildConfigField("String", "PUSH_BACKEND_URL", "\"$pushBackendUrl\"")
            buildConfigField("Boolean", "IS_GITHUB_BUILD", "true")
        }
        create("playstore") {
            dimension = "distribution"
            buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")
            buildConfigField("String", "PUSH_BACKEND_URL", "\"$pushBackendUrl\"")
            buildConfigField("Boolean", "IS_GITHUB_BUILD", "false")
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = keystoreProps.getProperty("storeFile")
            if (storeFilePath != null && storeFilePath != "") {
                storeFile = rootProject.file(storeFilePath)
            }
            storePassword = keystoreProps.getProperty("storePassword")
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (signingConfigs.getByName("release").storeFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeCompiler {
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "META-INF/NOTICE.md",
                "META-INF/LICENSE.md"
            )
            pickFirsts += listOf(
                "META-INF/mailcap",
                "META-INF/mailcap.default",
                "META-INF/mimetypes.default",
                "META-INF/javamail.default.providers",
                "META-INF/javamail.default.address.map"
            )
        }
    }
}

androidComponents {
    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localProps.load(FileInputStream(localPropsFile))
    }
    val isDevBuild = localProps.getProperty("devBuild") == "true"
    
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        if (isDevBuild) {
            variantBuilder.enable = false
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.graphics.path)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Auth
    "playstoreImplementation"(libs.androidx.credentials)
    "playstoreImplementation"(libs.androidx.credentials.play.services)
    "playstoreImplementation"(libs.google.identity.googleid)
    "playstoreImplementation"(libs.google.play.services.auth)
    "playstoreImplementation"(libs.firebase.messaging)

    // Local modules
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:pgp"))
    implementation(project(":core:data"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:inbox"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:compose"))
    implementation(project(":feature:settings"))

    // Networking (now decoupled)
    implementation(project(":core:network"))
    implementation("com.google.code.gson:gson:2.11.0") // Needed for workers & utils

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WebKit (algorithmic darkening for WebView)
    implementation(libs.androidx.webkit)

    // Markdown
    implementation("io.noties.markwon:core:4.6.2")

    // IMAP/SMTP — Eclipse Angus Mail (Jakarta Mail 2.x)
    implementation("org.eclipse.angus:angus-mail:2.0.3") {
        exclude(group = "jakarta.xml.soap")
    }
    implementation("org.eclipse.angus:angus-activation:2.0.2")

    // MSAL for Outlook Auth
    implementation("com.microsoft.identity.client:msal:5.4.0")

    // Security & Encryption
    implementation(libs.androidx.security.crypto)
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite.ktx)

    // PGP — PGPainless (OpenPGP encryption)
    implementation(libs.pgpainless.core)
    implementation(libs.androidx.webkit)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

tasks.matching { it.name.contains("Github", ignoreCase = true) && it.name.contains("GoogleServices", ignoreCase = true) }.configureEach {
    enabled = false
}

// Retain ProGuard/R8 mapping files for crash deobfuscation across releases.
// After each release build, the mapping file is copied to a versioned archive
// so crash reports from any published version can be deobfuscated.
// Capture version at config time (avoids serializing the android extension for cache).
val archiveVersionName = android.defaultConfig.versionName ?: "unknown"
val archiveVersionCode = android.defaultConfig.versionCode ?: 0
val archiveMapping by tasks.registering {
    group = "Reporting"
    description = "Archive all release ProGuard mapping files with version info"
    doLast {
        val variants = listOf("githubRelease", "playstoreRelease")
        for (variant in variants) {
            val mappingFile = layout.buildDirectory.file("outputs/mapping/$variant/mapping.txt").get().asFile
            if (mappingFile.exists()) {
                val archiveDir = layout.buildDirectory.dir("outputs/mapping/archive").get().asFile
                archiveDir.mkdirs()
                mappingFile.copyTo(
                    File(archiveDir, "mapping-$archiveVersionName-$archiveVersionCode-$variant.txt"),
                    overwrite = true
                )
            }
        }
    }
}
tasks.matching { it.name.matches(Regex("assemble(Github|Playstore)Release")) }
    .configureEach { finalizedBy(archiveMapping) }
