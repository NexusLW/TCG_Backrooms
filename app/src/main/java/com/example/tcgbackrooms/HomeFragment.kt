package com.example.tcgbackrooms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.TranslateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

//tab for opening daily packs and viewing the cards pulled from each pack
//pestana para abrir paquetes diarios y ver las cartas extraidas de cada paquete
class HomeFragment : Fragment() {

    private lateinit var db: DatabaseHelper

    //UI views for pack display and card reveal screens
    //vistas de UI para pantalla de paquetes y revelacion de cartas
    private lateinit var packsContainer: LinearLayout
    private lateinit var cardRevealContainer: LinearLayout
    private lateinit var packBig: FrameLayout
    private lateinit var tvPacksInfo: TextView
    private lateinit var tvPacksStatus: TextView
    private lateinit var cardStack: FrameLayout
    private lateinit var tvSwipeHint: TextView
    private lateinit var tvCardCounter: TextView
    private lateinit var btnBackToPacks: View

    //rarity distribution weights - add up to 100 to use as percentages
    //pesos de distribucion de rareza - suman 100 para usar como porcentajes
    private val WEIGHT_COMMON = 80
    private val WEIGHT_UNCOMMON = 12
    private val WEIGHT_RARE = 7
    private val WEIGHT_LEGENDARY = 1

    //tracks which card is currently visible (0-4)
    //rastrea cual carta es actualmente visible (0-4)
    private var currentCardIndex = 0
    //stores the 5 cards picked from a pack, cleared after closing reveal screen
    //almacena las 5 cartas elegidas de un paquete, se limpia al cerrar la pantalla
    private var pickedCards = mutableListOf<Card>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = DatabaseHelper(requireContext())

        //bind all UI elements from the layout
        //vincular todos los elementos de UI del layout
        packsContainer = view.findViewById(R.id.packsContainer)
        cardRevealContainer = view.findViewById(R.id.cardRevealContainer)
        packBig = view.findViewById(R.id.packBig)
        tvPacksInfo = view.findViewById(R.id.tvPacksInfo)
        tvPacksStatus = view.findViewById(R.id.tvPacksStatus)
        cardStack = view.findViewById(R.id.cardStack)
        tvSwipeHint = view.findViewById(R.id.tvSwipeHint)
        tvCardCounter = view.findViewById(R.id.tvCardCounter)
        btnBackToPacks = view.findViewById(R.id.btnBackToPacks)

        //display initial pack count based on current user's regeneration
        //mostrar conteo inicial de paquetes basado en regeneracion del usuario actual
        updatePackButtons()

        //clicking the pack initiates shake + open sequence
        //hacer clic en el paquete inicia la secuencia de sacudida + apertura
        packBig.setOnClickListener { openPackSequence() }

        //tapping any card in reveal screen shows the next one
        //tocar cualquier carta en la pantalla de revelacion muestra la siguiente
        cardStack.setOnClickListener { nextCard() }

        //back button returns to pack screen and resets state
        //boton atras vuelve a la pantalla de paquetes y reinicia el estado
        btnBackToPacks.setOnClickListener { backToPacks() }
    }

    //refresh pack display when returning to this tab (packs may have regenerated)
    //refrescar vista de paquetes al volver a esta pestana (los paquetes pueden haberse regenerado)
    override fun onResume() {
        super.onResume()
        updatePackButtons()
    }

    //checks database for available packs and updates button state and text
    //comprueba la db para paquetes disponibles y actualiza estado del boton y texto
    private fun updatePackButtons() {
        val userId = (activity as MainActivity).userId
        val packs = db.getPacksRemaining(userId)

        packBig.isEnabled = packs > 0

        //visual feedback: pack fades out when depleted
        //retroalimentacion visual: el paquete se desvanece cuando se agota
        packBig.alpha = if (packs > 0) 1.0f else 0.4f

        tvPacksInfo.text = if (packs > 0) "$packs packs remaining" else "come back later..."
    }

    //initiates pack opening: shake animation followed by card reveal
    //inicia apertura de paquete: animacion de sacudida seguida de revelacion de cartas
    private fun openPackSequence() {
        val packView = packBig

        //shake animation - 11 back and forth movements in 550ms total
        //animacion de sacudida - 11 movimientos de ida y vuelta en 550ms total
        val shake = TranslateAnimation(0f, 10f, 0f, 0f).apply {
            duration = 50
            repeatCount = 10
            repeatMode = android.view.animation.Animation.REVERSE
        }

        packView.startAnimation(shake)

        //after animation completes, open the pack and query database
        //despues de que se complete la animacion, abrir el paquete y consultar la db
        packView.postDelayed({
            openPack()
        }, 550)
    }

    //core pack opening logic: deduct pack from user, pick 5 cards, add to collection
    //logica principal de apertura: deducir paquete del usuario, elegir 5 cartas, anadir a coleccion
    private fun openPack() {
        val userId = (activity as MainActivity).userId

        //try to consume a pack from the user's account (respects hourly regeneration)
        //intentar consumir un paquete de la cuenta del usuario (respeta regeneracion por hora)
        if (!db.usePackForUser(userId)) {
            tvPacksStatus.text = "no packs available"
            return
        }

        val allCards = db.getAllCards()

        if (allCards.isEmpty()) {
            tvPacksStatus.text = "no cards found. check cards.json"
            return
        }

        //pick 5 cards using rarity-weighted random selection
        //elegir 5 cartas usando seleccion aleatoria ponderada por rareza
        pickedCards.clear()
        repeat(5) {
            pickedCards.add(pickCard(allCards))
        }

        //insert each card into the user's collection
        //insertar cada carta en la coleccion del usuario
        for (card in pickedCards) {
            db.addCardToUser(userId, card.id)
        }

        //transition to card reveal screen
        //transicion a pantalla de revelacion de cartas
        showCardReveal()
    }

    //rolls a random number to determine which rarity tier to pull from
    //lanza un numero aleatorio para determinar de cual nivel de rareza extraer
    private fun pickRarity(): String {
        val roll = (1..100).random()
        return when {
            roll <= WEIGHT_COMMON -> "common"
            roll <= WEIGHT_COMMON + WEIGHT_UNCOMMON -> "uncommon"
            roll <= WEIGHT_COMMON + WEIGHT_UNCOMMON + WEIGHT_RARE -> "rare"
            else -> "legendary"
        }
    }

    //picks a card from the rarity pool; if pool is empty, picks any random card
    //elige una carta del pool de rareza; si pool esta vacio, elige cualquier carta aleatoria
    private fun pickCard(allCards: List<Card>): Card {
        val rarity = pickRarity()
        val pool = allCards.filter { it.rarity == rarity }

        return if (pool.isNotEmpty()) {
            pool.random()
        } else {
            //fallback if the rolled rarity has no cards in the database
            //fallback si la rareza elegida no tiene cartas en la db
            allCards.random()
        }
    }

    //prepares the card reveal UI: creates 5 stacked imageviews, shows only current index
    //prepara la UI de revelacion: crea 5 imageviews apiladas, muestra solo el indice actual
    private fun showCardReveal() {
        currentCardIndex = 0
        packsContainer.visibility = View.GONE
        cardRevealContainer.visibility = View.VISIBLE

        cardStack.removeAllViews()

        //create 5 card imageviews stacked in the framelayout (but only current one visible)
        //crear 5 imageviews apiladas en el framelayout (pero solo la actual es visible)
        for (i in 0 until 5) {
            val cardImageView = ImageView(requireContext())
            cardImageView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            cardImageView.scaleType = ImageView.ScaleType.FIT_CENTER
            cardImageView.tag = i //tag to track which card this is

            //load card image from drawable resource or gallery file path
            //cargar imagen de la carta desde recurso drawable o ruta de archivo de galeria
            val card = pickedCards[i]
            loadCardImage(cardImageView, card.imageFilename)

            cardStack.addView(cardImageView)
        }

        //show the first card and update counter
        //mostrar la primera carta y actualizar contador
        updateCardDisplay()
    }

    //loads a card image from either a drawable resource or an internal storage file path
    //carga imagen de carta desde recurso drawable o ruta de archivo de almacenamiento interno
    private fun loadCardImage(imageView: ImageView, imageFilename: String) {
        when {
            //if path starts with /, it's a custom card image from gallery (internal storage)
            //si la ruta comienza con /, es una imagen de carta personalizada de galeria (almacenamiento interno)
            imageFilename.startsWith("/") -> {
                val bitmap = android.graphics.BitmapFactory.decodeFile(imageFilename)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                } else {
                    //fallback if file is corrupted or missing
                    //fallback si el archivo esta corrupto o falta
                    imageView.setImageResource(R.drawable.card_back)
                }
            }
            //otherwise resolve the filename as a drawable resource name
            //si no, resolver el nombre de archivo como nombre de recurso drawable
            else -> {
                val resId = requireContext().resources.getIdentifier(imageFilename, "drawable", requireContext().packageName)
                imageView.setImageResource(if (resId != 0) resId else R.drawable.card_back)
            }
        }
    }

    //updates card display: only current index visible, updates counter and hint visibility
    //actualiza visualizacion de carta: solo el indice actual visible, actualiza contador y visibilidad de pista
    private fun updateCardDisplay() {
        //hide all cards except current
        //ocultar todas las cartas excepto la actual
        for (i in 0 until cardStack.childCount) {
            cardStack.getChildAt(i).visibility = if (i == currentCardIndex) View.VISIBLE else View.GONE
        }

        tvCardCounter.text = "${currentCardIndex + 1} / 5"

        //hide tap hint on last card since there's no next card
        //ocultar pista de tap en la ultima carta ya que no hay siguiente
        tvSwipeHint.visibility = if (currentCardIndex == 4) View.GONE else View.VISIBLE
    }

    //advances to next card if available (max is 4, so 5 total cards)
    //avanza a la siguiente carta si esta disponible (maximo 4, entonces 5 cartas totales)
    private fun nextCard() {
        if (currentCardIndex < 4) {
            currentCardIndex++
            updateCardDisplay()
        }
    }

    //returns to pack screen and clears card state
    //vuelve a la pantalla de paquetes y limpia el estado de cartas
    private fun backToPacks() {
        currentCardIndex = 0
        pickedCards.clear()
        cardRevealContainer.visibility = View.GONE
        packsContainer.visibility = View.VISIBLE
        updatePackButtons()
    }

}