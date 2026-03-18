package com.example.tcgbackrooms

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    //-------Views / Vistas-------

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var acceptButton: Button
    private lateinit var cancelButton: Button
    //this label switches the form between login and register mode
    //esta etiqueta cambia el formulario entre modo login y registro
    private lateinit var switchMode: TextView
    private lateinit var titleText: TextView

    //tracks whether we are in register mode or login mode
    //controla si estamos en modo registro o modo login
    private var isRegisterMode = false

    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DatabaseHelper(this)

        usernameInput = findViewById(R.id.usernameInput)
        passwordInput = findViewById(R.id.passwordInput)
        acceptButton = findViewById(R.id.acceptButton)
        cancelButton = findViewById(R.id.cancelButton)
        switchMode = findViewById(R.id.switchMode)
        titleText = findViewById(R.id.titleText)

        //accept button handles both login and register depending on current mode
        //el botón aceptar maneja tanto login como registro según el modo actual
        acceptButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill in all fields / Rellena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isRegisterMode) {
                handleRegister(username, password)
            } else {
                handleLogin(username, password)
            }
        }

        //cancel button closes the app
        //el botón cancelar cierra la aplicación
        cancelButton.setOnClickListener {
            finish()
        }

        //tapping the switch label toggles between login and register mode
        //pulsar la etiqueta alterna entre modo login y registro
        switchMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            updateUi()
        }
    }

    //handles login — checks credentials against the database
    //maneja el login — comprueba las credenciales contra la base de datos
    private fun handleLogin(username: String, password: String) {
        val userId = db.loginUser(username, password)

        if (userId != -1) {
            //credentials are valid, go to main screen passing the user id
            //credenciales válidas, ir a la pantalla principal pasando el id del usuario
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("userId", userId)
            intent.putExtra("username", username)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "Wrong credentials / Credenciales incorrectas", Toast.LENGTH_SHORT).show()
        }
    }

    //handles registration — creates a new user in the database
    //maneja el registro — crea un nuevo usuario en la base de datos
    private fun handleRegister(username: String, password: String) {
        val success = db.registerUser(username, password)

        if (success) {
            //registration worked, switch back to login mode so they can sign in
            //registro exitoso, volver al modo login para que pueda iniciar sesión
            isRegisterMode = false
            updateUi()
            Toast.makeText(this, "Account created! / ¡Cuenta creada!", Toast.LENGTH_SHORT).show()
        } else {
            //registerUser returns false if the username already exists (UNIQUE constraint)
            //registerUser devuelve false si el nombre de usuario ya existe (restricción UNIQUE)
            Toast.makeText(this, "Username already taken / Nombre de usuario ya en uso", Toast.LENGTH_SHORT).show()
        }
    }

    //updates the ui text depending on the current mode
    //actualiza el texto de la ui según el modo actual
    private fun updateUi() {
        if (isRegisterMode) {
            titleText.text = "Create Account"
            acceptButton.text = "Register"
            switchMode.text = "Already have an account? Log in"
        } else {
            titleText.text = "Welcome Back"
            acceptButton.text = "Log In"
            switchMode.text = "No account? Register here"
        }
    }
}