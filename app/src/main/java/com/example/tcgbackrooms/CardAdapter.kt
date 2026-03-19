package com.example.tcgbackrooms

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class CardAdapter(
    private val context: Context,
    private val items: MutableList<CollectionItem>,
    //callback so the fragment can update the db when a card is discarded
    //callback para que el fragmento actualice la db cuando se descarta una carta
    private val onDiscard: (Card) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    //the adapter works with a sealed class so it can hold both headers and cards in one list
    //el adaptador usa una sealed class para poder tener cabeceras y cartas en una misma lista
    sealed class CollectionItem {
        data class Header(val title: String) : CollectionItem()
        data class CardItem(val card: Card) : CollectionItem()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CARD = 1
    }

    //view holders
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeader: TextView = itemView.findViewById(R.id.sectionHeader)
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
        val cardName: TextView = itemView.findViewById(R.id.cardName)
        val rarityLabel: TextView = itemView.findViewById(R.id.rarityLabel)
        val countLabel: TextView = itemView.findViewById(R.id.countLabel)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CollectionItem.Header -> VIEW_TYPE_HEADER
            is CollectionItem.CardItem -> VIEW_TYPE_CARD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_header, parent, false)
                HeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false)
                CardViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CollectionItem.Header -> {
                (holder as HeaderViewHolder).tvHeader.text = item.title
            }
            is CollectionItem.CardItem -> {
                bindCard(holder as CardViewHolder, item.card)
            }
        }
    }

    //handles all the card binding logic
    //maneja toda la logica de binding de la carta
    private fun bindCard(holder: CardViewHolder, card: Card) {
        if (card.unlocked) {
            loadCardImage(holder.cardImage, card.imageFilename)

            holder.cardName.text = card.name
            holder.rarityLabel.text = card.rarity

            //show count badge if the user has more than one copy
            //mostrar insignia de cantidad si el usuario tiene mas de una copia
            if (card.count > 1) {
                holder.countLabel.visibility = View.VISIBLE
                holder.countLabel.text = "x${card.count}"
            } else {
                holder.countLabel.visibility = View.GONE
            }

            //set rarity color on the label
            //poner color de rareza en la etiqueta
            val rarityColor = when (card.rarity) {
                "uncommon" -> R.color.rarity_uncommon
                "rare" -> R.color.rarity_rare
                "legendary" -> R.color.rarity_legendary
                else -> R.color.rarity_common
            }
            holder.rarityLabel.setTextColor(context.getColor(rarityColor))

            //tap to open fullscreen detail
            //tap para abrir detalle en pantalla completa
            holder.itemView.setOnClickListener {
                val intent = Intent(context, CardDetailActivity::class.java)
                intent.putExtra("imageFilename", card.imageFilename)
                intent.putExtra("cardName", card.name)
                intent.putExtra("rarity", card.rarity)
                context.startActivity(intent)
            }

            //long press to discard one copy
            //pulsacion larga para descartar una copia
            holder.itemView.setOnLongClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Discard card?")
                    .setMessage("Remove one copy of ${card.name} from your collection?")
                    .setPositiveButton("Discard") { _, _ -> onDiscard(card) }
                    .setNegativeButton("Cancel", null)
                    .show()
                true
            }

        } else {
            //locked card - show back and hide everything personal
            //carta bloqueada - mostrar reverso y ocultar todo lo personal
            holder.cardImage.setImageResource(R.drawable.card_back)
            holder.cardName.text = "???"
            holder.rarityLabel.text = "???"
            holder.rarityLabel.setTextColor(context.getColor(R.color.rarity_common))
            holder.countLabel.visibility = View.GONE

            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        }
    }

    //resolves an image filename to an actual image on the imageview
    //resuelve un nombre de imagen a una imagen real en el imageview
    //supports both bundled drawables and absolute file paths from the gallery
    //soporta tanto drawables del proyecto como rutas absolutas de la galeria
    private fun loadCardImage(imageView: ImageView, imageFilename: String) {
        when {
            //absolute path means it was picked from the gallery and copied to internal storage
            //ruta absoluta significa que fue elegida de la galeria y copiada al almacenamiento interno
            imageFilename.startsWith("/") -> {
                val bitmap = BitmapFactory.decodeFile(imageFilename)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    //file is missing or unreadable, fall back to card back
                    //el archivo falta o no se puede leer, usar el reverso como fallback
                    imageView.setImageResource(R.drawable.card_back)
                }
            }
            //otherwise try to resolve it as a drawable resource name
            //si no, intentar resolverlo como nombre de recurso drawable
            else -> {
                val resId = context.resources.getIdentifier(imageFilename, "drawable", context.packageName)
                imageView.setImageResource(if (resId != 0) resId else R.drawable.card_back)
            }
        }
    }

    override fun getItemCount(): Int = items.size
}