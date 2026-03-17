package com.example.tcgbackrooms

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import java.security.MessageDigest

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    //keeping context around so we can read the assets folder later
    //guardamos el contexto para poder leer la carpeta assets despues
    private val ctx = context

    companion object {

        //database name and version
        //nombre y version de la base de datos
        private const val DB_NAME = "backroomsDB"
        private const val DB_VERSION = 1

        //table names
        //nombres de las tablas
        private const val TABLE_USERS = "users"
        private const val TABLE_CARDS = "cards"
        private const val TABLE_USER_CARDS = "user_cards"

        //users columns
        //columnas de la tabla usuarios
        private const val USER_ID = "id"
        private const val USERNAME = "username"
        private const val PASSWORD = "password_md5"

        //cards columns
        //columnas de la tabla cartas
        private const val CARD_ID = "id"
        private const val CARD_NAME = "name"
        private const val CARD_IMAGE = "image_filename"
        private const val CARD_RARITY = "rarity"
        private const val CARD_CATEGORY = "category"

        //user_cards columns
        //columnas de la tabla cartas del usuario
        private const val UC_ID = "id"
        private const val UC_USER_ID = "user_id"
        private const val UC_CARD_ID = "card_id"
        //count replaces unlocked - 0 means locked, anything above is how many copies
        //count reemplaza unlocked - 0 significa bloqueada, cualquier numero mayor es cuantas copias
        private const val UC_COUNT = "count"
    }

    override fun onCreate(db: SQLiteDatabase) {

        //create users table
        //crear tabla de usuarios
        db.execSQL(
            "CREATE TABLE $TABLE_USERS (" +
                    "$USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$USERNAME TEXT UNIQUE NOT NULL, " +
                    "$PASSWORD TEXT NOT NULL)"
        )

        //create cards table
        //crear tabla de cartas
        db.execSQL(
            "CREATE TABLE $TABLE_CARDS (" +
                    "$CARD_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$CARD_NAME TEXT NOT NULL, " +
                    "$CARD_IMAGE TEXT NOT NULL, " +
                    "$CARD_RARITY TEXT NOT NULL, " +
                    "$CARD_CATEGORY TEXT NOT NULL)"
        )

        //create user_cards table
        //crear tabla user_cards
        db.execSQL(
            "CREATE TABLE $TABLE_USER_CARDS (" +
                    "$UC_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "$UC_USER_ID INTEGER NOT NULL, " +
                    "$UC_CARD_ID INTEGER NOT NULL, " +
                    "$UC_COUNT INTEGER DEFAULT 0)"
        )

        //seed cards from the json file
        //sembrar cartas desde el archivo json
        seedCards(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        //drop everything and start fresh on version bump
        //borrar todo y empezar de cero si sube la version
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_CARDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CARDS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    //reads cards.json from assets and inserts each card into the cards table
    //lee cards.json de assets e inserta cada carta en la tabla cards
    private fun seedCards(db: SQLiteDatabase) {
        try {
            //read the whole json file as a string
            //leer todo el archivo json como string
            val json = ctx.assets.open("cards.json")
                .bufferedReader()
                .use { it.readText() }

            val array = JSONArray(json)

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val cv = ContentValues()
                cv.put(CARD_NAME, obj.getString("name"))
                cv.put(CARD_IMAGE, obj.getString("imageFilename"))
                cv.put(CARD_RARITY, obj.getString("rarity"))
                cv.put(CARD_CATEGORY, obj.getString("category"))
                db.insert(TABLE_CARDS, null, cv)
            }
        } catch (e: Exception) {
            //if the json is missing or broken the cards table just stays empty
            //si el json falta o esta roto la tabla de cartas se queda vacia
            e.printStackTrace()
        }
    }

    //-------User methods / Metodos de usuario-------

    //hashes a string using md5
    //hashea una cadena usando md5
    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val hash = md.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    //registers a new user, returns true if successful, false if username already exists
    //registra un nuevo usuario, devuelve true si tiene exito, false si el nombre ya existe
    fun registerUser(username: String, password: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put(USERNAME, username)
        cv.put(PASSWORD, md5(password))
        val result = db.insert(TABLE_USERS, null, cv)
        //insert returns -1 if it failed due to duplicate username
        //insert devuelve -1 si fallo por nombre de usuario duplicado
        return result != -1L
    }

    //checks login credentials, returns the user id if valid, -1 if not
    //comprueba las credenciales, devuelve el id del usuario si son validas, -1 si no
    fun loginUser(username: String, password: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(USER_ID),
            "$USERNAME=? AND $PASSWORD=?",
            arrayOf(username, md5(password)),
            null, null, null
        )

        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(0)
        }
        cursor.close()
        return userId
    }

    //-------Card methods / Metodos de cartas-------

    //returns all cards in the game with the user's count for each
    //devuelve todas las cartas del juego con el conteo del usuario para cada una
    fun getAllCardsWithCount(userId: Int): List<Card> {
        val list = mutableListOf<Card>()
        val db = readableDatabase

        //join cards with user_cards so we get the count in one query
        //unir cards con user_cards para obtener el conteo en una sola consulta
        val cursor = db.rawQuery(
            "SELECT c.$CARD_ID, c.$CARD_NAME, c.$CARD_IMAGE, c.$CARD_RARITY, c.$CARD_CATEGORY, " +
                    "COALESCE(uc.$UC_COUNT, 0) AS count " +
                    "FROM $TABLE_CARDS c " +
                    "LEFT JOIN $TABLE_USER_CARDS uc " +
                    "ON c.$CARD_ID = uc.$UC_CARD_ID AND uc.$UC_USER_ID = ? " +
                    "ORDER BY c.$CARD_CATEGORY, c.$CARD_ID",
            arrayOf(userId.toString())
        )

        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val name = cursor.getString(1)
            val image = cursor.getString(2)
            val rarity = cursor.getString(3)
            val category = cursor.getString(4)
            val count = cursor.getInt(5)
            list.add(Card(id, name, image, rarity, category, count))
        }
        cursor.close()
        return list
    }

    //returns all cards regardless of user, used for rarity-based pack picking
    //devuelve todas las cartas sin importar el usuario, usado para elegir por rareza en sobres
    fun getAllCards(): List<Card> {
        val list = mutableListOf<Card>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CARDS", null)

        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(CARD_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(CARD_NAME))
            val image = cursor.getString(cursor.getColumnIndexOrThrow(CARD_IMAGE))
            val rarity = cursor.getString(cursor.getColumnIndexOrThrow(CARD_RARITY))
            val category = cursor.getString(cursor.getColumnIndexOrThrow(CARD_CATEGORY))
            list.add(Card(id, name, image, rarity, category))
        }
        cursor.close()
        return list
    }

    //adds one copy of a card to the user - inserts a row if first time, increments count if not
    //anade una copia de una carta al usuario - inserta fila si es la primera vez, incrementa count si no
    fun addCardToUser(userId: Int, cardId: Int) {
        val db = writableDatabase

        //check if the user already has a row for this card
        //comprobar si el usuario ya tiene una fila para esta carta
        val cursor = db.query(
            TABLE_USER_CARDS, arrayOf(UC_ID, UC_COUNT),
            "$UC_USER_ID=? AND $UC_CARD_ID=?",
            arrayOf(userId.toString(), cardId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            //row exists, just increment the count
            //la fila existe, solo incrementar el count
            val currentCount = cursor.getInt(1)
            val cv = ContentValues()
            cv.put(UC_COUNT, currentCount + 1)
            db.update(
                TABLE_USER_CARDS, cv,
                "$UC_USER_ID=? AND $UC_CARD_ID=?",
                arrayOf(userId.toString(), cardId.toString())
            )
        } else {
            //first time getting this card, insert with count 1
            //primera vez obteniendo esta carta, insertar con count 1
            val cv = ContentValues()
            cv.put(UC_USER_ID, userId)
            cv.put(UC_CARD_ID, cardId)
            cv.put(UC_COUNT, 1)
            db.insert(TABLE_USER_CARDS, null, cv)
        }
        cursor.close()
    }

    //removes one copy of a card from the user, if count reaches 0 the card becomes locked again
    //elimina una copia de una carta del usuario, si count llega a 0 la carta vuelve a estar bloqueada
    fun removeCardFromUser(userId: Int, cardId: Int) {
        val db = writableDatabase

        val cursor = db.query(
            TABLE_USER_CARDS, arrayOf(UC_ID, UC_COUNT),
            "$UC_USER_ID=? AND $UC_CARD_ID=?",
            arrayOf(userId.toString(), cardId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            val currentCount = cursor.getInt(1)
            val cv = ContentValues()
            //clamp to 0, never go negative
            //clamp a 0, nunca ir a negativo
            cv.put(UC_COUNT, maxOf(0, currentCount - 1))
            db.update(
                TABLE_USER_CARDS, cv,
                "$UC_USER_ID=? AND $UC_CARD_ID=?",
                arrayOf(userId.toString(), cardId.toString())
            )
        }
        cursor.close()
    }
}