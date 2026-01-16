package com.example.habittracker.data.achievements;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lưu trữ cục bộ cho thành tựu (thuần Java), an toàn khi truy cập đồng thời (synchronized).
 *
 * Lưu ý: File này dùng SharedPreferences + apply() (bất đồng bộ) để đảm bảo ghi an toàn
 * mà vẫn giữ dự án thuần Java.
 */
public class AchievementsLocalStore {

    // Tên file SharedPreferences dùng để lưu thành tựu local
    private static final String PREFS = "achievements_local";

    // Cửa sổ khử trùng lặp cho sự kiện "mở app" để tránh làm sai điều kiện "Welcome Back"
    // khi cùng một lần mở app bị gọi recordOpen 2 lần (ví dụ: lifecycle + auth listener).
    // 2 giây đủ để bao phủ các callback trùng nhưng không ảnh hưởng tới lần mở thật sự.
    private static final long OPEN_DEDUP_WINDOW_MS = 2_000L;

    public static String safeUser(String userIdOrNull) {
        String v = userIdOrNull == null ? "anon" : userIdOrNull.trim();
        return v.isEmpty() ? "anon" : v;
    }

    private static String keyUnlocked(String uid) { return "unlocked_ids_" + uid; }
    private static String keyUnlockedAt(String uid, AchievementId id) { return "unlocked_at_" + uid + "_" + id.name(); }
    private static String keyHabitsCreated(String uid) { return "habits_created_" + uid; }
    private static String keyPerfectDays(String uid) { return "perfect_days_" + uid; }
    private static String keyLastOpen(String uid) { return "last_open_" + uid; }

    private final SharedPreferences sp;
    private final String uid;

    public AchievementsLocalStore(@NonNull Context context, String userIdOrNull) {
        // Sử dụng application context để tránh rò rỉ memory leak.
        // getSharedPreference có 2 chế độ: MODE_PRIVATE (mặc định) và MODE_MULTI_PROCESS (ít dùng).
        // Ta dùng MODE_PRIVATE để đảm bảo chỉ có tiến trình hiện tại truy cập.
        // Còn lại MODE_MULTI_PROCESS dùng khi có nhiều tiến trình cùng truy cập một file prefs,
        // nhưng nó không đảm bảo đồng bộ tốt giữa các tiến trình.
        Context app = context.getApplicationContext();
        this.sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.uid = safeUser(userIdOrNull);
    }

    public synchronized Set<String> getUnlockedIds() {
        // Lấy set các ID thành tựu đã mở khóa.
        // return về một bản sao HashSet để tránh việc sửa trực tiếp trên set trả về.
        // Nếu không có thành tựu nào, trả về empty set.
        Set<String> s = sp.getStringSet(keyUnlocked(uid), null);
        if (s == null) return Collections.emptySet();
        return new HashSet<>(s);
    }

    public synchronized Map<String, Long> getUnlockedAtMap() {
        // Lấy map các ID thành tựu đã mở khóa kèm timestamp mở khóa.
        // Chỉ bao gồm các thành tựu đã mở khóa.
        // Nếu ts <= 0 thì xem như chưa mở khóa.
        Map<String, Long> map = new HashMap<>();
        for (AchievementDefinition def : AchievementCatalog.all()) {
            long ts = sp.getLong(keyUnlockedAt(uid, def.getId()), -1L);
            if (ts > 0) map.put(def.getId().name(), ts);
        }
        return map;
    }

    public synchronized void unlock(AchievementId id, long unlockedAtMillis) {
        // Luôn copy sang set có thể sửa (getUnlockedIds() có thể trả về Collections.emptySet()).
        // Đây là vì SharedPreferences không cho phép sửa trực tiếp set trả về,
        // và cũng không theo dõi các thay đổi trên set đó.
        // Vậy nên ta phải tạo một bản sao mới để sửa đổi.
        Set<String> unlocked = new HashSet<>(getUnlockedIds());
        if (unlocked.contains(id.name())) return;

        unlocked.add(id.name());

        // Quan trọng: luôn ghi một instance Set mới vào SharedPreferences.
        // Vì SharedPreferences không theo dõi các thay đổi trên set đã trả về,
        // nên nếu ta sửa set đó rồi ghi lại cùng một instance, nó sẽ không được lưu.
        // Do đó, ta phải tạo một instance mới (dùng HashSet) để đảm bảo nó được lưu đúng.
        sp.edit()
                .putStringSet(keyUnlocked(uid), new HashSet<>(unlocked))
                .putLong(keyUnlockedAt(uid, id), unlockedAtMillis)
                .apply();
    }

    public synchronized int incrementHabitsCreated() {
        // Lấy giá trị hiện tại, tăng lên 1, lưu lại và trả về.
        int current = sp.getInt(keyHabitsCreated(uid), 0);
        int next = current + 1;
        sp.edit().putInt(keyHabitsCreated(uid), next).apply();
        return next;
    }

    public synchronized int getHabitsCreated() {
        return sp.getInt(keyHabitsCreated(uid), 0);
    }

    public synchronized Set<String> getPerfectDays() {
        // Lấy set các ngày đã hoàn thành toàn bộ các thói quen.
        Set<String> s = sp.getStringSet(keyPerfectDays(uid), null);
        if (s == null) return Collections.emptySet();
        return new HashSet<>(s);
    }

    public synchronized int addPerfectDayKey(String dayKey) {
        // Luôn copy sang set có thể sửa (getPerfectDays() có thể trả về Collections.emptySet()).
        // Đây là vì SharedPreferences không cho phép sửa trực tiếp set trả về,
        // và cũng không theo dõi các thay đổi trên set đó.
        // Vậy nên ta phải tạo một bản sao mới để sửa đổi.
        Set<String> days = new HashSet<>(getPerfectDays());
        days.add(dayKey);
        sp.edit().putStringSet(keyPerfectDays(uid), new HashSet<>(days)).apply();
        return days.size();
    }

    /**
     * Trả về true nếu lần mở app này đủ điều kiện "welcome back".
     */
    public synchronized boolean recordOpenAndCheckWelcomeBack(long nowMillis) {
        long last = sp.getLong(keyLastOpen(uid), -1L);

        // Nếu bị gọi 2 lần trong thời gian rất ngắn, xem như cùng một sự kiện "mở app".
        // Quan trọng: KHÔNG ghi đè last_open trong trường hợp này,
        // nếu không lần gọi thứ 2 sẽ xoá timestamp cũ và làm mất khả năng phát hiện "Welcome Back".
        // Lý do mà cả lifecycle và auth listener đều gọi recordOpen:
        // - Lifecycle: khi app foreground - tức là user đã mở app nhưng có thể chưa đăng nhập (nếu dùng persistent login).
        // - Auth listener: khi user đăng nhập (có thể ngay sau khi app foreground nếu dùng persistent login).
        // Việc này giúp tránh việc "Welcome Back" bị bỏ lỡ do 2 lần gọi gần nhau.
        // Ví dụ: User mở app, auth listener gọi trước, sau đó lifecycle gọi sau trong vòng 1 giây.
        // Nếu không có cơ chế này, lần gọi thứ 2 sẽ ghi đè timestamp lần mở app đầu tiên,
        // làm mất khả năng phát hiện "Welcome Back".
        // Vậy tại sao không chỉ dùng lần gọi đầu tiên?
        // Vì ta không kiểm soát được thứ tự gọi giữa lifecycle và auth listener.
        // Do đó, ta cần một cơ chế để cả hai lần gọi trong thời gian ngắn đều được xem như cùng một sự kiện mở app.
        if (last > 0 && (nowMillis - last) >= 0 && (nowMillis - last) <= OPEN_DEDUP_WINDOW_MS) {
            return false;
        }

        sp.edit().putLong(keyLastOpen(uid), nowMillis).apply();
        if (last <= 0) return false;
        return (nowMillis - last) >= 24L * 60L * 60L * 1000L;
    }

    SharedPreferences getSharedPreferences() {
        return sp;
    }

    String getUserScopedPrefix() {
        return uid;
    }
}
