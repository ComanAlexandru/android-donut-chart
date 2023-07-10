package app.futured.donutsample.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import app.futured.donut.DonutProgressView
import app.futured.donut.DonutSection
import app.futured.donutsample.R
import app.futured.donutsample.data.model.BlackCategory
import app.futured.donutsample.data.model.DataCategory
import app.futured.donutsample.data.model.GreenCategory
import app.futured.donutsample.data.model.OrangeCategory
import app.futured.donutsample.tools.extensions.getColorCompat
import app.futured.donutsample.tools.extensions.gone
import app.futured.donutsample.tools.extensions.sumByFloat
import app.futured.donutsample.tools.extensions.visible

class PlaygroundActivity : AppCompatActivity() {

    private val donutProgressView by lazy { findViewById<DonutProgressView>(R.id.donut_view) }
    private val amountCapText by lazy { findViewById<TextView>(R.id.amount_cap_text) }
    private val amountTotalText by lazy { findViewById<TextView>(R.id.amount_total_text) }
    private val blackSectionText by lazy { findViewById<TextView>(R.id.black_section_text) }
    private val greenSectionText by lazy { findViewById<TextView>(R.id.green_section_text) }
    private val orangeSectionText by lazy { findViewById<TextView>(R.id.orange_section_text) }
    private val interpolatorRadioGroup by lazy { findViewById<RadioGroup>(R.id.interpolator_radio_group) }

    private var sections: List<DonutSection> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        sections = listOf(
            DonutSection(
                BlackCategory.name,
                getColorCompat(BlackCategory.colorRes),
                5f
            ),
            DonutSection(
                GreenCategory.name,
                getColorCompat(GreenCategory.colorRes),
                5f
            ),
            DonutSection(
                OrangeCategory.name,
                getColorCompat(OrangeCategory.colorRes),
                5f
            )
        )

        updateIndicators()
        initControls()
        Handler().postDelayed({
            fillData()
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

    private fun fillData() {
        donutProgressView.submitData(sections)
        updateIndicators()
    }

    private fun updateIndicators() {
        amountCapText.text = getString(R.string.amount_cap, donutProgressView.totalWeight)
        amountTotalText.text = getString(
            R.string.amount_total,
            sections.sumByFloat { it.weight }
        )

        updateIndicatorAmount(BlackCategory, blackSectionText)
        updateIndicatorAmount(GreenCategory, greenSectionText)
        updateIndicatorAmount(OrangeCategory, orangeSectionText)
    }

    private fun updateIndicatorAmount(category: DataCategory, textView: TextView) {
        sections
            .filter { it.label == category.name }
            .sumByFloat { it.weight }
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
                    fillData()
                    break
                }
            }
        }

    }
}
