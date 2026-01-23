package com.example.myreminders_claude2.utils

object TextFormatter {

    // List of common acronyms that should be ALL CAPS
    private val ACRONYMS = setOf(
        // Education
        "MBA", "PhD", "MD", "BA", "BS", "MA", "MS", "LLB", "JD", "BBA", "MFA",
        // Business/Tech
        "CEO", "CTO", "CFO", "COO", "VP", "HR", "IT", "AI", "ML", "API", "UI", "UX",
        "SEO", "SaaS", "B2B", "B2C", "ROI", "KPI", "CRM", "ERP",
        // Countries/Regions
        "USA", "UK", "UAE", "EU", "UN", "NATO", "ASEAN",
        // Common
        "AM", "PM", "ASAP", "FYI", "ETA", "TBD", "TBA", "FAQ", "DIY",
        "GPS", "WiFi", "USB", "PDF", "HTML", "CSS", "SQL", "JSON", "XML"
    )

    // Days of the week
    private val DAYS = setOf(
        "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
        "mon", "tue", "wed", "thu", "fri", "sat", "sun"
    )

    // Months
    private val MONTHS = setOf(
        "january", "february", "march", "april", "may", "june",
        "july", "august", "september", "october", "november", "december",
        "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
    )

    /**
     * Apply smart capitalization to text
     * - Capitalizes first letter of sentences
     * - Converts known acronyms to uppercase
     * - Capitalizes days and months
     * - Capitalizes "I" when used as a pronoun
     */
    fun smartCapitalize(text: String): String {
        if (text.isBlank()) return text

        val words = text.split(" ")
        val result = mutableListOf<String>()
        var startOfSentence = true

        for (word in words) {
            if (word.isBlank()) {
                result.add(word)
                continue
            }

            // Clean word (remove punctuation for checking)
            val cleanWord = word.trim().replace(Regex("[.,!?;:]"), "")
            val upperWord = cleanWord.uppercase()

            val processedWord = when {
                // Known acronym - make it all caps
                ACRONYMS.contains(upperWord) -> {
                    // Preserve any trailing punctuation
                    val punctuation = word.filter { it in ".,!?;:" }
                    upperWord + punctuation
                }

                // Day of week - capitalize
                DAYS.contains(cleanWord.lowercase()) -> {
                    val punctuation = word.filter { it in ".,!?;:" }
                    cleanWord.lowercase().replaceFirstChar { it.uppercase() } + punctuation
                }

                // Month - capitalize
                MONTHS.contains(cleanWord.lowercase()) -> {
                    val punctuation = word.filter { it in ".,!?;:" }
                    cleanWord.lowercase().replaceFirstChar { it.uppercase() } + punctuation
                }

                // Single "I" as pronoun - capitalize
                cleanWord.lowercase() == "i" -> {
                    val punctuation = word.filter { it in ".,!?;:" }
                    "I" + punctuation
                }

                // Start of sentence - capitalize first letter
                startOfSentence -> {
                    word.replaceFirstChar { it.uppercase() }
                }

                // Everything else - keep as is
                else -> word
            }

            result.add(processedWord)

            // Check if this word ends a sentence
            startOfSentence = word.any { it in ".!?" }
        }

        return result.joinToString(" ")
    }

    /**
     * Capitalize just the first letter of the entire text
     * Useful for single-line inputs
     */
    fun capitalizeFirst(text: String): String {
        return text.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
    }
}