package com.johnpena.minieco

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.johnpena.minieco.database.MiniEcoDatabase
import com.johnpena.minieco.database.Regla
import com.johnpena.minieco.database.ReporteTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TestActivity : AppCompatActivity() {

    // Vistas
    private lateinit var viewFinderTest: PreviewView
    private lateinit var tvContador: TextView
    private lateinit var tarjetaTest: CardView

    // Fase 1
    private lateinit var layoutFaseDesecho: LinearLayout
    private lateinit var tvNombreDesecho: TextView
    private lateinit var btnAudioDesecho: ImageButton
    private lateinit var btnSiguienteFaseColor: ImageButton

    // Fase 2
    private lateinit var layoutFaseColor: LinearLayout
    private lateinit var btnUsarCamaraColor: Button
    private lateinit var gridColores: GridLayout

    // Fase 2.5 Confirmar Color
    private lateinit var layoutConfirmarColor: LinearLayout
    private lateinit var tvColorConfirmar: TextView
    private lateinit var btnAudioColorConfirmar: ImageButton
    private lateinit var btnAceptarColor: ImageButton
    private lateinit var btnRechazarColor: ImageButton

    // Fase 3 Resultado
    private lateinit var layoutFaseResultado: LinearLayout
    private lateinit var tvResultadoEvaluacion: TextView
    private lateinit var btnAceptarResultado: Button

    // Fase 4 Final
    private lateinit var layoutFaseFinal: LinearLayout
    private lateinit var tvNotaFinal: TextView
    private lateinit var btnSalirMenu: Button

    private lateinit var database: MiniEcoDatabase
    private lateinit var clasificador: WasteClassifier
    private lateinit var cameraExecutor: ExecutorService

    // Datos de la prueba
    private var cursoId = -1; private var estudianteId = -1
    private var nombreEstudiante = ""; private var totalPreguntas = 1
    private var preguntaActual = 1; private var aciertosTotales = 0
    private val detallesPruebaJson = JSONArray()

    private var reglasActivas: List<Regla> = emptyList()
    private var usarReglasDefecto = false

    // Máquina de estados: 0=Desecho, 1=Menu Color, 2=Cámara Color, 2.5=Confirmar Color, 3=Resultado, 4=Final
    private var faseActual = 0
    private var desechoFijado = ""; private var colorCorrectoFijado = ""
    private var colorDetectadoTemporal = ""
    private var ultimoTiempoAnalisis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_test)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { mostrarDialogoSalida() }
        })

        cursoId = intent.getIntExtra("CURSO_ID", -1)
        estudianteId = intent.getIntExtra("ESTUDIANTE_ID", -1)
        nombreEstudiante = intent.getStringExtra("NOMBRE_ESTUDIANTE") ?: "Estudiante"
        totalPreguntas = intent.getIntExtra("TOTAL_PREGUNTAS", 1)

        enlazarVistas()
        tvContador.text = "Objeto $preguntaActual de $totalPreguntas"
        findViewById<ImageButton>(R.id.btnSalirTest).setOnClickListener { mostrarDialogoSalida() }

        database = MiniEcoDatabase.getDatabase(this)
        clasificador = WasteClassifier(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        cargarReglasEIniciar()
        configurarBotonesFlujo()
    }

    private fun enlazarVistas() {
        viewFinderTest = findViewById(R.id.viewFinderTest)
        tvContador = findViewById(R.id.tvContadorPrueba)
        tarjetaTest = findViewById(R.id.tarjetaTest)

        layoutFaseDesecho = findViewById(R.id.layoutFaseDesecho)
        tvNombreDesecho = findViewById(R.id.tvNombreDesechoTest)
        btnAudioDesecho = findViewById(R.id.btnAudioDesechoTest)
        btnSiguienteFaseColor = findViewById(R.id.btnSiguienteFaseColor)

        layoutFaseColor = findViewById(R.id.layoutFaseColor)
        btnUsarCamaraColor = findViewById(R.id.btnUsarCamaraColor)
        gridColores = findViewById(R.id.gridColoresManuales)

        layoutConfirmarColor = findViewById(R.id.layoutConfirmarColor)
        tvColorConfirmar = findViewById(R.id.tvColorConfirmar)
        btnAudioColorConfirmar = findViewById(R.id.btnAudioColorConfirmar)
        btnAceptarColor = findViewById(R.id.btnAceptarColor)
        btnRechazarColor = findViewById(R.id.btnRechazarColor)

        layoutFaseResultado = findViewById(R.id.layoutFaseResultado)
        tvResultadoEvaluacion = findViewById(R.id.tvResultadoEvaluacion)
        btnAceptarResultado = findViewById(R.id.btnAceptarResultado)

        layoutFaseFinal = findViewById(R.id.layoutFaseFinal)
        tvNotaFinal = findViewById(R.id.tvNotaFinal)
        btnSalirMenu = findViewById(R.id.btnSalirMenu)
    }

    private fun cargarReglasEIniciar() {
        lifecycleScope.launch {
            reglasActivas = database.miniEcoDao().obtenerReglasPorCurso(cursoId)
            usarReglasDefecto = reglasActivas.isEmpty()
            iniciarCamara()
        }
    }

    private fun configurarBotonesFlujo() {
        btnAudioDesecho.setOnClickListener {
            val audioRes = obtenerAudioDesecho(desechoFijado)
            if (audioRes != 0 && audioRes != R.raw.basura) {
                android.media.MediaPlayer.create(this@TestActivity, audioRes)?.apply {
                    setOnCompletionListener { release() } // Libera la memoria al terminar
                    start()
                }
            }
        }

        // Flecha a Fase 1 (Menú Color)
        btnSiguienteFaseColor.setOnClickListener {
            faseActual = 1
            layoutFaseDesecho.visibility = View.GONE
            layoutFaseColor.visibility = View.VISIBLE
            generarBotonesColorManuales()
        }

        // Fase 2 (Activar cámara color)
        btnUsarCamaraColor.setOnClickListener {
            faseActual = 2
            layoutFaseColor.visibility = View.GONE
            layoutFaseDesecho.visibility = View.VISIBLE
            tvNombreDesecho.text = "📷 Escaneando color..."
            btnAudioDesecho.visibility = View.GONE
            btnSiguienteFaseColor.visibility = View.GONE
        }

        // Fase 2.5: Validar Color Cámara
        btnAudioColorConfirmar.setOnClickListener {
            val audioRes = obtenerAudioColor(colorDetectadoTemporal)
            if (audioRes != 0) {
                android.media.MediaPlayer.create(this@TestActivity, audioRes)?.apply {
                    setOnCompletionListener { release() }
                    start()
                }
            }
        }

        btnRechazarColor.setOnClickListener {
            // Regresa al menú manual de color
            faseActual = 1
            layoutConfirmarColor.visibility = View.GONE
            layoutFaseColor.visibility = View.VISIBLE
            colorDetectadoTemporal = ""
        }

        btnAceptarColor.setOnClickListener {
            // Evaluamos el color confirmado
            evaluarRespuesta(colorDetectadoTemporal)
        }

        // Fase 3: Aceptar Resultado (Pasar a la siguiente pregunta)
        btnAceptarResultado.setOnClickListener {
            if (preguntaActual >= totalPreguntas) {
                mostrarPantallaFinal()
            } else {
                preguntaActual++
                tvContador.text = "Objeto $preguntaActual de $totalPreguntas"

                // Reset a Fase 0
                faseActual = 0
                desechoFijado = ""; colorCorrectoFijado = ""
                layoutFaseResultado.visibility = View.GONE
                layoutFaseDesecho.visibility = View.VISIBLE
                btnSiguienteFaseColor.visibility = View.GONE
                btnAudioDesecho.visibility = View.GONE
                tvNombreDesecho.text = "🔍 Escaneando objeto..."
                tarjetaTest.setCardBackgroundColor(Color.WHITE)
                tvNombreDesecho.setTextColor(Color.BLACK)
            }
        }

        // Fase 4: Salir al Menú
        btnSalirMenu.setOnClickListener { finalizarTestYGuardar() }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinderTest.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    val tiempoActual = System.currentTimeMillis()
                    if (tiempoActual - ultimoTiempoAnalisis >= 400) {
                        val bitmap = imageProxy.toBitmap()

                        if (faseActual == 0) {
                            val materialBruto = clasificador.classifyImage(bitmap)
                            runOnUiThread { procesarDesechoVivo(materialBruto) }
                        } else if (faseActual == 2) {
                            runOnUiThread { escanearColorConPalette(bitmap) }
                        }
                        ultimoTiempoAnalisis = tiempoActual
                    }
                    imageProxy.close()
                }
            }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (e: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    // --- CEREBRO: FASE 0 ---
    private fun procesarDesechoVivo(materialBruto: String) {
        if (materialBruto == "Buscando" || materialBruto == "Inseguro") return

        val desechoLimpio = normalizarNombreDesecho(materialBruto)

        if (usarReglasDefecto) {
            // MODO POR DEFECTO
            tvNombreDesecho.text = "¡Es $desechoLimpio!"
            tarjetaTest.setCardBackgroundColor(Color.WHITE)
            tvNombreDesecho.setTextColor(Color.BLACK)
            btnSiguienteFaseColor.visibility = View.VISIBLE
            btnAudioDesecho.visibility = View.VISIBLE
            desechoFijado = desechoLimpio
            colorCorrectoFijado = obtenerColorPorDefecto(desechoLimpio)

        } else {
            // MODO PROFESORA ESTRICTO
            val regla = reglasActivas.find { it.tipoDesecho.equals(desechoLimpio, ignoreCase = true) }
            if (regla != null) {
                tvNombreDesecho.text = "¡Es $desechoLimpio!"
                tarjetaTest.setCardBackgroundColor(Color.WHITE)
                tvNombreDesecho.setTextColor(Color.BLACK)
                btnSiguienteFaseColor.visibility = View.VISIBLE
                btnAudioDesecho.visibility = View.VISIBLE
                desechoFijado = desechoLimpio
                colorCorrectoFijado = regla.colorTacho
            } else {
                tvNombreDesecho.text = "Sin Regla ($desechoLimpio)"
                tarjetaTest.setCardBackgroundColor(Color.parseColor("#FFCDD2"))
                tvNombreDesecho.setTextColor(Color.RED)
                btnSiguienteFaseColor.visibility = View.GONE
                btnAudioDesecho.visibility = View.GONE
                desechoFijado = ""
            }
        }
    }

    // --- CEREBRO: FASE 2.5 (PALETTE) ---
    private fun escanearColorConPalette(bitmap: Bitmap) {
        Palette.from(bitmap).generate { palette ->
            val dominantSwatch = palette?.dominantSwatch ?: palette?.vibrantSwatch
            if (dominantSwatch != null) {
                val colorRGB = dominantSwatch.rgb
                val nombreColor = aproximarColorBasico(colorRGB)

                if (nombreColor != "Desconocido") {
                    faseActual = 25 // Pausamos la cámara
                    colorDetectadoTemporal = nombreColor

                    layoutFaseDesecho.visibility = View.GONE
                    layoutConfirmarColor.visibility = View.VISIBLE
                    tvColorConfirmar.text = nombreColor.uppercase()

                    // Pintamos el fondo según el color detectado para que sea más visual
                    val configHex = obtenerHexColor(nombreColor)
                    tarjetaTest.setCardBackgroundColor(Color.parseColor(configHex))
                }
            }
        }
    }

    private fun generarBotonesColorManuales() {
        gridColores.removeAllViews()
        val coloresMap = mapOf(
            "Rojo" to "#F44336", "Café" to "#795548", "Azul" to "#2196F3",
            "Amarillo" to "#FFEB3B", "Verde" to "#4CAF50", "Negro" to "#212121",
            "Blanco" to "#F5F5F5", "Donación" to "#9C27B0"
        )

        for ((nombre, hex) in coloresMap) {
            // 1. EL FONDO (Tarjeta para que se vea bonito)
            val card = androidx.cardview.widget.CardView(this).apply {
                setCardBackgroundColor(Color.parseColor(hex))
                radius = 12f
                cardElevation = 4f
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0; height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(12, 12, 12, 12)
                }
            }

            // 2. EL CONTENEDOR INTERNO (Horizontal)
            val innerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            val colorTexto = if (nombre == "Blanco" || nombre == "Amarillo") Color.BLACK else Color.WHITE

            // 3. LA BOCINA (Exclusiva para el audio)
            val btnAudio = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(colorTexto)
                layoutParams = LinearLayout.LayoutParams(120, 120) // Tamaño cuadrado táctil

                // ¡AQUÍ ESTÁ LA MAGIA! Solo suena, NO avanza.
                setOnClickListener {
                    val audioRes = obtenerAudioColor(nombre)
                    if (audioRes != 0) {
                        android.media.MediaPlayer.create(this@TestActivity, audioRes)?.apply {
                            setOnCompletionListener { release() }
                            start()
                        }
                    }
                }
            }

            // 4. EL TEXTO (Exclusivo para evaluar)
            val tvNombre = TextView(this).apply {
                text = nombre
                setTextColor(colorTexto)
                textSize = 18f
                setTypeface(null, android.graphics.Typeface.BOLD)
                gravity = android.view.Gravity.CENTER
                setPadding(0, 30, 20, 30) // Espaciado para que se vea como botón
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                // ¡AQUÍ SÍ AVANZA!
                setOnClickListener { evaluarRespuesta(nombre) }
            }

            // Unimos todo el rompecabezas
            innerLayout.addView(btnAudio)
            innerLayout.addView(tvNombre)
            card.addView(innerLayout)

            card.setOnClickListener { evaluarRespuesta(nombre) }

            gridColores.addView(card)
        }
    }

    // --- FASE 3: EVALUAR ---
    private fun evaluarRespuesta(colorSeleccionado: String) {
        faseActual = 3
        layoutFaseColor.visibility = View.GONE
        layoutConfirmarColor.visibility = View.GONE
        layoutFaseResultado.visibility = View.VISIBLE

        val esCorrecto = colorSeleccionado.equals(colorCorrectoFijado, ignoreCase = true)

        if (esCorrecto) {
            aciertosTotales++
            tvResultadoEvaluacion.text = "✅ ¡Correcto! ($colorSeleccionado)"
            tvResultadoEvaluacion.setTextColor(Color.parseColor("#4CAF50"))
            tarjetaTest.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
        } else {
            tvResultadoEvaluacion.text = "❌ Incorrecto.\nElegiste $colorSeleccionado\nEra: $colorCorrectoFijado"
            tvResultadoEvaluacion.setTextColor(Color.parseColor("#F44336"))
            tarjetaTest.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
        }

        val chisme = JSONObject().apply {
            put("desecho", desechoFijado)
            put("colorElegido", colorSeleccionado)
            put("colorCorrecto", colorCorrectoFijado)
            put("esCorrecto", esCorrecto)
        }
        detallesPruebaJson.put(chisme)

        if (preguntaActual >= totalPreguntas) {
            btnAceptarResultado.text = "Ver Nota Final 🏆"
            btnAceptarResultado.setBackgroundColor(Color.parseColor("#9C27B0"))
        } else {
            btnAceptarResultado.text = "Siguiente Pregunta ➡"
            btnAceptarResultado.setBackgroundColor(Color.parseColor("#2196F3"))
        }
    }

    // --- FASE 4 ---
    private fun mostrarPantallaFinal() {
        faseActual = 4
        layoutFaseResultado.visibility = View.GONE
        layoutFaseFinal.visibility = View.VISIBLE
        tarjetaTest.setCardBackgroundColor(Color.WHITE)
        tvNotaFinal.text = "Nota: $aciertosTotales / $totalPreguntas"
    }

    private fun finalizarTestYGuardar() {
        val jsonFinal = detallesPruebaJson.toString()
        val nuevoReporte = ReporteTest(
            cursoId = cursoId, estudianteId = estudianteId, nombreEstudiante = nombreEstudiante,
            fecha = System.currentTimeMillis(), aciertos = aciertosTotales,
            totalPreguntas = totalPreguntas, detallesJson = jsonFinal
        )
        lifecycleScope.launch(Dispatchers.IO) {
            database.miniEcoDao().insertarReporte(nuevoReporte)
            withContext(Dispatchers.Main) { finish() }
        }
    }

    // --- UTILIDADES ---
    private fun normalizarNombreDesecho(iaOutput: String): String {
        return when (iaOutput.lowercase().trim()) {
            "battery", "bateria", "batería", "baterías" -> "Batería"
            "biological", "biologico", "biológico" -> "Biológico"
            "paper", "papel" -> "Papel"
            "cardboard", "carton", "cartón" -> "Cartón"
            "metal" -> "Metal"
            "glass", "vidrio" -> "Vidrio"
            "plastic", "plastico", "plástico" -> "Plástico"
            "clothes", "ropa" -> "Ropa"
            "shoes", "zapatos" -> "Zapatos"
            else -> "Basura General"
        }
    }

    private fun obtenerColorPorDefecto(desecho: String): String {
        return when (desecho) {
            "Batería" -> "Rojo"; "Biológico" -> "Café"
            "Papel", "Cartón" -> "Azul"; "Metal" -> "Amarillo"
            "Vidrio" -> "Verde"; "Plástico" -> "Blanco"
            "Ropa", "Zapatos" -> "Donación"
            else -> "Negro"
        }
    }

    private fun obtenerHexColor(color: String): String {
        return when(color) {
            "Rojo" -> "#FFCDD2"; "Verde" -> "#C8E6C9"; "Azul" -> "#BBDEFB"
            "Amarillo" -> "#FFF9C4"; "Café" -> "#D7CCC8"; "Blanco" -> "#F5F5F5"
            "Negro" -> "#ECEFF1"; "Donación" -> "#F3E5F5"
            else -> "#FFFFFF"
        }
    }

    private fun aproximarColorBasico(rgb: Int): String {
        val r = Color.red(rgb); val g = Color.green(rgb); val b = Color.blue(rgb)
        return when {
            r > 150 && g < 100 && b < 100 -> "Rojo"
            r < 100 && g > 120 && b < 100 -> "Verde"
            r < 100 && g < 100 && b > 150 -> "Azul"
            r > 180 && g > 180 && b < 100 -> "Amarillo"
            r in 80..160 && g in 50..120 && b < 80 -> "Café"
            r > 200 && g > 200 && b > 200 -> "Blanco"
            r < 60 && g < 60 && b < 60 -> "Negro"
            else -> "Desconocido"
        }
    }

    private fun obtenerAudioDesecho(d: String) = when(d) {
        "Batería"->R.raw.bateria; "Biológico"->R.raw.biologico; "Cartón"->R.raw.carton; "Ropa"->R.raw.ropa
        "Vidrio"->R.raw.vidrio; "Metal"->R.raw.metal; "Papel"->R.raw.papel; "Plástico"->R.raw.plastico; "Zapatos"->R.raw.zapatos
        else->R.raw.basura
    }
    private fun obtenerAudioColor(c: String) = when(c) {
        "Rojo"->R.raw.rojo; "Café"->R.raw.cafe; "Azul"->R.raw.azul; "Amarillo"->R.raw.amarillo; "Verde"->R.raw.verde
        "Blanco"->R.raw.blanco; "Negro"->R.raw.negro; "Donación"->R.raw.donacion; else->0
    }

    private fun mostrarDialogoSalida() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Abandonar Prueba").setMessage("¿Salir sin guardar el progreso de $nombreEstudiante?")
        val inputPassword = EditText(this).apply { hint = "Contraseña"; inputType = 129 }
        val layout = LinearLayout(this).apply { setPadding(50,20,50,0); addView(inputPassword) }
        builder.setView(layout).setPositiveButton("Salir", null).setNegativeButton("Cancelar") { d, _ -> d.dismiss() }
        val dialog = builder.create(); dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (inputPassword.text.toString() == getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE).getString("admin_pass", "basura")) {
                dialog.dismiss(); finish()
            } else { Toast.makeText(this, "❌ Contraseña incorrecta", Toast.LENGTH_SHORT).show() }
        }
    }
    override fun onDestroy() { super.onDestroy(); cameraExecutor.shutdown() }
}