package com.example.tcgbackrooms

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CardDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)

        val detailImage: ImageView = findViewById(R.id.detailImage)
        val detailName: TextView = findViewById(R.id.detailName)
        val detailRarity: TextView = findViewById(R.id.detailRarity)

        //grab the card data passed from the adapter
        //obtener los datos de la carta pasados desde el adaptador
        val imageFilename = intent.getStringExtra("imageFilename") ?: ""
        val cardName = intent.getStringExtra("cardName") ?: ""
        val rarity = intent.getStringExtra("rarity") ?: ""

        //custom cards store an absolute file path
        //while built in cards store a drawable resource name(for example "card_lobby")
        //this handles both cases, same as CardAdapter.loadCardImage does
        //las cartas personalizadas guardan una ruta absoluta
        //mientras las cartas predefinidas guardan un nombre de recurso drawable(por ejemplo "card_lobby")
        //esto maneja ambos casos, igual que hace CardAdapter.loadCardImage
        if (imageFilename.startsWith("/")) {
            //absolute path - decode the file directly from internal storage
            //ruta absoluta - decodificar el archivo directamente del almacenamiento interno
            val bitmap = BitmapFactory.decodeFile(imageFilename)
            if (bitmap != null) {
                detailImage.setImageBitmap(bitmap)
            } else {
                //file is missing or unreadable, fall back to card back
                //el archivo falta o no se puede leer, usar el reverso como fallback
                detailImage.setImageResource(R.drawable.card_back)
            }
        } else {
            //drawable resource name: resolve it to a resource id at runtime
            //nombre de recurso drawable: resolver a un id de recurso en tiempo de ejecucion
            val resId = resources.getIdentifier(imageFilename, "drawable", packageName)
            if (resId != 0) {
                detailImage.setImageResource(resId)
            } else {
                //drawable name not found in the project, fall back to card back
                //nombre de drawable no encontrado en el proyecto, usar el reverso como fallback
                detailImage.setImageResource(R.drawable.card_back)
            }
        }

        detailName.text = cardName
        detailRarity.text = rarity

        //set rarity color
        //poner color de rareza
        val rarityColor = when (rarity) {
            "uncommon" -> R.color.rarity_uncommon
            "rare" -> R.color.rarity_rare
            "legendary" -> R.color.rarity_legendary
            else -> R.color.rarity_common
        }
        detailRarity.setTextColor(getColor(rarityColor))

        //tapping anywhere on the screen closes this activity
        //tocar cualquier parte de la pantalla cierra esta actividad
        detailImage.setOnClickListener { finish() }
    }
}