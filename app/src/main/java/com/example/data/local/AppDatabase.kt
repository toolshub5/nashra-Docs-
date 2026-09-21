package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.DocumentEntity
import com.example.data.model.DocumentTagCrossRef
import com.example.data.model.HighlightEntity
import com.example.data.model.OcrEntity
import com.example.data.model.SignatureEntity
import com.example.data.model.TagEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        DocumentEntity::class,
        HighlightEntity::class,
        SignatureEntity::class,
        OcrEntity::class,
        TagEntity::class,
        DocumentTagCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nashra_docs_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val dao = getDatabase(context).documentDao()
                                val defaultTags = listOf(
                                    TagEntity(name = "مهم / Urgent", colorHex = "#EF4444"),
                                    TagEntity(name = "عمل / Work", colorHex = "#0F766E"),
                                    TagEntity(name = "شخصي / Personal", colorHex = "#3B82F6"),
                                    TagEntity(name = "مالي / Finance", colorHex = "#10B981"),
                                    TagEntity(name = "دراسة / Study", colorHex = "#8B5CF6"),
                                    TagEntity(name = "عقود / Contracts", colorHex = "#F59E0B")
                                )
                                defaultTags.forEach { tag ->
                                    try {
                                        dao.insertTag(tag)
                                    } catch (_: Exception) {}
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
