package com.example.tcgbackrooms

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CollectionFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter

    //a flat list the adapter reads from, mix of headers and cards
    //una lista plana que lee el adaptador, mezcla de cabeceras y cartas
    private val itemList = mutableListOf<CardAdapter.CollectionItem>()

    //the order the sections to appear in
    //el orden en que aparezcan las secciones
    private val categoryOrder = listOf("levels", "entities", "items")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper(requireContext())
        recyclerView = view.findViewById(R.id.recyclerView)

        //grid with 3 columns, headers span all 3 columns
        //rejilla de 3 columnas, cabeceras ocupa las 3 columnas
        val layoutManager = GridLayoutManager(requireContext(), 3)

        //tell the layout manager that headers span the full width
        //decirle al layout manager que las cabeceras ocupan todo el ancho
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    0 -> 3 //header takes all 3 columns / la cabecera ocupa las 3 columnas
                    else -> 1 //card takes 1 column / la carta ocupa 1 columna
                }
            }
        }

        recyclerView.layoutManager = layoutManager

        adapter = CardAdapter(requireContext(), itemList) { card ->
            //runs when user confirms discard: removes one copy from db then refreshes
            //se ejecuta cuando el usuario confirma descarte: elimina una copia de la db y refresca
            val userId = (activity as MainActivity).userId
            db.removeCardFromUser(userId, card.id)
            loadCards()
        }

        recyclerView.adapter = adapter
        loadCards()
    }

    //refresh every time we come back to this tab
    //refrescar cada vez que volvemos a esta pestana
    override fun onResume() {
        super.onResume()
        loadCards()
    }

    //builds the sectioned list from the db data and notifies the adapter
    //construye la lista por secciones desde la db y notifica al adaptador
    @SuppressLint("NotifyDataSetChanged")
    private fun loadCards() {
        val userId = (activity as MainActivity).userId
        val allCards = db.getAllCardsWithCount(userId)

        //group cards by category
        //agrupar cartas por categoria
        val grouped = allCards.groupBy { it.category }

        itemList.clear()

        //add each category section in the defined order
        //anadir cada seccion de categoria en el orden definido
        for (category in categoryOrder) {
            val cards = grouped[category] ?: continue

            //add the header for this section
            //anadir la cabecera para esta seccion
            val headerTitle = category.replaceFirstChar { it.uppercase() }
            itemList.add(CardAdapter.CollectionItem.Header(headerTitle))

            //add each card under this header
            //anadir cada carta bajo esta cabecera
            for (card in cards) {
                itemList.add(CardAdapter.CollectionItem.CardItem(card))
            }
        }

        adapter.notifyDataSetChanged()
    }
}