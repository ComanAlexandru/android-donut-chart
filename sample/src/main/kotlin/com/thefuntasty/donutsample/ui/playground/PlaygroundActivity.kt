package com.thefuntasty.donutsample.ui.playground

import android.os.Bundle
import android.os.Handler
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.thefuntasty.donutsample.R
import com.thefuntasty.donutsample.data.model.BlackCategory
import com.thefuntasty.donutsample.data.model.DataCategory
import com.thefuntasty.donutsample.data.model.DataItem
import com.thefuntasty.donutsample.data.model.GreenCategory
import com.thefuntasty.donutsample.data.model.OrangeCategory
import com.thefuntasty.donutsample.tools.extensions.doOnProgressChange
import com.thefuntasty.donutsample.tools.extensions.getColorCompat
import com.thefuntasty.donutsample.tools.extensions.gone
import com.thefuntasty.donutsample.tools.extensions.sumByFloat
import com.thefuntasty.donutsample.tools.extensions.toDonutEntries
import com.thefuntasty.donutsample.tools.extensions.visible
import kotlinx.android.synthetic.main.activity_playground.*
import kotlin.random.Random

class PlaygroundActivity : AppCompatActivity() {

    companion object {
        private val ALL_CATEGORIES = listOf(
            BlackCategory,
            GreenCategory,
            OrangeCategory
        )
    }

    private val dataItems = mutableListOf<DataItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playground)

        setupDonut()
        updateIndicators()
        initControls()
        Handler().postDelayed({ fillInitialData() }, 500)
    }

    private fun setupDonut() {
        donut_view.cap = 5f

        ALL_CATEGORIES.forEach {
            donut_view.setColor(it.name, getColorCompat(it.colorRes))
        }
    }

    private fun fillInitialData() {
        dataItems += DataItem(BlackCategory, 1f)
        dataItems += DataItem(GreenCategory, 1.2f)
        dataItems += DataItem(OrangeCategory, 1.4f)

        donut_view.submitEntries(dataItems.toDonutEntries())
        updateIndicators()
    }

    private fun updateIndicators() {
        amount_cap_text.text = getString(R.string.amount_cap, donut_view.cap)
        amount_total_text.text = getString(R.string.amount_total, dataItems.sumByDouble { it.amount.toDouble() }.toFloat())

        updateIndicatorAmount(BlackCategory, black_dataset_text)
        updateIndicatorAmount(GreenCategory, green_dataset_text)
        updateIndicatorAmount(OrangeCategory, orange_dataset_text)
    }

    private fun updateIndicatorAmount(category: DataCategory, textView: TextView) {
        dataItems
            .filter { it.category == category }
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
            seekBar = master_progress_seekbar,
            titleTextView = master_progress_text,
            initProgress = (donut_view.masterProgress * 100).toInt(),
            getTitleText = { getString(R.string.master_progress, it) },
            onProgressChanged = { donut_view.masterProgress = it / 100f }
        )

        setupSeekbar(
            seekBar = gap_width_seekbar,
            titleTextView = gap_width_text,
            initProgress = donut_view.gapWidthDegrees.toInt(),
            getTitleText = { getString(R.string.gap_width, it) },
            onProgressChanged = { donut_view.gapWidthDegrees = it.toFloat() }
        )

        setupSeekbar(
            seekBar = gap_angle_seekbar,
            titleTextView = gap_angle_text,
            initProgress = donut_view.gapAngleDegrees.toInt(),
            getTitleText = { getString(R.string.gap_angle, it) },
            onProgressChanged = { donut_view.gapAngleDegrees = it.toFloat() }
        )

        setupSeekbar(
            seekBar = stroke_width_seekbar,
            titleTextView = stroke_width_text,
            initProgress = donut_view.strokeWidth.toInt(),
            getTitleText = { getString(R.string.stroke_width, it) },
            onProgressChanged = { donut_view.strokeWidth = it.toFloat() }
        )

        setupSeekbar(
            seekBar = cap_seekbar,
            titleTextView = cap_text,
            initProgress = donut_view.cap.toInt(),
            getTitleText = { getString(R.string.amount_cap, it.toFloat()) },
            onProgressChanged = {
                donut_view.cap = it.toFloat()
                updateIndicators()
            }
        )

        // Add entry with random category and random amount
        button_add.setOnClickListener {
            dataItems.add(DataItem(ALL_CATEGORIES.random(), Random.nextFloat()))
            donut_view.submitEntries(dataItems.toDonutEntries())
            updateIndicators()
        }

        // Remove random entry
        button_remove.setOnClickListener {
            if (dataItems.isNotEmpty()) {
                val randomIndex = dataItems.indexOf(dataItems.random())
                dataItems.removeAt(randomIndex)
                donut_view.submitEntries(dataItems.toDonutEntries())
                updateIndicators()
            }
        }

        // Randomize data set colors
        button_random_colors.setOnClickListener {
            ALL_CATEGORIES.forEach {
                donut_view.setColor(it.name, Random.nextInt())
            }
        }

        // Clear graph
        button_clear.setOnClickListener {
            dataItems.clear()
            donut_view.clear()
            updateIndicators()
        }
    }

    private fun setupSeekbar(
        seekBar: SeekBar,
        titleTextView: TextView,
        initProgress: Int,
        getTitleText: (progress: Int) -> String,
        onProgressChanged: (progress: Int) -> Unit
    ) {
        titleTextView.text = getTitleText(initProgress)
        seekBar.apply {
            progress = initProgress
            doOnProgressChange {
                onProgressChanged(progress)
                titleTextView.text = getTitleText(progress)
            }
        }
    }
}
