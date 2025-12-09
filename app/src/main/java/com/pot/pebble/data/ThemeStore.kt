package com.pot.pebble.data

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import com.pot.pebble.core.model.EntityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

object ThemeStore {
    private const val PREFS_NAME = "pebble_prefs"
    private const val KEY_THEME = "key_theme_mode"
    private const val KEY_DANMAKU = "key_custom_danmaku"
    private const val KEY_CUSTOM_IMG_CURRENT = "key_custom_img_current"
    private const val KEY_CUSTOM_IMG_HISTORY = "key_custom_img_history" // 🔥 新增：历史记录 Key

    private val _currentTheme = MutableStateFlow(EntityType.CIRCLE)
    val currentTheme = _currentTheme.asStateFlow()

    private val defaultDanmaku = setOf("放下手机!", "Focus!", "你在干嘛?", "别看了", "回去学习!", "自律给你自由")
    private val _danmakuList = MutableStateFlow(defaultDanmaku.toList())
    val danmakuList = _danmakuList.asStateFlow()

    // 当前选中的图片
    private val _customImageUri = MutableStateFlow<String?>(null)
    val customImageUri = _customImageUri.asStateFlow()

    // 图片历史记录
    private val _customHistory = MutableStateFlow<List<String>>(emptyList())
    val customHistory = _customHistory.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedType = prefs.getString(KEY_THEME, EntityType.CIRCLE.name) ?: EntityType.CIRCLE.name
        _currentTheme.value = try { EntityType.valueOf(savedType) } catch (e: Exception) { EntityType.CIRCLE }

        val savedDanmaku = prefs.getStringSet(KEY_DANMAKU, defaultDanmaku) ?: defaultDanmaku
        _danmakuList.value = savedDanmaku.toList()

        _customImageUri.value = prefs.getString(KEY_CUSTOM_IMG_CURRENT, null)

        // 加载历史记录
        val historySet = prefs.getStringSet(KEY_CUSTOM_IMG_HISTORY, emptySet()) ?: emptySet()
        _customHistory.value = historySet.toList().reversed() // 简单的倒序，让新的在前面
    }

    fun setTheme(context: Context, type: EntityType) {
        _currentTheme.value = type
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putString(KEY_THEME, type.name) }
    }

    fun saveDanmakuList(context: Context, list: List<String>) {
        _danmakuList.value = list
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { putStringSet(KEY_DANMAKU, list.toSet()) }
    }

    // 从历史记录中选中
    fun selectCustomImage(context: Context, uri: String) {
        _customImageUri.value = uri
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_CUSTOM_IMG_CURRENT, uri)
        }
    }

    // 删除历史记录
    fun deleteCustomImage(context: Context, uri: String) {
        // 从列表移除
        val newList = _customHistory.value - uri
        _customHistory.value = newList

        // 如果删除的是当前选中的，清空选中状态
        if (_customImageUri.value == uri) {
            _customImageUri.value = null
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                remove(KEY_CUSTOM_IMG_CURRENT)
            }
        }

        // 更新 SP
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putStringSet(KEY_CUSTOM_IMG_HISTORY, newList.toSet())
        }

        // 删除物理文件
        try {
            val file = File(Uri.parse(uri).path!!)
            if (file.exists()) file.delete()
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 保存新图片 (同时也加入历史记录)
    fun addCustomImage(context: Context, uri: Uri) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return
            // 使用时间戳防止文件名冲突
            val fileName = "custom_${System.currentTimeMillis()}.png"
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            val localUri = Uri.fromFile(file).toString()

            // 设置为当前
            _customImageUri.value = localUri

            // 加入历史记录
            val newHistory = _customHistory.value.toMutableList().apply {
                add(0, localUri) // 加到开头
            }
            _customHistory.value = newHistory

            // 保存
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(KEY_CUSTOM_IMG_CURRENT, localUri)
                putStringSet(KEY_CUSTOM_IMG_HISTORY, newHistory.toSet())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}