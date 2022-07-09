package xyz.emlyn.hexsweeper

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import kotlin.math.*

class GameFrameLayout(context : Context, attrs : AttributeSet) : FrameLayout(context, attrs) {

    private val debugTag = "hexsweeper.log"

    private val TAP_THRESHOLD_MS = 125
    private val SCALE_WEIGHT = 0.3

    private var scaleFactor = 1.0
    private var minScaleFactor = scaleFactor
    private var centerX = 0
    private var centerY = 0

    lateinit var nodes : List<HexNode>
    var numRings = 0

    private var fingerPos : ArrayList<Triple<Float, Float, Long>> = ArrayList() // [(posX, posY, ms)]
    
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {

        if (changed || centerX == 0) {

            val minSfX : Double
            val minSfY : Double
            try {
                minSfX = measuredWidth.toDouble() / ((numRings/2 - 1) * (nodes[0].height) + (numRings/2) * (nodes[0].width))
                minSfY = measuredHeight.toDouble() / (nodes[0].height * (2 * numRings + 1))

                scaleFactor = min(minSfX, minSfY)
                minScaleFactor = scaleFactor
            } catch (_ : Exception) {}


            centerX = measuredWidth / 2
            centerY = measuredHeight / 2

            postDelayed({
                drawHex()
            }, 10)
        }

        super.onLayout(changed, left, top, right, bottom)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        return when (event!!.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                //First finger down
                addFinger(event)
                true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                //second finger down
                if (fingerPos.size == 1) {
                    addFinger(event)
                }
                true
            }
            MotionEvent.ACTION_MOVE -> {

                if (System.currentTimeMillis() - fingerPos[0].third > android.view.ViewConfiguration.getLongPressTimeout()) {
                    triggerLongTap(event)
                    fingerPos.add(Triple(-Float.MAX_VALUE, -Float.MAX_VALUE, -Long.MAX_VALUE)) //prevent re-tap again by setting bullshit time/pos
                    fingerPos.remove(fingerPos[0])
                    killChildren()
                    drawHex()
                }

                if (fingerPos.size == 1) {
                    moveWindow(event)
                } else if (fingerPos.size == 2) {
                    scaleWindow(event)
                }
                true
            }
            MotionEvent.ACTION_POINTER_UP -> {

                fingerPos.remove(fingerPos[1])
                true
            }
            MotionEvent.ACTION_UP -> {

                if (System.currentTimeMillis() - fingerPos[0].third < TAP_THRESHOLD_MS) {
                    triggerTap(event)
                }

                fingerPos.remove(fingerPos[0])
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    private fun addFinger(event : MotionEvent) {
        fingerPos.add(Triple(event.x, event.y, System.currentTimeMillis()))
    }

    private fun axialRound(qF : Double, rF : Double) : Pair<Int, Int> {

        val sF = -qF-rF

        var q = round(qF)
        var r = round(rF)
        val s = round(sF)

        val qDiff = abs(q - qF)
        val rDiff = abs(r - rF)
        val sDiff = abs(s - sF)

        if (qDiff > rDiff && qDiff > sDiff) {
            q = -r - s
        }
        else if (rDiff > sDiff) {
            r = -q - s
        }
        return Pair(q.toInt(), r.toInt())
    }

    private fun uncover(node : HexNode, numCounted : Int) : Int {


        if (node.mineNeighbourCount != null || node.mine || node.flag) {
            return numCounted //Already handled OR mine
        }

        node.mineNeighbourCount = 0
        var newNumCounted = numCounted

        for (neighbour in node.neighbours) {
            if (neighbour.mine) {
                node.mineNeighbourCount = node.mineNeighbourCount!! + 1
            }
        }

        if (node.mineNeighbourCount!! == 0) {
            for (neighbour in node.neighbours) {
                if (!(neighbour.mine || neighbour.flag)) {
                    uncover(neighbour, newNumCounted+1)
                }
            }
        }

        return newNumCounted
    }

    private fun getTappedNode(event : MotionEvent) : HexNode? {
        val offsetX = event.x - centerX
        val offsetY = event.y - centerY

        val q = (offsetX * 2/3) / scaleFactor
        val r = (offsetX * -1/3 + offsetY * sqrt(3.0)/3) / scaleFactor

        val roundedAxial = axialRound(q, r)

        var tappedNode : HexNode? = null
        for (node in nodes) {
            if (node.q == roundedAxial.first && node.r == roundedAxial.second) {
                tappedNode = node
                break
            }
        }

        return tappedNode;
    }


    private fun triggerTap(event : MotionEvent) {

        val tappedNode = getTappedNode(event) ?: return;
        //"elvis operator" - Some invalid position

        if (tappedNode.mine) {
            //TODO: Apply mine image to all not-uncovered mines (.1s between each)

            Intent().also { intent ->
                intent.action = "xyz.emlyn.hexsweeper.GAME_LOSS"
                context.sendBroadcast(intent)
            }
        } else {
            val deltaNumHex = uncover(tappedNode, 0)
            Intent().also { intent ->
                intent.action = "xyz.emlyn.hexsweeper.CHANGE_HEX"
                intent.putExtra("deltaHex", deltaNumHex)
                context.sendBroadcast(intent)
            }
        }



        killChildren()
        drawHex()

        for (node in nodes) {
            if (node.mineNeighbourCount == null && !node.mine) {
                return
            }
        }
        Intent().also{ intent ->
            intent.action = "xyz.emlyn.hexsweeper.GAME_WIN"
            context.sendBroadcast(intent)
        }

    }

    private fun triggerLongTap(event : MotionEvent) {
        val tappedNode = getTappedNode(event) ?: return;
        //"elvis operator" - Some invalid position
        if (tappedNode.mineNeighbourCount == null) {
            tappedNode.flag = !(tappedNode.flag)
        }
    }

    private fun moveWindow(event : MotionEvent) {

        // This fucks up all the maths so don't even bother trying to correct for it
        if (event.x < 0 || event.y < 0) { return }

        val newCX = centerX - (fingerPos[0].first - event.x)
        val newCY = centerY - (fingerPos[0].second - event.y)

        fingerPos[0] = Triple(event.x, event.y, fingerPos[0].third)

        val hexWidth = (1 + numRings/2) * (nodes[0].height) + (numRings/2) * (nodes[0].width)
        val hexHeight = nodes[0].height * (2 * numRings + 1)

        val freeLeft = newCX - scaleFactor * hexWidth/2
        val freeRight = measuredWidth - (newCX + scaleFactor * hexWidth/2)
        val freeTop = newCY - scaleFactor * hexHeight/2
        val freeBottom = measuredHeight - (newCY + scaleFactor * hexHeight/2)

        if ((freeLeft <= 0 && freeRight <= 0) || (freeRight > 0 && freeLeft > 0)) {
            centerX = newCX.toInt()
        }
        if ((freeTop <= 0 && freeBottom <= 0) || (freeTop > 0 && freeBottom > 0)) {
            centerY = newCY.toInt()
        }

        killChildren()
        drawHex()
    }

    private fun scaleWindow(event : MotionEvent) {

        if (event.historySize == 0) {
            return
        }

        val p1x = event.getHistoricalX(0, 0)
        val p1y = event.getHistoricalY(0, 0)
        val p2x = event.getHistoricalX(1, 0)
        val p2y = event.getHistoricalY(1, 0)

        val p1xNew = event.getX(0)
        val p1yNew = event.getY(0)
        val p2xNew = event.getX(1)
        val p2yNew = event.getY(1)

        val lengthInit = sqrt((p2x - p1x).pow(2) + (p2y - p1y).pow(2))
        val lengthFinal = sqrt((p2xNew - p1xNew).pow(2) + (p2yNew - p1yNew).pow(2))

        val newSf = scaleFactor + SCALE_WEIGHT * (lengthFinal - lengthInit)
        if (newSf >= minScaleFactor) {

            // ugly processing to ensure new position is valid (it just centers it lmao)
            centerX = (measuredWidth / 2)
            centerY = (measuredHeight / 2)

            scaleFactor = newSf

            killChildren()
            drawHex()
        }

    }

    private fun killChildren() {
        removeAllViews()
    }

    private fun drawHex() {

        for (i in nodes.indices) {

            val trueX = centerX + (nodes[i].deltaX * scaleFactor)
            val trueY = centerY + (nodes[i].deltaY * scaleFactor)

            val hexIV = ImageView(context)
            hexIV.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_hexagon_border))
            if (nodes[i].flag) {
                Log.d(debugTag, "ftlag");
                hexIV.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_hexsweep_flag))
            }

            val hexIVLP = LayoutParams((nodes[i].width * scaleFactor).toInt(), (nodes[i].height * scaleFactor).toInt())
            hexIVLP.setMargins(trueX.toInt(), trueY.toInt(), 0, 0)

            hexIV.layoutParams = hexIVLP
            addView(hexIV)

            //TODO: Add animation
            if (nodes[i].mineNeighbourCount != null) {

                if (nodes[i].mineNeighbourCount!! == 0) {
                    //Update drawable
                    hexIV.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_hexagon_border_dark))

                } else {
                    //Add textview
                    val mineNeighbourCountTV = TextView(context)

                    var color = 0

                    when (nodes[i].mineNeighbourCount) {
                        1 -> color = context.getColor(R.color.mine_1)
                        2 -> color = context.getColor(R.color.mine_2)
                        3 -> color = context.getColor(R.color.mine_3)
                        4 -> color = context.getColor(R.color.mine_4)
                        5 -> color = context.getColor(R.color.mine_5)
                        6 -> color = context.getColor(R.color.mine_6)
                    }

                    mineNeighbourCountTV.setTextColor(color)
                    mineNeighbourCountTV.text = nodes[i].mineNeighbourCount.toString()
                    mineNeighbourCountTV.textSize = (nodes[i].height * scaleFactor / 4.5).toFloat()
                    mineNeighbourCountTV.typeface = Typeface.DEFAULT_BOLD

                    val mineCountTVLP = LayoutParams((nodes[i].width * scaleFactor).toInt(),
                        (nodes[i].height * scaleFactor).toInt())
                    mineCountTVLP.setMargins(trueX.toInt(), trueY.toInt(), 0, 0)
                    mineNeighbourCountTV.layoutParams = mineCountTVLP
                    mineNeighbourCountTV.gravity = Gravity.CENTER
                    addView(mineNeighbourCountTV)
                }
            }
        }

    }
}