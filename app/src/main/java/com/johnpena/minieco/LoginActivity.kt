package com.johnpena.minieco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_login)

        val etUsuario = findViewById<TextInputEditText>(R.id.etUsuario)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val sharedPref = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)

        btnIngresar.setOnClickListener {
            val userIngresado = etUsuario.text.toString()
            val passIngresado = etPassword.text.toString()
            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)


            val adminUser = prefs.getString("admin_user", "clasificacion")
            val adminPass = prefs.getString("admin_pass", "basura")
            val esPrimeraVez = prefs.getBoolean("is_first_login", true)

            if (userIngresado == adminUser && passIngresado == adminPass) {

                prefs.edit().putBoolean("is_logged_in", true).apply()

                if (esPrimeraVez) {
                    // SI ES LA PRIMERA VEZ, VA A CONFIGURAR
                    Toast.makeText(this, "⚠️ Cambio de seguridad obligatorio", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, ConfigurarPerfilActivity::class.java))
                } else {
                    // SI YA CAMBIÓ LA CLAVE ANTES, VA AL PANEL
                    Toast.makeText(this, "✅ Bienvenida, Profesora", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, AdminPanelActivity::class.java))
                }
                finish()
            } else if (userIngresado != adminUser) {
                Toast.makeText(this, "❌ Usuario incorrecto", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show()
            }
        }
    }
}