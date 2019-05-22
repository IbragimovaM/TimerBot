package com.internship

import java.util.logging.Logger
import java.util.regex.Pattern

enum class Duration(val pattern: Regex, val seconds: Long) {
    SECOND(Regex("(с\\.?|сек|секунд|секунды|секунду)", RegexOption.IGNORE_CASE), 1),
    MINUTE(Regex("(м\\.?|мин|минуту|минуты|минут)", RegexOption.IGNORE_CASE), 60 * SECOND.seconds),
    HOUR(Regex("(ч\\.?|час|часа|часов)", RegexOption.IGNORE_CASE), 60 * MINUTE.seconds),
    DAY(Regex("(д\\.?|дня|день|дней)", RegexOption.IGNORE_CASE), 24 * HOUR.seconds)
}

private val logger = Logger.getLogger("[ParseMessage]")

fun parseRequest(text: String) : Long {
    var value : Long? = null
    var result = 0L


    for (item in text.split(Pattern.compile("\\s+"))) {
        val intValue = item.toIntOrNull()

        if (intValue == null) {
            if (value == null) {
                logger.info("Duration specifier without value: $item")
                return 0
            }

            var ok = false
            for (duration in Duration.values()) {
                if (item matches duration.pattern) {
                    result += 1000 * value!! * duration.seconds
                    value = null
                    ok = true
                    break
                }
            }

            if (!ok) {
                logger.info("Cannot parse duration: $item")
                return 0
            }
        } else {
            if (value != null) {
                logger.info("Encountered two amounts in a row: $value $item")
                return 0
            }

            value = intValue.toLong()
        }
    }

    return result
}