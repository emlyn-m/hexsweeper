package xyz.emlyn.hexsweeper

import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class ActivityIngame : AppCompatActivity() {

    private lateinit var mBroadcastReceiver : BroadcastReceiver
    lateinit var outputFL : GameFrameLayout

    override fun onDestroy() {
        outputFL.killed = true
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ingame)

        val fullScreenLayout : ConstraintLayout = findViewById(R.id.activityIngameCL)
        mBroadcastReceiver = GameInfoReceiver()
        (mBroadcastReceiver as GameInfoReceiver).fullScreenLayout = fullScreenLayout
        (mBroadcastReceiver as GameInfoReceiver).activity = this

        val filter = IntentFilter()
        filter.addAction("xyz.emlyn.hexsweeper.GAME_WIN")
        filter.addAction("xyz.emlyn.hexsweeper.GAME_LOSS")
        filter.addAction("xyz.emlyn.hexsweeper.CHANGE_HEX")
        filter.addAction("xyz.emlyn.hexsweeper.DEC_MINE")
        this.registerReceiver(mBroadcastReceiver, filter)

        //Get info from bundle
        val numHex : Int = intent.getIntExtra("numHex", -1)
        val numMines : Int = intent.getIntExtra("numMines", -1)

        //update indicators
        findViewById<LinearLayout>(R.id.hexInfoLL).findViewById<TextView>(R.id.hexTV).text = (numHex - numMines).toString()
        findViewById<LinearLayout>(R.id.mineInfoLL).findViewById<TextView>(R.id.mineTV).text = numMines.toString()

        //For some reason data not properly sent - go back and re-try
        if (numHex == -1 || numMines == -1) {
            finish()
            return
        }

        val numRings = calculateRings(numHex) //Convert to a more useful form


        //Generate mine positions
        val minePositions = Array(numMines) { Random.nextInt(0, numHex-1) }

        //Iterate over q, p to generate nodes
        var mineIdx = 0

        val nodes : ArrayList<HexNode> = ArrayList()
        for (q in -(numRings-1) until numRings) {
            for (r in max(-(numRings-1), -q-(numRings-1))..min((numRings-1), -q+(numRings-1))) {
                nodes.add(HexNode(q, r))

                if (minePositions.contains(mineIdx)) {
                    nodes[nodes.size-1].mine = true
                }

                mineIdx += 1
            }
        }

        //Set neighbours
        for (node in nodes) {  // Shitty algorithm but eh - could maybe optimize later?
            for (node_ in nodes) {
                if (checkNeighbours(node, node_)) {
                    node.neighbours.add(node_)
                }
            }
        }

        //Setup window
        outputFL = findViewById(R.id.gameWindowFL)
        outputFL.nodes = nodes
        outputFL.numRings = numRings
    }

    private fun checkNeighbours(n1 : HexNode, n2 : HexNode) : Boolean {
        if (n1.q == n2.q && n1.r == n2.r) {
            return false //Same node
        }

        if (abs(n1.q - n2.q) > 1) {
            return false // Too far apart Q
        }

        if (abs(n1.r - n2.r) > 1) {
            return false // Too far apart R
        }

        if (abs((-n1.q - n1.r) - (-n2.q-n2.r)) > 1) {
            return false // Too far apart S
        }

        return true
    }

    private fun calculateRings(spaces : Int): Int {
        return ((3 + (12*spaces - 3).toDouble().pow(0.5))/6).toInt()
    }
}