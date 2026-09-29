package com.modmoney.data

import androidx.room.TypeConverter

/**
 * Enums are persisted by name (not ordinal) so that reordering an enum in a
 * future version cannot silently corrupt existing rows.
 */
class Converters {
    @TypeConverter fun toStatus(value: String): ModStatus = ModStatus.valueOf(value)
    @TypeConverter fun fromStatus(value: ModStatus): String = value.name

    @TypeConverter fun toPriority(value: String): ModPriority = ModPriority.valueOf(value)
    @TypeConverter fun fromPriority(value: ModPriority): String = value.name

    @TypeConverter fun toCategory(value: String): ModCategory = ModCategory.valueOf(value)
    @TypeConverter fun fromCategory(value: ModCategory): String = value.name
}
