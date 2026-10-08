package com.lucyprockova.alice.data

import android.content.Context
import android.content.res.Configuration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lucyprockova.alice.R
import com.lucyprockova.alice.ui.Pronouns
import java.util.Locale

/** local sqlite db, made once, shared by every screen */
@Database(
    entities = [Step::class, Entry::class, CheckInDay::class, WeeklyDone::class, Checkup::class, CheckupAnswer::class, SlipReason::class],
    version = 3,
    // schema json kept in app/schemas, so migrations can be tested
    exportSchema = true,
)
abstract class AliceDatabase : RoomDatabase() {
    abstract fun dao(): AliceDao

    companion object {
        /** default slip reasons, name string + icon, names stored as text so they come from resources */
        private val DEFAULT_REASON_RES = listOf(
            R.string.core_reason_too_tired to "bedtime", R.string.core_reason_craving to "cookie",
            R.string.core_reason_no_time to "schedule", R.string.core_reason_forgot to "help",
            R.string.core_reason_no_alternative to "storefront", R.string.core_reason_social to "group",
            R.string.core_reason_impulse to "bolt", R.string.core_reason_too_expensive to "payments",
            R.string.core_reason_away to "flight",
        )

        /** default reasons in the app's current language */
        fun defaultReasons(context: Context): List<SlipReason> =
            DEFAULT_REASON_RES.mapIndexed { i, (res, icon) -> SlipReason(Pronouns.string(context, res), icon, i) }

        /** default reason name to icon, current language and english both, old english rows keep their icon */
        fun defaultReasonIcons(context: Context): Map<String, String> {
            val en = Configuration(context.resources.configuration).apply { setLocale(Locale.ENGLISH) }
            val english = context.createConfigurationContext(en)
            return DEFAULT_REASON_RES.flatMap { (res, icon) -> listOf(english.getString(res) to icon, context.getString(res) to icon) }.toMap()
        }

        /** v3 keeps old schedules on edit, adds columns, keeps every step and tick */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // add columns only, no rebuild, so nothing lost
                db.execSQL("ALTER TABLE steps ADD COLUMN scheduleFromEpochDay INTEGER")
                // not null needs default, old rows get empty history
                db.execSQL("ALTER TABLE steps ADD COLUMN pastSchedules TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile private var instance: AliceDatabase? = null

        fun get(context: Context): AliceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AliceDatabase::class.java, "alice.db")
                    .addMigrations(MIGRATION_2_3)
                    // only v1 (test phones) may be rebuilt, a later version without a migration fails loudly instead of wiping habits
                    .fallbackToDestructiveMigrationFrom(true, 1)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            defaultReasons(context).forEach {
                                db.execSQL("INSERT INTO slip_reasons(name, icon, position) VALUES(?, ?, ?)", arrayOf<Any>(it.name, it.icon, it.position))
                            }
                        }
                    })
                    .build()
                    .also { instance = it }
            }
    }
}
