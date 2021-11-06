package com.mobileapps.cadence_app

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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

                findViewById<TextView>(R.id.acc_X).text = x.toString()
                findViewById<TextView>(R.id.acc_Y).text = y.toString()
                findViewById<TextView>(R.id.acc_Z).text = z.toString()

                data.add(calculateDistance(x,y,z))

                findViewById<TextView>(R.id.length).text = data.size.toString()

                if (data.size == 65) {
                    for (i:Int in 0 until data.size) { if (data[i] != 0.toFloat()) { println(data[i]) } }
                    data.removeElementAt(0)
                    val a = calculateFFT(data)
//                    for (i:Int in 0 until data.size){
//                        println(a[i].toString())
//                    }
                    findViewById<TextView>(R.id.fft_value).text = a[0].toString()
                }




//                findViewById<TextView>(R.id.cadenceValue).text = 1.toString()

            }
        }
    }

    // d(P1,P2) = (x2 x1)2 + (y2 y1)2 + (z2 z1)2.
    private fun calculateDistance(x : Float, y : Float, z : Float) : Float {
        return sqrt(x.toDouble().pow(2.0) + y.toDouble().pow(2.0) + z.toDouble().pow(2.0)).toFloat()
    }

    private fun calculateFFT(data : Vector<Float>) : Array<Complex> {
        val arrComplex : Array<Complex> = Array<Complex>(data.size) { Complex(0.0, 0.0) }

        for (i:Int in data.indices ) {
            arrComplex[i] = Complex(data[i].toDouble(),0.0)
        }

        return FFT.fft(arrComplex)
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
