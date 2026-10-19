package com.caraudiolabpro

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity() {
    private val engine = AudioEngine()
    private lateinit var status: TextView
    private lateinit var freqLabel: TextView
    private lateinit var volumeLabel: TextView
    private var isPlaying = false
    private var selectedMode = AudioEngine.Mode.SINE
    private var frequency = 60
    private var volume = 15

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 18, 31)
        window.navigationBarColor = Color.rgb(12, 18, 31)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(12, 18, 31))
        }
        fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
            TextView(this).apply {
                text = value; textSize = size; setTextColor(color)
                if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 8)
            }
        root.addView(text("CAR AUDIO LAB PRO", 24f, Color.rgb(69, 220, 190), true))
        root.addView(text("تست آفلاین سیستم صوتی خودرو", 15f, Color.LTGRAY))
        status = text("● آماده پخش", 15f, Color.rgb(69, 220, 190), true)
        root.addView(status)
        root.addView(text("فرکانس", 17f, Color.WHITE, true))
        freqLabel = text("$frequency Hz", 30f, Color.rgb(69, 220, 190), true)
        root.addView(freqLabel)
        val freqBar = SeekBar(this).apply { max = 19980; progress = 40 }
        root.addView(freqBar)
        freqBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                frequency = p + 20
                freqLabel.text = String.format(Locale.US, "%,d Hz", frequency)
                engine.frequency = frequency.toDouble()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        root.addView(text("حالت صدا", 17f, Color.WHITE, true))
        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        modes.addView(makeButton("تک‌فرکانس") { setMode(AudioEngine.Mode.SINE) }, weightParams())
        modes.addView(makeButton("Sweep") { setMode(AudioEngine.Mode.SWEEP) }, weightParams())
        modes.addView(makeButton("نویز سفید") { setMode(AudioEngine.Mode.WHITE_NOISE) }, weightParams())
        root.addView(modes)
        root.addView(text("ولوم دیجیتال", 17f, Color.WHITE, true))
        volumeLabel = text("$volume%", 20f, Color.WHITE, true)
        root.addView(volumeLabel)
        val volBar = SeekBar(this).apply { max = 50; progress = volume }
        root.addView(volBar)
        volBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, user: Boolean) {
                volume = p
                volumeLabel.text = "$volume%"
                engine.volume = volume / 100.0
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(30, 40, 50, 60, 80, 100).forEach { hz ->
            presets.addView(makeButton("$hz") {
                frequency = hz
                freqBar.progress = hz - 20
                freqLabel.text = "$hz Hz"
                engine.frequency = hz.toDouble()
            }, weightParams())
        }
        root.addView(text("پیش‌تنظیم باس (Hz)", 16f, Color.WHITE, true))
        root.addView(presets)
        val play = makeButton("▶ شروع پخش") {
            if (isPlaying) {
                engine.stop()
                isPlaying = false
                status.text = "● متوقف"
                status.setTextColor(Color.LTGRAY)
            } else {
                engine.mode = selectedMode
                engine.frequency = frequency.toDouble()
                engine.volume = volume / 100.0
                engine.start()
                isPlaying = true
                status.text = "● در حال پخش — ولوم دستگاه را پایین نگه دار"
                status.setTextColor(Color.rgb(255, 190, 70))
            }
            (it as Button).text = if (isPlaying) "■ توقف" else "▶ شروع پخش"
        }
        root.addView(play, LinearLayout.LayoutParams(-1, 56).apply { topMargin = 18 })
        root.addView(text("ابزارها", 17f, Color.WHITE, true))
        root.addView(makeButton("محاسبه ولتاژ آمپلی‌فایر") {
            Toast.makeText(this, "ولتاژ RMS = √(توان RMS × امپدانس)", Toast.LENGTH_LONG).show()
        })
        root.addView(text("هشدار: از صدای کم شروع کن. فرکانس‌های پایین و نویز با ولوم بالا ممکن است به ساب‌ووفر، بلندگو یا شنوایی آسیب بزنند. از خروجی گوشی برای تست طولانی در ولوم زیاد استفاده نکن.", 13f, Color.rgb(255, 190, 70)))
        setContentView(root)
    }

    private fun weightParams() = LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(3, 3, 3, 3) }

    private fun makeButton(label: String, action: (View) -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(36, 54, 76))
            setOnClickListener { action(it) }
        }

    private fun setMode(mode: AudioEngine.Mode) {
        selectedMode = mode
        engine.mode = mode
        if (isPlaying) {
            engine.stop()
            isPlaying = false
            buildUi()
            Toast.makeText(this, "حالت صدا تغییر کرد؛ دوباره پخش را شروع کن", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "حالت انتخاب شد", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        engine.stop()
        super.onDestroy()
    }
}
