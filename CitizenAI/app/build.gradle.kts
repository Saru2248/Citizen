import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            load(it)
        }
    }
}

val devLanHost = localProperties.getProperty("CITIZEN_AI_DEV_LAN_HOST") ?: "192.168.0.105"
val devPort = localProperties.getProperty("CITIZEN_AI_DEV_PORT") ?: "8000"
val stagingApiUrl = localProperties.getProperty("CITIZEN_AI_STAGING_API_URL") ?: "https://staging-api.citizenai.org/api/"
val prodApiUrl = localProperties.getProperty("CITIZEN_AI_PROD_API_URL") ?: "https://api.citizenai.org/api/"

android {
    namespace = "com.citizenai.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.citizenai.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:$devPort/api/\"")
        buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_USB\"")
        buildConfigField("String", "MAPS_API_KEY", "\"\"")

        manifestPlaceholders["MAPS_API_KEY"] = ""
    }

    flavorDimensions += "environment"

    productFlavors {
        create("localUsb") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:$devPort/api/\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_USB\"")
        }

        create("localLan") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"http://$devLanHost:$devPort/api/\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"LOCAL_DEBUG_LAN\"")
        }

        create("staging") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$stagingApiUrl\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"STAGING\"")
        }

        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"$prodApiUrl\"")
            buildConfigField("String", "API_ENVIRONMENT", "\"PRODUCTION\"")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            buildConfigField("String", "MAPS_API_KEY", "\"\"")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "MAPS_API_KEY", "\"\"")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.splashscreen)

    // Compose BOM & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Network & Socket.IO
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

    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // Coroutines & Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Firebase BOM & Services
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.storage)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}