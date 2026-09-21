package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.media.ExifInterface
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * CameraCaptureHelper provides a clean CameraX integration for document scanning and OCR.
 * It manages camera lifecycle, preview view binding, torch/flash controls,
 * tap-to-focus for crystal-clear text, and high-fidelity image capture for documents.
 */
class CameraCaptureHelper(private val context: Context) {

    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var isTorchEnabled: Boolean = false
        private set

    /**
     * Binds CameraX Preview and ImageCapture to the given PreviewView and LifecycleOwner.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onReady: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                cameraProvider?.unbindAll()
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                onReady?.invoke()
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Triggers autofocus and auto-exposure metering on a tapped coordinate of the preview.
     */
    fun focusOnPoint(x: Float, y: Float, previewView: PreviewView) {
        val cam = camera ?: return
        try {
            val factory: MeteringPointFactory = previewView.meteringPointFactory
            val point = factory.createPoint(x, y)
            val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            cam.cameraControl.startFocusAndMetering(action)
        } catch (e: Exception) {
            Log.e(TAG, "Focus and metering failed", e)
        }
    }

    /**
     * Toggles camera torch/flashlight for scanning in low light.
     */
    fun toggleTorch(onSuccess: (Boolean) -> Unit = {}) {
        camera?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                isTorchEnabled = !isTorchEnabled
                cam.cameraControl.enableTorch(isTorchEnabled)
                onSuccess(isTorchEnabled)
            }
        }
    }

    /**
     * Captures a high-resolution photo directly to a target File on disk.
     */
    fun capturePhotoToFile(
        outputFile: File,
        onSuccess: (File) -> Unit,
        onError: (ImageCaptureException) -> Unit
    ) {
        val capture = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    ContextCompat.getMainExecutor(context).execute {
                        onSuccess(outputFile)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exc)
                    }
                }
            }
        )
    }

    /**
     * Decodes the captured image file with correct EXIF orientation for preview or OCR.
     */
    fun decodeOrientedBitmap(file: File): Bitmap? {
        val original = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return original
            }
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        } catch (_: Exception) {
            original
        }
    }

    /**
     * Captures in-memory Bitmap and adjusts rotation for document analysis & OCR.
     */
    fun captureBitmap(
        onSuccess: (Bitmap) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: return

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                        val buffer = imageProxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)

                        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        val finalBitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        } else {
                            rawBitmap
                        }

                        ContextCompat.getMainExecutor(context).execute {
                            onSuccess(finalBitmap)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        ContextCompat.getMainExecutor(context).execute {
                            onError(e)
                        }
                    } finally {
                        imageProxy.close()
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Capture failed: ${exc.message}", exc)
                    ContextCompat.getMainExecutor(context).execute {
                        onError(exc)
                    }
                }
            }
        )
    }

    /**
     * Helper to create a timestamped file for saving document scans.
     */
    fun createDocumentScanFile(): File {
        val scanDir = File(context.filesDir, "scans")
        if (!scanDir.exists()) scanDir.mkdirs()
        return File(scanDir, "DOC_SCAN_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Releases executor and camera resources.
     */
    fun shutdown() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraCaptureHelper"
    }
}
