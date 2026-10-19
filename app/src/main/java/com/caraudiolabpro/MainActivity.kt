package com.caraudiolabpro

import android.app.Activity
import android.os.Bundle
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.sin
import kotlin.math.sqrt

private object C {
    const val bg = 0xFF080D14.toInt()
    const val card = 0xFF111B27.toInt()
    const val tile = 0xFF1A2939.toInt()
    const val line = 0xFF293B4D.toInt()
    const val lime = 0xFFB8FF3B.toInt()
    const val cyan = 0xFF43E0D0.toInt()
    const val white = 0xFFF4F7FA.toInt()
    const val muted = 0xFF8EA0B3.toInt()
    const val orange = 0xFFFFB45C.toInt()
}

class MainActivity : Activity() {
    private val engine = AudioEngine()
    private lateinit var status: TextView
    private lateinit var play: Button
    private lateinit var freqText: TextView
    private lateinit var volText: TextView
    private lateinit var wave: Wave
    private var playing = false
    private var mode = AudioEngine.Mode.SINE
    private var freq = 60
    private var volume = 12
    private var sweepStart = 20
    private var sweepEnd = 200

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, stroke: Int = 0, radius: Int = 18) =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != 0) setStroke(dp(1), stroke) }
    private fun txt(s: String, size: Float = 14f, color: Int = C.white, bold: Boolean = false) =
        TextView(this).apply { text=s; textSize=size; setTextColor(color); includeFontPadding=false; if (bold) typeface=Typeface.DEFAULT_BOLD }
    private fun col() = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
    private fun lp(w: Int=-1,h: Int=-2)=LinearLayout.LayoutParams(w,h)
    private fun gap(parent: LinearLayout, h: Int=12) { parent.addView(View(this), lp(1,dp(h))) }
    private fun card()=col().apply { setPadding(dp(16),dp(16),dp(16),dp(16)); background=shape(C.card,C.line,20) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=C.bg; window.navigationBarColor=C.bg
        buildUi()
    }

    private fun buildUi() {
        val scroll=ScrollView(this).apply { setBackgroundColor(C.bg); fillViewport=true }
        val root=col().apply { setPadding(dp(17),dp(20),dp(17),dp(26)) }
        scroll.addView(root)

        val header=row()
        header.addView(TextView(this).apply {
            text="CA"; textSize=18f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK)
            gravity=Gravity.CENTER; background=shape(C.lime,0,15)
        },lp(dp(52),dp(52)))
        val brand=col().apply { setPadding(dp(12),0,0,0) }
        brand.addView(txt("CAR AUDIO",11f,C.cyan,true))
        brand.addView(txt("LAB / PRO",23f,C.white,true))
        header.addView(brand,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(TextView(this).apply {
            text="● OFFLINE"; textSize=10f; typeface=Typeface.DEFAULT_BOLD; setTextColor(C.lime)
            setPadding(dp(10),dp(8),dp(10),dp(8)); background=shape(0xFF203126.toInt(),0,24)
        })
        root.addView(header); gap(root,18)

        val hero=card().apply {
            background=GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF18313A.toInt(),C.card,0xFF172039.toInt())).apply { cornerRadius=dp(22).toFloat(); setStroke(dp(1),C.line) }
        }
        hero.addView(txt("SIGNAL GENERATOR   /   01",10f,C.cyan,true)); gap(hero,10)
        val heroRow=row(); val big=col()
        freqText=txt("60 Hz",36f,C.white,true); big.addView(freqText)
        big.addView(txt("LOW FREQUENCY • LIVE CONTROL",9f,C.muted,true))
        heroRow.addView(big,LinearLayout.LayoutParams(0,-2,1f))
        heroRow.addView(TextView(this).apply {
            text="SINE\nWAVE"; gravity=Gravity.CENTER; textSize=11f; typeface=Typeface.DEFAULT_BOLD
            setTextColor(C.lime); setPadding(dp(14),dp(11),dp(14),dp(11)); background=shape(0xFF2A391E.toInt(),0,14)
        })
        hero.addView(heroRow); gap(hero,14)
        wave=Wave(); hero.addView(wave,lp(-1,dp(72))); gap(hero,12)
        status=txt("● SYSTEM READY  /  AUDIO STANDBY",10f,C.lime,true); hero.addView(status)
        root.addView(hero); gap(root,14)

        val fc=card(); section(fc,"01","FREQUENCY","20 Hz — 20 kHz"); gap(fc,12)
        val fbar=SeekBar(this).apply {
            max=19980; progress=freq-20
            progressTintList=android.content.res.ColorStateList.valueOf(C.lime)
            thumbTintList=android.content.res.ColorStateList.valueOf(C.lime)
        }
        fc.addView(fbar,lp(-1,dp(38)))
        val limits=row(); limits.addView(txt("20 Hz",10f,C.muted),LinearLayout.LayoutParams(0,-2,1f))
        limits.addView(txt("20,000 Hz",10f,C.muted).apply { gravity=Gravity.RIGHT }); fc.addView(limits)
        gap(fc,10)
        val quick=row()
        listOf(30,40,50,60,80,100,125).forEach { hz ->
            quick.addView(btn("$hz",small=true) { setFreq(hz,fbar) },
                LinearLayout.LayoutParams(0,dp(40),1f).apply { setMargins(dp(2),0,dp(2),0) })
        }
        fc.addView(quick)
        fbar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,p:Int,user:Boolean){freq=p+20; freqText.text=String.format(Locale.US,"%,d Hz",freq);engine.frequency=freq.toDouble();wave.invalidate()}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })
        root.addView(fc); gap(root,14)

        val mc=card(); section(mc,"02","SIGNAL TYPE","SELECT YOUR TEST"); gap(mc,12)
        val modes=row()
        modes.addView(btn("∿  SINE",selected=true){setMode(AudioEngine.Mode.SINE)},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(0,0,dp(4),0)})
        modes.addView(btn("↗  SWEEP"){setMode(AudioEngine.Mode.SWEEP)},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(3),0,dp(3),0)})
        modes.addView(btn("〰 NOISE"){setMode(AudioEngine.Mode.WHITE_NOISE)},LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(4),0,0,0)})
        mc.addView(modes); gap(mc,10)
        mc.addView(txt("SWEEP RANGE",10f,C.muted,true)); gap(mc,6)
        val sweep=row()
        sweep.addView(btn("20 Hz"){sweepStart=20;toast("Sweep start: 20 Hz")},LinearLayout.LayoutParams(0,dp(42),1f))
        sweep.addView(txt("  →  ",14f,C.cyan,true).apply{gravity=Gravity.CENTER})
        sweep.addView(btn("200 Hz"){sweepEnd=200;toast("Sweep end: 200 Hz")},LinearLayout.LayoutParams(0,dp(42),1f))
        mc.addView(sweep); root.addView(mc); gap(root,14)

        val vc=card(); section(vc,"03","OUTPUT LEVEL","DIGITAL LEVEL • NOT dB SPL"); gap(vc,10)
        val vr=row(); volText=txt("$volume%",24f,C.cyan,true); vr.addView(volText,LinearLayout.LayoutParams(0,-2,1f))
        vr.addView(txt("START LOW",10f,C.orange,true)); vc.addView(vr)
        val vbar=SeekBar(this).apply{max=25;progress=volume;progressTintList=android.content.res.ColorStateList.valueOf(C.cyan);thumbTintList=android.content.res.ColorStateList.valueOf(C.cyan)}
        vc.addView(vbar,lp(-1,dp(38)))
        vbar.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,p:Int,user:Boolean){volume=p.coerceAtLeast(1);volText.text="$volume%";engine.volume=volume/100.0}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })
        root.addView(vc); gap(root,14)

        play=btn("▶   START AUDIO TEST",selected=true){toggleAudio()}.apply{textSize=15f}
        root.addView(play,lp(-1,dp(58))); gap(root,8)
        root.addView(btn("■   STOP SIGNAL"){stopAudio()},lp(-1,dp(46))); gap(root,14)

        val tools=card(); section(tools,"04","QUICK TOOLS","OFFLINE"); gap(tools,12)
        tools.addView(txt("AMPLIFIER VOLTAGE",11f,C.cyan,true))
        tools.addView(txt("V RMS = √(Power × Impedance)",13f,C.muted)); gap(tools,8)
        val calc=row()
        calc.addView(btn("350 W RMS"){showVoltage()},LinearLayout.LayoutParams(0,dp(44),1f).apply{setMargins(0,0,dp(4),0)})
        calc.addView(btn("4 Ω LOAD"){showVoltage()},LinearLayout.LayoutParams(0,dp(44),1f).apply{setMargins(dp(4),0,0,0)})
        tools.addView(calc); gap(tools,12)
        tools.addView(txt("SPEAKER PRESETS",11f,C.cyan,true)); gap(tools,6)
        tools.addView(btn("SUBWOOFER   /   20–200 Hz"){setFreq(40,fbar)},lp(-1,dp(42))); gap(tools,5)
        tools.addView(btn("MIDRANGE   /   200–5,000 Hz"){setFreq(500,fbar)},lp(-1,dp(42))); gap(tools,5)
        tools.addView(btn("FULL RANGE   /   20–20,000 Hz"){setFreq(1000,fbar)},lp(-1,dp(42)))
        root.addView(tools); gap(root,14)

        val warning=col().apply{setPadding(dp(15),dp(14),dp(15),dp(14));background=shape(0xFF30251A.toInt(),0xFF6C4A28.toInt(),17)}
        warning.addView(txt("⚠  SAFETY FIRST",11f,C.orange,true)); gap(warning,5)
        warning.addView(txt("صدای گوشی و ضبط را از کمترین مقدار شروع کن. تست ممتد باس با ولوم بالا می‌تواند به ساب‌ووفر، آمپلی‌فایر و شنوایی آسیب بزند. توییتر را با فرکانس پایین تست نکن.",12f,0xFFFFD6AD.toInt()))
        root.addView(warning); gap(root,18)
        root.addView(txt("CAR AUDIO LAB PRO  •  MADE FOR ENTHUSIASTS",9f,C.muted,true).apply{gravity=Gravity.CENTER})
        setContentView(scroll)
    }

    private fun section(parent:LinearLayout,n:String,title:String,sub:String){
        val r=row()
        r.addView(TextView(this).apply{text=n;textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(Color.BLACK);gravity=Gravity.CENTER;background=shape(C.lime,0,10)},lp(dp(32),dp(32)))
        val c=col().apply{setPadding(dp(10),0,0,0)}
        c.addView(txt(title,13f,C.white,true));c.addView(txt(sub,9f,C.muted,true));r.addView(c);parent.addView(r)
    }
    private fun btn(textValue:String,selected:Boolean=false,small:Boolean=false,action:()->Unit)=Button(this).apply{
        text=textValue;isAllCaps=false;textSize=if(small)10f else 12f;typeface=Typeface.DEFAULT_BOLD
        setTextColor(if(selected)Color.BLACK else C.white);background=shape(if(selected)C.lime else C.tile,if(selected)0 else C.line,13)
        minHeight=0;minimumHeight=0;stateListAnimator=null;setPadding(dp(4),dp(3),dp(4),dp(3));setOnClickListener{action()}
    }
    private fun setFreq(value:Int,bar:SeekBar){freq=value.coerceIn(20,20000);bar.progress=freq-20;freqText.text=String.format(Locale.US,"%,d Hz",freq);engine.frequency=freq.toDouble();wave.invalidate()}
    private fun setMode(value:AudioEngine.Mode){mode=value;engine.mode=value;toast(when(value){AudioEngine.Mode.SINE->"Sine wave selected";AudioEngine.Mode.SWEEP->"Sweep selected: $sweepStart–$sweepEnd Hz";AudioEngine.Mode.WHITE_NOISE->"White noise selected"});wave.invalidate()}
    private fun toggleAudio(){
        if(playing){stopAudio();return}
        try{
            engine.frequency=freq.toDouble();engine.volume=volume/100.0;engine.mode=mode
            engine.sweepStart=sweepStart.toDouble();engine.sweepEnd=sweepEnd.toDouble();engine.start()
            playing=true;status.text="● SIGNAL ACTIVE  /  "+mode.name;status.setTextColor(C.orange)
            play.text="Ⅱ   STOP AUDIO TEST";play.background=shape(C.orange,0,14);wave.active=true;wave.invalidate()
        }catch(e:Exception){toast("Audio error: "+(e.message?:"output unavailable"))}
    }
    private fun stopAudio(){
        engine.stop();playing=false
        if(::status.isInitialized){status.text="● SYSTEM READY  /  AUDIO STANDBY";status.setTextColor(C.lime)}
        if(::play.isInitialized){play.text="▶   START AUDIO TEST";play.background=shape(C.lime,0,14)}
        if(::wave.isInitialized){wave.active=false;wave.invalidate()}
    }
    private fun showVoltage(){toast(String.format(Locale.US,"350 W @ 4 Ω = %.2f V RMS",sqrt(350.0*4.0)))}
    private fun toast(s:String){Toast.makeText(this,s,Toast.LENGTH_SHORT).show()}
    override fun onStop(){stopAudio();super.onStop()}
    override fun onDestroy(){stopAudio();super.onDestroy()}

    private inner class Wave:View(this@MainActivity){
        private val p=Paint(Paint.ANTI_ALIAS_FLAG);var active=false
        override fun onDraw(canvas:Canvas){
            super.onDraw(canvas);val w=width.toFloat();val h=height.toFloat();val mid=h/2
            p.color=C.line;p.strokeWidth=1f;canvas.drawLine(0f,mid,w,mid,p)
            p.color=if(active)C.lime else C.cyan;p.strokeWidth=dp(2).toFloat();p.style=Paint.Style.STROKE
            val path=android.graphics.Path();val cycles=when(mode){AudioEngine.Mode.SINE->(freq/35f).coerceIn(1f,15f);AudioEngine.Mode.SWEEP->4f;AudioEngine.Mode.WHITE_NOISE->28f}
            for(i in 0..220){val x=w*i/220f;val v=when(mode){AudioEngine.Mode.WHITE_NOISE->sin(i*12.98).toFloat();AudioEngine.Mode.SWEEP->sin(i*(0.05+i/1300.0)).toFloat();else->sin(i/220.0*cycles*2*Math.PI).toFloat()};val y=mid-v*h*.36f;if(i==0)path.moveTo(x,y)else path.lineTo(x,y)}
            canvas.drawPath(path,p);p.style=Paint.Style.FILL
        }
    }
}
