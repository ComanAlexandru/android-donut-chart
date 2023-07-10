package app.futured.donutsample.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.view.animation.AnimationUtils
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import app.futured.donut.DonutChartView
import app.futured.donut.DonutChartSection
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

    private val donutChartView by lazy { findViewById<DonutChartView>(R.id.donut_view) }
    private val blackSectionText by lazy { findViewById<TextView>(R.id.black_section_text) }
    private val greenSectionText by lazy { findViewById<TextView>(R.id.green_section_text) }
    private val orangeSectionText by lazy { findViewById<TextView>(R.id.orange_section_text) }

    private var sections: List<DonutChartSection> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        sections = listOf(
            DonutChartSection(
                "1",
                BlackCategory.name,
                getColorCompat(BlackCategory.colorRes),
                5f
            ),
            DonutChartSection(
                "2",
                GreenCategory.name,
                getColorCompat(GreenCategory.colorRes),
                5f
            ),
            DonutChartSection(
                "3",
                OrangeCategory.name,
                getColorCompat(OrangeCategory.colorRes),
                5f
            )
        )

        Handler().postDelayed({
            fillData()
        }, 800)
    }

    private fun fillData() {
        donutChartView.submitData(sections)

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

}
