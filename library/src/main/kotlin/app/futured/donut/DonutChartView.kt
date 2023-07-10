package app.futured.donut

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import app.futured.donut.extensions.sumByFloat

class DonutChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var width = 0
    private var height = 0
    private var circleRadius = 0f

    private val chartSections = arrayListOf<DonutChartSection>()
    private var totalWeight: Float = 0f

    private var strokeWidthPx: Float = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12f, resources.displayMetrics)
    private var animationInterpolator: Interpolator = DecelerateInterpolator(1.5f)
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
        canvas.translate(this.width / 2f, this.height / 2f)

        chartSections.forEach { it.drawableArc.draw(canvas) }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.width = w
        this.height = h

        updateLinesRadius()
    }

    fun submitData(sections: List<DonutChartSection>) {
        chartSections.clear()

        this.chartSections.addAll(ArrayList(sections.filter { it.weight >= 0f }))

        this.totalWeight = this.chartSections.sumByFloat { it.weight }

        this.chartSections.forEach { section ->
            section.drawableArc = DonutChartSection.SectionArc(
                circleRadius,
                section.color,
                strokeWidthPx,
                0f,
                calculateStartingAngle(section.weight, this.totalWeight)
            )
        }

        resolveState()
    }

    private fun calculateStartingAngle(weight: Float, totalWeight: Float): Float {
        return 270f
    }

    private fun resolveState() {
        animatorSet?.cancel()
        animatorSet = AnimatorSet()

        val sectionWeights = chartSections.map { it.weight }

        val drawPercentages = sectionWeights.mapIndexed { index, _ ->
            getDrawAmountForLine(sectionWeights, index) / totalWeight
        }

        drawPercentages.forEachIndexed { index, newPercentage ->
            val line = chartSections[index]
            val animator = animateLine(line.drawableArc, newPercentage) {

            }

            animatorSet?.play(animator)
        }

        animatorSet?.start()
    }

    private fun getDrawAmountForLine(amounts: List<Float>, index: Int): Float {
        if (index >= amounts.size) {
            return 0f
        }

        val thisLine = amounts[index]
        val previousLine = getDrawAmountForLine(amounts, index + 1) // Length of line above this one

        return thisLine + previousLine
    }

    private fun animateLine(line: DonutChartSection.SectionArc, to: Float, animationEnded: (() -> Unit)? = null): ValueAnimator {
        log("animateTo", to.toString())
        return ValueAnimator.ofFloat(line.mLength, to).apply {
            duration = 1000
            interpolator = animationInterpolator
            addUpdateListener {
                (it.animatedValue as? Float)?.let { animValue ->
                    line.mLength = animValue
                }
                invalidate()
            }

            doOnEnd {
                animationEnded?.invoke()
            }
        }
    }

    private fun updateLinesRadius() {
        val widthInner = width.toFloat() - (paddingLeft + paddingRight).toFloat()
        val heightInner = height.toFloat() - (paddingTop + paddingBottom).toFloat()

        this.circleRadius = Math.min(widthInner, heightInner) / 2f - strokeWidthPx / 2f

        chartSections.forEach { it.drawableArc.radius = circleRadius }
    }

    private fun log(label: String, value: String) {
        Log.w("DonutProgressView", "$label: $value")
    }
}
