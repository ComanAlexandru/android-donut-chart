package app.futured.donut

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import app.futured.donut.extensions.hasDuplicatesBy
import app.futured.donut.extensions.sumByFloat
import app.futured.donut.model.DonutSection

class DonutProgressView @JvmOverloads constructor(
    context: Context,
    private val attrs: AttributeSet? = null,
    private val defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "DonutProgressView"

        private const val DEFAULT_MASTER_PROGRESS = 1f
        private const val DEFAULT_STROKE_WIDTH_DP = 12f
        private val DEFAULT_BG_COLOR_RES = R.color.grey

        private val DEFAULT_INTERPOLATOR = DecelerateInterpolator(1.5f)
        private const val DEFAULT_ANIM_DURATION_MS = 1000
    }

    private var w = 0
    private var h = 0
    private var xPadd = 0f
    private var yPadd = 0f

    private var radius = 0f
    private var centerX = 0f
    private var centerY = 0f

    /**
     * Percentage of progress shown for all lines.
     *
     * Eg. when one line has 50% of total graph length,
     * setting this to 0.5f will result in that line being animated to 25% of total graph length.
     */
    private var masterProgress: Float = DEFAULT_MASTER_PROGRESS

    /**
     * Stroke width of all lines in pixels.
     */
    var strokeWidth = dpToPx(DEFAULT_STROKE_WIDTH_DP)
        set(value) {
            field = value

            donutSectionLines.forEach { it.mLineStrokeWidth = value }
            updateLinesRadius()
            invalidate()
        }

    /**
     * Maximum value of sum of all entries in view, after which
     * all lines start to resize proportionally to amounts in their entry categories.
     */
    var cap: Float = 0f
        private set

    /**
     * Color of background line.
     */
    @ColorInt
    var bgLineColor: Int = ContextCompat.getColor(context, DEFAULT_BG_COLOR_RES)
        set(value) {
            field = value
            invalidate()
        }

    /**
     * Size of gap opening in degrees.
     */
    private var gapWidthDegrees: Float = 0f

    /**
     * Angle in degrees, at which the gap will be displayed.
     */
    //TODO: gapAngleDegrees should disappear, instead a function should calculate gap angle for each section (gap angle is where the section starts in radians)
    private var gapAngleDegrees: Float = 270f
        set(value) {
            field = value

            //TODO: mGapAngleDegrees should be calculated individually for each section to precede the anterior one
            donutSectionLines.forEach { it.mGapAngleDegrees = value }
            invalidate()
        }

    /**
     * Interpolator used for state change animations.
     */
    var animationInterpolator: Interpolator = DEFAULT_INTERPOLATOR

    /**
     * Duration of state change animations.
     */
    var animationDurationMs: Long = DEFAULT_ANIM_DURATION_MS.toLong()

    private val donutSections = mutableListOf<DonutSection>()
    private val donutSectionLines = mutableListOf<DonutProgressLine>()
    private var animatorSet: AnimatorSet? = null

    init {
        obtainAttributes()
    }

    @SuppressLint("Recycle")
    private fun obtainAttributes() {
        context.obtainStyledAttributes(
            attrs,
            R.styleable.DonutProgressView,
            defStyleAttr,
            0
        ).use {
            strokeWidth = it.getDimensionPixelSize(
                R.styleable.DonutProgressView_donut_strokeWidth,
                dpToPx(DEFAULT_STROKE_WIDTH_DP).toInt()
            ).toFloat()

            bgLineColor =
                it.getColor(
                    R.styleable.DonutProgressView_donut_bgLineColor,
                    ContextCompat.getColor(
                        context,
                        DEFAULT_BG_COLOR_RES
                    )
                )

            animationDurationMs = it.getInt(
                R.styleable.DonutProgressView_donut_animationDuration,
                DEFAULT_ANIM_DURATION_MS
            ).toLong()

            animationInterpolator =
                it.getResourceId(R.styleable.DonutProgressView_donut_animationInterpolator, 0)
                    .let { id ->
                        if (id != 0) {
                            AnimationUtils.loadInterpolator(context, id)
                        } else {
                            DEFAULT_INTERPOLATOR
                        }
                    }
        }
    }

    /**
     * Returns current data.
     */
    fun getData() = donutSections.toList()

    /**
     * Submits new [sections] to the view.
     *
     * New progress line will be created for each non-existent section and view will be animated to new state.
     * Additionally, existing lines with no data set will be removed when animation completes.
     */
    fun submitData(sections: List<DonutSection>) {
        assertDataConsistency(sections)

        sections
            .filter { it.amount >= 0f }
            .forEach { section ->
                val newLineColor = section.color
                if (hasEntriesForSection(section.name).not()) {
                    donutSectionLines.add(
                        index = 0,
                        element = DonutProgressLine(
                            name = section.name,
                            radius = radius,
                            lineColor = newLineColor,
                            lineStrokeWidth = strokeWidth,
                            masterProgress = masterProgress,
                            length = 0f,
                            gapWidthDegrees = gapWidthDegrees,
                            gapAngleDegrees = gapAngleDegrees
                        )
                    )
                } else {
                    donutSectionLines
                        .filter { it.name == section.name }
                        .forEach { it.mLineColor = newLineColor }
                }
            }

        this.donutSections.apply {
            val copy = ArrayList(sections)
            clear()
            addAll(copy)
        }

        this.cap = sections.sumByFloat {  it.amount }

        resolveState()
    }

    /**
     * Adds [amount] to existing section specified by [sectionName]. If section does not exist and [color] is specified,
     * creates new section internally.
     */
    fun addAmount(sectionName: String, amount: Float, color: Int? = null) {
        for (i in 0 until donutSections.size) {
            if (donutSections[i].name == sectionName) {
                donutSections[i] = donutSections[i].copy(amount = donutSections[i].amount + amount)
                submitData(donutSections)
                return
            }
        }

        color?.let {
            submitData(
                donutSections + DonutSection(
                    name = sectionName,
                    color = it,
                    amount = amount
                )
            )
        }
            ?: warn {
                "Adding amount to non-existent section: $sectionName. " +
                    "Please specify color, if you want to have section created automatically."
            }
    }

    /**
     * Sets [amount] for existing section specified by [sectionName].
     * Removes section if amount is <= 0.
     * Does nothing if section does not exist.
     */
    fun setAmount(sectionName: String, amount: Float) {
        for (i in 0 until donutSections.size) {
            if (donutSections[i].name == sectionName) {
                if (amount > 0) {
                    donutSections[i] = donutSections[i].copy(amount = amount)
                } else {
                    donutSections.removeAt(i)
                }
                submitData(donutSections)
                return
            }
        }

        warn { "Setting amount for non-existent section: $sectionName" }
    }

    /**
     * Removes [amount] from existing section specified by [sectionName].
     * If amount gets below zero, removes the section from view.
     */
    fun removeAmount(sectionName: String, amount: Float) {
        for (i in 0 until donutSections.size) {
            if (donutSections[i].name == sectionName) {
                val resultAmount = donutSections[i].amount - amount
                if (resultAmount > 0) {
                    donutSections[i] = donutSections[i].copy(amount = resultAmount)
                } else {
                    donutSections.removeAt(i)
                }
                submitData(donutSections)
                return
            }
        }

        warn { "Removing amount from non-existent section: $sectionName" }
    }

    /**
     * Clear data, removing all lines.
     */
    fun clear() = submitData(listOf())

    private fun assertDataConsistency(data: List<DonutSection>) {
        if (data.hasDuplicatesBy { it.name }) {
            throw IllegalStateException("Multiple sections with same name found")
        }
    }

    private fun resolveState() {
        animatorSet?.cancel()
        animatorSet = AnimatorSet()

        val sectionAmounts = donutSectionLines.map { getAmountForSection(it.name) }
        val totalAmount = sectionAmounts.sumByFloat { it }

        val drawPercentages = sectionAmounts.mapIndexed { index, _ ->
            if (totalAmount > cap) {
                getDrawAmountForLine(sectionAmounts, index) / totalAmount
            } else {
                getDrawAmountForLine(sectionAmounts, index) / cap
            }
        }

        drawPercentages.forEachIndexed { index, newPercentage ->
            val line = donutSectionLines[index]
            val animator = animateLine(line, newPercentage) {
                if (!hasEntriesForSection(line.name)) {
                    removeLine(line)
                }
            }

            animatorSet?.play(animator)
        }

        animatorSet?.start()
    }

    private fun getAmountForSection(sectionName: String): Float {
        return donutSections
            .filter { it.name == sectionName }
            .sumByFloat { it.amount }
    }

    private fun getDrawAmountForLine(amounts: List<Float>, index: Int): Float {
        if (index >= amounts.size) {
            return 0f
        }

        val thisLine = amounts[index]
        val previousLine = getDrawAmountForLine(amounts, index + 1) // Length of line above this one

        return thisLine + previousLine
    }

    private fun hasEntriesForSection(section: String) =
        donutSections.indexOfFirst { it.name == section } > -1

    private fun animateLine(
        line: DonutProgressLine,
        to: Float,
        animationEnded: (() -> Unit)? = null
    ): ValueAnimator {
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

    private fun removeLine(line: DonutProgressLine) {
        donutSectionLines.remove(line)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.w = w
        this.h = h

        this.xPadd = (paddingLeft + paddingRight).toFloat()
        this.yPadd = (paddingTop + paddingBottom).toFloat()

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

    private fun updateLinesRadius() {
        val ww = w.toFloat() - xPadd
        val hh = h.toFloat() - yPadd
        this.radius = Math.min(ww, hh) / 2f - strokeWidth / 2f

        donutSectionLines.forEach { it.mRadius = radius }
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
