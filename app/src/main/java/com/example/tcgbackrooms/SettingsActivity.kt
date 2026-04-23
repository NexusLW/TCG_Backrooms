package com.example.tcgbackrooms

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val sortGroup = findViewById<RadioGroup>(R.id.sortGroup)

        //restore saved preference
        //restaurar preferencia guardada
        val current = prefs.getString("collection_sort", "category")
        sortGroup.check(
            when (current) {
                "name" -> R.id.sortByName
                "rarity" -> R.id.sortByRarity
                else -> R.id.sortByCategory
            }
        )

        findViewById<Button>(R.id.saveButton).setOnClickListener {
            val selected = when (sortGroup.checkedRadioButtonId) {
                R.id.sortByName -> "name"
                R.id.sortByRarity -> "rarity"
                else -> "category"
            }
            prefs.edit().putString("collection_sort", selected).apply()
            finish()
        }
    }
}