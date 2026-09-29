package com.modmoney.data

/**
 * Lifecycle of a single modification.
 *
 * Money semantics (documented decision):
 *  - PLANNED  -> counts toward "projected" total only, never toward "spent".
 *  - BOUGHT   -> counts toward "spent" (cash has left your account even if the
 *                part is still sitting in a box).
 *  - INSTALLED-> counts toward "spent".
 */
enum class ModStatus(val label: String) {
    PLANNED("Planned"),
    BOUGHT("Bought"),
    INSTALLED("Installed");

    /** True when this status represents money already committed. */
    val isSpent: Boolean get() = this == BOUGHT || this == INSTALLED
}

/** Build priority, used for sorting and for the "do this first" hints. */
enum class ModPriority(val label: String, val sortWeight: Int) {
    HIGH("High", 0),
    MEDIUM("Medium", 1),
    LOW("Low", 2)
}

/**
 * Fixed category list. Kept as an enum rather than free text so the
 * per-category dashboard breakdown and the rule engine can both rely on it.
 */
enum class ModCategory(val label: String) {
    WHEELS("Wheels"),
    TIRES("Tires"),
    SUSPENSION("Suspension"),
    BRAKES("Brakes"),
    ENGINE("Engine"),
    EXHAUST("Exhaust"),
    EXTERIOR("Exterior"),
    INTERIOR("Interior"),
    AUDIO("Audio"),
    LIGHTING("Lighting"),
    ELECTRONICS("Electronics"),
    MAINTENANCE("Maintenance"),
    OTHER("Other")
}

/** Severity of a generated compatibility warning. */
enum class WarningSeverity(val label: String) {
    INFO("Info"),
    CAUTION("Caution"),
    CRITICAL("Critical")
}
