package com.example.dam_front.models

enum class SportType(val value: String) {
    FOOTBALL("football"),
    BASKETBALL("basketball"),
    TENNIS("tennis"),
    NATATION("natation"),
    VOLLEYBALL("volleyball"),
    HANDBALL("handball"),
    RUGBY("rugby"),
    JUDO("judo"),
    KARATE("karate"),
    ATHLETISME("athletisme"),
    GYMNASTIQUE("gymnastique"),
    ESCALADE("escalade"),
    CYCLISME("cyclisme");

    companion object {
        fun fromValue(value: String): SportType? {
            return values().find { it.value == value }
        }
        
        fun getDisplayName(value: String): String {
            return when (value) {
                "football" -> "Football"
                "basketball" -> "Basketball"
                "tennis" -> "Tennis"
                "natation" -> "Natation"
                "volleyball" -> "Volleyball"
                "handball" -> "Handball"
                "rugby" -> "Rugby"
                "judo" -> "Judo"
                "karate" -> "Karate"
                "athletisme" -> "Athlétisme"
                "gymnastique" -> "Gymnastique"
                "escalade" -> "Escalade"
                "cyclisme" -> "Cyclisme"
                else -> value
            }
        }
    }
}


