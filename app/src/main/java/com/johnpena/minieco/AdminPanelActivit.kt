package com.johnpena.minieco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class AdminPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        val btnCursos = findViewById<MaterialButton>(R.id.btnGestionCursos)
        val btnCambiarPass = findViewById<MaterialButton>(R.id.btnCambiarPassword)
        val btnCerrarSesion = findViewById<MaterialButton>(R.id.btnCerrarSesion)

        btnCursos.setOnClickListener {
            startActivity(Intent(this, GestionCursosActivity::class.java))
        }

        btnCambiarPass.setOnClickListener {
            startActivity(Intent(this, CambiarPasswordActivity::class.java))
        }


        btnCerrarSesion.setOnClickListener {
            // 1. Apagar reglas, CERRAR SESIÓN real y volver al inicio
            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("cursoActivoId")
                .putBoolean("is_logged_in", false) // <-- ESTO FALTABA PARA MATAR LA SESIÓN
                .apply()

            Toast.makeText(this, "Sesión cerrada. Reglas por defecto activadas.", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Destruimos esta pantalla para que no pueda volver atrás
        }
    }
}