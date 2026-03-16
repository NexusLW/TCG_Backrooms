package com.example.tcgbackrooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private lateinit var db: DatabaseHelper

    private lateinit var btnPack1: Button
    private lateinit var btnPack2: Button
    private lateinit var btnPack3: Button
    private lateinit var tvPacksInfo: TextView
    private lateinit var tvResult: TextView

    //shared prefs keys for tracking daily pack usage
    //claves de shared prefs para rastrear el uso diario de sobres
    private val PREFS_NAME = "backrooms_prefs"
    private val KEY_DATE = "last_pack_date"
    private val KEY_PACKS_OPENED = "packs_opened_today"

    //rarity weights - these add up to 100 so we can treat them as percentages
    //pesos de rareza - suman 100 para tratarlos como porcentajes
    private val WEIGHT_COMMON = 60
    private val WEIGHT_UNCOMMON = 25
    private val WEIGHT_RARE = 12
    private val WEIGHT_LEGENDARY = 3

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper(requireContext())

        btnPack1 = view.findViewById(R.id.btnPack1)
        btnPack2 = view.findViewById(R.id.btnPack2)
        btnPack3 = view.findViewById(R.id.btnPack3)
        tvPacksInfo = view.findViewById(R.id.tvPacksInfo)
        tvResult = view.findViewById(R.id.tvResult)

        updatePackButtons()

        btnPack1.setOnClickListener { openPack(1) }
        btnPack2.setOnClickListener { openPack(2) }
        btnPack3.setOnClickListener { openPack(3) }
    }

    //refresh button states when coming back to this tab
    //actualizar estado de botones al volver a esta pestana
    override fun onResume() {
        super.onResume()
        updatePackButtons()
    }

    //returns today as "yyyy-MM-dd"
    //devuelve el dia de hoy como "yyyy-MM-dd"
    private fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    //resets the pack counter if the day has changed since last time
    //reinicia el contador si el dia ha cambiado desde la ultima vez
    private fun checkDateReset() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        val savedDate = prefs.getString(KEY_DATE, "")
        val today = getTodayDate()

        if (savedDate != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_PACKS_OPENED, 0)
                .apply()
        }
    }

    private fun getPacksOpenedToday(): Int {
        checkDateReset()
        return requireContext().getSharedPreferences(PREFS_NAME, 0)
            .getInt(KEY_PACKS_OPENED, 0)
    }

    private fun incrementPacksOpened() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        val current = prefs.getInt(KEY_PACKS_OPENED, 0)
        prefs.edit().putInt(KEY_PACKS_OPENED, current + 1).apply()
    }

    //enables or disables buttons based on how many packs were opened today
    //activa o desactiva botones segun cuantos sobres se han abierto hoy
    private fun updatePackButtons() {
        val opened = getPacksOpenedToday()

        btnPack1.isEnabled = opened < 1
        btnPack2.isEnabled = opened < 2
        btnPack3.isEnabled = opened < 3

        val remaining = 3 - opened
        tvPacksInfo.text = if (remaining > 0) "$remaining packs remaining today" else "come back tomorrow..."

        if (opened >= 3) tvResult.text = ""
    }

    //picks a rarity based on the weights defined above
    //elige una rareza basandose en los pesos definidos arriba
    private fun pickRarity(): String {
        val roll = (1..100).random()
        return when {
            roll <= WEIGHT_COMMON -> "common"
            roll <= WEIGHT_COMMON + WEIGHT_UNCOMMON -> "uncommon"
            roll <= WEIGHT_COMMON + WEIGHT_UNCOMMON + WEIGHT_RARE -> "rare"
            else -> "legendary"
        }
    }

    //picks one card from the pool using rarity weights
    //elige una carta del pool usando pesos de rareza
    //if there are no cards of the rolled rarity it falls back to common
    //si no hay cartas de la rareza elegida cae a common
    private fun pickCard(allCards: List<Card>): Card {
        val rarity = pickRarity()
        val pool = allCards.filter { it.rarity == rarity }

        return if (pool.isNotEmpty()) {
            pool.random()
        } else {
            //fallback - just pick any card at random
            //fallback - elegir cualquier carta al azar
            allCards.random()
        }
    }

    //opens a pack - picks 5 cards using rarity weights and adds them to the user's collection
    //abre un sobre - elige 5 cartas con pesos de rareza y las anade a la coleccion del usuario
    private fun openPack(packNumber: Int) {
        val userId = (activity as MainActivity).userId
        val allCards = db.getAllCards()

        if (allCards.isEmpty()) {
            tvResult.text = "no cards found. check cards.json"
            return
        }

        val picked = mutableListOf<Card>()
        repeat(5) {
            picked.add(pickCard(allCards))
        }

        //add each picked card to the user's collection in the db
        //anadir cada carta elegida a la coleccion del usuario en la db
        for (card in picked) {
            db.addCardToUser(userId, card.id)
        }

        //show what was found, including if a card appeared more than once in this pack
        //mostrar lo que se encontro, incluyendo si una carta aparecio mas de una vez en este sobre
        val resultText = picked.joinToString("\n") { card ->
            val rarityTag = "[${card.rarity}]"
            "${card.name} $rarityTag"
        }
        tvResult.text = "you found:\n$resultText"

        incrementPacksOpened()
        updatePackButtons()
    }
}