package com.gesturecontrol

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Persistent mapping from gesture name → accessibility action string.
 * Backed by SharedPreferences (JSON map). Changes survive process death.
 *
 * All known action constants live here so every screen/service uses one source of truth.
 */
object GestureActionMap {

    // ── Action constants ─────────────────────────────────────────────────────
    const val ACTION_NONE        = "NONE"
    const val ACTION_DOUBLE_TAP  = "DOUBLE_TAP"
    const val ACTION_SWIPE_UP    = "SWIPE_UP"
    const val ACTION_SWIPE_DOWN  = "SWIPE_DOWN"
    const val ACTION_VOLUME_UP   = "VOLUME_UP"
    const val ACTION_VOLUME_DOWN = "VOLUME_DOWN"
    const val ACTION_BACK        = "BACK"
    const val ACTION_HOME        = "HOME"
    const val ACTION_RECENTS     = "RECENTS"
    const val ACTION_SCROLL_UP   = "SCROLL_UP"
    const val ACTION_SCROLL_DOWN = "SCROLL_DOWN"

    /** Ordered list for UI dropdowns — keep NONE first. */
    val ALL_ACTIONS = listOf(
        ACTION_NONE,
        ACTION_DOUBLE_TAP,
        ACTION_SWIPE_UP,
        ACTION_SWIPE_DOWN,
        ACTION_VOLUME_UP,
        ACTION_VOLUME_DOWN,
        ACTION_BACK,
        ACTION_HOME,
        ACTION_RECENTS,
        ACTION_SCROLL_UP,
        ACTION_SCROLL_DOWN
    )

    /** Human-readable display label for each action. */
    fun actionLabel(action: String): String = when (action) {
        ACTION_NONE        -> "No Action"
        ACTION_DOUBLE_TAP  -> "Double Tap"
        ACTION_SWIPE_UP    -> "Swipe Up"
        ACTION_SWIPE_DOWN  -> "Swipe Down"
        ACTION_VOLUME_UP   -> "Volume Up"
        ACTION_VOLUME_DOWN -> "Volume Down"
        ACTION_BACK        -> "Back"
        ACTION_HOME        -> "Home"
        ACTION_RECENTS     -> "Recent Apps"
        ACTION_SCROLL_UP   -> "Scroll Up"
        ACTION_SCROLL_DOWN -> "Scroll Down"
        else               -> action
    }

    // ── Emoji for actions in UI ───────────────────────────────────────────────
    fun actionEmoji(action: String): String = when (action) {
        ACTION_NONE        -> "🚫"
        ACTION_DOUBLE_TAP  -> "👆"
        ACTION_SWIPE_UP    -> "⬆️"
        ACTION_SWIPE_DOWN  -> "⬇️"
        ACTION_VOLUME_UP   -> "🔊"
        ACTION_VOLUME_DOWN -> "🔈"
        ACTION_BACK        -> "◀️"
        ACTION_HOME        -> "🏠"
        ACTION_RECENTS     -> "⬛"
        ACTION_SCROLL_UP   -> "📜↑"
        ACTION_SCROLL_DOWN -> "📜↓"
        else               -> "❓"
    }

    // ── SharedPreferences key ────────────────────────────────────────────────
    private const val PREFS_NAME = "gesture_action_map"
    private const val KEY_MAP    = "map_json"
    private var prefs: SharedPreferences? = null

    /** Must be called once (e.g. in Application or MainActivity) before any get/set. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Write defaults only if nothing stored yet
        if (!prefs!!.contains(KEY_MAP)) {
            val defaults = mutableMapOf(
                "Thumb_Up"    to ACTION_DOUBLE_TAP,
                "Pointing_Up" to ACTION_SWIPE_UP
            )
            persist(defaults)
        }
    }

    /** Returns a snapshot copy of the entire gesture→action map. */
    fun getAll(): Map<String, String> {
        val json = prefs?.getString(KEY_MAP, "{}") ?: "{}"
        val obj = JSONObject(json)
        val result = mutableMapOf<String, String>()
        obj.keys().forEach { key -> result[key] = obj.getString(key) }
        return result
    }

    /** Returns the action for [gestureName], or [ACTION_NONE] if unmapped. */
    fun getAction(gestureName: String): String =
        getAll()[gestureName] ?: ACTION_NONE

    /** Persists the mapping for [gestureName]. Pass [ACTION_NONE] to clear. */
    fun set(gestureName: String, action: String) {
        val map = getAll().toMutableMap()
        if (action == ACTION_NONE) map.remove(gestureName)
        else map[gestureName] = action
        persist(map)
    }

    private fun persist(map: Map<String, String>) {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        prefs?.edit()?.putString(KEY_MAP, obj.toString())?.apply()
    }
}
