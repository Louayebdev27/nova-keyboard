package app.novakeys.data

import android.content.Context

/**
 * Emoji grouped by category. The set uses only widely supported emoji so it renders on every
 * Android version from 7 up. Extend the strings below to add more.
 */
object EmojiData {
    data class Category(val id: String, val icon: String, val emojis: List<String>)

    private fun split(s: String): List<String> {
        val out = ArrayList<String>()
        s.split(' ', '\n').filter { it.isNotBlank() }.forEach { out += it }
        return out
    }

    val categories: List<Category> = listOf(
        Category("smileys", "😀", split(
            "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🥳 😏 😒 😞 😔 😟 😕 🙁 ☹️ 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩 👻 💀 ☠️ 👽 👾 🤖")),
        Category("people", "👋", split(
            "👋 🤚 🖐️ ✋ 🖖 👌 🤏 ✌️ 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✍️ 💅 🤳 💪 🦾 🦵 🦶 👂 👃 🧠 👀 👁️ 👅 👄 👶 🧒 👦 👧 🧑 👨 👩 🧓 👴 👵 🙍 🙎 🙅 🙆 💁 🙋 🙇 🤦 🤷 👮 🕵️ 💂 👷 🤴 👸 🧙 🧚 🧛 🧜 🧝 🦸 🦹 🎅 🤶")),
        Category("hearts", "❤️", split(
            "❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ♥️ 💋 💌 💐 🌹 🥀 🌷 🌸 🌺 🌻 🌼 ✨ ⭐ 🌟 💫 🔥 💥 💯 ✅ ❌ ❓ ❗ ‼️ ⚡ 🎉 🎊 🎁 🏆 🥇 🥈 🥉")),
        Category("animals", "🐶", split(
            "🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🦂 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐅 🐆 🦓 🦍 🐘 🦏 🐪 🐫 🦒 🦘 🐃 🐂 🐄 🐎 🐖 🐏 🐑 🐐 🦌 🐕 🐩 🐈 🐓 🦃 🕊️ 🐇 🐁 🐀 🐿️ 🦔 🌵 🎄 🌲 🌳 🌴 🍀 🍁 🍂 🍃")),
        Category("food", "🍔", split(
            "🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🥭 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥒 🌶️ 🌽 🥕 🥔 🍠 🥐 🍞 🥖 🧀 🥚 🍳 🥞 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍘 🍥 🥠 🍢 🍡 🍧 🍨 🍦 🥧 🧁 🍰 🎂 🍮 🍭 🍬 🍫 🍿 🍩 🍪 🌰 🥜 🍯 🥛 ☕ 🍵 🥤 🍶 🍺 🍻 🥂 🍷 🥃 🍸 🍹 🍾")),
        Category("activity", "⚽", split(
            "⚽ 🏀 🏈 ⚾ 🥎 🎾 🏐 🏉 🎱 🏓 🏸 🥅 🏒 🏑 🏏 ⛳ 🏹 🎣 🥊 🥋 ⛸️ 🎿 ⛷️ 🏂 🏋️ 🤸 ⛹️ 🤺 🤾 🏌️ 🏇 🧘 🏄 🏊 🤽 🚣 🧗 🚵 🚴 🎮 🕹️ 🎲 🧩 ♟️ 🎯 🎳 🎰 🎨 🎬 🎤 🎧 🎼 🎹 🥁 🎷 🎺 🎸 🎻")),
        Category("travel", "🚗", split(
            "🚗 🚕 🚙 🚌 🚎 🏎️ 🚓 🚑 🚒 🚐 🚚 🚛 🚜 🛵 🏍️ 🚲 🛴 🚂 🚆 🚇 🚊 ✈️ 🛫 🛬 🚀 🛸 🚁 ⛵ 🚤 🛳️ ⚓ 🏠 🏡 🏢 🏥 🏦 🏨 🏪 🏫 🏰 🕌 🕍 ⛪ 🗼 🗽 🗺️ 🏔️ ⛰️ 🌋 🏖️ 🏝️ 🏜️ 🌅 🌄 🌃 🌉 🌈 ☀️ 🌤️ ⛅ ☁️ 🌧️ ⛈️ ❄️ ☃️ 🌙 🌍 🌎 🌏")),
        Category("objects", "💡", split(
            "⌚ 📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💽 💾 📷 📹 🎥 📞 ☎️ 📺 📻 ⏰ ⏳ 🔋 🔌 💡 🔦 🕯️ 💸 💵 💳 💎 🔧 🔨 ⚙️ 🔩 🧲 💣 🔪 🔑 🗝️ 🚪 🛏️ 🛋️ 🚿 🛁 🧴 🧻 🧼 🧽 🛒 📦 📫 ✉️ 📝 📁 📅 📌 📎 ✂️ 📏 🔒 🔓 🔍 📚 📖 🎓 💼 🌐 🔔 🔕 🎵 🎶")),
    )

    private const val RECENT_MAX = 32

    fun recents(context: Context): List<String> =
        (context.getSharedPreferences("nova_emoji", Context.MODE_PRIVATE).getString("recent", "") ?: "")
            .split('|').filter { it.isNotEmpty() }

    fun pushRecent(context: Context, emoji: String) {
        val sp = context.getSharedPreferences("nova_emoji", Context.MODE_PRIVATE)
        val list = (recents(context) - emoji).toMutableList().also { it.add(0, emoji) }.take(RECENT_MAX)
        sp.edit().putString("recent", list.joinToString("|")).apply()
    }
}
