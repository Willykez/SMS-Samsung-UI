plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.oneui.sms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.oneui.sms"
        minSdk = 26 // required realistically for default-SMS-app role APIs used here
        targetSdk = 35
        versionCode = 8
        versionName = "0.8.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "META-INF/androidx.appcompat_appcompat.version"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
}

// The SESL/One UI libraries intentionally fork several AndroidX modules while
// keeping the original androidx.* package names. Keeping the stock and SESL
// copies together produces duplicate classes. We keep the SESL fork for the
// modules it replaces and retain official AndroidX for the rest of the stack.
configurations.configureEach {
    exclude(group = "androidx.core", module = "core")
    exclude(group = "androidx.appcompat", module = "appcompat")
    exclude(group = "androidx.fragment", module = "fragment")
    exclude(group = "androidx.viewpager", module = "viewpager")
    exclude(group = "androidx.drawerlayout", module = "drawerlayout")
    exclude(group = "androidx.customview", module = "customview")
    exclude(group = "androidx.recyclerview", module = "recyclerview")
    exclude(group = "androidx.coordinatorlayout", module = "coordinatorlayout")
    exclude(group = "androidx.swiperefreshlayout", module = "swiperefreshlayout")
    exclude(group = "androidx.preference", module = "preference")
}

dependencies {
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended") // FilterList, PushPin, Restore, Schedule, StarBorder, etc.
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room for local thread/message cache mirroring the Telephony provider
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // #1 scheduled send, #11 auto-delete retention purge
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // One UI / SESL picker catalog: native One UI date/time picker dialogs.
    implementation("io.github.oneuiproject.sesl:core:1.3.0")
    implementation("io.github.oneuiproject.sesl:appcompat:1.4.0")
    implementation("io.github.oneuiproject.sesl:picker-basic:1.2.0")
}
