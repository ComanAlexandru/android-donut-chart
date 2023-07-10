package app.futured.donut

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.core.animation.doOnEnd
import app.futured.donut.extensions.sumByFloat

class DonutProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DonutProgressView"

        private const val DEFAULT_STROKE_WIDTH_DP = 12f

        private val DEFAULT_INTERPOLATOR = DecelerateInterpolator(1.5f)
        private const val DEFAULT_ANIM_DURATION_MS = 1000
    }

    private var width = 0
    private var height = 0
    private var paddingHorizontal = 0f
    private var paddingVertical = 0f

    private var circleRadius = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var strokeWidthPx = dpToPx(DEFAULT_STROKE_WIDTH_DP)

    /**
     * Maximum value of sum of all entries in view, after which
     * all lines start to resize proportionally to amounts in their entry categories.
     */
    var totalWeight: Float = 0f
        private set

    var animationInterpolator: Interpolator = DEFAULT_INTERPOLATOR
    private var animationDurationMs: Long = DEFAULT_ANIM_DURATION_MS.toLong()

    private val donutSections = mutableListOf<DonutSection>()
    private var donutSectionLines = mutableListOf<DonutSectionLine>()
    private var animatorSet: AnimatorSet? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.DonutProgressView, defStyleAttr, 0)

        strokeWidthPx = typedArray.getDimensionPixelSize(
            R.styleable.DonutProgressView_donut_strokeWidth,
            dpToPx(DEFAULT_STROKE_WIDTH_DP).toInt()
        ).toFloat()

        animationDurationMs = typedArray.getInt(
            R.styleable.DonutProgressView_donut_animationDuration,
            DEFAULT_ANIM_DURATION_MS
        ).toLong()

        animationInterpolator = typedArray.getResourceId(R.styleable.DonutProgressView_donut_animationInterpolator, 0)
            .let { id ->
                if (id != 0) {
                    AnimationUtils.loadInterpolator(context, id)
                } else {
                    DEFAULT_INTERPOLATOR
                }
            }

        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.width = w
        this.height = h

        this.paddingHorizontal = (paddingLeft + paddingRight).toFloat()
        this.paddingVertical = (paddingTop + paddingBottom).toFloat()

        this.centerX = w / 2f
        this.centerY = h / 2f

        updateLinesRadius()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val originalWidth = MeasureSpec.getSize(widthMeasureSpec)

        super.onMeasure(
            widthMeasureSpec,
            MeasureSpec.makeMeasureSpec(originalWidth, MeasureSpec.EXACTLY)
        )
    }

    fun submitData(sections: List<DonutSection>) {
        donutSectionLines.clear()

        this.totalWeight = sections.sumByFloat { it.weight }

        sections
            .filter { it.weight >= 0f }
            .forEach { section ->
                val newLineColor = section.color
                donutSectionLines.add(
                    index = 0,
                    element = DonutSectionLine(
                        label = section.label,
                        radius = circleRadius,
                        lineColor = newLineColor,
                        lineStrokeWidth = strokeWidthPx,
                        length = 0f,
                        startAngleDegrees = calculateStartingAngle(section.weight, this.totalWeight)
                    )
                )
            }

        this.donutSections.apply {
            val copy = ArrayList(sections)
            clear()
            addAll(copy)
        }

        resolveState()
    }

    private fun calculateStartingAngle(weight: Float, totalWeight: Float): Float {
        return 270f
    }

    private fun resolveState() {
        animatorSet?.cancel()
        animatorSet = AnimatorSet()

        val sectionAmounts = donutSectionLines.map { getAmountForSection(it.label) }
        val totalAmount = sectionAmounts.sumByFloat { it }

        val drawPercentages = sectionAmounts.mapIndexed { index, _ ->
            if (totalAmount > totalWeight) {
                getDrawAmountForLine(sectionAmounts, index) / totalAmount
            } else {
                getDrawAmountForLine(sectionAmounts, index) / totalWeight
            }
        }

        drawPercentages.forEachIndexed { index, newPercentage ->
            val line = donutSectionLines[index]
            val animator = animateLine(line, newPercentage) {

            }

            animatorSet?.play(animator)
        }

        animatorSet?.start()
    }

    private fun getAmountForSection(sectionName: String): Float {
        return donutSections
            .filter { it.label == sectionName }
            .sumByFloat { it.weight }
    }

    private fun getDrawAmountForLine(amounts: List<Float>, index: Int): Float {
        if (index >= amounts.size) {
            return 0f
        }

        val thisLine = amounts[index]
        val previousLine = getDrawAmountForLine(amounts, index + 1) // Length of line above this one

        return thisLine + previousLine
    }

    private fun animateLine(line: DonutSectionLine, to: Float, animationEnded: (() -> Unit)? = null): ValueAnimator {
        return ValueAnimator.ofFloat(line.mLength, to).apply {
            duration = animationDurationMs
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
        val widthInner = width.toFloat() - paddingHorizontal
        val heightInner = height.toFloat() - paddingVertical
        this.circleRadius = Math.min(widthInner, heightInner) / 2f - strokeWidthPx / 2f

        donutSectionLines.forEach { it.radius = circleRadius }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(centerX, centerY)

        donutSectionLines.forEach { it.draw(canvas) }
    }

    private fun dpToPx(dp: Float) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        resources.displayMetrics
    )

    private fun warn(text: () -> String) {
        Log.w(TAG, text())
    }
}
