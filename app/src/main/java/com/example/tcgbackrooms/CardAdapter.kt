package com.example.tcgbackrooms

import android.content.Context
import android.content.Intent
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

    //-------View holders / View holders-------

    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHeader: TextView = itemView.findViewById(R.id.tvSectionHeader)
    }

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCard: ImageView = itemView.findViewById(R.id.ivCard)
        val tvCardName: TextView = itemView.findViewById(R.id.tvCardName)
        val tvRarity: TextView = itemView.findViewById(R.id.tvRarity)
        val tvCount: TextView = itemView.findViewById(R.id.tvCount)
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
            //resolve drawable name to actual resource id
            //resolver nombre del drawable a id de recurso real
            val resId = context.resources.getIdentifier(card.imageFilename, "drawable", context.packageName)
            holder.ivCard.setImageResource(if (resId != 0) resId else R.drawable.card_back)

            holder.tvCardName.text = card.name
            holder.tvRarity.text = card.rarity

            //show count badge if the user has more than one copy
            //mostrar insignia de cantidad si el usuario tiene mas de una copia
            if (card.count > 1) {
                holder.tvCount.visibility = View.VISIBLE
                holder.tvCount.text = "x${card.count}"
            } else {
                holder.tvCount.visibility = View.GONE
            }

            //set rarity color on the label
            //poner color de rareza en la etiqueta
            val rarityColor = when (card.rarity) {
                "uncommon" -> R.color.rarity_uncommon
                "rare" -> R.color.rarity_rare
                "legendary" -> R.color.rarity_legendary
                else -> R.color.rarity_common
            }
            holder.tvRarity.setTextColor(context.getColor(rarityColor))

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
            holder.ivCard.setImageResource(R.drawable.card_back)
            holder.tvCardName.text = "???"
            holder.tvRarity.text = "???"
            holder.tvRarity.setTextColor(context.getColor(R.color.rarity_common))
            holder.tvCount.visibility = View.GONE

            holder.itemView.setOnClickListener(null)
            holder.itemView.setOnLongClickListener(null)
        }
    }

    override fun getItemCount(): Int = items.size
}