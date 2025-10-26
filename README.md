<<<<<<< HEAD
# NT118.Q11
=======
<h1 align="center">🌱 Habit Tracker App</h1>
<h3 align="center">NT118.Q11 — Ứng dụng theo dõi thói quen hằng ngày</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Android%20Studio-Java-green?style=flat-square">
  <img src="https://img.shields.io/badge/Firebase-Cloud%20Firestore-orange?style=flat-square">
  <img src="https://img.shields.io/badge/MVVM-Architecture-blue?style=flat-square">
  <img src="https://img.shields.io/badge/Version-1.0.0-lightgrey?style=flat-square">
</p>

---

## 🧭 Giới thiệu

**Habit Tracker App** là ứng dụng giúp người dùng **xây dựng, duy trì và phân tích thói quen hằng ngày**.  
Được phát triển trong khuôn khổ môn học **NT118 - Phát triển ứng dụng di động**, nhóm **Q11**.

Ứng dụng tích hợp với **Firebase** để:
- Đăng ký / Đăng nhập bằng email
- Lưu trữ và đồng bộ dữ liệu người dùng
- Gửi thông báo và thống kê tiến trình thói quen

---

## 🧱 Cấu trúc thư mục (Code Architecture)

```plaintext
com.example.habittracker
│
├── data                     // --- LỚP DATA (MODEL) ---
│   │
│   ├── model                // Các đối tượng dữ liệu (POJO/Data Class)
│   │   ├── User.java        // (Lưu thông tin người dùng: email, tên...)
│   │   ├── Habit.java       // (Lưu thông tin thói quen: tên, icon, màu, tần suất...)
│   │   ├── HabitEntry.java  // (Lưu 1 lần thực hiện thói quen: habitId, ngày, trạng thái...)
│   │   └── Achievement.java // (Lưu thông tin thành tựu: tên, điều kiện, icon...)
│   │
│   ├── repository           // Nơi quản lý dữ liệu (Single Source of Truth)
│   │   ├── AuthRepository.java      // (Quản lý logic Đăng nhập/Đăng ký/Đăng xuất)
│   │   ├── HabitRepository.java     // (Quản lý logic CRUD thói quen, theo dõi tiến độ)
│   │   └── UserRepository.java      // (Quản lý logic hồ sơ người dùng, thành tựu, cài đặt)
│   │
│   └── remote               // Nguồn dữ liệu từ xa (Firebase)
│       ├── FirebaseAuthSource.java  // (Chỉ gọi các hàm của Firebase Authentication)
│       └── FirestoreSource.java   // (Chỉ gọi các hàm của Cloud Firestore)
│
├── ui                       // --- LỚP UI (VIEW & VIEWMODEL) ---
│   │
│   ├── main                 // Chứa Activity chính
│   │   └── MainActivity.java  // (Activity chính chứa Bottom Navigation hoặc NavHost)
│   │
│   ├── auth                 // Chức năng: Xác thực
│   │   ├── LoginFragment.java
│   │   ├── RegisterFragment.java
│   │   └── AuthViewModel.java
│   │
│   ├── home                 // Chức năng: Trang chủ (Danh sách thói quen hôm nay)
│   │   ├── HomeFragment.java
│   │   ├── HomeViewModel.java
│   │   └── HabitAdapter.java    // (Adapter cho RecyclerView hiển thị danh sách thói quen)
│   │
│   ├── habit                // Chức năng: CRUD và Theo dõi chi tiết
│   │   ├── AddEditHabitFragment.java  // (Fragment để Thêm & Sửa thói quen)
│   │   ├── HabitDetailsFragment.java  // (Xem chi tiết 1 thói quen, lịch sử, calendar)
│   │   └── HabitViewModel.java        // (Dùng chung cho cả 2 fragment trên)
│   │
│   ├── analytics            // Chức năng: Thống kê
│   │   ├── AnalyticsFragment.java
│   │   └── AnalyticsViewModel.java
│   │
│   ├── achievements         // Chức năng: Thành tựu
│   │   ├── AchievementsFragment.java
│   │   ├── AchievementsViewModel.java
│   │   └── AchievementAdapter.java  // (Adapter cho RecyclerView hiển thị thành tựu)
│   │
│   ├── chatbot              // Chức năng: Chatbot AI
│   │   ├── ChatbotFragment.java
│   │   ├── ChatbotViewModel.java
│   │   └── ChatMessageAdapter.java  // (Adapter cho RecyclerView hiển thị tin nhắn)
│   │
│   └── settings             // Chức năng: Cài đặt & Hồ sơ
│       ├── SettingsFragment.java
│       ├── ProfileFragment.java
│       └── SettingsViewModel.java
│
├── service                  // --- DỊCH VỤ NỀN ---
│   └── HabitNotificationService.java // (Xử lý nhận thông báo từ Firebase Cloud Messaging)
│
└── utils                    // --- TIỆN ÍCH ---
    ├── Constants.java         // (Lưu các hằng số: tên collection, key...)
    ├── DateHelper.java        // (Các hàm tiện ích xử lý Ngày/Giờ)
    └── ViewModelFactory.java  // (Rất quan trọng để tạo ra các ViewModel)

```

---

## 🔥 Tính năng nổi bật (Key Features)

* ✅ **Xác thực người dùng:** Đăng ký, đăng nhập (Email/Password, Google).
* ✅ **Quản lý thói quen (CRUD):** Tạo, sửa, xóa và theo dõi thói quen hằng ngày.
* ✅ **Thống kê tiến độ:** Phân tích và trực quan hóa dữ liệu (biểu đồ, lịch).
* ✅ **Hệ thống thành tựu:** Ghi nhận và trao thưởng cho các cột mốc.
* ✅ **Thông báo nhắc nhở:** Giúp người dùng duy trì thói quen (sử dụng FCM).
* ✅ **Giao diện hiện đại:** Dễ sử dụng, thiết kế theo Material Design.
* ✅ **Trợ lý AI:** Tích hợp Chatbot để hỗ trợ và tạo động lực.

---

## 🧠 Công nghệ sử dụng (Tech Stack)

| Thành phần | Công nghệ |
| :--- | :--- |
| Ngôn ngữ | Java |
| Framework | Android SDK |
| Kiến trúc | MVVM (Model-View-ViewModel) |
| Database | Firebase Firestore |
| Authentication | Firebase Authentication |
| Notification | Firebase Cloud Messaging |
| UI | Figma + XML Layout |

---

## 📜 Quy tắc làm việc nhóm (Git Workflow)

Để đảm bảo chất lượng code và quy trình làm việc suôn sẻ, nhóm tuân thủ các quy tắc sau:

* **Branching:** Mỗi thành viên code trên nhánh riêng theo tên đã tạo.
* **Merging:** Khi hoàn thành, tạo **Pull Request (PR)** để merge vào nhánh `main`.
* **Deadline:** Hạn cuối tạo PR là **trước ngày họp nhóm 1 ngày** để có thời gian review.
* **Review:** Mọi PR cần ít nhất **1 review** (từ thành viên khác) trước khi được merge.
* **Protected Branch:** **Không commit** trực tiếp vào nhánh `main`.
>>>>>>> 39d5963 (Create README.md for Habit Tracker App)
