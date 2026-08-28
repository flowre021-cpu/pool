# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Glance App Widget
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.example.pool.widget.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker

# Keep data models used by Room / JSON
-keep class com.example.pool.data.** { *; }

# WebView 教务导入（若启用 JS Bridge）
-keepclassmembers class com.example.pool.ui.course.importing.ScheduleImportBridge {
    @android.webkit.JavascriptInterface <methods>;
}
