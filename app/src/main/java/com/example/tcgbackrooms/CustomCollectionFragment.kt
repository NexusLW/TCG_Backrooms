package com.example.tcgbackrooms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CustomCollectionFragment : Fragment() {

    private lateinit var db: DatabaseHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private lateinit var fab: FloatingActionButton

    //the flat list the adapter reads from, mix of headers and cards
    //la lista plana que lee el adaptador, mezcla de cabeceras y cartas
    private val itemList = mutableListOf<CardAdapter.CollectionItem>()

    //launcher that listens for the result of CreateCardActivity
    //lanzador que escucha el resultado de CreateCardActivity
    //if the user saved a card we reload the list, otherwise we do nothing
    //si el usuario guardo una carta recargamos la lista, si no no hacemos nada
    private val createCardLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadCards()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_custom_collection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper(requireContext())
        recyclerView = view.findViewById(R.id.recyclerViewCustom)
        fab = view.findViewById(R.id.fabCreateCard)

        //grid with 3 columns, headers span all 3
        //rejilla de 3 columnas, las cabeceras ocupan las 3
        val layoutManager = GridLayoutManager(requireContext(), 3)
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
            //runs when the user confirms discard - removes one copy from db then refreshes
            //se ejecuta cuando el usuario confirma descarte - elimina una copia de la db y refresca
            val userId = (activity as MainActivity).userId
            db.removeCardFromUser(userId, card.id)
            loadCards()
        }
        recyclerView.adapter = adapter

        //fab opens the create card screen passing the current user id
        //el fab abre la pantalla de crear carta pasando el id del usuario actual
        fab.setOnClickListener {
            val intent = Intent(requireContext(), CreateCardActivity::class.java)
            intent.putExtra("userId", (activity as MainActivity).userId)
            createCardLauncher.launch(intent)
        }

        loadCards()
    }

    //refresh every time we come back to this tab
    //refrescar cada vez que volvemos a esta pestana
    override fun onResume() {
        super.onResume()
        loadCards()
    }

    //pulls all custom cards for this user from the db and rebuilds the adapter list
    //obtiene todas las cartas personalizadas del usuario desde la db y reconstruye la lista del adaptador
    private fun loadCards() {
        val userId = (activity as MainActivity).userId
        val cards = db.getCustomCardsForUser(userId)

        itemList.clear()

        if (cards.isNotEmpty()) {
            //single header for the whole custom section
            //cabecera unica para toda la seccion personalizada
            itemList.add(CardAdapter.CollectionItem.Header("My Cards"))
            cards.forEach { card ->
                itemList.add(CardAdapter.CollectionItem.CardItem(card))
            }
        }

        adapter.notifyDataSetChanged()
    }
}