package app.futured.donut

import android.graphics.Canvas
import android.graphics.ComposePathEffect
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.ceil

class DonutChartSection(
    val id: String,
    val label: String,
    val color: Int,
    val weight: Float
) {

    internal lateinit var drawableArc: SectionArc

    internal class SectionArc(
        radius: Float,
        lineColor: Int,
        strokeWidth: Float,
        length: Float,
        startAngleDegrees: Float,
    ) {

        private val arcDrawLinesCount = 64
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var path: Path = createPath()

        var radius: Float = 0.0f
            set(value) {
                field = value
                updatePath()
                updatePathEffect()
            }

        var mLength: Float = 0.0f
            set(value) {
                field = value
                updatePathEffect()
            }

        private val startAngleRadians: Double

        init {

            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeWidth = strokeWidth
            paint.color = lineColor

            this.startAngleRadians = startAngleDegrees.toRadians()

            this.radius = radius
            this.mLength = length

        }

        fun draw(canvas: Canvas) {
            canvas.drawPath(path, paint)
        }

        private fun createPath(): Path {
            val path = Path()

            val endAngle = Math.PI * 2.0
            val angleStep = endAngle / arcDrawLinesCount

            path.moveTo(
                radius * Math.cos(startAngleRadians).toFloat(),
                radius * Math.sin(startAngleRadians).toFloat()
            )

            for (i in 1 until arcDrawLinesCount + 1) {
                path.lineTo(
                    radius * Math.cos(i * angleStep + startAngleRadians).toFloat(),
                    radius * Math.sin(i * angleStep + startAngleRadians).toFloat()
                )
            }

            return path
        }

        private fun updatePath() {
            this.path = createPath()
        }

        private fun updatePathEffect() {
            val pathLen = PathMeasure(path, false).length
            val drawnLength = ceil(pathLen.toDouble() * mLength).toFloat()

            paint.pathEffect = ComposePathEffect(
                CornerPathEffect(pathLen / arcDrawLinesCount),
                DashPathEffect(
                    floatArrayOf(
                        drawnLength,
                        pathLen - drawnLength
                    ),
                    0f
                )
            )
        }

        private fun Float.toRadians() = Math.toRadians(this.toDouble())
    }

}
