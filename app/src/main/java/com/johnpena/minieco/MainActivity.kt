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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
                restablecerAnimaciones(tarjetaResultadosVivo)
                btnParlanteVivo.visibility = View.GONE
                AudioPlayerHelper.detener()
            }
        }

        btnTomarFoto.setOnClickListener { camaraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
        btnSubirGaleria.setOnClickListener { galeriaLauncher.launch("image/*") }

        val btnAccesoProfesor = findViewById<Button>(R.id.btnAccesoProfesor)
        btnAccesoProfesor.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        val tvActualizaciones = findViewById<TextView>(R.id.tvActualizaciones)
        tvActualizaciones.setOnClickListener {
            val url = "https://drive.google.com/drive/folders/1YyViO_cn4WBKtMW8Z_ejuYsTvUVSyk_q?usp=sharing"
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val btnAcceso = findViewById<Button>(R.id.btnAccesoProfesor)
        val prefs = getSharedPreferences("MiniEcoPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (isLoggedIn) {
            btnAcceso.text = "Panel de Administración"
            btnAcceso.setBackgroundColor(android.graphics.Color.parseColor("#1B5E20"))
            btnAcceso.setOnClickListener {
                startActivity(Intent(this, AdminPanelActivity::class.java))
            }
        } else {
            btnAcceso.text = "Acceso Profesores"
            btnAcceso.setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            btnAcceso.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
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
        if (isNightTheme == Configuration.UI_MODE_NIGHT_YES) {
            btnModoOscuro.text = "☀️ Cambiar a Modo Claro"
        } else {
            btnModoOscuro.text = "🌙 Cambiar a Modo Oscuro"
        }

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
            } catch (exc: Exception) {
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun actualizarInterfazEnVivo(material: String) {
        runOnUiThread {
            if (material == "Buscando" || material == "Inseguro") {
                txtMaterial.text = "🔍 Escaneando..."
                txtConsejo.text = "Acércate un poco más y centra el objeto en el cuadro."
                tarjetaResultadosVivo.setCardBackgroundColor(Color.WHITE)
                restablecerAnimaciones(tarjetaResultadosVivo)
                btnParlanteVivo.visibility = View.GONE
                return@runOnUiThread
            }

            viewFinder.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            aplicarColores(material, txtMaterial, txtConsejo, tarjetaResultadosVivo)

            btnParlanteVivo.visibility = View.VISIBLE
            audioDesechoActualVivo = obtenerAudioDesecho(material)
            audioColorActualVivo = obtenerAudioColorPorDefecto(material)
        }
    }

    private fun analizarFotoEstatica(bitmap: Bitmap) {
        val resultado = clasificador.classifyImage(bitmap)

        if (resultado == "Buscando" || resultado == "Inseguro") {
            txtResultadoFoto.text = "Mmm... no se detecta muy bien el objeto.\n\nColócalo o enfócalo bien en un fondo blanco."
            tarjetaResultadoFoto.setCardBackgroundColor(Color.WHITE)
            restablecerAnimaciones(tarjetaResultadoFoto)
            btnParlanteFoto.visibility = View.GONE
            return
        }

        aplicarColores(resultado, txtResultadoFoto, null, tarjetaResultadoFoto)

        btnParlanteFoto.visibility = View.VISIBLE
        audioDesechoActualFoto = obtenerAudioDesecho(resultado)
        audioColorActualFoto = obtenerAudioColorPorDefecto(resultado)
    }

    private fun restablecerAnimaciones(tarjeta: CardView) {
        tarjeta.animate().cancel()
        tarjeta.rotation = 0f
        tarjeta.scaleX = 1f
        tarjeta.scaleY = 1f
        tarjeta.translationX = 0f
        tarjeta.translationY = 0f
        tarjeta.alpha = 1f
    }

    // --- NUEVA LÓGICA DE 8 COLORES ---
    private fun aplicarColores(material: String, txtTitulo: TextView, txtSubtitulo: TextView?, tarjeta: CardView) {
        val nombreMayuscula = material.replaceFirstChar { it.uppercase() }
        txtTitulo.text = "¡Es $nombreMayuscula!"

        val textoConsejo: String
        val colorTarjeta: Int

        restablecerAnimaciones(tarjeta)

        when (material.lowercase().trim()) {
            "batería", "battery", "bateria", "baterías" -> {
                colorTarjeta = Color.parseColor("#FFCDD2") // Rojo
                textoConsejo = "⚠️ ¡Al tacho ROJO! 🔴\nResiduo peligroso y tóxico."
                tarjeta.animate().translationX(15f).setDuration(80).withEndAction { tarjeta.animate().translationX(-15f).setDuration(80).withEndAction { tarjeta.animate().translationX(0f).setDuration(80).start() }.start() }.start()
            }
            "biológico", "biological", "biologico" -> {
                colorTarjeta = Color.parseColor("#D7CCC8") // Café
                textoConsejo = "🍂 ¡Al tacho CAFÉ! 🟤\nRestos orgánicos."
                tarjeta.animate().translationY(15f).setDuration(100).withEndAction { tarjeta.animate().translationY(0f).setDuration(100).start() }.start()
            }
            "papel", "cartón", "paper", "cardboard", "carton" -> {
                colorTarjeta = Color.parseColor("#BBDEFB") // Azul
                textoConsejo = "♻️ ¡Al tacho AZUL! 🔵\nPara reciclar papel y cartón."
                tarjeta.animate().rotation(6f).setDuration(200).withEndAction { tarjeta.animate().rotation(-6f).setDuration(200).withEndAction { tarjeta.animate().rotation(0f).setDuration(200).start() }.start() }.start()
            }
            "metal" -> {
                colorTarjeta = Color.parseColor("#FFF9C4") // Amarillo
                textoConsejo = "♻️ ¡Al tacho AMARILLO! 🟡\nObjetos de metal."
                tarjeta.animate().scaleY(0.85f).scaleX(0.9f).setDuration(200).withEndAction { tarjeta.animate().scaleY(1f).scaleX(1f).setDuration(200).start() }.start()
            }
            "vidrio", "glass" -> {
                colorTarjeta = Color.parseColor("#C8E6C9") // Verde
                textoConsejo = "♻️ ¡Al tacho VERDE! 🟢\nEnvases de vidrio."
                tarjeta.animate().translationY(15f).setDuration(100).withEndAction { tarjeta.animate().translationY(0f).setDuration(100).start() }.start()
            }
            "plástico", "plastic", "plastico" -> {
                colorTarjeta = Color.parseColor("#F5F5F5") // Blanco
                textoConsejo = "♻️ ¡Al tacho BLANCO! ⚪\nEnvases de plástico."
                tarjeta.animate().scaleY(0.85f).scaleX(0.9f).setDuration(200).withEndAction { tarjeta.animate().scaleY(1f).scaleX(1f).setDuration(200).start() }.start()
            }
            "ropa", "clothes", "zapatos", "shoes" -> {
                colorTarjeta = Color.parseColor("#F3E5F5") // Morado suave (Donación)
                textoConsejo = "💖 ¡Para DONACIÓN! 👕👟\nSi está en buen estado."
                tarjeta.animate().translationY(-15f).setDuration(100).withEndAction { tarjeta.animate().translationY(0f).setDuration(100).start() }.start()
            }
            else -> { // trash / basura general
                colorTarjeta = Color.parseColor("#ECEFF1") // Gris/Negro
                textoConsejo = "🗑️ ¡Al tacho NEGRO! ⚫\nNo aprovechable (basura general)."
                tarjeta.animate().translationX(15f).setDuration(80).withEndAction { tarjeta.animate().translationX(-15f).setDuration(80).withEndAction { tarjeta.animate().translationX(0f).setDuration(80).start() }.start() }.start()
            }
        }

        tarjeta.setCardBackgroundColor(colorTarjeta)

        if (txtSubtitulo != null) {
            txtSubtitulo.text = textoConsejo
        } else {
            txtTitulo.text = "¡Es $nombreMayuscula!\n\n$textoConsejo"
        }
    }

    // --- NUEVO MAPEO DE AUDIOS (CORREGIDO CON TILDES) ---
    private fun obtenerAudioDesecho(claseDetectada: String): Int {
        return when (claseDetectada.lowercase().trim()) {
            "battery", "bateria", "batería", "baterías" -> R.raw.bateria
            "biological", "biologico", "biológico" -> R.raw.biologico
            "cardboard", "carton", "cartón" -> R.raw.carton
            "clothes", "ropa" -> R.raw.ropa
            "glass", "vidrio" -> R.raw.vidrio
            "metal" -> R.raw.metal
            "paper", "papel" -> R.raw.papel
            "plastic", "plastico", "plástico" -> R.raw.plastico
            "shoes", "zapatos" -> R.raw.zapatos
            else -> R.raw.basura
        }
    }

    private fun obtenerAudioColorPorDefecto(claseDetectada: String): Int {
        return when (claseDetectada.lowercase().trim()) {
            "battery", "bateria", "batería", "baterías" -> R.raw.rojo
            "biological", "biologico", "biológico" -> R.raw.cafe
            "paper", "papel", "cardboard", "carton", "cartón" -> R.raw.azul
            "metal" -> R.raw.amarillo
            "glass", "vidrio" -> R.raw.verde
            "plastic", "plastico", "plástico" -> R.raw.blanco
            "clothes", "ropa", "shoes", "zapatos" -> R.raw.donacion
            else -> R.raw.negro
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        AudioPlayerHelper.detener()
    }
}