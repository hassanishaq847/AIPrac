package com.be.aiprac.hair_background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.graphics.ColorUtils
import com.be.aiprac.R
import com.be.aiprac.databinding.ActivityMainBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Timer

class MainActivity : AppCompatActivity(), ImageSegmenterHelper.SegmenterListener {

    private val TAG: String = MainActivity::class.java.name
    private lateinit var binding: ActivityMainBinding
    private lateinit var origBitmap: Bitmap
    private var targetColor: Int = Color.RED

    private var imageSegmenterHelper: ImageSegmenterHelper? = null
    private var backgroundScope: CoroutineScope? = null
    private var fixedRateTimer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setListeners()
    }

    private fun setListeners() {
        binding.iv1.setOnClickListener {
            callToProcess(R.drawable.image1)
        }

        binding.iv2.setOnClickListener {
            callToProcess(R.drawable.image2)
        }

        binding.iv3.setOnClickListener {
            callToProcess(R.drawable.image3)
        }

        binding.iv4.setOnClickListener {
            callToProcess(R.drawable.image4)
        }

        binding.iv5.setOnClickListener {
            callToProcess(R.drawable.image8)
        }

        binding.iv6.setOnClickListener {
            callToProcess(R.drawable.image11)
        }

        binding.btnRed.setOnClickListener {
            targetColor = Color.RED
        }

        binding.btnGreen.setOnClickListener {
            targetColor = Color.GREEN
        }

        binding.btnBlue.setOnClickListener {
            targetColor = Color.BLUE
        }

        binding.btnGray.setOnClickListener {
            targetColor = Color.BLACK
        }
    }

    private fun callToProcess(image: Int) {
        origBitmap = BitmapFactory.decodeResource(resources, image)
        runSegmentationOnImage(origBitmap)
    }

    // Load and display the image.
    private fun runSegmentationOnImage(bitmap: Bitmap) {
        binding.overlayView.setRunningMode(RunningMode.IMAGE)
        var inputImage = bitmap
        inputImage = inputImage.scaleDown(512f)
        origBitmap = inputImage
        // display image on UI
        binding.imageResult.setImageBitmap(inputImage)

        backgroundScope = CoroutineScope(Dispatchers.IO)

        imageSegmenterHelper = ImageSegmenterHelper(
            context = this@MainActivity,
            runningMode = RunningMode.IMAGE,
            currentDelegate = ImageSegmenterHelper.DELEGATE_CPU,
            imageSegmenterListener = this
        )

        // Run image segmentation on the input image
        backgroundScope?.launch {
            val mpImage = BitmapImageBuilder(inputImage).build()
            val result = imageSegmenterHelper?.segmentImageFile(mpImage)
            if (result != null) {
                updateOverlay(result)
            }
        }
    }

    private fun updateOverlay(result: ImageSegmenterResult) {
        val newImage = result.categoryMask().get()
        updateOverlay(
            ImageSegmenterHelper.ResultBundle(
                ByteBufferExtractor.extract(newImage),
                newImage.width,
                newImage.height,
                result.timestampMs()
            )
        )
    }

    private fun updateOverlay(resultBundle: ImageSegmenterHelper.ResultBundle) {
        runBlocking {
            withContext(Dispatchers.Main) {
//                    fragmentGalleryBinding.bottomSheetLayout.inferenceTimeVal.text =
//                        String.format("%d ms", resultBundle.inferenceTime)
//                binding.overlayView.setResults(
//                    resultBundle.results,
//                    resultBundle.width,
//                    resultBundle.height
//                )

                val intArray: ArrayList<Int> = ArrayList()
                val resultBuffer = resultBundle.results

                for (i in 0 until resultBuffer.capacity()) {
                    intArray.add(resultBuffer.get(i).toInt())
                }
                applyRealisticHairColorWithBoundaries(origBitmap, intArray)
//                changBackgroundColor(origBitmap, intArray)
//                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.bg2)
//                replaceBackgroundWithBitmap(origBitmap, intArray, bitmap)
            }
        }
    }

    private fun Bitmap.scaleDown(targetWidth: Float): Bitmap {
        // if this image smaller than widthSize, return original image
        if (targetWidth >= width) return this
        val scaleFactor = targetWidth / width
        return Bitmap.createScaledBitmap(
            this,
            (width * scaleFactor).toInt(),
            (height * scaleFactor).toInt(),
            false
        )
    }

    override fun onError(error: String, errorCode: Int) {
        backgroundScope?.launch {
            withContext(Dispatchers.Main) {
                segmentationError()
                Log.d(TAG, "onError: $error")
            }
        }
    }

    override fun onResults(resultBundle: ImageSegmenterHelper.ResultBundle) {
        updateOverlay(resultBundle)
    }

    private fun segmentationError() {
        stopAllTasks()
    }

    private fun stopAllTasks() {
        // cancel all jobs
        fixedRateTimer?.cancel()
        fixedRateTimer = null
        backgroundScope?.cancel()
        backgroundScope = null

        // clear Image Segmenter
        imageSegmenterHelper?.clearListener()
        imageSegmenterHelper?.clearImageSegmenter()
        imageSegmenterHelper = null

        with(binding) {
            overlayView.clear()
            progress.visibility = View.GONE
        }
    }


    private fun applyRealisticHairColorWithBoundaries(
        originalBitmap: Bitmap,
        segmentedPixels: ArrayList<Int>
    ): Bitmap {
        val hairLabel = 1
        val opacity = 0.3f
        val featheringDistance = 5 // Adjust the feathering distance as needed

        // Ensure that the sizes match
        require(originalBitmap.width * originalBitmap.height == segmentedPixels.size) {
            "Size mismatch between bitmap and segmentedPixels array"
        }

        val modifiedBitmap = originalBitmap.copy(originalBitmap.config, true)

        for (y in 0 until originalBitmap.height) {
            for (x in 0 until originalBitmap.width) {
                try {
                    val index = y * originalBitmap.width + x

                    // Check if the index is within the valid range
                    if (index < segmentedPixels.size) {
                        val label = segmentedPixels[index]

                        // Check if the pixel belongs to the hair region
                        if (label == hairLabel) {
                            // Get the original color of the pixel
                            val originalColor = originalBitmap.getPixel(x, y)
                            // Modify the color - for example, add some red tint
                            val modifiedColor = Color.argb(
                                Color.alpha(originalColor),
                                (Color.red(originalColor) * (1 - opacity)).toInt() + (Color.red(
                                    targetColor
                                ) * opacity).toInt(),
                                (Color.green(originalColor) * (1 - opacity)).toInt() + (Color.green(
                                    targetColor
                                ) * opacity).toInt(),
                                (Color.blue(originalColor) * (1 - opacity)).toInt() + (Color.blue(
                                    targetColor
                                ) * opacity).toInt()
                            )

                            // Apply feathering along the boundaries
                            val featheredColor = applyFeathering(
                                originalBitmap,
                                x,
                                y,
                                featheringDistance,
                                modifiedColor
                            )

                            // Apply the modified color with blending
                            modifiedBitmap.setPixel(x, y, featheredColor)
                        }
                        // Add more conditions for other classes if needed
                    } else {
                        Log.e("applyRealisticHairColor", "Index out of bounds: $index")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "applyRealisticHairColor",
                        "Exception while processing pixel at ($x, $y): ${e.message}"
                    )
                }
            }
        }

        // Set the modified bitmap to the ImageView
        binding.imageResult.setImageBitmap(modifiedBitmap)

        return modifiedBitmap
    }

    private fun applyFeathering(bitmap: Bitmap, x: Int, y: Int, distance: Int, color: Int): Int {
        val featheredColor = ColorUtils.blendARGB(
            bitmap.getPixel(x, y),
            color,
            0.5f // Adjust the blending factor as needed
        )

        return featheredColor
    }

    private fun changBackgroundColor(
        originalBitmap: Bitmap,
        segmentedPixels: ArrayList<Int>
    ) {
        val bgLabel = 0

        // Ensure that the sizes match
        require(originalBitmap.width * originalBitmap.height == segmentedPixels.size) {
            "Size mismatch between bitmap and segmentedPixels array"
        }

        val modifiedBitmap = originalBitmap.copy(originalBitmap.config, true)

        for (y in 0 until originalBitmap.height) {
            for (x in 0 until originalBitmap.width) {
                try {
                    val index = y * originalBitmap.width + x

                    // Check if the index is within the valid range
                    if (index < segmentedPixels.size) {
                        val label = segmentedPixels[index]

                        // Check if the pixel belongs to the hair region
                        if (label == bgLabel) {
                            // Apply an effect, for example, change the color to red
                            modifiedBitmap.setPixel(x, y, targetColor)
                        }
                        // Add more conditions for other classes if needed
                    } else {
                        Log.e("applyEffectsToHair", "Index out of bounds: $index")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "applyEffectsToHair",
                        "Exception while processing pixel at ($x, $y): ${e.message}"
                    )
                }
            }
        }

        // Set the modified bitmap to the ImageView
        binding.imageResult.setImageBitmap(modifiedBitmap)
    }

    fun replaceBackgroundWithBitmap(
        originalBitmap: Bitmap,
        segmentedPixels: ArrayList<Int>,
        backgroundBitmap: Bitmap
    ): Bitmap {
        val backgroundLabel = 0

        // Ensure that the sizes match
        require(originalBitmap.width * originalBitmap.height == segmentedPixels.size) {
            "Size mismatch between bitmap and segmentedPixels array"
        }

        // Create a new bitmap with the same dimensions as the original bitmap
        val modifiedBitmap =
            Bitmap.createBitmap(originalBitmap.width, originalBitmap.height, originalBitmap.config)

        val canvas = Canvas(modifiedBitmap)

        // Draw the background bitmap onto the canvas
        canvas.drawBitmap(backgroundBitmap, 0f, 0f, null)

        for (y in 0 until originalBitmap.height) {
            for (x in 0 until originalBitmap.width) {
                try {
                    val index = y * originalBitmap.width + x

                    // Check if the index is within the valid range
                    if (index < segmentedPixels.size) {
                        val label = segmentedPixels[index]

                        // Check if the pixel belongs to the background
                        if (label == backgroundLabel) {
                            // Do nothing, background is already drawn
                        } else {
                            // Draw the original pixel onto the canvas for non-background pixels
                            modifiedBitmap.setPixel(x, y, originalBitmap.getPixel(x, y))
                        }
                    } else {
                        Log.e("replaceBackgroundWithBitmap", "Index out of bounds: $index")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "replaceBackgroundWithBitmap",
                        "Exception while processing pixel at ($x, $y): ${e.message}"
                    )
                }
            }
        }

        // Set the modified bitmap to the ImageView
        binding.imageResult.setImageBitmap(modifiedBitmap)

        return modifiedBitmap
    }

}