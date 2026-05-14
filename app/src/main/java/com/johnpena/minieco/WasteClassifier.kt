package com.johnpena.minieco

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.min

class WasteClassifier(context: Context) {

    private var interpreter: Interpreter
    private var labels: List<String>

    // TAMAÑO EXACTO DEL MODELO FINAL (384x384)
    private val IMAGE_SIZE = 384

    init {
        // Cargar etiquetas en español
        labels = context.assets.open("etiquetas.txt").bufferedReader().readLines()

        // Cargar el modelo nuevo
        val fileDescriptor = context.assets.openFd("garbage_classification_android.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)

        interpreter = Interpreter(modelBuffer)

        // --- LA MAGIA PARA ARREGLAR EL ERROR DE LOS 12 BYTES ---
        // Obligamos al modelo a "abrir los ojos" al tamaño correcto (384x384)
        interpreter.resizeInput(0, intArrayOf(1, IMAGE_SIZE, IMAGE_SIZE, 3))
        interpreter.allocateTensors()
    }

    fun classifyImage(bitmap: Bitmap): String {
        try {
            // 1. RECORTE PERFECTO CENTRADO (Para UX de los niños)
            val dimension = min(bitmap.width, bitmap.height)
            val x = (bitmap.width - dimension) / 2
            val y = (bitmap.height - dimension) / 2
            val imagenCuadrada = Bitmap.createBitmap(bitmap, x, y, dimension, dimension)

            // 2. REDIMENSIONAR AL TAMAÑO EXACTO DE LA IA (384x384)
            val resizedBitmap = Bitmap.createScaledBitmap(imagenCuadrada, IMAGE_SIZE, IMAGE_SIZE, true)

            // 3. Crear el buffer de memoria (1,769,472 bytes exactos)
            val byteBuffer = ByteBuffer.allocateDirect(4 * IMAGE_SIZE * IMAGE_SIZE * 3)
            byteBuffer.order(ByteOrder.nativeOrder())

            // 4. Extraer los colores
            val intValues = IntArray(IMAGE_SIZE * IMAGE_SIZE)
            resizedBitmap.getPixels(intValues, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

// 5. Normalización Cruda (¡El secreto para EfficientNetV2!)
            var pixel = 0
            for (i in 0 until IMAGE_SIZE) {
                for (j in 0 until IMAGE_SIZE) {
                    val valPixel = intValues[pixel++]

                    val r = ((valPixel shr 16) and 0xFF).toFloat()
                    val g = ((valPixel shr 8) and 0xFF).toFloat()
                    val b = (valPixel and 0xFF).toFloat()

                    // Enviamos los colores puros, sin divisiones
                    byteBuffer.putFloat(r)
                    byteBuffer.putFloat(g)
                    byteBuffer.putFloat(b)
                }
            }

            // 6. Ejecutar la IA
            val output = Array(1) { FloatArray(labels.size) }
            interpreter.run(byteBuffer, output)

            // 7. Buscar probabilidad mayor
            var maxIndex = 0
            var maxProb = output[0][0]
            for (i in 1 until labels.size) {
                if (output[0][i] > maxProb) {
                    maxIndex = i
                    maxProb = output[0][i]
                }
            }

            android.util.Log.d("IA_MINIECO", "Detectado: ${labels[maxIndex]} con ${(maxProb * 100).toInt()}%")

            // 8. LA REGLA DEL UMBRAL: Si duda (menos del 75%), es "Inseguro"
            if (maxProb < 0.75f) {
                return "Inseguro"
            }

            return labels[maxIndex]

        } catch (e: Exception) {
            // SI ALGO EXPLOTA, LO ATRAPAMOS AQUÍ Y LA APP NO SE CIERRA
            android.util.Log.e("IA_MINIECO_ERROR", "Error atrapado: ${e.message}")
            return "Inseguro"
        }
    }
}