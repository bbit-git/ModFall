plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun gitOutput(vararg args: String): String? {
    val process = ProcessBuilder(listOf("git", *args))
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText().trim()
    return output.takeIf { process.waitFor() == 0 && it.isNotBlank() }
}

val gitVersionName = gitOutput("describe", "--tags", "--exact-match", "HEAD")
    ?: "dev-${gitOutput("rev-parse", "--short", "HEAD") ?: "unknown"}"

val releaseKeystorePath = System.getenv("ANDROID_UPLOAD_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }

fun requireReleaseSigningEnv(name: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: throw GradleException("$name must be set when ANDROID_UPLOAD_KEYSTORE_PATH is set")

val buildLibOpenMpt = tasks.register<Exec>("buildLibOpenMpt") {
    group = "build"
    description = "Download and build libopenmpt for the Android ABIs used by this app."
    workingDir = rootProject.projectDir
    commandLine("bash", "scripts/build-libopenmpt.sh")
    onlyIf("libopenmpt prebuilt missing") {
        listOf("arm64-v8a", "armeabi-v7a", "x86_64").any { abi ->
            !file("src/main/cpp/libopenmpt/prebuilt/$abi/$abi/libopenmpt.so").exists()
        }
    }
}

android {
    namespace = "com.bigbangit.modfall"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bigbangit.modfall"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = gitVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath)
                storePassword = requireReleaseSigningEnv("ANDROID_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = requireReleaseSigningEnv("ANDROID_UPLOAD_KEY_ALIAS")
                keyPassword = requireReleaseSigningEnv("ANDROID_UPLOAD_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    ndkVersion = "27.0.12077973"

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn(buildLibOpenMpt)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")

    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.google.android.material:material:1.13.0")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
