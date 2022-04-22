package com.coloryr.facetrack

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.coloryr.facetrack.live2d.CubismParam
import com.coloryr.facetrack.live2d.GLRenderer
import com.coloryr.facetrack.live2d.JniBridgeJava
import java.util.*

class GLFragment : Fragment() {
    private var constraintLayout: RelativeLayout? = null
    private lateinit var button: Button
    private val mos = 0
    private val exs = 0
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_dashboard, container, false)
        constraintLayout = root.findViewById(R.id.gl_root)
        button = root.findViewById(R.id.show_button)
        fps = root.findViewById(R.id.fps)
        timer.schedule(object : TimerTask() {
            override fun run() {
                val fps: Int = GLRenderer.getFps()
                MainActivity.run(Runnable { Companion.fps.text = fps.toString() })
            }
        }, 0, 1000)
        JniBridgeJava.ChangeModel()
        MainActivity.glView!!.callAdd(constraintLayout)
        JniBridgeJava.LoadModel("sizuku", "shizuku")
        button.setOnClickListener { v: View? ->
            list = JniBridgeJava.cubismParams
            if (list == null) {
                Log.i("mian", "get fail")
            }
            val list1 = JniBridgeJava.cubismParts
            if (list1 == null) {
                Log.i("mian", "get part fail")
            }
        }
        return root
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var fps: TextView
        var list: Array<CubismParam?>? = null
        private val timer = Timer()
    }
}