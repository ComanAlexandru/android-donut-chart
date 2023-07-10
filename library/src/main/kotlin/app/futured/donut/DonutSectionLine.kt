package app.futured.donut

import android.graphics.Canvas
import android.graphics.ComposePathEffect
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import kotlin.math.ceil

internal class DonutSectionLine(
    val label: String,
    radius: Float,
    lineColor: Int,
    lineStrokeWidth: Float,
    length: Float,
    startAngleDegrees: Float,
) {

    companion object {
        const val SIDES = 64
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@DonutSectionLine.lineWidth
        color = this@DonutSectionLine.lineColor
    }

    var radius: Float = 0.0f
        set(value) {
            field = value
            updatePath()
            updatePathEffect()
        }

    var lineColor: Int = 0
        set(value) {
            field = value
            paint.color = value
        }

    var lineWidth: Float = 0.0f
        set(value) {
            field = value
            paint.strokeWidth = value
        }

    var mLength: Float = 0.0f
        set(value) {
            field = value
            updatePathEffect()
        }

    var angleStartDegrees = 270f
        set(value) {
            field = value
            updatePath()
            updatePathEffect()
        }

    private var path: Path = createPath()

    init {
        this.radius = radius
        this.lineColor = lineColor
        this.lineWidth = lineStrokeWidth
        this.mLength = length
        this.angleStartDegrees = startAngleDegrees
    }

    private fun createPath(): Path {
        val path = Path()

        val startAngleRadians = angleStartDegrees.toRadians()

        val endAngle = Math.PI * 2.0
        val angleStep = endAngle / SIDES

        path.moveTo(
            radius * Math.cos(startAngleRadians).toFloat(),
            radius * Math.sin(startAngleRadians).toFloat()
        )

        for (i in 1 until SIDES + 1) {
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
            CornerPathEffect(pathLen / SIDES),
            DashPathEffect(
                floatArrayOf(
                    drawnLength,
                    pathLen - drawnLength
                ),
                0f
            )
        )
    }

    fun draw(canvas: Canvas) {
        canvas.drawPath(path, paint)
    }

    private fun Float.toRadians() = Math.toRadians(this.toDouble())
}
