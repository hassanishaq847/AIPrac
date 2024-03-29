package com.be.aiprac.face_landmarks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.os.Bundle
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
import java.util.LinkedList
import java.util.Queue

class FaceMeshActivity : AppCompatActivity() {

    private val TAG: String = FaceMeshActivity::class.java.name
    private lateinit var binding: ActivityFaceMeshBinding
    private lateinit var origBitmap: Bitmap


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceMeshBinding.inflate(layoutInflater)
        setContentView(binding.root)

        origBitmap = BitmapFactory.decodeResource(resources, R.drawable.image3)
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


                            val listUpper: ArrayList<Point> = ArrayList()
                            val listLower: ArrayList<Point> = ArrayList()
                            Constants.lipsUpper.forEach {
                                val normalizedLandmark =
                                    faceLandmarkerResult.faceLandmarks()[0][it]
                                val x = normalizedLandmark.x() * origBitmap.width * 1f
                                val y = normalizedLandmark.y() * origBitmap.height * 1f
                                listUpper.add(Point(x.toInt(), y.toInt()))
                            }


                            Constants.lipsLower.forEach {
                                val normalizedLandmark =
                                    faceLandmarkerResult.faceLandmarks()[0][it]
                                val x = normalizedLandmark.x() * origBitmap.width * 1f
                                val y = normalizedLandmark.y() * origBitmap.height * 1f
                                listLower.add(Point(x.toInt(), y.toInt()))
                            }

                            val pixelsListUpper = getPixelsBetweenPoints(listUpper)
                            val pixelsListLower = getPixelsBetweenPoints(listLower)
//                            Log.d(TAG, "goForFaceLandMarks: ${pixelsList.size}")

                            fillPolygonLips(modifiedBitmap, pixelsListUpper, Color.RED, 0.5f, 1.0f)
                            fillPolygonLips(modifiedBitmap, pixelsListLower, Color.RED, 0.5f, 1.0f)


                            withContext(Dispatchers.Main) {
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
        featheringDistance: Float
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

    private fun applyFeathering(bitmap: Bitmap, x: Int, y: Int, distance: Float, color: Int): Int {
        val featheredColor = ColorUtils.blendARGB(
            bitmap.getPixel(x, y),
            color,
            distance // Adjust the blending factor as needed
        )

        return featheredColor
    }

    private fun getPixelsBetweenPoints(points: List<Point>): List<Point> {
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

////////////////////////////////////////////

    class LipFiller(
        private val lipBoundaryPoints: Set<Point>
    ) {

        fun fillLipsRegion(bitmap: Bitmap): List<Point> {
            val resultPixels = mutableListOf<Point>()

            // Create a mutable bitmap to perform the flood-fill operation
            val mutableBitmap = Bitmap.createBitmap(bitmap)
            val canvas = Canvas(mutableBitmap)

            val paint = Paint()
            paint.color =
                Color.RED // You can set any color, it won't be visible in the final result

            for (point in lipBoundaryPoints) {
                // Draw the lip boundary on the canvas
                canvas.drawPoint(point.x.toFloat(), point.y.toFloat(), paint)
            }

            val width = mutableBitmap.width
            val height = mutableBitmap.height
            val visited = Array(width) { BooleanArray(height) }

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (!visited[x][y] && isPointInsideLips(x, y, mutableBitmap)) {
                        // Perform flood-fill from the current point
                        val filledRegion = floodFill(x, y, mutableBitmap, visited)
                        resultPixels.addAll(filledRegion)
                    }
                }
            }

            return resultPixels
        }

        private fun isPointInsideLips(x: Int, y: Int, bitmap: Bitmap): Boolean {
            return if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) {
                false
            } else {
                // Check if the pixel at the specified coordinates is not transparent
                bitmap.getPixel(x, y) != Color.TRANSPARENT
            }
        }

        private fun floodFill(
            x: Int,
            y: Int,
            bitmap: Bitmap,
            visited: Array<BooleanArray>
        ): List<Point> {
            val resultPixels = mutableListOf<Point>()
            val queue: Queue<Point> = LinkedList()
            queue.add(Point(x, y))

            val targetColor = bitmap.getPixel(x, y)

            while (queue.isNotEmpty()) {
                val current = queue.poll()

                if (current!!.x < 0 || current.x >= bitmap.width || current.y < 0 || current.y >= bitmap.height ||
                    visited[current.x][current.y] || bitmap.getPixel(
                        current.x,
                        current.y
                    ) != targetColor
                ) {
                    continue
                }

                visited[current.x][current.y] = true
                resultPixels.add(Point(current.x, current.y))

                // Add neighboring pixels to the queue
                queue.add(Point(current.x + 1, current.y))
                queue.add(Point(current.x - 1, current.y))
                queue.add(Point(current.x, current.y + 1))
                queue.add(Point(current.x, current.y - 1))
            }

            return resultPixels
        }
    }
}