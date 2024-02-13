package com.be.aiprac.face_landmarks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.be.aiprac.R
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayViewMesh(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1

    init {
        initPaints()
    }

    fun clear() {
        results = null
        linePaint.reset()
        pointPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        linePaint.color =
            ContextCompat.getColor(context!!, R.color.black)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (results == null || results!!.faceLandmarks().isEmpty()) {
            clear()
            return
        }

        results?.let { faceLandmarkerResult ->

            for (landmark in faceLandmarkerResult.faceLandmarks()) {
                for (normalizedLandmark in landmark) {
                    val x = normalizedLandmark.x() * imageWidth * scaleFactor
                    val y = normalizedLandmark.y() * imageHeight * scaleFactor
                    canvas.drawPoint(x, y, pointPaint)

                }
            }

            FaceLandmarker.FACE_LANDMARKS_LIPS.forEach {
                Log.d(TAG, "FACE_LANDMARKS_LIPS: ${it.start()},${it.end()}")
                canvas.drawLine(
                    faceLandmarkerResult.faceLandmarks()[0][it!!.start()]
                        .x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks()[0][it.start()]
                        .y() * imageHeight * scaleFactor,
                    faceLandmarkerResult.faceLandmarks()[0][it.end()]
                        .x() * imageWidth * scaleFactor,
                    faceLandmarkerResult.faceLandmarks()[0][it.end()]
                        .y() * imageHeight * scaleFactor,
                    linePaint
                )
            }
        }
    }


    fun setResults(
        faceLandmarkerResults: FaceLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = faceLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / imageWidth, height * 1f / imageHeight)
            }

            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / imageWidth, height * 1f / imageHeight)
            }
        }
        Log.d(TAG, "setResults: $scaleFactor : $width,$height")
        scaleFactor = .5f
        invalidate()
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 4F
        private const val TAG = "Face Landmarker Overlay"
    }
}
