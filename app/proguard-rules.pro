# Thêm các rule ProGuard/R8 riêng của dự án tại đây.
# Bạn có thể điều khiển tập file cấu hình được áp dụng thông qua
# thuộc tính proguardFiles trong build.gradle.
#
# Xem thêm:
#   http://developer.android.com/guide/developing/tools/proguard.html

# Nếu dự án dùng WebView với JavaScript, hãy bỏ comment phần dưới
# và chỉ định tên class đầy đủ (fully qualified) của JavaScript interface:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Bỏ comment phần này để giữ lại thông tin số dòng,
# phục vụ debug stack trace.
#-keepattributes SourceFile,LineNumberTable

# Nếu bạn giữ thông tin số dòng, hãy bỏ comment phần này để
# ẩn tên file nguồn gốc.
#-renamesourcefileattribute SourceFile