package com.example.data

import org.json.JSONArray
import org.json.JSONObject

object CustomGroupParser {
    val DEFAULT_GROUPS: Map<String, List<Int>> = mapOf(
        "Drums" to listOf(1, 2, 3, 4, 5, 6, 7, 8),
        "Bass" to listOf(9, 10),
        "Guitars" to listOf(11, 12, 13, 14),
        "Keys" to listOf(15, 16, 17, 18),
        "Vocals" to listOf(19, 20, 21, 22, 23, 24),
        "Horns" to listOf(25, 26, 27, 28),
        "FX" to listOf(29, 30, 31, 32)
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
                val arr = json.getJSONArray(key)
                val list = mutableListOf<Int>()
                for (i in 0 until arr.length()) {
                    list.add(arr.getInt(i))
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
