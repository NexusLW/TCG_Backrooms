package com.example.tcgbackrooms

import android.annotation.SuppressLint
import android.content.Context
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

    private val itemList = mutableListOf<CardAdapter.CollectionItem>()
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

        val layoutManager = GridLayoutManager(requireContext(), 3)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    0 -> 3
                    else -> 1
                }
            }
        }

        recyclerView.layoutManager = layoutManager

        adapter = CardAdapter(requireContext(), itemList) { card ->
            val userId = (activity as MainActivity).userId
            db.removeCardFromUser(userId, card.id)
            loadCards()
        }

        recyclerView.adapter = adapter
        loadCards()
    }

    override fun onResume() {
        super.onResume()
        loadCards()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadCards() {
        val userId = (activity as MainActivity).userId
        val allCards = db.getAllCardsWithCount(userId)

        val sort = requireContext()
            .getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("collection_sort", "category")

        itemList.clear()

        when (sort) {
            "name" -> {
                //flat list sorted alphabetically, single header
                //lista plana ordenada alfabeticamente, una sola cabecera
                itemList.add(CardAdapter.CollectionItem.Header("All Cards"))
                allCards.sortedBy { it.name }.forEach {
                    itemList.add(CardAdapter.CollectionItem.CardItem(it))
                }
            }
            "rarity" -> {
                //grouped by rarity in ascending order
                //agrupadas por rareza en orden ascendente
                val rarityOrder = listOf("common", "uncommon", "rare", "legendary")
                val grouped = allCards.groupBy { it.rarity }
                for (rarity in rarityOrder) {
                    val cards = grouped[rarity] ?: continue
                    itemList.add(CardAdapter.CollectionItem.Header(rarity.replaceFirstChar { it.uppercase() }))
                    cards.forEach { itemList.add(CardAdapter.CollectionItem.CardItem(it)) }
                }
            }
            else -> {
                //default: grouped by category
                //por defecto: agrupadas por categoria
                val grouped = allCards.groupBy { it.category }
                for (category in categoryOrder) {
                    val cards = grouped[category] ?: continue
                    itemList.add(CardAdapter.CollectionItem.Header(category.replaceFirstChar { it.uppercase() }))
                    cards.forEach { itemList.add(CardAdapter.CollectionItem.CardItem(it)) }
                }
            }
        }

        adapter.notifyDataSetChanged()
    }
}