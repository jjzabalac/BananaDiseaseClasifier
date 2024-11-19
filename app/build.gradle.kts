plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id ("com.google.gms.google-services")
}

android {
    signingConfigs {
        create("release") {
            storeFile =
                file("C:\\Users\\ZABROSS\\Documents\\Academia\\Master\'s degree Applied Statistics and Data Science\\Proyecto de Grado\\BCD_App_key")
            storePassword = "f04u43c26K."
            keyAlias = "BDC_App_key"
            keyPassword = "f04u43c26K."
        }
    }
    namespace = "com.bananascan.classifier"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bananascan.classifier"
        minSdk = 24
        targetSdk = 34
        versionCode = 18
        versionName = "1.9.4" //

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Habilitar la ofuscación
            isMinifyEnabled = true
            // Habilitar la reducción de recursos
            isShrinkResources = true
            // Reglas de ProGuard
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"  // Ajustar según la versión de Compose
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Añadir si es necesario
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Versión corregida a 1.1.0
    implementation(libs.androidx.material)
    implementation(libs.accompanist.permissions)
    implementation(libs.accompanist.coil)
    implementation(libs.coil.compose)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.gpu) // Include this if you need GPU acceleration
    implementation(libs.tensorflow.lite.support)
    implementation (libs.androidx.ui.v105)
    implementation (libs.androidx.material.v105)
    implementation (libs.androidx.ui.tooling.preview.v105)
    implementation (libs.androidx.activity.compose.v131)
    implementation (libs.kotlinx.coroutines.android)
    implementation (libs.androidx.material3.v112)
    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))
    // TODO: Add the dependencies for Firebase products you want to use
    // When using the BoM, don't specify versions in Firebase dependencies
    implementation(libs.firebase.analytics)
    // Add the dependencies for any other desired Firebase products
    // https://firebase.google.com/docs/android/setup#available-libraries
    implementation (libs.firebase.auth.ktx)
    implementation (libs.kotlinx.coroutines.play.services)
    implementation (libs.firebase.firestore.ktx)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // StateFlow
    implementation(libs.kotlinx.coroutines.android.v173)
    // Para el manejo de ViewModels en Compose
    implementation(libs.androidx.activity.compose.v182)
    implementation(libs.androidx.webkit)



    // Dependencias para Debugging y Testing
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Dependencias de Pruebas
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

}