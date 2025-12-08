# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保护 Room 实体
-keep class com.pot.pebble.data.entity.** { *; }
-keep class com.pot.pebble.data.dao.** { *; }
-keep class com.pot.pebble.data.AppDatabase { *; }

# 保护 JBox2D 物理引擎
-keep class org.jbox2d.** { *; }

# 保护 UsageStats 相关
-keep class android.app.usage.** { *; }

# 保护协程和 Service 逻辑
-keep class com.pot.pebble.service.** { *; }
-keep class com.pot.pebble.monitor.** { *; }

# 保留所有枚举
-keepclassmembers enum * { *; }