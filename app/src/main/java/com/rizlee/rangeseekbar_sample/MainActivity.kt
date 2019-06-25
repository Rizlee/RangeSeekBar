package com.rizlee.rangeseekbar_sample

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        test.setOnClickListener{
            rangeSeekBar.setRange(10, 30, 2)
            rangeSeekBar.setCurrentValues(10f, 12.0f)
        }
    }
}
