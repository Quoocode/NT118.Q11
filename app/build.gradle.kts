plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.habittracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.habittracker"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Các thư viện giao diện có sẵn của bạn (Giữ nguyên)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // --- PHẦN SỬA ĐỔI FIREBASE (QUAN TRỌNG) ---

    // 1. Thêm Firebase BoM (Bill of Materials)
    // Nó đóng vai trò là "Trọng tài", quy định phiên bản chung cho cả nhóm Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

    // 2. Thêm Auth (KHÔNG CẦN version number nữa)
    // Lưu ý: Tạm thời comment dòng libs.firebase.auth lại để tránh xung đột
    // implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-auth")

    // 3. Thêm Firestore (KHÔNG CẦN version number)
    // Lưu ý: Google đã gộp KTX vào bản chính, nên dùng "firebase-firestore" thay vì "-ktx"
    implementation("com.google.firebase:firebase-firestore")

    // --- KẾT THÚC PHẦN FIREBASE ---

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}