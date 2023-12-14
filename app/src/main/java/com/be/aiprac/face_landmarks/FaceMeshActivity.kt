package com.be.aiprac.face_landmarks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import com.be.aiprac.Constants
import com.be.aiprac.R
import com.be.aiprac.databinding.ActivityFaceMeshBinding
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class FaceMeshActivity : AppCompatActivity() {

    private val TAG: String = FaceMeshActivity::class.java.name
    private lateinit var binding: ActivityFaceMeshBinding
    private lateinit var origBitmap: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceMeshBinding.inflate(layoutInflater)
        setContentView(binding.root)

        origBitmap = BitmapFactory.decodeResource(resources, R.drawable.image10)
        binding.ivMain.setImageBitmap(origBitmap)
        goForFaceLandMarks(origBitmap)
    }

    private fun goForFaceLandMarks(bitmap: Bitmap) {
        val faceLandmarkerHelper =
            FaceLandmarkerHelper(
                context = this,
                runningMode = RunningMode.IMAGE,
                minFaceDetectionConfidence = FaceLandmarkerHelper.DEFAULT_FACE_DETECTION_CONFIDENCE,
                minFaceTrackingConfidence = FaceLandmarkerHelper.DEFAULT_FACE_TRACKING_CONFIDENCE,
                minFacePresenceConfidence = FaceLandmarkerHelper.DEFAULT_FACE_PRESENCE_CONFIDENCE,
                maxNumFaces = FaceLandmarkerHelper.DEFAULT_NUM_FACES,
                currentDelegate = FaceLandmarkerHelper.DELEGATE_CPU
            )

        faceLandmarkerHelper.detectImage(bitmap)
            .let { resultBundle ->
                resultBundle!!.result.let { faceLandmarkerResult ->

                    CoroutineScope(Dispatchers.IO).launch {
                        runBlocking {
                            val modifiedBitmap = origBitmap.copy(origBitmap.config, true)

                            val list: ArrayList<Point> = ArrayList()
                            Constants.arrayLipsMesh.forEach {

                                val normalizedLandmark = faceLandmarkerResult.faceLandmarks()[0][it]

                                val x = normalizedLandmark.x() * origBitmap.width * 1f
                                val y = normalizedLandmark.y() * origBitmap.height * 1f
                                list.add(Point(x.toInt(), y.toInt()))
//                                modifiedBitmap.setPixel(x.toInt(), y.toInt(), Color.BLACK)

                            }

                            val pixelsList = getPixelsBetweenPoints(list)
                            Log.d(TAG, "goForFaceLandMarks: ${pixelsList.size}")

                            fillPolygonLips(modifiedBitmap, pixelsList, Color.RED, 0.6f, 5)
//                            pixelsList.forEach {
//                                modifiedBitmap.setPixel(it.x, it.y, Color.BLACK)
//                            }


                            withContext(Dispatchers.Main) {
                                Log.d(TAG, "goForFaceLandMarks: true")
                                binding.ivMain.setImageBitmap(modifiedBitmap)
                            }
                        }
                    }
                }

//                binding.overlayMesh.setResults(
//                    resultBundle!!.result,
//                    origBitmap.height,
//                    origBitmap.width,
//                    RunningMode.IMAGE
//                )

                /*resultBundle.result.facialTransformationMatrixes().get().forEach { values ->
                    Log.d(
                        "result_facialTransformationMatrixes2",
                        values.size.toString() + "\n\n"
                    )
                    values.forEach {
                        Log.d(
                            "result_facialTransformationMatrixes",
                            it.toString()
                        )
                    }
                }*/

            }
    }

    private fun fillPolygonLips(
        bitmap: Bitmap,
        boundaryPoints: List<Point>,
        targetColor: Int,
        opacity: Float,
        featheringDistance: Int
    ) {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            val intersections = mutableListOf<Int>()

            for (i in boundaryPoints.indices) {
                val p1 = boundaryPoints[i]
                val p2 = boundaryPoints[(i + 1) % boundaryPoints.size]

                if ((p1.y > y && p2.y <= y) || (p2.y > y && p1.y <= y)) {
                    val xIntersection =
                        ((p1.x + (y - p1.y) / (p2.y - p1.y) * (p2.x - p1.x))).toInt()
                    intersections.add(xIntersection)
                }
            }

            intersections.sort()

            for (i in 0 until intersections.size / 2) {
                val xStart = intersections[2 * i]
                val xEnd = intersections[2 * i + 1]

                for (x in xStart until xEnd) {
                    // Get the original color of the pixel
                    val originalColor = bitmap.getPixel(x, y)

                    // Modify the color with blending
                    val modifiedColor = blendColors(
                        originalColor,
                        targetColor,
                        opacity
                    )

                    // Apply feathering along the boundaries
                    val featheredColor = applyFeathering(
                        bitmap,
                        x,
                        y,
                        featheringDistance,
                        modifiedColor
                    )

                    // Apply the modified color with blending
                    bitmap.setPixel(x, y, featheredColor)
                }
            }
        }
    }

    private fun blendColors(existingColor: Int, targetColor: Int, opacity: Float): Int {
        return Color.argb(
            Color.alpha(existingColor),
            (Color.red(existingColor) * (1 - opacity)).toInt() + (Color.red(targetColor) * opacity).toInt(),
            (Color.green(existingColor) * (1 - opacity)).toInt() + (Color.green(targetColor) * opacity).toInt(),
            (Color.blue(existingColor) * (1 - opacity)).toInt() + (Color.blue(targetColor) * opacity).toInt()
        )
    }

    private fun applyFeathering(bitmap: Bitmap, x: Int, y: Int, distance: Int, color: Int): Int {
        val featheredColor = ColorUtils.blendARGB(
            bitmap.getPixel(x, y),
            color,
            0.5f // Adjust the blending factor as needed
        )

        return featheredColor
    }


    fun getPixelsBetweenPoints(points: List<Point>): List<Point> {
        val pixels = mutableListOf<Point>()

        for (i in 0 until points.size - 1) {
            pixels.addAll(getPixelsBetweenTwoPoints(points[i], points[i + 1]))
        }

        return pixels
    }

    private fun getPixelsBetweenTwoPoints(point1: Point, point2: Point): List<Point> {
        val pixels = mutableListOf<Point>()

        val x1 = point1.x
        val y1 = point1.y
        val x2 = point2.x
        val y2 = point2.y

        val dx = Math.abs(x2 - x1)
        val dy = Math.abs(y2 - y1)

        val sx = if (x1 < x2) 1 else -1
        val sy = if (y1 < y2) 1 else -1

        var err = dx - dy

        var x = x1
        var y = y1

        while (true) {
            pixels.add(Point(x, y))

            if (x == x2 && y == y2) {
                break
            }

            val e2 = 2 * err

            if (e2 > -dy) {
                err -= dy
                x += sx
            }

            if (e2 < dx) {
                err += dx
                y += sy
            }
        }

        return pixels
    }

}