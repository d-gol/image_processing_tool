import java.io.{File, PrintWriter}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scalafx.collections.ObservableBuffer
import scalafx.scene.image.WritableImage
import Utilities._

/* Class to hold open pictures */
class Pictures {
  /* The index in ListBuffer represents a layer! */
  val pictures: ListBuffer[Picture] = new ListBuffer[Picture]()

  def addPicture(picture: Option[Picture]) {
    picture match {
      case Some(p) =>
        for (pic <- pictures)
          if (pic.filepath.equals(p.filepath))
            return
        pictures += p
      case _ =>
    }
  }

  def addPictureByUrl(url: String) {
    val picture = Some(new Picture(url))
    addPicture(picture)
  }

  def containsPictureWithUrl(url: String): Boolean = {
    pictures.foreach(p => {if (p.filepath.equals(url)) true})
    false
  }

  /* To be used when accessing picture from ListView */
  def getPictureByUrl(url: String): Option[Picture] = {
    pictures.foreach(p => if (p.filepath.equals(url)) return Some(p))
    None
  }

  def getPictureByIndex(ind: Integer): Picture = {
    pictures(ind)
  }

  /* Moving the picture towards us, higher in layer hierarchy. CAUTION: Only one layer up! */
  def movePictureLayerUp(picture: Option[Picture]) {
    movePictureLayer(picture, up = true)
  }

  /* Moving the picture back from the viewer, lower in layer hierarchy. CAUTION: Only one layer down! */
  def movePictureLayerDown(picture: Option[Picture]) {
    movePictureLayer(picture, up = false)
  }

  def movePictureLayer(picture: Option[Picture], up: Boolean) {
    picture match {
      case Some(p) =>
        val pictureIndex = pictures.indexOf(p)
        /* If first or last index do nothing ie return */
        if ((up && pictureIndex == 0) || (!up && pictureIndex == pictures.length - 1)) return
        val newIndex = if (up) pictureIndex - 1 else pictureIndex + 1
        removePicture(Some(p))
        pictures.insert(newIndex, p)
      case _ =>
    }
  }

  def removePictureByUrl(url: String) {
    getPictureByUrl(url) match {
      case Some(p) => pictures -= p
      case _ =>
    }
  }

  def removePicture(picture: Option[Picture]) {
    picture match {
      case Some(p) => pictures -= p
      case _ =>
    }
  }

  /* Used in ListView */
  def getPicturesFilenames: ObservableBuffer[String] = {
    val filenames = new ObservableBuffer[String]()
    pictures.foreach(p => {
      filenames += p.filepath
    })
    // Return
    filenames
  }

  def saveCompositePicture(fileUrl: String) {
    val pw = new PrintWriter(new File(fileUrl))
    for (pic <- pictures) {
      if (pic.isActive) {
        pw.write(pic.filepath + " " + pic.transparency)
        for (sel <- pic.selections) {
          if (sel.isActive) {
            pw.write(" " + sel.name + "|" + sel.upperLeft._1 + "," + sel.upperLeft._2 + "|" + sel.downRight._1 + "," + sel.downRight._2)
          }
        }
      }
      pw.write("\n")
    }
    pw.close()
  }

  def readCompositePicture(fileUrl: String) {
    for (line <- Source.fromFile(fileUrl).getLines) {
      val tokens = line.split(" ")
      val pic: Option[Picture] = Some(new Picture(tokens(0)))
      pic match {
        case Some(p) =>
          p.transparency = tokens(1).toDouble
          for (i <- 2 until tokens.size) {
            val selTokens = tokens(i).split("\\|")
            val sel = new Selection(selTokens(0), readXY(selTokens(1)), readXY(selTokens(2)), true)
            p.addSelection(sel)
          }
          addPicture(pic)
        case _ =>
      }
    }
  }

  def makeCompositeImage(): WritableImage = {
    val wImage = new WritableImage(1600, 900)
    var currentTransp: Double = 0
    for (picture <- pictures) {
      picture.writableImage.pixelReader match {
        case Some(pr) =>
          wImage.pixelReader match {
            case Some(wIpr) =>
              for (x <- 0 to 1599) {
                for (y <- 0 to 899) {
                  val argbPicture = pr.getArgb(x, y)
                  val argbWImage = wIpr.getArgb(x, y)
                  val red = (currentTransp * ((argbWImage >> 16) & 255)).toInt + (((argbPicture >> 16) & 255) * (1 - currentTransp) * picture.transparency).toInt
                  val green = (currentTransp * ((argbWImage >> 8) & 255)).toInt + (((argbPicture >> 8) & 255) * (1 - currentTransp) * picture.transparency).toInt
                  val blue = (currentTransp * (argbWImage & 255)).toInt + ((argbPicture & 255) * (1 - currentTransp) * picture.transparency).toInt

                  // Return
                  val pixelValue = (255 << 24) + (red << 16) + (green << 8) + blue
                  wImage.pixelWriter.setArgb(x, y, pixelValue)
                }
              }
              currentTransp += (1 - currentTransp) * picture.transparency
              if (currentTransp >= 1) {
                return wImage
              }
            case _ =>
          }
        case _ =>
      }
    }
    wImage
  }
}