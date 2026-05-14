package com.johnpena.minieco

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AdminPanelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_admin_panel)

        findViewById<Button>(R.id.btnCursos).setOnClickListener {
            startActivity(Intent(this, GestionCursosActivity::class.java))
        }

        findViewById<Button>(R.id.btnEstudiantes).setOnClickListener {
            startActivity(Intent(this, GestionEstudiantesActivity::class.java))
        }

        findViewById<Button>(R.id.btnRegistros).setOnClickListener {
            Toast.makeText(this, "Próximamente: Módulo de Exámenes", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_logged_in", false).apply()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}