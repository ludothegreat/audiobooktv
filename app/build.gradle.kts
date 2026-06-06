plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "xyz.ludothegreat.audiobooktv"
    compileSdk = 35

    defaultConfig {
        applicationId = "xyz.ludothegreat.audiobooktv"
        minSdk = 28
        targetSdk = 35
        versionCode = 10
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload-only project: use the debug signing key so anyone with
            // gradle can build a working installable APK without secrets.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Media3 ExoPlayer's source-factory + chapter timeline APIs are
        // marked @UnstableApi. The annotation site itself is gated, so we
        // opt in at the compile-arg level for the whole module instead of
        // sprinkling @OptIn at every call site.
        freeCompilerArgs +=
            listOf(
                "-opt-in=androidx.media3.common.util.UnstableApi",
            )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        checkDependencies = false
        checkReleaseBuilds = false
        lintConfig = file("lint.xml")
        // Architectural disables -- each is required by a locked decision:
        //   * Banner is hard-pinned to Android TV launcher spec (320x180).
        //   * Self-signed cert support + TOFU TLS-pin enrollment is the
        //     entire point of decision #27 -- AcceptAll/Pinned/Capture
        //     TrustManagers and the user-CA base config are load-bearing,
        //     not accidents.
        //   * allowBackup="false" is explicit; DataExtractionRules is a
        //     recommendation that conflicts with the no-backup decision.
        //   * Dep "newer version available" is version-catalog policy, not
        //     a per-build correctness gate.
        disable +=
            setOf(
                "VectorRaster",
                "GradleDependency",
                "AndroidGradlePluginVersion",
                "NewerVersionAvailable",
                "AcceptsUserCertificates",
                "CustomX509TrustManager",
                "InsecureBaseConfiguration",
                "DataExtractionRules",
            )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}

tasks.named("check") {
    dependsOn(":spotlessCheck", ":detekt")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.guava.listenablefuture)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.okhttp)
    testImplementation(libs.retrofit)
    testImplementation(libs.retrofit.kotlinx.serialization)
    testImplementation(libs.kotlinx.serialization.json)
}
