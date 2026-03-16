package com.example.tcgbackrooms

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CardDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        val ivDetail: ImageView = findViewById(R.id.ivDetail)
        val tvDetailName: TextView = findViewById(R.id.tvDetailName)
        val tvDetailRarity: TextView = findViewById(R.id.tvDetailRarity)

        //grab the card data passed from the adapter
        //obtener los datos de la carta pasados desde el adaptador
        val imageFilename = intent.getStringExtra("imageFilename") ?: ""
        val cardName = intent.getStringExtra("cardName") ?: ""
        val rarity = intent.getStringExtra("rarity") ?: ""

        //resolve the drawable name to an actual resource id
        //resolver el nombre del drawable a un id de recurso real
        val resId = resources.getIdentifier(imageFilename, "drawable", packageName)
        if (resId != 0) {
            ivDetail.setImageResource(resId)
        } else {
            //fallback to card back if the image is missing
            //fallback al reverso si falta la imagen
            ivDetail.setImageResource(R.drawable.card_back)
        }

        tvDetailName.text = cardName
        tvDetailRarity.text = rarity

        //set rarity color
        //poner color de rareza
        val rarityColor = when (rarity) {
            "uncommon" -> R.color.rarity_uncommon
            "rare" -> R.color.rarity_rare
            "legendary" -> R.color.rarity_legendary
            else -> R.color.rarity_common
        }
        tvDetailRarity.setTextColor(getColor(rarityColor))

        //tapping anywhere on the screen closes this activity
        //tocar cualquier parte de la pantalla cierra esta actividad
        ivDetail.setOnClickListener { finish() }
    }
}