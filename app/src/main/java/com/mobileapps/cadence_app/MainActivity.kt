package com.mobileapps.cadence_app

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var mSensorManager : SensorManager
    private var mAccelerometer : Sensor ?= null
    private var resume = false
    private var data : Vector<Float> = Vector(0)
    private var cadenceVector : Vector<Float> = Vector(0)
    private var windowSize = 128
    private var seconds60 = 60
    private var samplingPeriod = 0.03f
    private var minCadence = 20f
    private var maxCadence = 120f
    // e.g. mSID = 120 / 60 * 0.05
    // maxCadence  / 60s => 2Hz,
    private var minSampleIndexDistance = seconds60 / maxCadence / samplingPeriod
    private var maxSampleIndexDistance = seconds60 / minCadence / samplingPeriod

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            (samplingPeriod * 1000000).toInt());
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        print("accuracy changed")
    }


    override fun onSensorChanged(event: SensorEvent?) {

        if (event != null && resume) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                var x = event.values[0]
                var y = event.values[1]
                var z = event.values[2]

                data.add(calculateDistance(x,y,z))
//                calculateCadency(data)

                findViewById<TextView>(R.id.length).text = data.size.toString()

                if (data.size >= windowSize && data.size % (windowSize) == 0 ) {  // power of two
//                    for (i:Int in 0 until data.size) {
//                        if (data[i] != 0.toFloat()) {
//                            println(data[i])
//                        }
//                    }
//                    data.removeElementAt(0)
//                    for (i:Int in 0 until data.size){
//                        println(a[i].toString())
//                    }
                    val result = calculateCadency(data)
                    findViewById<TextView>(R.id.cadence_value).text = result.first.toString()
                    findViewById<TextView>(R.id.avg_cadency).text = result.second.toString()
                }




//                findViewById<TextView>(R.id.cadenceValue).text = 1.toString()

            }
        }
    }

    private fun calculateCadency(data : Vector<Float>): Pair<Float, Float> {
        // Okienkowanie
        var dataWindow = takeLastElements(data, windowSize)

        val instantCadency = calculateInstantCadency(dataWindow)

        val averageCadency = calculateAverageCadency(instantCadency)

        // Note: jeżeli 1 extremum to zwróć 0 - jedzeisz za wolno żeby zmierzyć
        return Pair(instantCadency, averageCadency)
    }


    private fun calculateInstantCadency(data : Vector<Float>) :  Float{
        /* TODO: funkcja, ktora okienkuje fft i liczy wszystko:
            - FFT
            - okieno / filtrowanie
            - wypluwanie informacji o kadencji sredniej do tej pory i chwilowej
         */
        // FFT
        val fft = calculateFFT(data)
        val fftModules = getModuleVector(fft)

        // Filtracja pasmowo-przepustowa

        // Obliczenie maximum
        var extremes = getFrequency(fftModules)

        return extremes * seconds60 //* samplingPeriod    // Hz * 60 => cadence
    }

    /**
     *
     */

    private fun getFrequency(data : Vector<Float>) : Float {
        //TODO: wyliczenie ilosci ekstremow
        // f = il-ekstr/2 / windowSize * Sensor_Freq
        var counter = 0f
        var lastExtremeIndex = 0
        var highest = 0f;
        for (i: Int in 1 until data.size - 1) {
            if (data[i] > data[i-1] && data[i] > data[i+1] && data[i] > highest) {
//                if ((i - lastExtremeIndex) > minSampleIndexDistance && (i - lastExtremeIndex) < maxSampleIndexDistance) {
                    counter = i * 1/samplingPeriod / windowSize
                highest = data[i]
//                    lastExtremeIndex = i
//                }
            }
//            if (data[i] < data[i - 1] && data[i] < data[i + 1]) {
////                if ((i - lastExtremeIndex) > minSampleIndexDistance && (i - lastExtremeIndex) < maxSampleIndexDistance) {
//                    counter++
////                    lastExtremeIndex = i
////                }
//            }
        }
        return counter.toFloat()     // Mocno przybliżona wartość okresu
    }

    private fun takeLastElements(data:Vector<Float>, range:Int ) : Vector<Float> {
        var tmp : Vector<Float> = Vector(0)
        for (i:Int in 0 until range)
            tmp.add(data[data.size - range + i])
        return tmp
    }

    private fun calculateAverageCadency(data: Float): Float {

        cadenceVector.add(data)
        var sum = cadenceVector.sum()

        return sum / cadenceVector.size //* samplingPeriod    // Hz * 60 => cadence
    }

    // d(P1,P2) = (x2 x1)2 + (y2 y1)2 + (z2 z1)2.
    private fun calculateDistance(x : Float, y : Float, z : Float) : Float {
        return sqrt(x.toDouble().pow(2.0) + y.toDouble().pow(2.0) + z.toDouble().pow(2.0)).toFloat()
    }

    // d(P1,P2) = (x2 x1)2 + (y2 y1)2
    private fun calculateDistance2D(x : Float, y : Float) : Float {
        return sqrt(x.toDouble().pow(2.0) + y.toDouble().pow(2.0)).toFloat()
    }

    private fun calculateFFT(data : Vector<Float>) : Array<Complex> {
        val arrComplex : Array<Complex> = Array<Complex>(data.size) { Complex(0.0, 0.0) }

        for (i:Int in data.indices ) {
            arrComplex[i] = Complex(data[i].toDouble(),0.0)
        }

        return FFT.fft(arrComplex)
    }

    private fun getModuleVector(data : Array<Complex>) : Vector<Float> {
        val moduleVector = Vector<Float>(data.size)

        for (element: Complex in data) {
//            moduleVector.add(calculateDistance2D(element.a.toFloat(), element.b.toFloat()))
            moduleVector.add(element.a.toFloat())
        }
        return moduleVector
    }

    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)

    }

    fun resumeReading(view: View) {
        this.resume = true
    }

    fun pauseReading(view: View) {
        this.resume = false
    }
}
