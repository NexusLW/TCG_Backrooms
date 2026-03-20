package com.example.tcgbackrooms

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class CreateCardActivity : AppCompatActivity() {

    private lateinit var cardNameInput: EditText
    private lateinit var spinnerRarity: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var previewImage: ImageView
    private lateinit var pickImageButton: Button
    private lateinit var saveButton: Button
    private lateinit var backButton: Button

    private lateinit var db: DatabaseHelper

    //absolute path to the image the user creates, empty if they skipped it
    //ruta absoluta a la imagen creada por el usuario, vacia si no eligio ninguna
    private var savedImagePath: String = ""

    //photo picker launcher: fires when the user selects an image from the gallery
    //lanzador del selector de fotos: se dispara cuando el usuario elige una imagen de la galeria
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            //copy the image to internal storage so the app has the file and the path doesn't expire
            //copiar la imagen al almacenamiento interno para que la applicacion tiene el archivo y la ruta no expire
            savedImagePath = copyImageToInternalStorage(uri)

            //show a preview of the picked image
            //mostrar una vista previa de la imagen elegida
            val bitmap = BitmapFactory.decodeFile(savedImagePath)
            if (bitmap != null) previewImage.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_card)

        db = DatabaseHelper(this)

        cardNameInput = findViewById(R.id.cardNameInput)
        spinnerRarity = findViewById(R.id.spinnerRarity)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        previewImage = findViewById(R.id.previewImage)
        pickImageButton = findViewById(R.id.pickImageButton)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        //populate rarity spinner with the four possible values
        //rellenar el spinner de rareza con los cuatro valores posibles
        spinnerRarity.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("common", "uncommon", "rare", "legendary")
        )

        //populate category spinner with the three possible values
        //rellenar el spinner de categoria con los tres valores posibles
        spinnerCategory.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("levels", "entities", "items")
        )

        //launch the system photo picker when the user taps pick image
        //lanzar el selector de fotos del sistema cuando el usuario pulsa elegir imagen
        pickImageButton.setOnClickListener {
            pickImage.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        saveButton.setOnClickListener { saveCard() }

        //cancel button just closes the activity without saving anything
        //el boton cancelar cierra la actividad sin guardar nada
        backButton.setOnClickListener { finish() }
    }

    //validates the form, saves the card to the db, and returns ok to the caller
    //valida el formulario, guarda la carta en la db y devuelve ok al llamador
    private fun saveCard() {
        val name = cardNameInput.text.toString().trim()

        //name is the only mandatory field
        //el nombre es el unico campo obligatorio
        if (name.isEmpty()) {
            Toast.makeText(this, "Enter a card name", Toast.LENGTH_SHORT).show()
            return
        }

        val rarity = spinnerRarity.selectedItem.toString()
        val category = spinnerCategory.selectedItem.toString()

        //grab the user id that was passed from the fragment via the intent
        //obtener el id de usuario que paso el fragmento a traves del intent
        val userId = intent.getIntExtra("userId", -1)

        //fall back to the card back drawable name if the user didnt pick an image
        //usar el nombre del drawable card_back si el usuario no eligio imagen
        val imagePath = if (savedImagePath.isNotEmpty()) savedImagePath else "card_back"

        val cardId = db.addCustomCard(userId, name, imagePath, rarity, category)

        if (cardId != -1) {
            Toast.makeText(this, "Card created!", Toast.LENGTH_SHORT).show()
            //tell the fragment that launched us that the save was successful
            //decirle al fragmento que nos lanzo que el guardado fue exitoso
            setResult(RESULT_OK)
            finish()
        } else {
            Toast.makeText(this, "Failed to save card", Toast.LENGTH_SHORT).show()
        }
    }

    //copies a gallery path to a file in internal storage and returns the absolute path
    //copia una ruta de la galeria a un archivo en almacenamiento interno y devuelve la ruta absoluta
    //we do this because content paths can become invalid after the app restarts or if the image gets deleted
    //hacemos esto porque las rutas de contenido pueden volverse invalidas al reiniciar la app o si la imagen esta borrado
    private fun copyImageToInternalStorage(uri: Uri): String {
        val filename = "custom_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, filename)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }
}