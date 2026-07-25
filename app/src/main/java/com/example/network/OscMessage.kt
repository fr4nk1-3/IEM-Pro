package com.example.network

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class OscMessage(
    val address: String,
    val arguments: List<Any> = emptyList()
) {
    fun encode(): ByteArray {
        val buffer = ByteBuffer.allocate(2048)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // Write address string with null padding to 4 bytes
        writePaddedString(buffer, address)

        if (arguments.isEmpty()) {
            writePaddedString(buffer, ",")
        } else {
            val typeTags = StringBuilder(",")
            for (arg in arguments) {
                when (arg) {
                    is Float -> typeTags.append('f')
                    is Int -> typeTags.append('i')
                    is String -> typeTags.append('s')
                    else -> typeTags.append('f')
                }
            }
            writePaddedString(buffer, typeTags.toString())

            for (arg in arguments) {
                when (arg) {
                    is Float -> buffer.putFloat(arg)
                    is Int -> buffer.putInt(arg)
                    is String -> writePaddedString(buffer, arg)
                    else -> buffer.putFloat((arg as Number).toFloat())
                }
            }
        }

        val bytes = ByteArray(buffer.position())
        buffer.flip()
        buffer.get(bytes)
        return bytes
    }

    companion object {
        private fun writePaddedString(buffer: ByteBuffer, str: String) {
            val bytes = str.toByteArray(Charsets.UTF_8)
            buffer.put(bytes)
            buffer.put(0.toByte()) // trailing null
            var pad = 4 - ((bytes.size + 1) % 4)
            if (pad < 4) {
                while (pad > 0) {
                    buffer.put(0.toByte())
                    pad--
                }
            }
        }

        fun decode(bytes: ByteArray, length: Int): OscMessage? {
            if (length < 4) return null
            val buffer = ByteBuffer.wrap(bytes, 0, length)
            buffer.order(ByteOrder.BIG_ENDIAN)

            val address = readPaddedString(buffer) ?: return null
            if (!address.startsWith("/")) return null

            if (!buffer.hasRemaining()) {
                return OscMessage(address)
            }

            val typeTags = readPaddedString(buffer) ?: ","
            val args = mutableListOf<Any>()

            if (typeTags.startsWith(",")) {
                for (i in 1 until typeTags.length) {
                    val tag = typeTags[i]
                    when (tag) {
                        'f' -> {
                            if (buffer.remaining() >= 4) args.add(buffer.getFloat())
                        }
                        'i' -> {
                            if (buffer.remaining() >= 4) args.add(buffer.getInt())
                        }
                        's' -> {
                            val str = readPaddedString(buffer)
                            if (str != null) args.add(str)
                        }
                        else -> {
                            if (buffer.remaining() >= 4) args.add(buffer.getFloat())
                        }
                    }
                }
            }

            return OscMessage(address, args)
        }

        private fun readPaddedString(buffer: ByteBuffer): String? {
            val start = buffer.position()
            var len = 0
            while (buffer.hasRemaining()) {
                val b = buffer.get()
                if (b == 0.toByte()) {
                    break
                }
                len++
            }
            if (len == 0 && !buffer.hasRemaining()) return null

            val bytes = ByteArray(len)
            val currentPos = buffer.position()
            buffer.position(start)
            buffer.get(bytes, 0, len)

            // Skip null padding
            var totalRead = len + 1
            var pad = 4 - (totalRead % 4)
            if (pad < 4) {
                totalRead += pad
            }
            buffer.position(start + totalRead)

            return String(bytes, Charsets.UTF_8)
        }
    }
}
