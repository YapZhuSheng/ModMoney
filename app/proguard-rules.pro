# Room generates implementations reflectively referenced at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# Keep enum valueOf/values used by Room type converters.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
