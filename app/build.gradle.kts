import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Llave de OpenWeatherMap: nunca en el código fuente ni en git — vive solo en
// local.properties (gitignorada a nivel de repo) y llega al código vía BuildConfig.
//
// Para compilar el APK que se distribuye públicamente (Releases de GitHub), usar
// -PtcaPublicBuild=true: fuerza la llave vacía sin tocar local.properties, para que
// el binario nunca lleve la llave personal (queda en BuildConfig como ""; cada quien
// pone la suya desde la app, ver ClimaScreen). Ejemplo:
//   ./gradlew assembleRelease -PtcaPublicBuild=true
val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}
val owmApiKey: String =
    if (project.hasProperty("tcaPublicBuild")) "" else propiedadesLocales.getProperty("OWM_API_KEY", "")

android {
    namespace = "com.pablock.tca"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pablock.tca"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2"
        buildConfigField("String", "OWM_API_KEY", "\"$owmApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-process:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.0")

    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    implementation("androidx.datastore:datastore-preferences:1.2.1")
}
