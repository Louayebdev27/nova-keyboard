package app.novakeys.ui

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaTheme {
                val nav = rememberNavController()
                val back: () -> Unit = { nav.popBackStack() }
                NavHost(nav, startDestination = "home") {
                    composable("home") { HomeScreen(resumeTick, onNavigate = { nav.navigate(it) }) }
                    composable("themes") { ThemeGalleryScreen(onBack = back, onEdit = { nav.navigate("designer/$it") }) }
                    composable("designer/{id}") { entry ->
                        ThemeDesignerScreen(entry.arguments?.getString("id") ?: "new", onBack = back)
                    }
                    composable("keys") { KeysScreen(onBack = back) }
                    composable("typing") { TypingScreen(onBack = back) }
                    composable("feedback") { FeedbackScreen(onBack = back) }
                    composable("shortcuts") { ShortcutsScreen(onBack = back) }
                    composable("dictionary") { DictionaryScreen(onBack = back) }
                    composable("stickers") { StickersScreen(onBack = back) }
                    composable("backup") { BackupScreen(onBack = back) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick++
    }

    companion object {
        fun isEnabled(ctx: Context): Boolean {
            val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            return imm.enabledInputMethodList.any { it.packageName == ctx.packageName }
        }

        fun isDefault(ctx: Context): Boolean {
            val id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
            return id.startsWith(ctx.packageName + "/")
        }
    }
}
