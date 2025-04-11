plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.androidepub"
    compileSdk = 35
    ndkVersion = "26.2.11394342" // Version of the NDK we're using

    defaultConfig {
        applicationId = "com.example.androidepub"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Configure native library loading
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
            abiFilters.add("armeabi-v7a")
            abiFilters.add("x86")
        }
    }

    buildTypes {
        release {
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
    
    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }
}

// Task to copy native libraries to jniLibs directory
tasks.register("copyNativeLibs") {
    doLast {
        // Create the jniLibs directory if it doesn't exist
        val jniLibsDir = file("${projectDir}/src/main/jniLibs")
        if (!jniLibsDir.exists()) {
            jniLibsDir.mkdirs()
        }
        
        // Define the target architectures and their corresponding directories
        val architectures = mapOf(
            "arm64-v8a" to "aarch64-linux-android",
            "armeabi-v7a" to "armv7-linux-androideabi",
            "x86" to "i686-linux-android",
            "x86_64" to "x86_64-linux-android"
        )
        
        // Copy the native library for each architecture
        architectures.forEach { (abi, _) ->
            val targetDir = file("${jniLibsDir}/${abi}")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            
            // Copy the library from the Rust target directory
            val sourceFile = file("${rootDir}/native/target/release/libhub.so")
            if (sourceFile.exists()) {
                sourceFile.copyTo(file("${targetDir}/libhub.so"), overwrite = true)
                println("Copied libhub.so to ${targetDir}")
            } else {
                println("Warning: Source library not found at ${sourceFile}")
            }
        }
    }
}

// Make the preBuild task depend on copyNativeLibs
tasks.named("preBuild") {
    dependsOn("copyNativeLibs")
}

dependencies {
    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    
    // Coroutines for asynchronous programming
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // No database storage needed
    
    // Using UniFFI for Rust integration
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("net.java.dev.jna:jna-platform:5.12.1")
    
    // No HTML processing needed in Kotlin (handled by Rust)
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
