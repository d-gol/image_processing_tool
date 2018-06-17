import javax.imageio.ImageIO

import scala.collection.mutable.ListBuffer
import scalafx.collections.ObservableBuffer
import scalafx.embed.swing.SwingFXUtils
import scalafx.scene.image.{Image, WritableImage}

/* Class which represents a picture */
class Picture(url: String) {
  // checkExtension() - required - da ekstenzija bude u skupu nekih vrednosti

  def openPicture(filepath: String): Image = {
    new Image(url, 1600, 900, false, false)
  }

  val filepath: String = url
  private val image: Image = openPicture(url)
  private val basicSelection: Selection = new Selection("Basic", (0, 0), (image.height.value.toInt - 1, image.width.value.toInt - 1), true)
  val selections: ListBuffer[Selection] = new ListBuffer[Selection]()
  selections += basicSelection

  var isActive: Boolean = true
  var transparency: Double = 1

  var writableImage: WritableImage = image.pixelReader match {
    case Some(pr) => new WritableImage(pr, image.width.value.toInt, image.height.value.toInt) // default
    case _ => new WritableImage(image.width.value.toInt, image.height.value.toInt)
  }

  def isOverlappingAdd(newSelection: Selection): Boolean = {
    for (selection <- selections) {
      if (selection.name.equals(newSelection.name) || 1 == 0 /* && geometrija */) {
        return true
      }
    }
    false
  }

  def isOverlappingModify(newSelection: Selection): Boolean = {
    for (selection <- selections) {
      if (!selection.name.equals(newSelection.name) && 1 == 0 /* && geometrija */) {
        return true
      }
    }
    false
  }

  def addSelection(newSelection: Selection): Boolean = {
    if (newSelection.name.equals("Basic"))
      return false
    if (selections.contains(basicSelection))
      selections -= basicSelection
    else {
      if (isOverlappingAdd(newSelection))
        return false
    }
    selections += newSelection
    true
  }

  def modifySelection(newSelection: Selection): Boolean =
    getSelectionByName(newSelection.name) match {
      case Some(sel) =>
        if (isOverlappingModify(newSelection))
          false
        else {
          selections(selections.indexOf(sel)) = newSelection
          true
        }
      case None => false
    }

  def getSelectionByName(name: String): Option[Selection] = {
    selections.foreach(sel => if (sel.name.equals(name)) return Some(sel))
    None
  }

  def removeSelection(name: String) {
    if (!name.equals("Basic")) {
      getSelectionByName(name) match {
        case Some(sel) =>
          image.pixelReader match {
            case Some(pr) =>
              for (x <- sel.upperLeft._2 to sel.downRight._2) {
                for (y <- sel.upperLeft._1 to sel.downRight._1) {
                  writableImage.pixelWriter.setArgb(x, y, pr.getArgb(x, y))
                }
              }
            case _ =>
          }

          selections -= sel

          if (selections.isEmpty)
            selections += basicSelection
        case _ =>
      }
    }
  }

  def getSelectionsAsString: ObservableBuffer[String] = {
    val selectionDescriptions = new ObservableBuffer[String]()
    selections.foreach(p => {
      selectionDescriptions += p.toString
    })

    selectionDescriptions
  }

  def addWritableImage(wImage: WritableImage) {
    writableImage = wImage
  }

  def savePicture(url: String) {
    ImageIO.write(SwingFXUtils.fromFXImage(new Image(writableImage), null), "png", new java.io.File(url))
  }

  override def toString: String = {
    filepath
  }
}
