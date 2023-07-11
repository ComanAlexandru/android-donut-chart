package app.futured.donut

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ComposePathEffect
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.Log
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import kotlin.math.ceil

class DonutChartSection(
    val label: String,
    val color: Int,
    val weight: Float
) {

    internal val drawableArc = SectionArc(color)

    internal class SectionArc(lineColor: Int) {

        private val SEGMENTS_GAP_PERCENTAGE = 3f

        private val arcDrawLinesCount = 64
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var animationInterpolator: Interpolator = AccelerateDecelerateInterpolator()
        private var path: Path = Path()

        private var radius: Float = 0.0f

        private var weightPercentage: Float = 0.0f

        var startPercentage = 0.0f
            private set(value) {
                field = value + SEGMENTS_GAP_PERCENTAGE
                this.startAngleRadians = (field - SEGMENTS_GAP_PERCENTAGE / 2).toRadiansFromPercentage()
            }

        var endPercentage = 0.0f
            private set(value) {
                field = value
                this.endAngleRadians = (field - SEGMENTS_GAP_PERCENTAGE / 2).toRadiansFromPercentage()
            }

        private var startAngleRadians: Double = 0.0
        private var endAngleRadians: Double = 0.0

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = lineColor
        }

        fun setProps(weightPercentage: Float, strokeWidth: Float, radius: Float, startPercentage: Float, endPercentage: Float) {
            paint.strokeWidth = strokeWidth
            this.weightPercentage = weightPercentage
            this.startPercentage = startPercentage
            this.endPercentage = endPercentage
            this.radius = radius
        }

        fun getAnimation(animationProgress: (() -> Unit)? = null): ValueAnimator {
            return ValueAnimator.ofFloat(0f, 1f).apply {
                duration = (500 * weightPercentage / 100 + 200).toLong()
                interpolator = animationInterpolator
                addUpdateListener { listener ->
                    (listener.animatedValue as? Float)?.let { animValue ->
                        updatePathEffect(animValue)
                    }
                    animationProgress?.invoke()
                }

                doOnStart {
                    computePath()
                }
            }
        }

        fun draw(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }

        private fun computePath() {
            val newPath = Path()

            val angleRange = endAngleRadians - startAngleRadians
            val angleStep = angleRange / arcDrawLinesCount

            newPath.moveTo(
                radius * Math.cos(startAngleRadians).toFloat(),
                radius * Math.sin(startAngleRadians).toFloat()
            )

            for (i in 1 until arcDrawLinesCount + 1) {
                val angle = startAngleRadians + i * angleStep
                newPath.lineTo(
                    radius * Math.cos(angle).toFloat(),
                    radius * Math.sin(angle).toFloat()
                )
            }

            this.path = newPath
        }

        private fun updatePathEffect(animationProgress: Float) {
            val pathLen = PathMeasure(path, false).length
            val drawnLength = ceil(pathLen * animationProgress)

            paint.pathEffect = ComposePathEffect(
                CornerPathEffect(pathLen / arcDrawLinesCount),
                DashPathEffect(floatArrayOf(drawnLength, pathLen - drawnLength), 0f)
            )
        }

        private fun Float.toRadiansFromPercentage() = Math.toRadians(this.toDouble() * 360.0 / 100.0 - 90.0)

    }

}
