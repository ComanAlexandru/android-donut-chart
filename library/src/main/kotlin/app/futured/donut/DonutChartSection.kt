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
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import kotlin.math.ceil

class DonutChartSection(
    val id: String,
    val label: String,
    val color: Int,
    val weight: Float
) {

    internal val drawableArc = SectionArc(color)

    internal class SectionArc(lineColor: Int) {

        private val arcDrawLinesCount = 64
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var animationInterpolator: Interpolator = DecelerateInterpolator(1.5f)
        private var path: Path = Path()

        private var radius: Float = 0.0f

        var startPercentage = 0.0f
            private set(value) {
                field = value
                this.startAngleRadians = value.toRadians()
            }

        var endPercentage = 0.0f
            private set(value) {
                field = value
                this.endAngleRadians = value.toRadians()
            }

        private var startAngleRadians: Double = 0.0
            set(value) {
                field = value + 270f.toRadians()
            }

        private var endAngleRadians: Double = 0.0
            set(value) {
                field = value + 270f.toRadians()
            }

        init {
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = lineColor
        }

        fun setProps(strokeWidth: Float, radius: Float, startPercentage: Float, endPercentage: Float) {
            Log.e("setProps", "Start[$startPercentage] End[$endPercentage]")
            paint.strokeWidth = strokeWidth
            this.startPercentage = startPercentage
            this.endPercentage = endPercentage
            this.radius = radius
            computePath()
            updatePathEffect()
        }

        fun getAnimation(animationProgress: (() -> Unit)? = null): ValueAnimator {
            return ValueAnimator.ofFloat(startPercentage.toFloat(), endPercentage.toFloat()).apply {
                duration = 1000
                interpolator = animationInterpolator
                addUpdateListener { listener ->
                    (listener.animatedValue as? Float)?.let { animValue ->
                        updatePathEffect(animValue)
                    }
//                    Log.e("animProgress", "Start[$startAngleRadians] Progress[${listener.animatedValue}] End[$endAngleRadians]")
                    animationProgress?.invoke()
                }

                doOnEnd {
                    Log.e("animProgress", "END for $endAngleRadians")
                }
            }
        }

//        fun getAnimationOld(
//            to: Float,
//            animationProgress: (() -> Unit)? = null,
//            animationEnded: (() -> Unit)? = null
//        ): ValueAnimator {
//
//            return ValueAnimator.ofFloat(mAngleAnimationProgress, to).apply {
//                duration = 1000
//                interpolator = animationInterpolator
//                addUpdateListener {
//                    (it.animatedValue as? Float)?.let { animValue ->
//                        mAngleAnimationProgress = animValue
//                    }
//                    Log.e("getAnimationOld", "From $mAngleAnimationProgress to $to")
//                    animationProgress?.invoke()
//                }
//
//                doOnEnd {
//                    animationEnded?.invoke()
//                }
//            }
//        }

        fun draw(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }

        private fun computePath() {
            val newPath = Path()

            val endAngle = Math.PI * 2.0
            val angleStep = endAngle / arcDrawLinesCount

            newPath.moveTo(
                radius * Math.cos(startAngleRadians).toFloat(),
                radius * Math.sin(startAngleRadians).toFloat()
            )

            for (i in 1 until arcDrawLinesCount + 1) {
                newPath.lineTo(
                    radius * Math.cos(i * angleStep + startAngleRadians).toFloat(),
                    radius * Math.sin(i * angleStep + startAngleRadians).toFloat()
                )
            }

            this.path = newPath
        }

        private fun updatePathEffect(animationProgress: Float = endAngleRadians.toFloat()) {
            val pathLen = PathMeasure(path, false).length
            val drawnLength = ceil(pathLen * animationProgress)

            paint.pathEffect = ComposePathEffect(
                CornerPathEffect(pathLen / arcDrawLinesCount),
                DashPathEffect(floatArrayOf(drawnLength, pathLen - drawnLength), 0f)
            )
        }

        private fun Float.toRadians() = Math.toRadians(this.toDouble())
    }

}
