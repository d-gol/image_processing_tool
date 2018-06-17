object Utilities {

  def isDouble(value: String): Boolean = try {
    value.toDouble
    true
  } catch {
    case _: Throwable => false
  }

  def isInt(value: String): Boolean = try {
    value.toInt
    true
  } catch {
    case _: Throwable => false
  }

  def canReadXY(value: String): Boolean = {
    val tokens = value.split(",")
    if (tokens.size != 2) return false
    tokens.foreach(token => { if (!isInt(token)) return false })
    true
  }

  def readXY(value: String): (Int, Int) = {
    val tokens = value.split(",")
    (tokens(0).toInt, tokens(1).toInt)
  }
}
