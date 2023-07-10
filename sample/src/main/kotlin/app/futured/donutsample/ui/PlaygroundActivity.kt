package app.futured.donutsample.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import app.futured.donut.DonutProgressView
import app.futured.donut.model.DonutSection
import app.futured.donutsample.R
import app.futured.donutsample.data.model.BlackCategory
import app.futured.donutsample.data.model.DataCategory
import app.futured.donutsample.data.model.GreenCategory
import app.futured.donutsample.data.model.OrangeCategory
import app.futured.donutsample.tools.extensions.getColorCompat
import app.futured.donutsample.tools.extensions.gone
import app.futured.donutsample.tools.extensions.sumByFloat
import app.futured.donutsample.tools.extensions.visible
import app.futured.donutsample.tools.view.setupSeekbar
import kotlin.random.Random

class PlaygroundActivity : AppCompatActivity() {

    companion object {
        private val ALL_CATEGORIES = listOf(
            BlackCategory,
            GreenCategory,
            OrangeCategory
        )
    }

    private val donutProgressView by lazy { findViewById<DonutProgressView>(R.id.donut_view) }
    private val strokeWidthSeekbar by lazy { findViewById<SeekBar>(R.id.stroke_width_seekbar) }
    private val strokeWidthText by lazy { findViewById<TextView>(R.id.stroke_width_text) }
    private val animationDurationSeekbar by lazy { findViewById<SeekBar>(R.id.anim_duration_seekbar) }
    private val animationDurationText by lazy { findViewById<TextView>(R.id.anim_duration_text) }
    private val addButton by lazy { findViewById<TextView>(R.id.button_add) }
    private val removeButton by lazy { findViewById<TextView>(R.id.button_remove) }
    private val clearButton by lazy { findViewById<TextView>(R.id.button_clear) }
    private val amountCapText by lazy { findViewById<TextView>(R.id.amount_cap_text) }
    private val amountTotalText by lazy { findViewById<TextView>(R.id.amount_total_text) }
    private val blackSectionText by lazy { findViewById<TextView>(R.id.black_section_text) }
    private val greenSectionText by lazy { findViewById<TextView>(R.id.green_section_text) }
    private val orangeSectionText by lazy { findViewById<TextView>(R.id.orange_section_text) }
    private val interpolatorRadioGroup by lazy { findViewById<RadioGroup>(R.id.interpolator_radio_group) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        updateIndicators()
        initControls()
        Handler().postDelayed({
            fillInitialData()
            runInitialAnimation()
        }, 800)
    }

    private fun runInitialAnimation() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener {
                donutProgressView.alpha = it.animatedValue as Float
            }

            start()
        }
    }

    private fun fillInitialData() {
        val sections = listOf(
            DonutSection(
                BlackCategory.name,
                getColorCompat(BlackCategory.colorRes),
                1f
            ),
            DonutSection(
                GreenCategory.name,
                getColorCompat(GreenCategory.colorRes),
                2f
            ),
            DonutSection(
                OrangeCategory.name,
                getColorCompat(OrangeCategory.colorRes),
                4f
            )
        )

        donutProgressView.submitData(sections)

        updateIndicators()
    }

    private fun updateIndicators() {
        amountCapText.text = getString(R.string.amount_cap, donutProgressView.cap)
        amountTotalText.text = getString(
            R.string.amount_total,
            donutProgressView.getData().sumByFloat { it.amount }
        )

        updateIndicatorAmount(BlackCategory, blackSectionText)
        updateIndicatorAmount(GreenCategory, greenSectionText)
        updateIndicatorAmount(OrangeCategory, orangeSectionText)
    }

    private fun updateIndicatorAmount(category: DataCategory, textView: TextView) {
        donutProgressView.getData()
            .filter { it.name == category.name }
            .sumByFloat { it.amount }
            .also {
                if (it > 0f) {
                    textView.visible()
                    textView.text = getString(R.string.float_2f, it)
                } else {
                    textView.gone()
                }
            }
    }

    private fun initControls() {

        setupSeekbar(
            seekBar = strokeWidthSeekbar,
            titleTextView = strokeWidthText,
            initProgress = donutProgressView.strokeWidth.toInt(),
            getTitleText = { getString(R.string.stroke_width, it) },
            onProgressChanged = { donutProgressView.strokeWidth = it.toFloat() }
        )

        // Add random amount to random section
        addButton.setOnClickListener {
            val randomCategory = ALL_CATEGORIES.random()
            donutProgressView.addAmount(
                randomCategory.name,
                Random.nextFloat(),
                getColorCompat(randomCategory.colorRes)
            )

            updateIndicators()
        }

        // Remove random value from random section
        removeButton.setOnClickListener {
            val existingSections = donutProgressView.getData().map { it.name }
            if (existingSections.isNotEmpty()) {
                donutProgressView.removeAmount(existingSections.random(), Random.nextFloat())
                updateIndicators()
            }
        }

        // Clear graph
        clearButton.setOnClickListener {
            donutProgressView.clear()
            updateIndicators()
        }

        // region Animations

        setupSeekbar(
            seekBar = animationDurationSeekbar,
            titleTextView = animationDurationText,
            initProgress = donutProgressView.animationDurationMs.toInt(),
            getTitleText = { getString(R.string.animation_duration, it) },
            onProgressChanged = { donutProgressView.animationDurationMs = it.toLong() }
        )

        val interpolators = listOf(
            AnimationUtils.loadInterpolator(this, android.R.interpolator.decelerate_quint),
            AnimationUtils.loadInterpolator(this, android.R.interpolator.accelerate_quint),
            AnimationUtils.loadInterpolator(this, android.R.interpolator.accelerate_decelerate),
            AnimationUtils.loadInterpolator(this, android.R.interpolator.linear),
            AnimationUtils.loadInterpolator(this, android.R.interpolator.bounce)
        )

        interpolatorRadioGroup.setOnCheckedChangeListener { radioGroup, checkedId ->
            for (i in 0 until radioGroup.childCount) {
                if (radioGroup.getChildAt(i).id == checkedId) {
                    donutProgressView.animationInterpolator = interpolators[i]
                    break
                }
            }
        }

        // endregion
    }
}
