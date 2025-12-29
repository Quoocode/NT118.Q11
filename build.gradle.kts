// File build Gradle cấp cao nhất: nơi khai báo các cấu hình dùng chung cho toàn bộ module/dự án.
plugins {
    alias(libs.plugins.android.application) apply false

    alias(libs.plugins.google.gms.google.services) apply false
    alias(libs.plugins.kotlin.android) apply false

}
