package app.futured.donut

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import app.futured.donut.extensions.sumByFloat

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var circleRadius = 0f
        private set(value) {
            field = value
//            chartSections.forEach { it.drawableArc.radius = field }
        }

    private val chartSections = arrayListOf<DonutChartSection>()

    private var strokeWidthPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, resources.displayMetrics)
    private var animatorSet: AnimatorSet? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)

        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(width / 2f, height / 2f)

        chartSections.forEach { it.drawableArc.draw(canvas) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.circleRadius = Math.min(width, height) / 2f - strokeWidthPx / 2f
    }

    fun submitData(sections: List<DonutChartSection>) {
        chartSections.clear()

        val totalWeight = sections.sumByFloat { it.weight }

        sections.filter { it.weight >= 0f }.forEach { section ->
            chartSections.add(section)
            val arcStartPercentage = getStartPercentage(section)
            section.drawableArc.setProps(
                strokeWidthPx,
                circleRadius,
                arcStartPercentage,
                getEndPercentage(section.weight, arcStartPercentage, totalWeight)
            )
        }

        runAnimations()
    }

    private fun getStartPercentage(section: DonutChartSection): Float {
        val sectionIndex = chartSections.indexOf(section)
        if (sectionIndex <= 0) {
            return 0f
        } else {
            return chartSections[sectionIndex - 1].drawableArc.endPercentage

        }
    }

    private fun getEndPercentage(weight: Float, startPercentage: Float, totalWeight: Float): Float {
        return weight * 100f / totalWeight + startPercentage
    }

    private fun runAnimations() {
        animatorSet?.cancel()
        animatorSet = AnimatorSet()

        animatorSet?.playSequentially(
            chartSections.map { section ->
                section.drawableArc.getAnimation(animationProgress = { invalidate() })
            }
        )

        animatorSet?.start()
    }

    private fun log(label: String, value: String) {
        Log.e("DonutProgressView", "$label: $value")
    }
}
