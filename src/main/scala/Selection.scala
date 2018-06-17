/* Class which represents a selection in a picture */
class Selection(n: String, upperL: (Int, Int), downR: (Int, Int), isAct: Boolean) {
  val name: String = n
  val upperLeft: (Int, Int) = upperL
  val downRight: (Int, Int)  = downR
  val isActive: Boolean = isAct

  override def toString: String = name + " " + upperLeft._1 + "," + upperLeft._2 + " " +
    downRight._1 + "," + downRight._2 + " " + (if (isActive) "Active" else "Inactive")
}
