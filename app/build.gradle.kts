plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

fun getVersionCodeFromGit(providers: ProviderFactory): Int {
    return try {
        val output = providers.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
        }.standardOutput.asText.get().trim()
        output.toIntOrNull() ?: 10
    } catch (e: Exception) {
        10
    }
}

fun getVersionNameFromGit(providers: ProviderFactory): String {
    return try {
        var latestTag = providers.exec {
            commandLine("git", "describe", "--tags", "--abbrev=0")
        }.standardOutput.asText.get().trim()
        
        if (latestTag.isEmpty()) {
            latestTag = "v1.0.0"
        }
        
        val cleanTag = latestTag.removePrefix("v")
        val tagParts = cleanTag.split(".").mapNotNull { it.toIntOrNull() }
        var major = if (tagParts.isNotEmpty()) tagParts[0] else 1
        var minor = if (tagParts.size > 1) tagParts[1] else 0
        var patch = if (tagParts.size > 2) tagParts[2] else 0

        val commitsStr = providers.exec {
            commandLine("git", "log", "$latestTag..HEAD", "--oneline")
        }.standardOutput.asText.get().trim()
        
        val commits = commitsStr.split("\n")
        
        var isMajor = false
        var isMinor = false
        var isPatch = false

        for (commit in commits) {
            if (commit.isBlank()) continue
            val message = commit.substringAfter(" ").lowercase()
            if (message.contains("breaking change") || message.contains("!") || message.startsWith("major:") || message.startsWith("major")) {
                isMajor = true
            } else if (message.startsWith("feat") || message.startsWith("medium:") || message.startsWith("medium")) {
                isMinor = true
            } else if (message.startsWith("fix") || message.startsWith("small:") || message.startsWith("small") || message.startsWith("chore") || message.startsWith("refactor")) {
                isPatch = true
            }
        }

        if (isMajor) {
            major += 1
            minor = 0
            patch = 0
        } else if (isMinor) {
            minor += 1
            patch = 0
        } else if (isPatch) {
            patch += 1
        } else if (commits.any { it.isNotBlank() }) {
            patch += 1
        }

        "$major.$minor.$patch"
    } catch (e: Exception) {
        "1.6.0"
    }
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.invoice.generator"
    minSdk = 24
    targetSdk = 36
    versionCode = getVersionCodeFromGit(providers)
    versionName = getVersionNameFromGit(providers)

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

signingConfigs {
     create("release") {
       val keystorePath = System.getenv("KEYSTORE_PATH") ?: project.findProperty("KEYSTORE_PATH")?.toString() ?: "${rootDir}/my-upload-key.jks"
       storeFile = file(keystorePath)
       storePassword = System.getenv("STORE_PASSWORD") ?: project.findProperty("STORE_PASSWORD")?.toString() ?: "android"
       keyAlias = "upload"
       keyPassword = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD")?.toString() ?: "android"
     }
     create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = true
      isMinifyEnabled = false
      isShrinkResources = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
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
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation(libs.zxing.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
