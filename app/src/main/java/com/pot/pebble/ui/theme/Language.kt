package com.pot.pebble.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 定义支持的语言
enum class Lang { CN, EN }

// 全局语言状态管理
object LanguageManager {
    // 默认使用中文 (CN)
    var currentLang by mutableStateOf(Lang.CN)

    fun toggle() {
        currentLang = if (currentLang == Lang.CN) Lang.EN else Lang.CN
    }

    // 获取当前对应的字符串对象
    val s: Strings get() = if (currentLang == Lang.CN) StringsCN else StringsEN
}

// 字符串接口 (保证中英文 Key 一致)
interface Strings {
    val appName: String
    val listTitle: String
    val activeCountSuffix: String
    val searchHint: String
    val noAppsFound: String
    val groupActive: String
    val groupIdle: String
    val tagBlocked: String

    val toastOverlay: String
    val toastUsage: String
    val toastBattery: String
    val toastAutoStart: String
    val toastAutoStartFail: String

    val focusTimerTitle: String
    val statStonesDropped: String
    val btnStopFocus: String
    val btnStartFocus: String

    val getStarted: String
    val guideTitle: String
    val guideDesc: String
    val permRequired: String
    val permOverlay: String
    val permOverlayDesc: String
    val permUsage: String
    val permUsageDesc: String
    val permRecommended: String
    val permBattery: String
    val permBatteryDesc: String
    val permAutoStart: String
    val permAutoStartDesc: String
    val settingsLanguage: String

    val settingsTitle: String
    val themeTitle: String
    val themeRock: String
    val themeTetris: String
    val themeDanmaku: String
    val themeCrack: String

    val themeCustom: String
    val btnSelectImage: String

    val settingsSystemTitle: String
    val settingsPermissionTitle: String
    val settingsPermissionDesc: String

    val dialogDanmakuTitle: String
    val dialogDanmakuHint: String
    val dialogBtnDone: String
    val dialogBtnAdd: String

    val statTitle: String
    val statBlockCount: String
    val statSavedTime: String
    val statWeekTrend: String
    val emptyData: String
}

// 中文文案
object StringsCN : Strings {
    override val appName = "Pebble"
    override val listTitle = "Pebble 清单"
    override val activeCountSuffix = "个应用受控"
    override val searchHint = "搜索应用..."
    override val noAppsFound = "未找到相关应用"
    override val groupActive = "生效区"
    override val groupIdle = "待选区"
    override val tagBlocked = "受控中"

    override val toastOverlay = "请在列表中找到 Pebble 并开启【显示在其他应用上层】"
    override val toastUsage = "请在列表中找到 Pebble 并开启【使用情况访问】"
    override val toastBattery = "请选择【无限制】或允许后台活动"
    override val toastAutoStart = "请开启【自启动】权限"
    override val toastAutoStartFail = "无法自动跳转，请手动在设置中查找"

    override val focusTimerTitle = "专注时长"
    override val statStonesDropped = "落石触发"
    override val btnStopFocus = "停止专注"
    override val btnStartFocus = "启动专注"

    override val getStarted = "开始专注"
    override val guideTitle = "设置 Pebble"
    override val guideDesc = "请授予以下权限以激活物理引擎与监测功能。"
    override val permRequired = "必要权限"
    override val permOverlay = "悬浮窗权限"
    override val permOverlayDesc = "用于显示物理落石干扰。"
    override val permUsage = "使用情况访问"
    override val permUsageDesc = "用于精准识别前台应用。"
    override val permRecommended = "后台保活 (推荐)"
    override val permBattery = "忽略电池优化"
    override val permBatteryDesc = "防止后台服务被冻结。"
    override val permAutoStart = "自启动管理"
    override val permAutoStartDesc = "请手动前往设置开启。"
    override val settingsLanguage = "多语言 / Language"

    override val settingsTitle = "设置"
    override val themeTitle = "干扰模式"
    override val themeRock = "物理落石 (默认)"
    override val themeTetris = "俄罗斯方块"
    override val themeDanmaku = "文字弹幕"
    override val themeCrack = "屏幕裂纹"

    override val themeCustom = "自定义贴图"
    override val btnSelectImage = "选择图片"

    override val settingsSystemTitle = "系统设置"
    override val settingsPermissionTitle = "权限与引导"
    override val settingsPermissionDesc = "检查权限状态或重新运行向导"

    override val dialogDanmakuTitle = "编辑弹幕语录"
    override val dialogDanmakuHint = "输入打击你的话..."
    override val dialogBtnDone = "完成"
    override val dialogBtnAdd = "添加"

    override val statTitle = "今日战绩"
    override val statBlockCount = "次拦截"
    override val statSavedTime = "分钟专注"
    override val statWeekTrend = "近7日趋势"
    override val emptyData = "暂无数据"
}

// 英文文案
object StringsEN : Strings {
    override val appName = "Pebble"
    override val listTitle = "Pebble List"
    override val activeCountSuffix = "active stones"
    override val searchHint = "Search apps..."
    override val noAppsFound = "No apps found"
    override val groupActive = "Active Zone"
    override val groupIdle = "Idle Apps"
    override val tagBlocked = "BLOCKED"

    override val toastOverlay = "Please find Pebble and allow 'Display over other apps'"
    override val toastUsage = "Please find Pebble and allow 'Usage Access'"
    override val toastBattery = "Please select 'Unrestricted' or allow background activity"
    override val toastAutoStart = "Please enable 'Auto-Start' permission"
    override val toastAutoStartFail = "Unable to open settings manually"


    override val focusTimerTitle = "FOCUS TIME"
    override val statStonesDropped = "Stones Dropped"
    override val btnStopFocus = "Stop Session"
    override val btnStartFocus = "Start Focus"

    override val getStarted = "Get Started"
    override val guideTitle = "Setup Pebble"
    override val guideDesc = "Please grant permissions to enable the physics engine."
    override val permRequired = "Required"
    override val permOverlay = "Display over other apps"
    override val permOverlayDesc = "To render falling stones."
    override val permUsage = "Usage Access"
    override val permUsageDesc = "To detect foreground apps."
    override val permRecommended = "Recommended"
    override val permBattery = "Ignore Battery Opt"
    override val permBatteryDesc = "Prevent background freezing."
    override val permAutoStart = "Auto-Start"
    override val permAutoStartDesc = "Enable manually in Settings."
    override val settingsLanguage = "Language"

    override val settingsTitle = "Settings"
    override val themeTitle = "Interference Mode"
    override val themeRock = "Falling Rocks (Default)"
    override val themeTetris = "Tetris Blocks"
    override val themeDanmaku = "Text Danmaku"
    override val themeCrack = "Broken Screen"

    override val themeCustom = "Custom Image"
    override val btnSelectImage = "Pick Image"

    override val settingsSystemTitle = "System"
    override val settingsPermissionTitle = "Permission & Setup"
    override val settingsPermissionDesc = "Check permissions or re-run guide"

    override val dialogDanmakuTitle = "Edit Danmaku"
    override val dialogDanmakuHint = "Enter text..."
    override val dialogBtnDone = "Done"
    override val dialogBtnAdd = "Add"

    override val statTitle = "Today's Stats"
    override val statBlockCount = "Interventions"
    override val statSavedTime = "Mins Focused"
    override val statWeekTrend = "7-Day Trend"
    override val emptyData = "No data yet"
}