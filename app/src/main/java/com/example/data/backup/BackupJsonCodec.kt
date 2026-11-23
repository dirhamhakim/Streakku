package com.example.data.backup

import com.example.domain.model.BackupParseResult
import com.example.domain.model.BackupPayload
import com.example.domain.model.EntryStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitEntry
import java.time.LocalDate
import java.time.format.DateTimeParseException

object BackupJsonCodec {

    private const val CURRENT_SCHEMA_VERSION = 1
    private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6}$")

    fun encode(payload: BackupPayload): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": ").append(payload.schemaVersion).append(",\n")
        sb.append("  \"app\": \"Streakku\",\n")
        sb.append("  \"exportedAt\": ").append(payload.exportedAt).append(",\n")

        sb.append("  \"habits\": [")
        if (payload.habits.isNotEmpty()) sb.append("\n")
        payload.habits.forEachIndexed { index, habit ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(quote(habit.id)).append(",\n")
            sb.append("      \"name\": ").append(quote(habit.name)).append(",\n")
            sb.append("      \"colorHex\": ").append(quote(habit.colorHex)).append(",\n")
            sb.append("      \"createdDate\": ").append(quote(habit.createdDate.toString())).append(",\n")
            sb.append("      \"archived\": ").append(habit.archived).append(",\n")
            sb.append("      \"sortOrder\": ").append(habit.sortOrder).append(",\n")
            sb.append("      \"autoFail\": ").append(habit.autoFail).append(",\n")
            sb.append("      \"backfillLimitDays\": ")
                .append(habit.backfillLimitDays?.toString() ?: "null").append(",\n")
            sb.append("      \"activeWeekdays\": [")
                .append(habit.activeWeekdays.sorted().joinToString(", "))
                .append("]\n")
            sb.append("    }")
            if (index < payload.habits.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append(if (payload.habits.isNotEmpty()) "  ],\n" else "],\n")

        sb.append("  \"entries\": [")
        if (payload.entries.isNotEmpty()) sb.append("\n")
        payload.entries.forEachIndexed { index, entry ->
            sb.append("    {\n")
            sb.append("      \"habitId\": ").append(quote(entry.habitId)).append(",\n")
            sb.append("      \"date\": ").append(quote(entry.date.toString())).append(",\n")
            sb.append("      \"status\": ").append(quote(entry.status.name)).append(",\n")
            sb.append("      \"updatedAt\": ").append(entry.updatedAt).append("\n")
            sb.append("    }")
            if (index < payload.entries.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append(if (payload.entries.isNotEmpty()) "  ]\n" else "]\n")
        sb.append("}")
        return sb.toString()
    }

    @Suppress("UNCHECKED_CAST")
    fun decode(rawJson: String): BackupParseResult {
        if (rawJson.isBlank()) {
            return BackupParseResult.Error("File cadangan kosong.")
        }

        val parsed = try {
            SimpleJsonParser(rawJson).parseRoot()
        } catch (_: JsonParseException) {
            return BackupParseResult.Error(
                "Format JSON tidak valid. Pastikan Anda memilih file cadangan Streakku."
            )
        }

        val root = parsed as? Map<String, Any?>
            ?: return BackupParseResult.Error("Format JSON harus berupa objek cadangan Streakku.")

        val schemaVersionNum = root["schemaVersion"]
        val schemaVersion = when (schemaVersionNum) {
            null -> CURRENT_SCHEMA_VERSION
            is Number -> schemaVersionNum.toInt()
            else -> return BackupParseResult.Error("Nilai 'schemaVersion' tidak valid.")
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            return BackupParseResult.Error(
                "Versi cadangan ($schemaVersion) tidak didukung oleh aplikasi ini."
            )
        }

        if (!root.containsKey("habits") || root["habits"] == null) {
            return BackupParseResult.Error(
                "Struktur cadangan tidak lengkap: daftar kegiatan ('habits') tidak ditemukan."
            )
        }
        if (!root.containsKey("entries") || root["entries"] == null) {
            return BackupParseResult.Error(
                "Struktur cadangan tidak lengkap: daftar catatan ('entries') tidak ditemukan."
            )
        }

        val habitsRaw = root["habits"] as? List<Any?>
            ?: return BackupParseResult.Error("Format daftar kegiatan ('habits') harus berupa array JSON.")
        val entriesRaw = root["entries"] as? List<Any?>
            ?: return BackupParseResult.Error("Format daftar catatan ('entries') harus berupa array JSON.")

        val habits = mutableListOf<Habit>()
        for (i in habitsRaw.indices) {
            val obj = habitsRaw[i] as? Map<String, Any?>
                ?: return BackupParseResult.Error("Data kegiatan ke-${i + 1} tidak valid.")

            val id = (obj["id"] as? String)?.trim().orEmpty()
            if (id.isEmpty()) {
                return BackupParseResult.Error("Kegiatan ke-${i + 1} tidak memiliki ID.")
            }

            val name = (obj["name"] as? String)?.trim().orEmpty()
            if (name.isEmpty()) {
                return BackupParseResult.Error("Kegiatan ke-${i + 1} tidak memiliki nama.")
            }

            val colorHex = (obj["colorHex"] as? String)?.trim() ?: "#0E7C5B"
            if (!HEX_COLOR_REGEX.matches(colorHex)) {
                return BackupParseResult.Error("Kode warna pada kegiatan \"$name\" tidak valid ($colorHex).")
            }

            val createdDateStr = (obj["createdDate"] as? String)?.trim().orEmpty()
            val createdDate = try {
                LocalDate.parse(createdDateStr)
            } catch (_: DateTimeParseException) {
                return BackupParseResult.Error(
                    "Tanggal pembuatan pada kegiatan \"$name\" tidak valid (harus YYYY-MM-DD)."
                )
            }

            val archived = (obj["archived"] as? Boolean) ?: false
            val sortOrder = (obj["sortOrder"] as? Number)?.toInt() ?: i
            val autoFail = (obj["autoFail"] as? Boolean) ?: false

            val backfillLimitDays: Int? = when (val raw = obj["backfillLimitDays"]) {
                null -> null
                is Number -> {
                    val intVal = raw.toInt()
                    if (intVal < 0) {
                        return BackupParseResult.Error(
                            "Batas waktu input pada kegiatan \"$name\" tidak boleh negatif."
                        )
                    }
                    intVal
                }
                else -> return BackupParseResult.Error(
                    "Batas waktu input pada kegiatan \"$name\" harus berupa angka."
                )
            }

            val activeWeekdays = mutableSetOf<Int>()
            val rawDays = obj["activeWeekdays"]
            if (rawDays != null) {
                val wdList = rawDays as? List<Any?>
                    ?: return BackupParseResult.Error(
                        "Format hari aktif pada kegiatan \"$name\" tidak valid."
                    )
                for (item in wdList) {
                    val day = (item as? Number)?.toInt() ?: -1
                    if (day !in 1..7) {
                        return BackupParseResult.Error(
                            "Nilai hari aktif pada kegiatan \"$name\" harus antara 1 sampai 7."
                        )
                    }
                    activeWeekdays.add(day)
                }
            }
            if (activeWeekdays.isEmpty()) {
                activeWeekdays.addAll(Habit.ALL_WEEKDAYS)
            }

            habits.add(
                Habit(
                    id = id,
                    name = name,
                    colorHex = colorHex,
                    createdDate = createdDate,
                    archived = archived,
                    sortOrder = sortOrder,
                    autoFail = autoFail,
                    backfillLimitDays = backfillLimitDays,
                    activeWeekdays = activeWeekdays
                )
            )
        }

        val entries = mutableListOf<HabitEntry>()
        for (i in entriesRaw.indices) {
            val obj = entriesRaw[i] as? Map<String, Any?>
                ?: return BackupParseResult.Error("Data catatan ke-${i + 1} tidak valid.")

            val habitId = (obj["habitId"] as? String)?.trim().orEmpty()
            if (habitId.isEmpty()) {
                return BackupParseResult.Error("Catatan ke-${i + 1} tidak memiliki habitId.")
            }

            val dateStr = (obj["date"] as? String)?.trim().orEmpty()
            val date = try {
                LocalDate.parse(dateStr)
            } catch (_: DateTimeParseException) {
                return BackupParseResult.Error(
                    "Tanggal catatan ke-${i + 1} tidak valid (harus YYYY-MM-DD)."
                )
            }

            val statusStr = (obj["status"] as? String)?.trim().orEmpty()
            val status = try {
                EntryStatus.valueOf(statusStr)
            } catch (_: IllegalArgumentException) {
                return BackupParseResult.Error(
                    "Status catatan ke-${i + 1} tidak dikenali ('$statusStr'). Hanya DONE atau FAILED."
                )
            }

            val updatedAt = (obj["updatedAt"] as? Number)?.toLong() ?: -1L
            if (updatedAt < 0L) {
                return BackupParseResult.Error("Waktu pembaruan catatan ke-${i + 1} tidak valid.")
            }

            entries.add(
                HabitEntry(
                    habitId = habitId,
                    date = date,
                    status = status,
                    updatedAt = updatedAt
                )
            )
        }

        val exportedAt = (root["exportedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return BackupParseResult.Success(
            BackupPayload(
                schemaVersion = schemaVersion,
                exportedAt = exportedAt,
                habits = habits,
                entries = entries
            )
        )
    }

    private fun quote(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch.code < 0x20) {
                    sb.append(String.format("\\u%04x", ch.code))
                } else {
                    sb.append(ch)
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class JsonParseException(message: String) : RuntimeException(message)

    private class SimpleJsonParser(private val input: String) {
        private var pos = 0

        fun parseRoot(): Any? {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            if (pos != input.length) {
                throw JsonParseException("Unexpected trailing characters at $pos")
            }
            return value
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (pos >= input.length) throw JsonParseException("Unexpected end of input")
            return when (val ch = input[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> throw JsonParseException("Unexpected char '$ch' at $pos")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            skipWhitespace()
            val map = LinkedHashMap<String, Any?>()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> throw JsonParseException("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            skipWhitespace()
            val list = ArrayList<Any?>()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> throw JsonParseException("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < input.length) {
                val ch = input[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= input.length) throw JsonParseException("Unterminated escape")
                        when (val esc = input[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > input.length) throw JsonParseException("Invalid unicode escape")
                                val hex = input.substring(pos, pos + 4)
                                val code = hex.toIntOrNull(16)
                                    ?: throw JsonParseException("Invalid hex '$hex'")
                                sb.append(code.toChar())
                                pos += 4
                            }
                            else -> throw JsonParseException("Unknown escape '\\$esc'")
                        }
                    }
                    else -> {
                        if (ch.code < 0x20) throw JsonParseException("Unescaped control char")
                        sb.append(ch)
                    }
                }
            }
            throw JsonParseException("Unterminated string")
        }

        private fun parseLiteral(literal: String, value: Any?): Any? {
            if (input.startsWith(literal, pos)) {
                pos += literal.length
                return value
            }
            throw JsonParseException("Expected '$literal' at $pos")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (peek() == '-') pos++
            var hasDigits = false
            while (pos < input.length && input[pos] in '0'..'9') {
                pos++
                hasDigits = true
            }
            if (!hasDigits) throw JsonParseException("Invalid number at $start")
            var isFloating = false
            if (peek() == '.') {
                isFloating = true
                pos++
                var fracDigits = false
                while (pos < input.length && input[pos] in '0'..'9') {
                    pos++
                    fracDigits = true
                }
                if (!fracDigits) throw JsonParseException("Invalid fraction at $pos")
            }
            if (peek() == 'e' || peek() == 'E') {
                isFloating = true
                pos++
                if (peek() == '+' || peek() == '-') pos++
                var expDigits = false
                while (pos < input.length && input[pos] in '0'..'9') {
                    pos++
                    expDigits = true
                }
                if (!expDigits) throw JsonParseException("Invalid exponent at $pos")
            }
            val token = input.substring(start, pos)
            return if (isFloating) {
                token.toDoubleOrNull() ?: throw JsonParseException("Invalid double '$token'")
            } else {
                token.toLongOrNull() ?: throw JsonParseException("Invalid long '$token'")
            }
        }

        private fun skipWhitespace() {
            while (pos < input.length && input[pos] in charArrayOf(' ', '\n', '\r', '\t')) {
                pos++
            }
        }

        private fun peek(): Char? = if (pos < input.length) input[pos] else null

        private fun expect(expected: Char) {
            if (pos >= input.length || input[pos] != expected) {
                throw JsonParseException("Expected '$expected' at $pos")
            }
            pos++
        }
    }
}
