plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.dam_front"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dam_front"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")




    // Compose - Updated BOM for Stripe compatibility
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material:material-icons-extended")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Retrofit pour les appels API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // OkHttp pour la gestion des requêtes HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Gson pour la sérialisation JSON
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coroutines pour la programmation asynchrone
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // DataStore pour stocker le token JWT
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Lifecycle pour ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Socket.IO pour la communication temps réel
    implementation("io.socket:socket.io-client:2.0.1") {
        exclude(group = "org.json", module = "json")
    }
    
    // Coil pour le chargement d'images depuis URL
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-svg:2.6.0")
    
    // Accompanist System UI Controller pour gérer la status bar
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    
    // Debug tools for preview
    debugImplementation("androidx.compose.ui:ui-tooling")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")

    // Stripe
    implementation("com.stripe:stripe-android:21.5.0")

    // Jetpack Glance (Widgets)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // PDF Generation (iText7)
    implementation("com.itextpdf:itext7-core:7.2.5") {
        exclude(group = "org.json", module = "json")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
        exclude(group = "org.bouncycastle", module = "bcpkix-jdk15on")
    }
}
