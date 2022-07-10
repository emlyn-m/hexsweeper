package xyz.emlyn.hexsweeper

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.URL
import java.net.URLConnection
import java.util.*
import kotlin.math.pow

// numSpirals = [2, 10]
// numMines = [1, numHex]

val debugTag = "hexsweeper.log"
const val BUILD_NO = 1.0

class MenuActivity : AppCompatActivity() {
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.update_channel)
            val descriptionText = getString(R.string.update_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel("iloveu", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        setContentView(R.layout.activity_main)

        lifecycleScope.launch(Dispatchers.IO) {
            var content = ""
            var connection: URLConnection?
            try {
                connection = URL("https://emlyn.xyz/hexsweep_buildno").openConnection()
                val scanner = Scanner(connection.getInputStream())
                scanner.useDelimiter("\\Z")
                content = scanner.next()
                scanner.close()
            } catch (ignored: Exception) {Log.e(debugTag, ignored.toString())}

            var contentNum = content.toFloatOrNull()
            if (contentNum == null) { contentNum = -1.0f }
            //here switch to Main thread to Update your UI related task

            withContext(Dispatchers.Main) {
                if (contentNum > BUILD_NO) {
                    //Create update notification

                    val updateIntent = Intent(Intent.ACTION_VIEW)
                    updateIntent.data = Uri.parse("http://emlyn.xyz/#screen-4")

                    val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext,
                        0,
                        updateIntent,
                        PendingIntent.FLAG_IMMUTABLE)


                    val updateBuilder = NotificationCompat.Builder(applicationContext, "iloveu")
                        .setSmallIcon(R.drawable.ic_hexsweep_logo)
                        .setContentTitle("Update avaliable!")
                        .setPriority(NotificationCompat.PRIORITY_LOW)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)

                    with(NotificationManagerCompat.from(applicationContext)) {
                        notify(0x8923, updateBuilder.build())
                    }

                }
            }
        }


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