
import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.googleServices)
}

android {
    namespace = "yiwoo.prototype.gabobell"
    compileSdk = 34

    val properties = Properties()
    properties.load(FileInputStream(rootProject.file("local.properties")))

    val nativeAppKey = properties.getProperty("KAKAO_NATIVE_APP_KEY_NONE_DOUBLE_QUOTES") ?: ""

    defaultConfig {
        applicationId = "yiwoo.prototype.gabobell"
        minSdk = 23
        targetSdk = 34
        versionCode = 12
        versionName = "0.0.12"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SEARCH_REST_API_KEY",
            properties.getProperty("SEARCH_REST_API_KEY")
        )
        buildConfigField(
            "String",
            "KAKAO_NATIVE_APP_KEY",
            properties.getProperty("KAKAO_NATIVE_APP_KEY")
        )
        buildConfigField("Boolean", "DEBUG_MODE", "false")
        manifestPlaceholders["NATIVE_APP_KEY"] = nativeAppKey
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            /**
             * - pplicationVariants.configureEach는 기본적으로 모든 빌드 변형에 대해 작동
             *   그래서 release 내부에서도 구현이 가능
             * - buildType.name을 사용하여 빌드 타입에 따라 파일명을 설정하고자 할 때,
             *   빌드 프로세스가 debug 빌드를 기준으로 실행될 경우, 이 값은 debug로 설정
             */
            applicationVariants.configureEach {
                outputs.configureEach {
                    val project = "ansimi"
                    // 날짜 포맷 설정
                    val date = Date()
                    val formattedDate = SimpleDateFormat("yyMMdd_HHmm").format(date)
                    (this as? ApkVariantOutputImpl)?.outputFileName =
                        "${project}_${"v$versionName"}_${buildType.name}_${formattedDate}.apk"
                }
            }
        }
//        debug {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//            applicationVariants.configureEach {
//                outputs.configureEach {
//                    val project = "ansimi"
//                    // 날짜 포맷 설정
//                    val date = Date()
//                    val formattedDate = SimpleDateFormat("yyMMdd_HHmm").format(date)
//                    (this as? ApkVariantOutputImpl)?.outputFileName =
//                        "${project}_${"v$versionName"}_${buildType.name}_${formattedDate}.apk"
//                }
//            }
//        }
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
        buildConfig = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":retrofit")) // retrofit 대한 의존성

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")

//location
    implementation("com.google.android.gms:play-services-location:21.3.0")

//retrofit2
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.2")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

//cameraX
    implementation("androidx.camera:camera-core:1.1.0-beta01")
    implementation("androidx.camera:camera-camera2:1.1.0-beta01")
    implementation("androidx.camera:camera-lifecycle:1.1.0-beta01")
    implementation("androidx.camera:camera-video:1.1.0-beta01")

    implementation("androidx.camera:camera-view:1.1.0-beta01")
    implementation("androidx.camera:camera-extensions:1.1.0-beta01")

//encryptedSharedPreferences
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha04")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

//kakao
    implementation("com.kakao.sdk:v2-all:2.20.0")

    implementation("com.kakao.maps.open:android:2.12.8")

//Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-messaging:24.1.0")


}