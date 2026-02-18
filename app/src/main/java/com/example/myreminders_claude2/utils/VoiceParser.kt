package com.example.myreminders_claude2.utils

import java.util.*
import kotlin.math.abs

data class ParsedReminder(
    val title: String,
    val notes: String,
    val dateTime: Long,
    val mainCategory: String,
    val subCategory: String?,
    val recurrenceType: String,
    val recurrenceInterval: Int,
    val recurrenceDayOfWeek: Int?,
    val confidence: Float
)

object VoiceParser {

    // ===== KEYWORD DICTIONARIES =====

    private val WORK_KEYWORDS = setOf(
        "project", "meeting", "client", "deadline", "report", "email", "presentation",
        "colleague", "boss", "office", "conference", "proposal", "contract", "work",
        "business", "team", "manager", "department", "company", "task", "assignment"
    )

    private val PERSONAL_KEYWORDS = setOf(
        "home", "family", "friend", "groceries", "shopping", "errand", "house",
        "kids", "birthday", "anniversary", "social", "party", "dinner", "lunch",
        "personal", "visit", "pick up", "drop off", "son", "daughter", "wife",
        "husband", "mom", "dad", "mother", "father", "brother", "sister"
    )

    private val HEALTH_KEYWORDS = setOf(
        "doctor", "medication", "pills", "exercise", "gym", "checkup", "appointment",
        "therapy", "hospital", "dentist", "prescription", "health", "medical",
        "medicine", "workout", "run", "jog", "yoga", "vitamins", "nurse", "clinic"
    )

    private val FINANCE_KEYWORDS = setOf(
        "pay", "bill", "rent", "mortgage", "tax", "budget", "invoice", "payment",
        "bank", "salary", "insurance", "finance", "money", "credit card", "loan",
        "investment", "deposit", "withdrawal"
    )

    private val TYPE_KEYWORDS_NEUTRAL = mapOf(
        "call" to "Call",
        "phone" to "Call",
        "ring" to "Call",
        "meeting" to "Meeting",
        "meet" to "Meeting",
        "conference" to "Meeting",
        "email" to "Email",
        "send email" to "Email",
        "write email" to "Email"
    )

    private val TYPE_KEYWORDS_HEALTH = mapOf(
        "medication" to "Medication",
        "pills" to "Medication",
        "medicine" to "Medication",
        "take pills" to "Medication",
        "exercise" to "Exercise",
        "workout" to "Exercise",
        "gym" to "Exercise",
        "checkup" to "Checkup",
        "doctor" to "Doctor",
        "dentist" to "Doctor"
    )

    private val TYPE_KEYWORDS_FINANCE = mapOf(
        "pay" to "Payment",
        "bill" to "Bill",
        "payment" to "Payment",
        "tax" to "Tax"
    )

    private val TYPE_KEYWORDS_WORK = mapOf(
        "deadline" to "Deadline",
        "submit" to "Deadline",
        "report" to "Report"
    )

    private val TYPE_KEYWORDS_PERSONAL = mapOf(
        "errand" to "Errand",
        "buy" to "Errand",
        "get" to "Errand",
        "pick up" to "Errand",
        "appointment" to "Appointment",
        "event" to "Event"
    )

    private val FILLER_WORDS = setOf(
        "uh", "um", "like", "you know", "so", "well", "actually", "basically",
        "literally", "i mean", "kind of", "sort of"
    )

    private val DAYS_OF_WEEK = mapOf(
        "sunday" to 1, "monday" to 2, "tuesday" to 3, "wednesday" to 4,
        "thursday" to 5, "friday" to 6, "saturday" to 7
    )

    // ===== MAIN PARSING FUNCTION =====

    fun parseVoiceInput(input: String): ParsedReminder? {
        android.util.Log.d("VoiceParser", "===== RAW INPUT =====")
        android.util.Log.d("VoiceParser", "Input: '$input'")

        var text = input.trim()
        if (text.isBlank()) return null

        // Phase 1: Clean and normalize
        text = cleanAndNormalize(text)
        if (text.isBlank()) return null

        // Phase 2: Check for relative time (highest priority - shortcut)
        val relativeTime = extractRelativeTime(text)
        if (relativeTime != null) {
            val cleanTitle = removeRelativeTimeText(text)
            val category = detectCategory(cleanTitle)
            val type = detectType(cleanTitle, category)
            val title = smartCapitalize(cleanTitle.trim())

            return ParsedReminder(
                title = title,
                notes = "",
                dateTime = relativeTime,
                mainCategory = category,
                subCategory = type,
                recurrenceType = "ONE_TIME",
                recurrenceInterval = 1,
                recurrenceDayOfWeek = null,
                confidence = 0.9f
            )
        }

        // Phase 3: Extract recurrence
        val (recurrenceType, recurrenceInterval, recurrenceDayOfWeek) = extractRecurrence(text)
        android.util.Log.d("VoiceParser", "Recurrence: $recurrenceType, interval: $recurrenceInterval, dayOfWeek: $recurrenceDayOfWeek")
        val textWithoutRecurrence = removeRecurrenceText(text)
        android.util.Log.d("VoiceParser", "After recurrence removal: '$textWithoutRecurrence'")

        // Phase 4 & 5: Extract date and time
        var (dateTime, textWithoutDateTime) = extractDateTimeAndClean(textWithoutRecurrence)
        android.util.Log.d("VoiceParser", "After date/time extraction: '$textWithoutDateTime'")

        // Phase 4.5: Adjust date to match recurrence day if specified
        if (recurrenceDayOfWeek != null) {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = dateTime
            setToDayOfWeek(calendar, recurrenceDayOfWeek, true) // Force next occurrence
            dateTime = calendar.timeInMillis
        }

        // Phase 6: Extract title (what remains is the task)
        val title = smartCapitalize(textWithoutDateTime.trim())

        // Phase 7: Detect category and type
        val category = detectCategory(text) // Use original text for better context
        val type = detectType(text, category)

        return ParsedReminder(
            title = title,
            notes = "",
            dateTime = dateTime,
            mainCategory = category,
            subCategory = type,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceDayOfWeek = recurrenceDayOfWeek,
            confidence = 0.85f
        )
    }

    // ===== PHASE 1: CLEAN AND NORMALIZE =====

    private fun cleanAndNormalize(text: String): String {
        var result = text

        // Remove filler words
        FILLER_WORDS.forEach { filler ->
            result = result.replace(Regex("\\b$filler\\b", RegexOption.IGNORE_CASE), "")
        }

        // Remove common prefixes
        val prefixes = listOf(
            "remind me to ",
            "reminder to ",
            "remind to ",
            "remind me ",
            "remind ",
            "set a reminder to ",
            "set a reminder for ",
            "set reminder to ",
            "set reminder for "
        )

        val lowerResult = result.lowercase()
        for (prefix in prefixes) {
            if (lowerResult.startsWith(prefix)) {
                result = result.substring(prefix.length)
                break
            }
        }

        // Normalize spacing
        result = result.replace(Regex("\\s+"), " ").trim()

        return result
    }

    // ===== PHASE 2: RELATIVE TIME (SHORTCUT) =====

    private fun extractRelativeTime(text: String): Long? {
        val lowerText = text.lowercase()

        // "in X minutes/hours"
        var pattern = Regex("in\\s+(\\d+)\\s+(minute|hour)s?", RegexOption.IGNORE_CASE)
        var match = pattern.find(lowerText)
        if (match != null) {
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            val calendar = Calendar.getInstance()
            when (unit) {
                "minute" -> calendar.add(Calendar.MINUTE, amount)
                "hour" -> calendar.add(Calendar.HOUR_OF_DAY, amount)
            }
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        // "X minutes/hours from now"
        pattern = Regex("(\\d+)\\s+(minute|hour)s?\\s+from\\s+now", RegexOption.IGNORE_CASE)
        match = pattern.find(lowerText)
        if (match != null) {
            val amount = match.groupValues[1].toIntOrNull() ?: return null
            val unit = match.groupValues[2].lowercase()
            val calendar = Calendar.getInstance()
            when (unit) {
                "minute" -> calendar.add(Calendar.MINUTE, amount)
                "hour" -> calendar.add(Calendar.HOUR_OF_DAY, amount)
            }
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        return null
    }

    private fun removeRelativeTimeText(text: String): String {
        var result = text
        result = result.replace(Regex("in\\s+\\d+\\s+(minute|hour)s?", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("\\d+\\s+(minute|hour)s?\\s+from\\s+now", RegexOption.IGNORE_CASE), "")
        return result.replace(Regex("\\s+"), " ").trim()
    }

    // ===== PHASE 3: RECURRENCE =====

    private fun extractRecurrence(text: String): Triple<String, Int, Int?> {
        val lowerText = text.lowercase()

        // "every morning/afternoon/evening/night" = DAILY
        if (lowerText.contains("every morning") || lowerText.contains("every afternoon") ||
            lowerText.contains("every evening") || lowerText.contains("every night")) {
            return Triple("DAILY", 1, null)
        }

        // "every [weekday]"
        for ((day, dayNum) in DAYS_OF_WEEK) {
            if (lowerText.contains("every $day")) {
                return Triple("WEEKLY", 1, dayNum)
            }
        }

        // "every X [unit]"
        val intervalPattern = Regex("every\\s+(\\d+)\\s+(hour|day|week|month|year)s?", RegexOption.IGNORE_CASE)
        intervalPattern.find(lowerText)?.let { match ->
            val interval = match.groupValues[1].toIntOrNull() ?: 1
            val unit = match.groupValues[2].lowercase()
            return when (unit) {
                "hour" -> Triple("HOURLY", interval, null)
                "day" -> Triple("DAILY", interval, null)
                "week" -> Triple("WEEKLY", interval, null)
                "month" -> Triple("MONTHLY", interval, null)
                "year" -> Triple("ANNUAL", interval, null)
                else -> Triple("ONE_TIME", 1, null)
            }
        }

        // Simple keywords
        return when {
            lowerText.contains("every hour") || lowerText.contains("hourly") -> Triple("HOURLY", 1, null)
            lowerText.contains("every day") || lowerText.contains("everyday") || lowerText.contains("daily") -> Triple("DAILY", 1, null)
            lowerText.contains("every week") || lowerText.contains("weekly") -> Triple("WEEKLY", 1, null)
            lowerText.contains("every month") || lowerText.contains("monthly") -> Triple("MONTHLY", 1, null)
            lowerText.contains("every year") || lowerText.contains("annually") || lowerText.contains("yearly") -> Triple("ANNUAL", 1, null)
            else -> Triple("ONE_TIME", 1, null)
        }
    }

    private fun removeRecurrenceText(text: String): String {
        var result = text

        // Remove "every morning/afternoon/evening/night" and "everyday"
        result = result.replace(Regex("everyday", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("every\\s+(morning|afternoon|evening|night)", RegexOption.IGNORE_CASE), "")
        // Remove "every X unit"
        result = result.replace(Regex("every\\s+\\d+\\s+(hour|day|week|month|year)s?", RegexOption.IGNORE_CASE), "")

        // Remove "every unit"
        result = result.replace(Regex("every\\s+(hour|day|week|month|year)", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("(hourly|daily|weekly|monthly|annually|yearly)", RegexOption.IGNORE_CASE), "")

        // Remove "every [weekday]"
        DAYS_OF_WEEK.keys.forEach { day ->
            result = result.replace(Regex("every\\s+$day", RegexOption.IGNORE_CASE), "")
        }

        return result.replace(Regex("\\s+"), " ").trim()
    }

    // ===== PHASE 4 & 5: DATE AND TIME =====

    private fun extractDateTimeAndClean(text: String): Pair<Long, String> {
        val lowerText = text.lowercase()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var cleanedText = text
        var hasTimeOfDay = false
        var explicitAmPm = false

        // Extract explicit time first (3pm, 3:30pm, 3 p.m., 15:00)
        val timePatterns = listOf(
            Regex("(\\d{1,2})\\s*:?\\s*(\\d{2})?\\s*([ap]\\.?m\\.?)", RegexOption.IGNORE_CASE),
            Regex("at\\s+(\\d{1,2})\\s*:?\\s*(\\d{2})?\\s*([ap]\\.?m\\.?)?", RegexOption.IGNORE_CASE),
            Regex("(\\d{1,2})\\s*:?\\s*(\\d{2})?(?=\\s|$)", RegexOption.IGNORE_CASE)
        )

        for (pattern in timePatterns) {
            val match = pattern.find(lowerText)
            if (match != null) {
                val hour = match.groupValues[1].toIntOrNull() ?: continue
                val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                val meridiem = match.groupValues.getOrNull(3)?.lowercase() ?: ""

                var finalHour = hour

                if (meridiem.isNotEmpty()) {
                    explicitAmPm = true
                    val cleanMeridiem = meridiem.replace(".", "").lowercase()
                    if (cleanMeridiem == "pm" && hour < 12) {
                        finalHour += 12
                    } else if (cleanMeridiem == "am" && hour == 12) {
                        finalHour = 0
                    }
                } else {
                    // Smart AM/PM inference
                    finalHour = inferAmPm(hour, lowerText)
                }

                calendar.set(Calendar.HOUR_OF_DAY, finalHour)
                calendar.set(Calendar.MINUTE, minute)
                hasTimeOfDay = true

                // Remove time from text
                cleanedText = cleanedText.replace(match.value, "")
                break
            }
        }

        // Extract time-of-day keywords
        if (!hasTimeOfDay) {
            when {
                lowerText.contains("morning") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    cleanedText = cleanedText.replace(Regex("\\b(this\\s+)?morning\\b", RegexOption.IGNORE_CASE), "")
                }
                lowerText.contains("afternoon") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 14)
                    calendar.set(Calendar.MINUTE, 0)
                    cleanedText = cleanedText.replace(Regex("\\b(this\\s+)?afternoon\\b", RegexOption.IGNORE_CASE), "")
                }
                lowerText.contains("evening") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 18)
                    calendar.set(Calendar.MINUTE, 0)
                    cleanedText = cleanedText.replace(Regex("\\b(this\\s+)?evening\\b", RegexOption.IGNORE_CASE), "")
                }
                lowerText.contains("tonight") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 20)
                    calendar.set(Calendar.MINUTE, 0)
                    cleanedText = cleanedText.replace(Regex("\\btonight\\b", RegexOption.IGNORE_CASE), "")
                }
            }
        }

        // Extract date
        var dayShift = 0
        when {
            lowerText.contains("tomorrow") -> {
                dayShift = 1
                cleanedText = cleanedText.replace(Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE), "")
            }
            lowerText.contains("today") -> {
                dayShift = 0
                cleanedText = cleanedText.replace(Regex("\\btoday\\b", RegexOption.IGNORE_CASE), "")
            }
        }

        // Check for weekdays
        for ((day, dayNum) in DAYS_OF_WEEK) {
            if (lowerText.contains("next $day") || (lowerText.contains(day) && !lowerText.contains("this $day"))) {
                setToDayOfWeek(calendar, dayNum, true)
                cleanedText = cleanedText.replace(Regex("\\b(next\\s+)?$day\\b", RegexOption.IGNORE_CASE), "")
                dayShift = -1 // Prevent tomorrow logic
                break
            } else if (lowerText.contains("this $day")) {
                setToDayOfWeek(calendar, dayNum, false)
                cleanedText = cleanedText.replace(Regex("\\bthis\\s+$day\\b", RegexOption.IGNORE_CASE), "")
                dayShift = -1
                break
            }
        }

        if (dayShift >= 0) {
            calendar.add(Calendar.DAY_OF_YEAR, dayShift)
        }

        // Handle "on the [number]" for monthly reminders (both digits and words)
        val wordToNumber = mapOf(
            "first" to 1, "second" to 2, "third" to 3, "fourth" to 4, "fifth" to 5,
            "sixth" to 6, "seventh" to 7, "eighth" to 8, "ninth" to 9, "tenth" to 10,
            "eleventh" to 11, "twelfth" to 12, "thirteenth" to 13, "fourteenth" to 14,
            "fifteenth" to 15, "sixteenth" to 16, "seventeenth" to 17, "eighteenth" to 18,
            "nineteenth" to 19, "twentieth" to 20, "twenty-first" to 21, "twenty-second" to 22,
            "twenty-third" to 23, "twenty-fourth" to 24, "twenty-fifth" to 25,
            "twenty-sixth" to 26, "twenty-seventh" to 27, "twenty-eighth" to 28,
            "twenty-ninth" to 29, "thirtieth" to 30, "thirty-first" to 31
        )

        // Try numeric pattern first: "on the 1st", "on the 15th"
        var dayOfMonthPattern = Regex("on\\s+the\\s+(\\d{1,2})(st|nd|rd|th)?", RegexOption.IGNORE_CASE)
        var match = dayOfMonthPattern.find(lowerText)
        var dayOfMonth: Int? = match?.groupValues?.get(1)?.toIntOrNull()

        // Try word pattern: "on the first", "on the fifteenth"
        if (dayOfMonth == null) {
            val wordPattern = Regex("on\\s+the\\s+(\\w+(?:-\\w+)?)", RegexOption.IGNORE_CASE)
            match = wordPattern.find(lowerText)
            match?.let {
                val word = it.groupValues[1].lowercase()
                dayOfMonth = wordToNumber[word]
            }
        }

        // Set the day if found
        if (dayOfMonth != null && dayOfMonth in 1..31) {
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth!!)
            // If that day already passed this month, move to next month
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                calendar.add(Calendar.MONTH, 1)
            }
            if (match != null) {
                cleanedText = cleanedText.replace(match.value, "")
            }
        }

        // Remove temporal prepositions and time markers (with word boundaries!)
        cleanedText = cleanedText.replace(Regex("\\bat\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bon\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bby\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\b[ap]\\.?m\\.?\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bo'clock\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bafternoon\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bmorning\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bevening\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\btonight\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\btoday\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bthe\\s+first\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\bthe\\s+1st\\b", RegexOption.IGNORE_CASE), "")
        cleanedText = cleanedText.replace(Regex("\\s+"), " ").trim()

        // Remove leading "to" (task indicator)
        cleanedText = cleanedText.replace(Regex("^to\\s+", RegexOption.IGNORE_CASE), "")

        // If time ended up in the past, move to tomorrow (unless a specific future day was set)
        if (dayShift == 0 && calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return Pair(calendar.timeInMillis, cleanedText)
    }

    // Smart AM/PM inference
    private fun inferAmPm(hour: Int, context: String): Int {
        if (hour >= 13) return hour // Already 24-hour format
        if (hour == 0) return 0 // Midnight

        // Context clues
        val isPm = context.contains("afternoon") || context.contains("evening") ||
                context.contains("night") || context.contains("dinner") ||
                context.contains("lunch")

        val isAm = context.contains("morning") || context.contains("breakfast")

        return when {
            isPm && hour < 12 -> hour + 12
            isAm && hour == 12 -> 0
            isAm -> hour
            // Default heuristic: business hours = PM, early hours = AM
            hour in 8..11 -> hour // Morning hours stay AM
            hour in 1..7 -> hour + 12 // Afternoon/evening = PM
            hour == 12 -> 12 // Noon
            else -> hour
        }
    }

    private fun setToDayOfWeek(calendar: Calendar, targetDay: Int, forceNext: Boolean = false) {
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDay - currentDay

        if (daysToAdd <= 0 || forceNext) {
            daysToAdd += 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }

    // ===== CATEGORY AND TYPE DETECTION =====

    private fun detectCategory(text: String): String {
        val lowerText = text.lowercase()
        val workScore = WORK_KEYWORDS.count { lowerText.contains(it) }
        val personalScore = PERSONAL_KEYWORDS.count { lowerText.contains(it) }
        val healthScore = HEALTH_KEYWORDS.count { lowerText.contains(it) }
        val financeScore = FINANCE_KEYWORDS.count { lowerText.contains(it) }

        val maxScore = maxOf(workScore, personalScore, healthScore, financeScore)

        return when {
            maxScore == 0 -> "PERSONAL"
            healthScore == maxScore -> "HEALTH"
            financeScore == maxScore -> "FINANCE"
            workScore == maxScore -> "WORK"
            personalScore == maxScore -> "PERSONAL"
            else -> "PERSONAL"
        }
    }

    private fun detectType(text: String, category: String): String? {
        val lowerText = text.lowercase()

        val categoryTypes = when (category) {
            "HEALTH" -> TYPE_KEYWORDS_HEALTH
            "FINANCE" -> TYPE_KEYWORDS_FINANCE
            "WORK" -> TYPE_KEYWORDS_WORK
            "PERSONAL" -> TYPE_KEYWORDS_PERSONAL
            else -> emptyMap()
        }

        for ((keyword, type) in categoryTypes) {
            if (lowerText.contains(keyword)) return type
        }

        for ((keyword, type) in TYPE_KEYWORDS_NEUTRAL) {
            if (lowerText.contains(keyword)) return type
        }

        return null
    }

    // ===== SMART CAPITALIZATION =====

    private fun smartCapitalize(text: String): String {
        if (text.isBlank()) return text

        val words = text.split(Regex("\\s+"))
        val result = words.mapIndexed { index, word ->
            when {
                index == 0 -> word.replaceFirstChar { it.uppercase() }
                setOf("the", "a", "an", "and", "or", "but", "of", "in", "on", "at", "to", "for").contains(word.lowercase()) -> word.lowercase()
                else -> word.lowercase()
            }
        }

        return result.joinToString(" ")
    }
}