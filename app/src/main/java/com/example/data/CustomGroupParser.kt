package com.example.data

import org.json.JSONArray
import org.json.JSONObject

object CustomGroupParser {
    val DEFAULT_GROUPS: Map<String, List<Int>> = mapOf(
        "Drums" to emptyList(),
        "Bass" to emptyList(),
        "Guitars" to emptyList(),
        "Keys" to emptyList(),
        "Vocals" to emptyList(),
        "Horns" to emptyList(),
        "FX" to emptyList()
    )

    fun parseGroups(jsonStr: String): Map<String, List<Int>> {
        if (jsonStr.isBlank() || jsonStr == "{}") {
            return DEFAULT_GROUPS
        }
        val map = mutableMapOf<String, List<Int>>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = json.optJSONArray(key)
                val list = mutableListOf<Int>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        list.add(arr.getInt(i))
                    }
                }
                map[key] = list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return if (map.isEmpty()) DEFAULT_GROUPS else map
    }

    fun toJson(map: Map<String, List<Int>>): String {
        val json = JSONObject()
        map.forEach { (name, chIds) ->
            val arr = JSONArray()
            chIds.forEach { arr.put(it) }
            json.put(name, arr)
        }
        return json.toString()
    }
}

