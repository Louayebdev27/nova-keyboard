package app.novakeys.data

import android.content.Context
import android.content.SharedPreferences

/** All user settings in one place. Shared by the keyboard service and the settings app. */
class Prefs private constructor(context: Context) {
    val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("nova_settings", Context.MODE_PRIVATE)

    var activeTheme: String
        get() = sp.getString("activeTheme", "midnight") ?: "midnight"
        set(v) = sp.edit().putString("activeTheme", v).apply()

    /** Enabled language ids in switch order: en, fr, ar. */
    var languages: List<String>
        get() = (sp.getString("languages", "en,fr,ar") ?: "en").split(",").filter { it.isNotBlank() }
        set(v) = sp.edit().putString("languages", v.joinToString(",")).apply()

    var currentLanguage: String
        get() = sp.getString("currentLanguage", "en") ?: "en"
        set(v) = sp.edit().putString("currentLanguage", v).apply()

    var numberRow: Boolean
        get() = sp.getBoolean("numberRow", true)
        set(v) = sp.edit().putBoolean("numberRow", v).apply()

    /** Ids from [BottomKeys]. Space must be present. */
    var bottomRow: List<String>
        get() = (sp.getString("bottomRow", "symbols,emoji,comma,space,period,enter") ?: "").split(",")
            .filter { it.isNotBlank() }
        set(v) = sp.edit().putString("bottomRow", v.joinToString(",")).apply()

    var keyHeightScale: Float
        get() = sp.getFloat("keyHeightScale", 1f)
        set(v) = sp.edit().putFloat("keyHeightScale", v).apply()

    /** 0 = off, 1 = left hand, 2 = right hand. */
    var oneHanded: Int
        get() = sp.getInt("oneHanded", 0)
        set(v) = sp.edit().putInt("oneHanded", v).apply()

    var soundEnabled: Boolean
        get() = sp.getBoolean("soundEnabled", false)
        set(v) = sp.edit().putBoolean("soundEnabled", v).apply()

    /** pop, click, soft, chime */
    var soundPack: String
        get() = sp.getString("soundPack", "pop") ?: "pop"
        set(v) = sp.edit().putString("soundPack", v).apply()

    var soundVolume: Float
        get() = sp.getFloat("soundVolume", 0.5f)
        set(v) = sp.edit().putFloat("soundVolume", v).apply()

    var vibrationEnabled: Boolean
        get() = sp.getBoolean("vibrationEnabled", true)
        set(v) = sp.edit().putBoolean("vibrationEnabled", v).apply()

    var vibrationMs: Int
        get() = sp.getInt("vibrationMs", 12)
        set(v) = sp.edit().putInt("vibrationMs", v).apply()

    var keyPreview: Boolean
        get() = sp.getBoolean("keyPreview", true)
        set(v) = sp.edit().putBoolean("keyPreview", v).apply()

    var longPressMs: Int
        get() = sp.getInt("longPressMs", 350)
        set(v) = sp.edit().putInt("longPressMs", v).apply()

    var autoCapitalize: Boolean
        get() = sp.getBoolean("autoCapitalize", true)
        set(v) = sp.edit().putBoolean("autoCapitalize", v).apply()

    var doubleSpacePeriod: Boolean
        get() = sp.getBoolean("doubleSpacePeriod", true)
        set(v) = sp.edit().putBoolean("doubleSpacePeriod", v).apply()

    var suggestionsEnabled: Boolean
        get() = sp.getBoolean("suggestionsEnabled", true)
        set(v) = sp.edit().putBoolean("suggestionsEnabled", v).apply()

    var learnWords: Boolean
        get() = sp.getBoolean("learnWords", true)
        set(v) = sp.edit().putBoolean("learnWords", v).apply()

    var clipboardHistory: Boolean
        get() = sp.getBoolean("clipboardHistory", true)
        set(v) = sp.edit().putBoolean("clipboardHistory", v).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean("onboardingDone", false)
        set(v) = sp.edit().putBoolean("onboardingDone", v).apply()

    companion object {
        @Volatile private var instance: Prefs? = null
        fun get(context: Context): Prefs =
            instance ?: synchronized(this) { instance ?: Prefs(context).also { instance = it } }
    }
}
