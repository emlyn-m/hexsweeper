package xyz.emlyn.hexsweeper

import kotlin.math.sqrt

class HexNode(val q: Int, val r: Int) {

    val height = 2
    val width = 2 + 2 * sqrt(3.0)

    val neighbours : ArrayList<HexNode> = ArrayList()
    var mineNeighbourCount : Int? = null;

    var deltaX = (1.5 * q) - width/2
    var deltaY = (q * sqrt(3.0)/2 + r * sqrt(3.0)) - height/2

    var mine : Boolean = false // Default false
    var flag : Boolean = false
    var ucMine : Boolean = false
}