package com.example.myreminders_claude2.utils

import java.util.*

data class ParsedReminder(
    val title: String,
    val notes: String,
    val dateTime: Long,
    val mainCategory: String,          // WORK, PERSONAL, HEALTH, FINANCE, or GENERAL
    val subCategory: String?,          // Call, Meeting, Email, etc. or null
    val recurrenceType: String,        // ONE_TIME, DAILY, WEEKLY, MONTHLY, ANNUAL
    val recurrenceInterval: Int,       // 1, 2, 3, etc.
    val recurrenceDayOfWeek: Int?,     // For weekly (1=Sun, 2=Mon, etc.)
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

    // Type keywords - NO CATEGORY ASSOCIATIONS (neutral)
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

    // Type keywords specific to categories (used AFTER category is determined)
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

    // Common acronyms to capitalize
    private val ACRONYMS = setOf(
        "fbi", "cia", "nsa", "ceo", "cfo", "cto", "hr", "it", "api", "nasa", "usa",
        "uk", "eu", "un", "who", "atm", "gps", "diy", "asap", "rsvp", "eta", "fyi",
        "btw", "lol", "omg", "pdf", "jpg", "png", "sql", "html", "css", "json", "xml"
    )

    // Articles and prepositions to NOT capitalize (unless first word)
    private val LOWERCASE_WORDS = setOf(
        "the", "a", "an", "and", "or", "but", "of", "in", "on", "at", "to", "for",
        "with", "from", "by", "as", "is", "are", "was", "were", "be", "been"
    )

    // Filler words to remove
    private val FILLER_WORDS = setOf(
        "uh", "um", "like", "you know", "so", "well", "actually", "basically",
        "literally", "i mean", "kind of", "sort of"
    )

    // Days of week
    private val DAYS_OF_WEEK = mapOf(
        "sunday" to 1, "monday" to 2, "tuesday" to 3, "wednesday" to 4,
        "thursday" to 5, "friday" to 6, "saturday" to 7
    )

    // ===== MAIN PARSING FUNCTION =====

    fun parseVoiceInput(input: String): ParsedReminder? {
        var text = input.trim()

        // Remove filler words
        text = removeFillerWords(text)

        // Clean prefix
        text = cleanPrefix(text)

        if (text.isBlank()) return null

        // Detect category FIRST (before type detection)
        val detectedCategory = detectCategory(text)

        // Detect type based on category context
        val detectedType = detectType(text, detectedCategory)

        // Detect recurrence
        val (recurrenceType, recurrenceInterval, recurrenceDayOfWeek) = detectRecurrence(text)

        // Remove recurrence text from input for cleaner parsing
        val textWithoutRecurrence = removeRecurrenceText(text)

        // Extract title and notes
        val (rawTitle, rawNotes) = extractTitleAndNotes(textWithoutRecurrence)

        // Capitalize properly
        val title = smartCapitalize(rawTitle)
        val notes = smartCapitalize(rawNotes)

        // Extract date/time
        val dateTime = extractDateTime(text)

        return ParsedReminder(
            title = title,
            notes = notes,
            dateTime = dateTime,
            mainCategory = detectedCategory,
            subCategory = detectedType,
            recurrenceType = recurrenceType,
            recurrenceInterval = recurrenceInterval,
            recurrenceDayOfWeek = recurrenceDayOfWeek,
            confidence = 0.8f
        )
    }

    // ===== HELPER FUNCTIONS =====

    private fun removeFillerWords(text: String): String {
        var result = text
        FILLER_WORDS.forEach { filler ->
            result = result.replace(Regex("\\b$filler\\b", RegexOption.IGNORE_CASE), "")
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun cleanPrefix(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.startsWith("remind me to ") -> text.substring(13)
            lowerText.startsWith("reminder to ") -> text.substring(12)
            lowerText.startsWith("remind ") -> text.substring(7)
            lowerText.startsWith("remind me ") -> text.substring(10)
            else -> text
        }
    }

    private fun detectCategory(text: String): String {
        val lowerText = text.lowercase()

        // Count keyword matches for each category
        val workScore = WORK_KEYWORDS.count { lowerText.contains(it) }
        val personalScore = PERSONAL_KEYWORDS.count { lowerText.contains(it) }
        val healthScore = HEALTH_KEYWORDS.count { lowerText.contains(it) }
        val financeScore = FINANCE_KEYWORDS.count { lowerText.contains(it) }

        val maxScore = maxOf(workScore, personalScore, healthScore, financeScore)

        return when {
            maxScore == 0 -> "GENERAL"
            healthScore == maxScore -> "HEALTH"  // Health has priority
            financeScore == maxScore -> "FINANCE"
            workScore == maxScore -> "WORK"
            personalScore == maxScore -> "PERSONAL"
            else -> "GENERAL"
        }
    }

    private fun detectType(text: String, category: String): String? {
        val lowerText = text.lowercase()

        // First check category-specific types
        val categoryTypes = when (category) {
            "HEALTH" -> TYPE_KEYWORDS_HEALTH
            "FINANCE" -> TYPE_KEYWORDS_FINANCE
            "WORK" -> TYPE_KEYWORDS_WORK
            "PERSONAL" -> TYPE_KEYWORDS_PERSONAL
            else -> emptyMap()
        }

        // Check category-specific types first
        for ((keyword, type) in categoryTypes) {
            if (lowerText.contains(keyword)) {
                return type
            }
        }

        // Then check neutral types (like "call", "email", "meeting")
        for ((keyword, type) in TYPE_KEYWORDS_NEUTRAL) {
            if (lowerText.contains(keyword)) {
                return type
            }
        }

        return null
    }

    private fun detectRecurrence(text: String): Triple<String, Int, Int?> {
        val lowerText = text.lowercase()

        // Check for specific day of week with "every"
        for ((day, dayNum) in DAYS_OF_WEEK) {
            if (lowerText.contains("every $day")) {
                return Triple("WEEKLY", 1, dayNum)
            }
        }

        // Check for interval patterns (e.g., "every 2 weeks")
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

        // Check for simple recurrence keywords
        return when {
            lowerText.contains("every hour") || lowerText.contains("hourly") ->
                Triple("HOURLY", 1, null)
            lowerText.contains("every day") || lowerText.contains("daily") ->
                Triple("DAILY", 1, null)
            lowerText.contains("every week") || lowerText.contains("weekly") ->
                Triple("WEEKLY", 1, null)
            lowerText.contains("every month") || lowerText.contains("monthly") ->
                Triple("MONTHLY", 1, null)
            lowerText.contains("every year") || lowerText.contains("annually") || lowerText.contains("yearly") ->
                Triple("ANNUAL", 1, null)
            else -> Triple("ONE_TIME", 1, null)
        }
    }

    private fun removeRecurrenceText(text: String): String {
        var result = text

        // Remove recurrence patterns
        result = result.replace(Regex("every\\s+\\d+\\s+(hour|day|week|month|year)s?", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("every\\s+(hour|day|week|month|year)", RegexOption.IGNORE_CASE), "")
        result = result.replace(Regex("(hourly|daily|weekly|monthly|annually|yearly)", RegexOption.IGNORE_CASE), "")

        // Remove "every [day of week]"
        DAYS_OF_WEEK.keys.forEach { day ->
            result = result.replace(Regex("every\\s+$day", RegexOption.IGNORE_CASE), "")
        }

        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun extractTitleAndNotes(text: String): Pair<String, String> {
        val lowerText = text.lowercase()

        // Look for context introducers (about, regarding, for, etc.)
        val contextPatterns = listOf(
            "about " to 6,
            "regarding " to 10,
            "for " to 4,
            "to discuss " to 11,
            "concerning " to 11
        )

        for ((pattern, length) in contextPatterns) {
            val index = lowerText.indexOf(pattern)
            if (index >= 0) {
                val title = text.substring(0, index).trim()
                val notesStart = index + length
                val notes = extractNotesBeforeTimeIndicators(text.substring(notesStart))
                return Pair(title, notes)
            }
        }

        // No context introducer found - extract title before time indicators
        val titleEnd = findTimeIndicatorPosition(lowerText)
        val title = if (titleEnd > 0) {
            text.substring(0, titleEnd).trim()
        } else {
            text.trim()
        }

        return Pair(title, "")
    }

    private fun extractNotesBeforeTimeIndicators(text: String): String {
        val endPos = findTimeIndicatorPosition(text.lowercase())
        return if (endPos > 0) {
            text.substring(0, endPos).trim()
        } else {
            text.trim()
        }
    }

    private fun findTimeIndicatorPosition(lowerText: String): Int {
        val timeIndicators = listOf(
            " tomorrow", " today", " tonight", " this week", " next week",
            " this month", " next month", " this time", " at ", " on ",
            " in ", " next ", " monday", " tuesday", " wednesday", " thursday",
            " friday", " saturday", " sunday"
        )

        var minPos = lowerText.length
        for (indicator in timeIndicators) {
            val pos = lowerText.indexOf(indicator)
            if (pos in 1 until minPos) {
                minPos = pos
            }
        }

        return if (minPos < lowerText.length) minPos else -1
    }

    private fun smartCapitalize(text: String): String {
        if (text.isBlank()) return text

        val words = text.split(Regex("\\s+"))
        val capitalized = words.mapIndexed { index, word ->
            val lowerWord = word.lowercase()

            when {
                // Capitalize acronyms
                ACRONYMS.contains(lowerWord) -> word.uppercase()

                // Capitalize days of week
                DAYS_OF_WEEK.keys.contains(lowerWord) -> word.replaceFirstChar { it.uppercase() }

                // Capitalize months
                isMonth(lowerWord) -> word.replaceFirstChar { it.uppercase() }

                // First word always capitalized
                index == 0 -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                // Don't capitalize articles and prepositions (unless first word)
                LOWERCASE_WORDS.contains(lowerWord) -> lowerWord

                // Capitalize words after certain verbs (potential names)
                index > 0 && isActionVerb(words[index - 1].lowercase()) ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

                // Keep as is if already has capitals (might be a name)
                word.any { it.isUpperCase() } -> word

                // Otherwise lowercase
                else -> lowerWord
            }
        }

        return capitalized.joinToString(" ")
    }

    private fun isMonth(word: String): Boolean {
        val months = setOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )
        return months.contains(word)
    }

    private fun isActionVerb(word: String): Boolean {
        val actionVerbs = setOf(
            "call", "email", "meet", "contact", "message", "text", "phone",
            "see", "visit", "remind"
        )
        return actionVerbs.contains(word)
    }

    private fun extractDateTime(text: String): Long {
        val lowerText = text.lowercase()
        val calendar = Calendar.getInstance()

        // Default to current time
        var timeWasSpecified = false

        // Check for specific times (e.g., "3 PM", "3:30 PM", "15:00")
        val timePatterns = listOf(
            "(\\d{1,2})\\s*:?\\s*(\\d{2})?\\s*(am|pm)".toRegex(),
            "at\\s+(\\d{1,2})\\s*:?\\s*(\\d{2})?\\s*(am|pm)?".toRegex()
        )

        for (pattern in timePatterns) {
            val match = pattern.find(lowerText)
            if (match != null) {
                val hour = match.groupValues[1].toIntOrNull() ?: 0
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val meridiem = match.groupValues[3]

                var finalHour = hour
                if (meridiem.contains("pm") && hour < 12) {
                    finalHour += 12
                } else if (meridiem.contains("am") && hour == 12) {
                    finalHour = 0
                }

                calendar.set(Calendar.HOUR_OF_DAY, finalHour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                timeWasSpecified = true
                break
            }
        }

        // Check for "in X minutes/hours"
        if (!timeWasSpecified) {
            val relativePattern = Regex("in\\s+(\\d+)\\s+(minute|hour)s?", RegexOption.IGNORE_CASE)
            relativePattern.find(lowerText)?.let { match ->
                val amount = match.groupValues[1].toIntOrNull() ?: 0
                val unit = match.groupValues[2].lowercase()

                when (unit) {
                    "minute" -> calendar.add(Calendar.MINUTE, amount)
                    "hour" -> calendar.add(Calendar.HOUR_OF_DAY, amount)
                }
                timeWasSpecified = true
            }
        }

        // Check for time of day keywords
        if (!timeWasSpecified) {
            when {
                lowerText.contains("this morning") || lowerText.contains("tomorrow morning") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 9)
                    calendar.set(Calendar.MINUTE, 0)
                    timeWasSpecified = true
                }
                lowerText.contains("this afternoon") || lowerText.contains("tomorrow afternoon") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 14)
                    calendar.set(Calendar.MINUTE, 0)
                    timeWasSpecified = true
                }
                lowerText.contains("this evening") || lowerText.contains("tomorrow evening") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 18)
                    calendar.set(Calendar.MINUTE, 0)
                    timeWasSpecified = true
                }
                lowerText.contains("tonight") -> {
                    calendar.set(Calendar.HOUR_OF_DAY, 20)
                    calendar.set(Calendar.MINUTE, 0)
                    timeWasSpecified = true
                }
            }
        }

        // If no time was specified, keep current time (already set by Calendar.getInstance())
        // Just ensure seconds are 0
        if (!timeWasSpecified) {
            calendar.set(Calendar.SECOND, 0)
        }

        // Check for day modifiers
        when {
            lowerText.contains("next week") || lowerText.contains("this time next week") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            lowerText.contains("next month") -> {
                calendar.add(Calendar.MONTH, 1)
            }
            lowerText.contains("tomorrow") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            lowerText.contains("today") -> {
                // Keep today
            }
        }

        // Check for specific days of week
        for ((day, dayNum) in DAYS_OF_WEEK) {
            if (lowerText.contains("next $day") || lowerText.contains("every $day") || (lowerText.contains(day) && !lowerText.contains("this $day"))) {
                setToDayOfWeek(calendar, dayNum, true)
                break
            } else if (lowerText.contains("this $day")) {
                setToDayOfWeek(calendar, dayNum, false)
                break
            }
        }

        // If the time is in the past, move it to tomorrow (unless "next week" was specified)
        if (calendar.timeInMillis < System.currentTimeMillis() && !lowerText.contains("next week")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calendar.timeInMillis
    }

    private fun setToDayOfWeek(calendar: Calendar, targetDay: Int, forceNext: Boolean = false) {
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
        var daysToAdd = targetDay - currentDay

        if (daysToAdd <= 0 || forceNext) {
            daysToAdd += 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
    }
}