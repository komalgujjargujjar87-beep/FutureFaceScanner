package com.example.futurefacescanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var overlay: ScannerOverlay
    private lateinit var status: TextView
    private lateinit var cameraExecutor: ExecutorService

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission is needed for face detection.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(3, 8, 12)) }
        previewView = PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
        root.addView(previewView, FrameLayout.LayoutParams(-1, -1))

        overlay = ScannerOverlay(this)
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))

        val header = TextView(this).apply {
            text = "◈  FUTURE FACE SCANNER\nLIVE CAMERA • ON-DEVICE DETECTION"
            setTextColor(Color.rgb(65, 255, 190))
            textSize = 15f
            typeface = Typeface.create("monospace", Typeface.BOLD)
            setPadding(20, 22, 20, 18)
            setBackgroundColor(0xCC03080C.toInt())
        }
        root.addView(header, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))

        status = TextView(this).apply {
            text = "INITIALIZING SCANNER..."
            setTextColor(Color.rgb(65, 255, 190))
            textSize = 14f
            typeface = Typeface.create("monospace", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(12, 16, 12, 20)
            setBackgroundColor(0xCC03080C.toInt())
        }
        root.addView(status, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
        setContentView(root)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
            val detector = FaceDetection.getClient(options)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(cameraExecutor) { proxy ->
                val mediaImage = proxy.image
                if (mediaImage == null) {
                    proxy.close()
                } else {
                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                    detector.process(image)
                        .addOnSuccessListener { faces ->
                            overlay.setFaces(faces.map { it.boundingBox })
                            runOnUiThread {
                                status.text = if (faces.isEmpty()) {
                                    "SCANNING...  NO FACE DETECTED"
                                } else {
                                    "FACE DETECTED: ${faces.size}   •   VISUAL DEMO"
                                }
                            }
                        }
                        .addOnFailureListener {
                            runOnUiThread { status.text = "SCANNER READY • TRY BETTER LIGHTING" }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
            }

            try {
                provider.unbindAll()
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Toast.makeText(this, "Could not start camera: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

private class ScannerOverlay(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    @Volatile private var boxes: List<Rect> = emptyList()
    private var scanY = 0f

    init {
        post(object : Runnable {
            override fun run() {
                scanY = if (height > 0) (scanY + 7f) % height else 0f
                invalidate()
                postDelayed(this, 35)
            }
        })
    }

    fun setFaces(newBoxes: List<Rect>) {
        boxes = newBoxes
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Futuristic corner marks / HUD framing
        paint.color = Color.rgb(65, 255, 190)
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        val margin = 28f
        val len = 34f
        val corners = listOf(
            floatArrayOf(margin, margin, 1f, 1f),
            floatArrayOf(w-margin, margin, -1f, 1f),
            floatArrayOf(margin, h-margin, 1f, -1f),
            floatArrayOf(w-margin, h-margin, -1f, -1f)
        )
        for (c in corners) {
            canvas.drawLine(c[0], c[1], c[0] + len*c[2], c[1], paint)
            canvas.drawLine(c[0], c[1], c[0], c[1] + len*c[3], paint)
        }

        // Animated scan line (visual effect only)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0xCC41FFBE.toInt()
        canvas.drawLine(margin, scanY, w-margin, scanY, paint)

        // ML Kit returns image-space coordinates; map approximately to preview.
        val sx = w / 480f
        val sy = h / 640f
        for (box in boxes) {
            val left = box.left * sx
            val top = box.top * sy
            val right = box.right * sx
            val bottom = box.bottom * sy
            paint.color = Color.rgb(255, 65, 105)
            paint.strokeWidth = 4f
            canvas.drawRoundRect(left, top, right, bottom, 18f, 18f, paint)
            paint.style = Paint.Style.FILL
            paint.textSize = 25f
            paint.typeface = Typeface.create("monospace", Typeface.BOLD)
            canvas.drawText("FACE DETECTED", left, (top - 10f).coerceAtLeast(100f), paint)
            paint.style = Paint.Style.STROKE
        }
        paint.style = Paint.Style.FILL
        paint.color = 0xCC41FFBE.toInt()
        paint.textSize = 12f
        paint.typeface = Typeface.create("monospace", Typeface.NORMAL)
        canvas.drawText("HUD / DEMO MODE", margin, h - 78f, paint)
    }
}
