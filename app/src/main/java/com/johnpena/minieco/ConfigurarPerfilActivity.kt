package com.johnpena.minieco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class ConfigurarPerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Ocultamos la barra de arriba para que se vea más limpio
        setContentView(R.layout.activity_configurar_perfil)

        val etUser = findViewById<TextInputEditText>(R.id.etNuevoUsuario)
        val etPass = findViewById<TextInputEditText>(R.id.etNuevoPass)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)

        btnGuardar.setOnClickListener {
            val u = etUser.text.toString().trim()
            val p = etPass.text.toString().trim()

            if (u.isNotEmpty() && p.isNotEmpty()) {
                // Guardamos en la memoria pequeña del celular (SharedPreferences)
                val sharedPref = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putString("admin_user", u)
                    putString("admin_pass", p)
                    putBoolean("is_first_login", false) // Marcamos que ya cambió sus datos
                    apply()
                }

                Toast.makeText(this, "✅ Perfil configurado correctamente", Toast.LENGTH_SHORT).show()

                // Saltamos al Panel de Administración
                val intent = Intent(this, AdminPanelActivity::class.java)
                startActivity(intent)
                finish() // Cerramos esta pantalla para que no pueda volver atrás
            } else {
                Toast.makeText(this, "⚠️ Por favor, llena ambos campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}