package com.example.cet6vocabulary.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cet6vocabulary.data.local.database.AppDatabase
import com.example.cet6vocabulary.data.repository.LearningRecordRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug builds only: pretends the first words were practised so the AI article flow can be
 * exercised on a fresh install. Declared in src/debug/AndroidManifest.xml, never in release.
 *
 *   adb shell am broadcast -a com.example.cet6vocabulary.SEED_RECORDS \
 *       -n com.example.cet6vocabulary/.debug.SeedLearningRecordsReceiver --ei count 3
 */
class SeedLearningRecordsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val count = intent.getIntExtra("count", 3).coerceIn(1, 50)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val records = LearningRecordRepository(
                    AppDatabase.getInstance(context.applicationContext).learningRecordDao()
                )
                (1..count).forEach { records.recordCorrect(it) }
                Log.i("CET6_SEED", "seeded $count learning records")
            } catch (error: Exception) {
                Log.e("CET6_SEED", "seeding failed", error)
            } finally {
                pending.finish()
            }
        }
    }
}
