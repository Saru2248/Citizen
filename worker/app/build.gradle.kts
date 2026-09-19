import java.util.Properties
import java.net.NetworkInterface
import java.net.Inet4Address

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            load(it)
        }
    }
}

fun detectLocalHostIp(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                    val ip = addr.hostAddress
                    if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("172.")) {
                        return ip
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return "127.0.0.1"
}

val configuredLanHost = localProperties.getProperty("CITIZEN_AI_DEV_LAN_HOST")?.trim()
val devLanHost = if (configuredLanHost.isNullOrEmpty() || configuredLanHost.equals("auto", ignoreCase = true)) {
    detectLocalHostIp()
} else {
    configuredLanHost
}
val devPort = localProperties.getProperty("CITIZEN_AI_DEV_PORT") ?: "8000"
val stagingApiUrl = localProperties.getProperty("CITIZEN_AI_STAGING_API_URL") ?: "https://staging-api.citizenai.org/api/"
val prodApiUrl = localProperties.getProperty("CITIZEN_AI_PROD_API_URL") ?: "https://api.citizenai.org/api/"

fun normalizeApiBaseUrl(url: String): String {
    val clean = url.trim().trimEnd('/')
    return if (clean.endsWith("/api")) "$clean/" else "$clean/api/"
}

fun extractOriginUrl(url: String): String {
    val clean = url.trim().trimEnd('/')
    return if (clean.endsWith("/api")) clean.removeSuffix("/api").trimEnd('/') else clean
}

android {
    namespace = "com.citizenai.worker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.citizenai.worker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:$devPort/api/\"")
        buildConfigField("String", "SOCKET_URL", "\"http://127.0.0.1:$devPort\"")
        buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_USB\"")
    }

    flavorDimensions += "environment"

    productFlavors {
        create("localUsb") {
            dimension = "environment"
            isDefault = true
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:$devPort/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"http://127.0.0.1:$devPort\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_USB\"")
        }

        create("localLan") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://$devLanHost:$devPort/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"http://$devLanHost:$devPort\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_LAN\"")
        }

        create("staging") {
            dimension = "environment"
            val base = normalizeApiBaseUrl(stagingApiUrl)
            val socket = extractOriginUrl(stagingApiUrl)
            buildConfigField("String", "API_BASE_URL", "\"$base\"")
            buildConfigField("String", "SOCKET_URL", "\"$socket\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"STAGING\"")
        }

        create("production") {
            dimension = "environment"
            val base = normalizeApiBaseUrl(prodApiUrl)
            val socket = extractOriginUrl(prodApiUrl)
            buildConfigField("String", "API_BASE_URL", "\"$base\"")
            buildConfigField("String", "SOCKET_URL", "\"$socket\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"PRODUCTION\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)

    // Compose BOM & Material3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking (Retrofit, OkHttp, Socket.IO)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.socket.io.client)

    // Room Local Storage Cache
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Image Loading & CameraX
    implementation(libs.coil.compose)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // Location & Maps
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Firebase BOM & Auth
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    // Kotlin Coroutines & Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.ui.tooling)
}
