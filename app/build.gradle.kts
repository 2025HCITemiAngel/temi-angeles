plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.temidummyapp"

    // 최신 SDK로 빌드하되, 타깃은 23 (Marshmallow)
    //noinspection GradleDependency
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.temidummyapp"
        minSdk = 23
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 23
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // local.properties에서 API 키 읽어오기
        val properties = java.util.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        
        val openaiApiKey = properties.getProperty("OPENAI_API_KEY") ?: "YOUR_API_KEY"
        val picovoiceAccessKey = properties.getProperty("PICOVOICE_ACCESS_KEY") ?: "YOUR_PICOVOICE_KEY"
        val adminPin = properties.getProperty("ADMIN_PIN") ?: "1234"
        
        buildConfigField("String", "OPENAI_API_KEY", "\"$openaiApiKey\"")
        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"$picovoiceAccessKey\"")
        buildConfigField("String", "ADMIN_PIN", "\"$adminPin\"")
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

    // Java 8로 낮춰 안정적으로 (Marshmallow 호환)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    // BuildConfig 생성 활성화
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)

    // ConstraintLayout 사용 중이므로 반드시 추가
    implementation(libs.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Temi SDK 추가
    implementation(libs.temi.sdk)

    implementation(libs.cardview)

    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // QR code generation
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")

    // Porcupine Wake Word SDK
    implementation("ai.picovoice:porcupine-android:3.0.0")
}