package com.example.tcgbackrooms

//data class representing a card in the game
//clase de datos que representa una carta del juego
data class Card(
    val id: Int,
    val name: String,
    //drawable resource name, e.g. "card_lobby"
    //nombre del recurso drawable, ej. "card_lobby"
    val imageFilename: String,
    //rarity: common, uncommon, rare, legendary
    //rareza: common, uncommon, rare, legendary
    val rarity: String,
    //category: levels, entities, items - used for collection grouping
    //categoria: levels, entities, items - usado para agrupar en la coleccion
    val category: String,
    //how many copies the user has, 0 means locked
    //cuantas copias tiene el usuario, 0 significa bloqueada
    var count: Int = 0
) {
    //helper so we don't have to check count > 0 everywhere
    //ayuda para no tener que comprobar count > 0 en todos lados
    val unlocked: Boolean get() = count > 0
}