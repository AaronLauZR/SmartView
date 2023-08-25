package my.edu.tarc.smartview.ui.news.db

import androidx.room.TypeConverter
import my.edu.tarc.smartview.ui.news.models.Source

class Converters {

    @TypeConverter
    fun fromSource(source: Source): String {
        return source.name
    }

    @TypeConverter
    fun toSource(name: String): Source {
        return Source(name, name)
    }
}