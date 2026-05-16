package com.johnpena.minieco

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.johnpena.minieco.database.MiniEcoDatabase
import com.johnpena.minieco.database.Regla
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var layoutSplash: ConstraintLayout
    private lateinit var layoutTiempoReal: ConstraintLayout
    private lateinit var layoutFoto: ConstraintLayout
    private lateinit var layoutInfo: View

    private lateinit var viewFinder: PreviewView
    private lateinit var txtMaterial: TextView
    private lateinit var txtConsejo: TextView
    private lateinit var tarjetaResultadosVivo: CardView
    private lateinit var btnReiniciar: FloatingActionButton

    private lateinit var imagenEstatica: ImageView
    private lateinit var btnTomarFoto: Button
    private lateinit var btnSubirGaleria: Button
    private lateinit var txtResultadoFoto: TextView
    private lateinit var tarjetaResultadoFoto: CardView
    private lateinit var btnModoOscuro: Button

    private lateinit var btnParlanteVivo: ImageButton
    private lateinit var btnParlanteFoto: ImageButton

    private lateinit var clasificador: WasteClassifier
    private lateinit var cameraExecutor: ExecutorService
    private var ultimoTiempoAnalisis: Long = 0

    private var audioDesechoActualVivo: Int = 0
    private var audioColorActualVivo: Int = 0
    private var audioDesechoActualFoto: Int = 0
    private var audioColorActualFoto: Int = 0

    // --- CEREBRO DE REGLAS ---
    private lateinit var database: MiniEcoDatabase
    private var reglasActivas: List<Regla> = emptyList()
    private var cursoActivoId: Int = -1

    private val solicitarPermisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) {
            iniciarCamara()
        } else {
            Toast.makeText(this, "⚠️ MiniEco necesita la cámara para reciclar.", Toast.LENGTH_LONG).show()
        }
    }

    private val camaraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val bitmap = result.data?.extras?.get("data") as Bitmap?
            if (bitmap != null) {
                imagenEstatica.setImageBitmap(bitmap)
                analizarFotoEstatica(bitmap)
            }
        }
    }

    private val galeriaLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(contentResolver, uri)
                }
                val bitmapSeguro = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                imagenEstatica.setImageBitmap(bitmapSeguro)
                analizarFotoEstatica(bitmapSeguro)
            } catch (e: Exception) {
                Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        database = MiniEcoDatabase.getDatabase(this)

        layoutSplash = findViewById(R.id.layoutSplash)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        layoutTiempoReal = findViewById(R.id.layoutTiempoReal)
        layoutFoto = findViewById(R.id.layoutFoto)
        layoutInfo = findViewById(R.id.layoutInfo)

        viewFinder = findViewById(R.id.viewFinder)
        txtMaterial = findViewById(R.id.txtMaterial)
        txtConsejo = findViewById(R.id.txtConsejo)
        tarjetaResultadosVivo = findViewById(R.id.tarjetaResultadosVivo)
        btnReiniciar = findViewById(R.id.btnReiniciar)

        imagenEstatica = findViewById(R.id.imagenEstatica)
        btnTomarFoto = findViewById(R.id.btnTomarFoto)
        btnSubirGaleria = findViewById(R.id.btnSubirGaleria)
        txtResultadoFoto = findViewById(R.id.txtResultadoFoto)
        tarjetaResultadoFoto = findViewById(R.id.tarjetaResultadoFoto)
        btnModoOscuro = findViewById(R.id.btnModoOscuro)

        btnParlanteVivo = findViewById(R.id.btnParlanteVivo)
        btnParlanteFoto = findViewById(R.id.btnParlanteFoto)

        clasificador = WasteClassifier(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        configurarMenu()
        configurarModoOscuro()

        btnParlanteVivo.setOnClickListener {
            if (audioDesechoActualVivo != 0 && audioColorActualVivo != 0) {
                AudioPlayerHelper.reproducir(this, audioDesechoActualVivo, audioColorActualVivo)
            }
        }

        btnParlanteFoto.setOnClickListener {
            if (audioDesechoActualFoto != 0 && audioColorActualFoto != 0) {
                AudioPlayerHelper.reproducir(this, audioDesechoActualFoto, audioColorActualFoto)
            }
        }

        if (savedInstanceState == null) {
            layoutSplash.visibility = View.VISIBLE
            bottomNavigation.visibility = View.GONE

            Handler(Looper.getMainLooper()).postDelayed({
                layoutSplash.visibility = View.GONE
                bottomNavigation.visibility = View.VISIBLE
                bottomNavigation.selectedItemId = R.id.nav_info
                verificarYPedirPermisos()
            }, 2000)
        } else {
            layoutSplash.visibility = View.GONE
            bottomNavigation.visibility = View.VISIBLE
            bottomNavigation.selectedItemId = R.id.nav_info
            verificarYPedirPermisos()
        }

        btnReiniciar.setOnClickListener {
            ultimoTiempoAnalisis = 0
            runOnUiThread {
                txtMaterial.text = "🔍 Escaneando..."
                txtConsejo.text = "Acércate un poco más y centra el objeto en el cuadro."
                tarjetaResultadosVivo.setCardBackgroundColor(Color.WHITE)
                txtMaterial.setTextColor(Color.BLACK)
                txtConsejo.setTextColor(Color.DKGRAY)
                restablecerAnimaciones(tarjetaResultadosVivo)
                btnParlanteVivo.visibility = View.GONE
                AudioPlayerHelper.detener()
            }
        }

        btnTomarFoto.setOnClickListener { camaraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
        btnSubirGaleria.setOnClickListener { galeriaLauncher.launch("image/*") }

        val btnAccesoProfesor = findViewById<Button>(R.id.btnAccesoProfesor)
        btnAccesoProfesor.setOnClickListener {
            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("is_logged_in", false)

            if (isLoggedIn) {
                startActivity(Intent(this, AdminPanelActivity::class.java))
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        val btnQuitarReglasMain = findViewById<Button>(R.id.btnQuitarReglasMain)
        btnQuitarReglasMain.setOnClickListener {
            val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
            prefs.edit().remove("cursoActivoId").apply()
            cursoActivoId = -1
            reglasActivas = emptyList()
            btnQuitarReglasMain.isEnabled = false
            btnQuitarReglasMain.alpha = 0.5f
            Toast.makeText(this, "Reglas desactivadas. MiniEco ha vuelto a la normalidad.", Toast.LENGTH_SHORT).show()
        }

        val tvActualizaciones = findViewById<TextView>(R.id.tvActualizaciones)
        tvActualizaciones.setOnClickListener {
            val url = "https://drive.google.com/drive/folders/1YyViO_cn4WBKtMW8Z_ejuYsTvUVSyk_q?usp=sharing"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        cursoActivoId = prefs.getInt("cursoActivoId", -1)

        val btnQuitarReglasMain = findViewById<Button>(R.id.btnQuitarReglasMain)
        val btnAccesoProfesor = findViewById<Button>(R.id.btnAccesoProfesor)

        if (cursoActivoId != -1) {
            lifecycleScope.launch {
                reglasActivas = database.miniEcoDao().obtenerReglasPorCurso(cursoActivoId)
            }
        } else {
            reglasActivas = emptyList()
        }

        if (isLoggedIn) {
            btnAccesoProfesor.text = "Panel de Profesores"
            btnQuitarReglasMain.visibility = View.VISIBLE
            if (cursoActivoId == -1) {
                btnQuitarReglasMain.isEnabled = false
                btnQuitarReglasMain.alpha = 0.5f
            } else {
                btnQuitarReglasMain.isEnabled = true
                btnQuitarReglasMain.alpha = 1.0f
            }
        } else {
            btnAccesoProfesor.text = "Acceso Profesores"
            btnQuitarReglasMain.visibility = View.GONE
        }
    }

    private fun configurarMenu() {
        bottomNavigation.setOnItemSelectedListener { item ->
            layoutTiempoReal.visibility = View.GONE
            layoutFoto.visibility = View.GONE
            layoutInfo.visibility = View.GONE
            AudioPlayerHelper.detener()

            when (item.itemId) {
                R.id.nav_camara -> { layoutTiempoReal.visibility = View.VISIBLE; true }
                R.id.nav_foto -> { layoutFoto.visibility = View.VISIBLE; true }
                R.id.nav_info -> { layoutInfo.visibility = View.VISIBLE; true }
                else -> false
            }
        }
    }

    private fun configurarModoOscuro() {
        val isNightTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        btnModoOscuro.text = if (isNightTheme == Configuration.UI_MODE_NIGHT_YES) "☀️ Cambiar a Modo Claro" else "🌙 Cambiar a Modo Oscuro"

        btnModoOscuro.setOnClickListener {
            if (isNightTheme == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }
    }

    private fun verificarYPedirPermisos() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamara()
        } else {
            solicitarPermisoCamara.launch(Manifest.permission.CAMERA)
        }
    }

    private fun iniciarCamara() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(viewFinder.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        if (layoutTiempoReal.visibility == View.VISIBLE) {
                            val tiempoActual = System.currentTimeMillis()
                            if (tiempoActual - ultimoTiempoAnalisis >= 400) {
                                val bitmap = imageProxy.toBitmap()
                                val resultado = clasificador.classifyImage(bitmap)
                                actualizarInterfazEnVivo(resultado)
                                ultimoTiempoAnalisis = tiempoActual
                            }
                        }
                        imageProxy.close()
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
            } catch (exc: Exception) {}
        }, ContextCompat.getMainExecutor(this))
    }

    private fun actualizarInterfazEnVivo(materialBruto: String) {
        runOnUiThread {
            if (materialBruto == "Buscando" || materialBruto == "Inseguro") {
                txtMaterial.text = "🔍 Escaneando..."
                txtConsejo.text = "Acércate un poco más y centra el objeto en el cuadro."
                tarjetaResultadosVivo.setCardBackgroundColor(Color.WHITE)
                txtMaterial.setTextColor(Color.BLACK)
                txtConsejo.setTextColor(Color.DKGRAY)
                restablecerAnimaciones(tarjetaResultadosVivo)
                btnParlanteVivo.visibility = View.GONE
                return@runOnUiThread
            }

            viewFinder.performHapticFeedback(HapticFeedbackConstants.CONFIRM)

            procesarDesechoEnUI(materialBruto, txtMaterial, txtConsejo, tarjetaResultadosVivo, true)
        }
    }

    private fun analizarFotoEstatica(bitmap: Bitmap) {
        val materialBruto = clasificador.classifyImage(bitmap)

        if (materialBruto == "Buscando" || materialBruto == "Inseguro") {
            txtResultadoFoto.text = "Mmm... no se detecta muy bien el objeto.\n\nColócalo o enfócalo bien en un fondo blanco."
            tarjetaResultadoFoto.setCardBackgroundColor(Color.WHITE)
            txtResultadoFoto.setTextColor(Color.BLACK)
            restablecerAnimaciones(tarjetaResultadoFoto)
            btnParlanteFoto.visibility = View.GONE
            return
        }

        procesarDesechoEnUI(materialBruto, txtResultadoFoto, null, tarjetaResultadoFoto, false)
    }

    // Funcion para determinar el demo o con reglas del curso.
    private fun procesarDesechoEnUI(materialBruto: String, txtTitulo: TextView, txtSubtitulo: TextView?, tarjeta: CardView, esVivo: Boolean) {
        val desechoLimpio = normalizarNombreDesecho(materialBruto)
        val audioDesecho = obtenerAudioDesecho(desechoLimpio)

        var textoConsejo = ""
        var colorFondoHex = Color.WHITE
        var audioColor = 0
        var evaluado = true

        restablecerAnimaciones(tarjeta)

        if (cursoActivoId != -1 && reglasActivas.isNotEmpty()) {
            // Modo Profe
            val regla = reglasActivas.find { it.tipoDesecho.equals(desechoLimpio, ignoreCase = true) }
            if (regla != null) {
                val colorProfe = regla.colorTacho
                val config = obtenerConfigColor(colorProfe)
                colorFondoHex = config.fondoHex
                audioColor = config.audioRes
                textoConsejo = "Regla de clase:\n¡Al tacho ${colorProfe.uppercase()}!"
                animarTarjeta(colorProfe, tarjeta)
            } else {
                // No está en la regla del curso
                evaluado = false
                colorFondoHex = Color.parseColor("#E0E0E0") // Gris neutro opaco
                textoConsejo = "Este desecho no se evalúa hoy."
            }
        } else {
            // Modo normal Original
            val colorDefecto = obtenerColorPorDefecto(desechoLimpio)
            val config = obtenerConfigColor(colorDefecto)
            colorFondoHex = config.fondoHex
            audioColor = config.audioRes
            textoConsejo = obtenerConsejoPorDefecto(desechoLimpio)
            animarTarjeta(colorDefecto, tarjeta)
        }

        // Actualizar Textos
        if (txtSubtitulo != null) {
            txtTitulo.text = "¡Es $desechoLimpio!"
            txtSubtitulo.text = textoConsejo
        } else {
            txtTitulo.text = "¡Es $desechoLimpio!\n\n$textoConsejo"
        }

        // Aplicar Colores
        tarjeta.setCardBackgroundColor(colorFondoHex)
        if (!evaluado) {
            txtTitulo.setTextColor(Color.DKGRAY)
            txtSubtitulo?.setTextColor(Color.DKGRAY)
        } else {
            txtTitulo.setTextColor(Color.BLACK)
            txtSubtitulo?.setTextColor(Color.BLACK)
        }

        // Actualizar Audios
        val btnParlante = if (esVivo) btnParlanteVivo else btnParlanteFoto
        if (evaluado && audioDesecho != 0 && audioColor != 0) {
            btnParlante.visibility = View.VISIBLE
            if (esVivo) {
                audioDesechoActualVivo = audioDesecho
                audioColorActualVivo = audioColor
            } else {
                audioDesechoActualFoto = audioDesecho
                audioColorActualFoto = audioColor
            }
        } else {
            btnParlante.visibility = View.GONE
        }
    }

    // Los 10 desechos exactos
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

    // Los colores por defecto
    private fun obtenerConfigColor(colorTacho: String): ConfigColor {
        return when (colorTacho.lowercase().trim()) {
            "rojo" -> ConfigColor(Color.parseColor("#FFCDD2"), R.raw.rojo)
            "café", "cafe" -> ConfigColor(Color.parseColor("#D7CCC8"), R.raw.cafe)
            "azul" -> ConfigColor(Color.parseColor("#BBDEFB"), R.raw.azul)
            "amarillo" -> ConfigColor(Color.parseColor("#FFF9C4"), R.raw.amarillo)
            "verde" -> ConfigColor(Color.parseColor("#C8E6C9"), R.raw.verde)
            "negro" -> ConfigColor(Color.parseColor("#ECEFF1"), R.raw.negro)
            "blanco" -> ConfigColor(Color.parseColor("#F5F5F5"), R.raw.blanco)
            "donación", "donacion", "para donación" -> ConfigColor(Color.parseColor("#F3E5F5"), R.raw.donacion)
            else -> ConfigColor(Color.WHITE, 0)
        }
    }


    private fun obtenerColorPorDefecto(desecho: String): String {
        return when (desecho) {
            "Batería" -> "Rojo"
            "Biológico" -> "Café"
            "Papel", "Cartón" -> "Azul"
            "Metal" -> "Amarillo"
            "Vidrio" -> "Verde"
            "Plástico" -> "Blanco"
            "Ropa", "Zapatos" -> "Donación"
            else -> "Negro"
        }
    }

    private fun obtenerConsejoPorDefecto(desecho: String): String {
        return when (desecho) {
            "Batería" -> "⚠️ ¡Al tacho ROJO! 🔴\nResiduo peligroso y tóxico."
            "Biológico" -> "🍂 ¡Al tacho CAFÉ! 🟤\nRestos orgánicos."
            "Papel", "Cartón" -> "♻️ ¡Al tacho AZUL! 🔵\nPara reciclar papel y cartón."
            "Metal" -> "♻️ ¡Al tacho AMARILLO! 🟡\nObjetos de metal."
            "Vidrio" -> "♻️ ¡Al tacho VERDE! 🟢\nEnvases de vidrio."
            "Plástico" -> "♻️ ¡Al tacho BLANCO! ⚪\nEnvases de plástico."
            "Ropa", "Zapatos" -> "💖 ¡Para DONACIÓN! 👕👟\nSi está en buen estado."
            else -> "🗑️ ¡Al tacho NEGRO! ⚫\nNo aprovechable (basura general)."
        }
    }

    private fun obtenerAudioDesecho(desecho: String): Int {
        return when (desecho) {
            "Batería" -> R.raw.bateria
            "Biológico" -> R.raw.biologico
            "Cartón" -> R.raw.carton
            "Papel" -> R.raw.papel
            "Ropa" -> R.raw.ropa
            "Vidrio" -> R.raw.vidrio
            "Metal" -> R.raw.metal
            "Plástico" -> R.raw.plastico
            "Zapatos" -> R.raw.zapatos
            else -> R.raw.basura
        }
    }

    // Animaciones
    private fun animarTarjeta(colorTacho: String, tarjeta: CardView) {
        when (colorTacho.lowercase().trim()) {
            "rojo", "negro" -> tarjeta.animate().translationX(15f).setDuration(80).withEndAction { tarjeta.animate().translationX(-15f).setDuration(80).withEndAction { tarjeta.animate().translationX(0f).setDuration(80).start() }.start() }.start()
            "café", "cafe", "verde" -> tarjeta.animate().translationY(15f).setDuration(100).withEndAction { tarjeta.animate().translationY(0f).setDuration(100).start() }.start()
            "azul" -> tarjeta.animate().rotation(6f).setDuration(200).withEndAction { tarjeta.animate().rotation(-6f).setDuration(200).withEndAction { tarjeta.animate().rotation(0f).setDuration(200).start() }.start() }.start()
            "amarillo", "blanco" -> tarjeta.animate().scaleY(0.85f).scaleX(0.9f).setDuration(200).withEndAction { tarjeta.animate().scaleY(1f).scaleX(1f).setDuration(200).start() }.start()
            "donación", "donacion", "para donación" -> tarjeta.animate().translationY(-15f).setDuration(100).withEndAction { tarjeta.animate().translationY(0f).setDuration(100).start() }.start()
        }
    }

    private fun restablecerAnimaciones(tarjeta: CardView) {
        tarjeta.animate().cancel()
        tarjeta.rotation = 0f; tarjeta.scaleX = 1f; tarjeta.scaleY = 1f
        tarjeta.translationX = 0f; tarjeta.translationY = 0f; tarjeta.alpha = 1f
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        AudioPlayerHelper.detener()
    }
}

// Clase de apoyo
data class ConfigColor(val fondoHex: Int, val audioRes: Int)