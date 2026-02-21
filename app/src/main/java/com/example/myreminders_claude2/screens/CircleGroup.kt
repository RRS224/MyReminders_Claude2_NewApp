package com.example.myreminders_claude2.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CircleGroup(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val memberUids: List<String> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("memberUids", JSONArray(memberUids))
    }

    companion object {
        fun fromJson(obj: JSONObject) = CircleGroup(
            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
            name = obj.optString("name", ""),
            memberUids = (0 until (obj.optJSONArray("memberUids")?.length() ?: 0))
                .map { obj.getJSONArray("memberUids").getString(it) }
        )
    }
}

object CircleGroupManager {
    private const val PREFS_KEY = "circle_groups"
    private const val PREFS_NAME = "my_circle_prefs"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGroups(context: Context): List<CircleGroup> {
        val json = getPrefs(context).getString(PREFS_KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { CircleGroup.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveGroup(context: Context, group: CircleGroup) {
        val groups = getGroups(context).toMutableList()
        val index = groups.indexOfFirst { it.id == group.id }
        if (index >= 0) groups[index] = group else groups.add(group)
        val arr = JSONArray(groups.map { it.toJson() })
        getPrefs(context).edit().putString(PREFS_KEY, arr.toString()).apply()
    }

    fun deleteGroup(context: Context, groupId: String) {
        val groups = getGroups(context).filter { it.id != groupId }
        val arr = JSONArray(groups.map { it.toJson() })
        getPrefs(context).edit().putString(PREFS_KEY, arr.toString()).apply()
    }
}
