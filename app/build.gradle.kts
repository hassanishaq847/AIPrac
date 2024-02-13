plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("de.undercouch.download")
}

android {
    namespace = "com.be.aiprac"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.be.aiprac"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {

            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }

            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {

            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "armeabi")
            }

            isMinifyEnabled = false
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
        viewBinding = true
    }
}

// Downloads the TFLite and Task files used for plugins
project.ext.set("ASSET_DIR", "$projectDir/src/main/assets")
apply(from = "download_tasks.gradle")

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

//    implementation("com.google.mediapipe:tasks-vision:0.20230731")
    implementation("com.google.mediapipe:tasks-vision:0.10.9")
//    implementation("com.google.mediapipe:tasks-vision-image-generator:0.10.5.2")

    val camerax_version = "1.4.0-alpha02"
    // CameraX core library
    implementation("androidx.camera:camera-core:$camerax_version")

    // CameraX Camera2 extensions
    implementation("androidx.camera:camera-camera2:$camerax_version")

    // CameraX Lifecycle library

    implementation("androidx.camera:camera-lifecycle:$camerax_version")

    // CameraX View class
    implementation("androidx.camera:camera-view:$camerax_version")

    //WindowManager
    implementation("androidx.window:window:1.3.0-alpha01")
}