package lol.hanyuu.iccardscanner.data.db.converter

import androidx.room.TypeConverter
import lol.hanyuu.iccardscanner.domain.model.CardType
import lol.hanyuu.iccardscanner.domain.model.ProcessType

class Converters {
    @TypeConverter
    fun fromCardType(v: CardType): String = v.name

    @TypeConverter
    fun toCardType(v: String): CardType = CardType.valueOf(v)

    @TypeConverter
    fun fromProcessType(v: ProcessType): String = v.name

    @TypeConverter
    fun toProcessType(v: String): ProcessType = ProcessType.valueOf(v)
}
