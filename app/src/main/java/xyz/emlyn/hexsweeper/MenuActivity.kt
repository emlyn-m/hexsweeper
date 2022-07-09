package xyz.emlyn.hexsweeper

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import kotlin.math.pow

// numSpirals = [2, 10]
// numMines = [1, numHex]

val debugTag = "hexsweeper.log"

class MenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var currNumMines = 63
        val numMinesSB : SeekBar = findViewById(R.id.numMinesSB)
        val numMinesTV : TextView  = findViewById(R.id.numMinesTV)
        numMinesSB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                currNumMines = p1
                numMinesTV.text = "$p1 MINES"
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        var currNumRings = 6
        val numHexSB : SeekBar = findViewById(R.id.numRingSB)
        val numHexTV : TextView = findViewById(R.id.numHexTV)
        numHexSB.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                currNumRings = p1
                numHexTV.text = "${p1} RINGS"
                numMinesSB.max = (.5 * calculateHex(p1)).toInt()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        findViewById<TextView>(R.id.beginTV).setOnClickListener {
            val startGameIntent = Intent(this, ActivityIngame::class.java)
            startGameIntent.putExtra("numHex", calculateHex(currNumRings))
            startGameIntent.putExtra("numMines", currNumMines)
            startActivity(startGameIntent)
        }
    }

    fun calculateHex(numSpirals : Int) : Int {
        return (3*(numSpirals.toDouble().pow(2.0) - numSpirals) + 1).toInt()
    }


}