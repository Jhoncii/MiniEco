package com.johnpena.minieco

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CambiarPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cambiar_password)

        val etPassActual = findViewById<EditText>(R.id.etPassActual)
        val etPassNueva = findViewById<EditText>(R.id.etPassNueva)
        val etPassConfirmar = findViewById<EditText>(R.id.etPassConfirmar)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPassword)

        btnGuardar.setOnClickListener {
            val passActual = etPassActual.text.toString()
            val passNueva = etPassNueva.text.toString()
            val passConfirmar = etPassConfirmar.text.toString()

            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
            val passReal = prefs.getString("admin_pass", "basura")

            // 1. Validar que la actual sea correcta
            if (passActual != passReal) {
                Toast.makeText(this, "❌ La contraseña actual es incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validar que las nuevas coincidan
            if (passNueva != passConfirmar) {
                Toast.makeText(this, "❌ Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Validaciones de longitud y caracteres (Sin espacios, max 15)
            val regexAlfanumerico = "^[a-zA-Z0-9]+$".toRegex()

            if (passNueva.length < 4 || passNueva.length > 15 || !passNueva.matches(regexAlfanumerico)) {
                Toast.makeText(this, "❌ La contraseña debe tener entre 4 y 15 letras o números, sin espacios", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ¡TODO PERFECTO! Guardamos en memoria
            prefs.edit().putString("admin_pass", passNueva).apply()
            Toast.makeText(this, "✅ Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
            finish() // Cierra la pantalla
        }
    }
}