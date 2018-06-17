import java.io.{File, PrintWriter}
import java.nio.file.Paths
import javax.imageio.ImageIO

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.collections.ObservableBuffer
import scalafx.geometry._
import scalafx.scene.control.Alert.AlertType
import scalafx.scene.control._
import scalafx.scene.image._
import scalafx.scene.input.{KeyCode, KeyCodeCombination, KeyCombination}
import scalafx.scene.layout.{GridPane, HBox}
import scalafx.scene.{Group, Scene}
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
import Utilities._

import scala.io.Source
import scalafx.embed.swing.SwingFXUtils

object helloSBT extends JFXApp {
  // Control variables
  val openPictures = new Pictures()
  var currentPicture: Option[Picture] = None
  var operationOnSinglePicture: Boolean = true
  var numberOfCompositeOperations = 0

  // Constants
  val imageViewWidth = 900
  val imageViewHeight = 600
  val listViewWidth = 420
  val listViewHeight = 160
  val basicOperationsPaneHeight = 40
  val operationsLabelsWidth = 50
  val leftPadding = 15
  val rightPadding = 15
  val buttonOperationWidth = 40
  val blankImagePath: String = "file:///" + Paths.get(".").toAbsolutePath.normalize.toString + "/src/images/blank.jpg"
  val maxNumberOfCompositeOperations = 29

  // GUI
  val compositeOperationsPane = new GridPane
  val menuBar = new MenuBar

  val imageView = new ImageView
  imageView.preserveRatio = true
  imageView.setFitWidth(imageViewWidth)
  imageView.setFitHeight(imageViewHeight)
  imageView.image = new Image(blankImagePath)
  imageView.alignmentInParent = Pos.Center

  val hBox = new HBox()
  hBox.setStyle("-fx-border-color: grey; -fx-border-width: 1; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);")
  hBox.getChildren.add(imageView)

  // Picture control components
  val moveLayerUpButton: Button = createButton("Move Up", buttonOperationWidth * 3, basicOperationsPaneHeight / 2)
  val moveLayerDownButton: Button = createButton("Move Down", buttonOperationWidth * 3, basicOperationsPaneHeight / 2)
  val showCompositePictureButton: Button = createButton("Show Composite", buttonOperationWidth * 3, basicOperationsPaneHeight / 2)

  val alphaLabel: Label = createLabel("Alpha", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val alphaTextField: TextField = createTextField(buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  val isActivePictureLabel: Label = createLabel("Active", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val isActivePictureCheckBox: CheckBox = new CheckBox
  isActivePictureCheckBox.alignment = Pos.Center
  isActivePictureCheckBox.alignmentInParent = Pos.Center

  val picturesListView: ListView[String] = new ListView[String]() {
    selectionModel().selectedItem.onChange {
      (_, _, newValue) => {
        openPictures.getPictureByUrl(newValue) match {
          case Some(p) =>
            currentPicture = Some(p)
            alphaTextField.text = p.transparency.toString
            isActivePictureCheckBox.selected = p.isActive
            refreshViews()
          case _ =>
        }
      }
    }
  }
  picturesListView.setPrefSize(listViewWidth, listViewHeight)

  val modifyPictureButton: Button = createButton("Modify", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  // Selections control components
  val nameLabel: Label = createLabel("Name", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val nameTextField: TextField = createTextField(buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  val upperLeftLabel: Label = createLabel("Upper Left", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val upperLeftTextField: TextField = createTextField(buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  val downRightLabel: Label = createLabel("Down Right", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val downRightTextField: TextField = createTextField(buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  val isActiveSelectionLabel: Label = createLabel("Active", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val isActiveSelectionCheckBox: CheckBox = new CheckBox
  isActiveSelectionCheckBox.alignment = Pos.Center
  isActiveSelectionCheckBox.alignmentInParent = Pos.Center

  val addSelectionButton: Button = createButton("Add", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val modifySelectionButton: Button = createButton("Modify", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val removeSelectionButton: Button = createButton("Remove", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  val selectionsListView: ListView[String] = new ListView[String]() {
    selectionModel().selectedItem.onChange {
      (_, _, newValue) => {
        newValue match {
          case value: String =>
            val tokens = value.split(" ")
            nameTextField.text = tokens(0)
            upperLeftTextField.text = tokens(1)
            downRightTextField.text = tokens(2)
            isActiveSelectionCheckBox.selected = if (tokens(3).equals("Active")) true else false
          case _ =>
        }
      }
    }
  }
  selectionsListView.setPrefSize(listViewWidth, listViewHeight)

  // Operation buttons
  val rgbTextField: TextField = createTextField(buttonOperationWidth * 3, basicOperationsPaneHeight / 2)
  rgbTextField.alignmentInParent = Pos.BottomCenter

  val rgbButton: Button = createButton("Fill (R G B) [0..1]", buttonOperationWidth * 3, basicOperationsPaneHeight * 2)
  rgbButton.alignmentInParent = Pos.BottomCenter

  val addButton: Button = createButton("+", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val addTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val subButton: Button = createButton("-", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val subTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val invSubButton: Button = createButton("!-", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val invSubTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val mulButton: Button = createButton("*", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val mulTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val divButton: Button = createButton("/", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val divTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val invDivButton: Button = createButton("!/", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val invDivTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val powButton: Button = createButton("pow", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val powTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val logButton: Button = createButton("log", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val logTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val absButton: Button = createButton("abs", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val absTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val minButton: Button = createButton("min", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val minTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val maxButton: Button = createButton("max", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val maxTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val inversionButton: Button = createButton("inv", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val greyscaleButton: Button = createButton("grey", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val oldButton: Button = createButton("old", buttonOperationWidth, basicOperationsPaneHeight / 2)

  val filterMedianButton: Button = createButton("med", buttonOperationWidth, basicOperationsPaneHeight / 2)
  val filterMedianTextField: TextField = createTextField(buttonOperationWidth, basicOperationsPaneHeight / 2)

  val filterPondButton: Button = createButton("pond", buttonOperationWidth * 2, basicOperationsPaneHeight / 2)
  val filterPondTextField: TextField = createTextField(buttonOperationWidth * 2, basicOperationsPaneHeight / 2)

  def guiAddMenu() {
    // Adding components
    val menuFile = new Menu("File")
    val menuFileOpenOnePicture = new MenuItem("Open")
    menuFileOpenOnePicture.accelerator = new KeyCodeCombination(KeyCode.O, KeyCombination.ShortcutDown)
    //val menuFileOpenCompositePicture = new MenuItem("Open Composite Picture")
    val menuFileSaveOnePicture = new MenuItem("Save")
    menuFileSaveOnePicture.accelerator = new KeyCodeCombination(KeyCode.S, KeyCombination.ShortcutDown)
    val menuFileSaveCompositePicture = new MenuItem("Save Composite")
    val menuFileClosePicture = new MenuItem("Close Picture")
    val menuOperationSaveComposite = new MenuItem("Save Composite")
    val menuOperationLoadComposite = new MenuItem("Load Composite")
    val menuOperationRemoveComposite = new MenuItem("Remove All Composite")
    val menuOperationSaveSequences = new MenuItem("Save Sequences")
    val menuOperationLoadSequences = new MenuItem("Load Sequences")
    val menuOperationRemoveSequences = new MenuItem("Remove All Sequences")
    val menuFileExitItem = new MenuItem("Exit Program")
    menuFileExitItem.accelerator = new KeyCodeCombination(KeyCode.F4, KeyCombination.AltDown)

    menuFile.items = List(menuFileOpenOnePicture, menuFileSaveOnePicture, /*menuFileOpenCompositePicture,*/
      menuFileSaveCompositePicture, menuFileClosePicture, new SeparatorMenuItem(), menuFileExitItem)

    val menuOperation = new Menu("Operation")
    val menuOperationAddComposite = new MenuItem("Add Composite Operation")
    val menuOperationAddSequence = new MenuItem("Add Sequence of Operations")
    val menuOperationTG = new ToggleGroup()
    val menuOperationSingle = new RadioMenuItem("Operation Single Picture")
    val menuOperationAll = new RadioMenuItem("Operation All Pictures")
    menuOperationSingle.setToggleGroup(menuOperationTG)
    menuOperationSingle.selected = true
    menuOperationAll.setToggleGroup(menuOperationTG)

    menuOperation.items = List(menuOperationSingle, menuOperationAll, new SeparatorMenuItem(),
      menuOperationSaveComposite, menuOperationLoadComposite, menuOperationRemoveComposite, new SeparatorMenuItem(),
      menuOperationSaveSequences, menuOperationLoadSequences, menuOperationRemoveSequences, new SeparatorMenuItem(),
      menuOperationAddComposite, menuOperationAddSequence)

    menuBar.menus = List(menuFile, menuOperation)

    // Adding listeners
    menuFileOpenOnePicture.onAction = _ => { guiShowOpenPictureDialog() }
    menuFileSaveOnePicture.onAction = _ => { guiShowSavePictureDialog(false) }
    menuFileSaveCompositePicture.onAction = _ => { guiShowSavePictureDialog(true) }
    menuFileClosePicture.onAction = _ => {
      openPictures.removePicture(currentPicture)
      picturesListView.getSelectionModel.selectNext()
      currentPicture = openPictures.getPictureByUrl(picturesListView.getSelectionModel.getSelectedItem)
      refreshViews()
    }
    menuFileExitItem.onAction = _ => { stage.close() }
    menuOperationSingle.onAction = _ => { operationOnSinglePicture = true }
    menuOperationAll.onAction = _ => { operationOnSinglePicture = false }
    menuOperationSaveComposite.onAction = _ => {
      val fileChooser = new FileChooser
      fileChooser.setTitle("Save File with Composite Operations")
      fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
      fileChooser.getExtensionFilters.addAll(new ExtensionFilter("tsv", "*.tsv"))
      val selectedFile = fileChooser.showSaveDialog(stage)
      if (selectedFile != null) {
        val pw = new PrintWriter(new File(selectedFile.getAbsolutePath))
        for ((key, value) <- compositeMap) {
          pw.write(key)
          for ((operName, constVal) <- value) {
            constVal match {
              case Some(v) => pw.write(" " + operName + "|" + v)
              case _ => pw.write(" " + operName)
            }
          }
          pw.write("\n")
        }
        pw.close()
      }
    }
    menuOperationLoadComposite.onAction = _ => {
      val fileChooser = new FileChooser
      fileChooser.setTitle("Load File with Composite Operations")
      fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
      fileChooser.getExtensionFilters.addAll(new ExtensionFilter("tsv", "*.tsv"))
      val selectedFile = fileChooser.showOpenDialog(stage)
      if (selectedFile != null) {
        for (line <- Source.fromFile(selectedFile.getAbsolutePath).getLines) {
          val tokens = line.split(" ")
          val compName = tokens(0)
          if (!compositeMap.contains(compName)) {
            val fList: ListBuffer[(String, Option[Double])] = ListBuffer[(String, Option[Double])]()
            for (i <- 1 until tokens.size) {
              val operNameConstTokens = tokens(i).split("\\|")
              if (operNameConstTokens.length == 1) {
                val newPair: (String, Option[Double]) = (operNameConstTokens(0), None)
                fList += newPair
              }
              else if (operNameConstTokens.length == 2) {
                val newPair: (String, Option[Double]) = (operNameConstTokens(0), Some(operNameConstTokens(1).toDouble))
                fList += newPair
              }
            }
            if (fList.nonEmpty) {
              val b: Button = createButton(compName, compName.length * 15, basicOperationsPaneHeight)
              compositeOperationsPane.add(b, numberOfCompositeOperations, 0)
              numberOfCompositeOperations += 1
              def composedOperation(pixelValue: Double): Double = repeatedComposeFromList(fList, fList.size - 1)(pixelValue)
              fMapNoConst.put(compName, (composedOperation, b))
              compositeMap.put(compName, fList)
              newAddListenersToNoConstButtons()
            }
          } else {
            showFailDialog("Sequence not loaded", "One of the sequences already exists")
          }
        }
      }
    }
    menuOperationRemoveComposite.onAction = _ => {
      for ((key, _) <- compositeMap) {
        if (fMapNoConst.contains(key)) {
          compositeOperationsPane.children.remove(fMapNoConst(key)._2)
          fMapNoConst.remove(key)
          numberOfCompositeOperations -= 1
        }
      }
      compositeMap.clear()
    }
    menuOperationSaveSequences.onAction = _ => {
      val fileChooser = new FileChooser
      fileChooser.setTitle("Save File with Sequence of Operations")
      fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
      fileChooser.getExtensionFilters.addAll(new ExtensionFilter("tsv", "*.tsv"))
      val selectedFile = fileChooser.showSaveDialog(stage)
      if (selectedFile != null) {
        val pw = new PrintWriter(new File(selectedFile.getAbsolutePath))
        for ((key, value) <- sequenceMap) {
          pw.write(key)
          for ((operName, constVal) <- value._1) {
            constVal match {
              case Some(v) => pw.write(" " + operName + "|" + v)
              case _ => pw.write(" " + operName)
            }
          }
          pw.write("\n")
        }
        pw.close()
      }
    }
    menuOperationLoadSequences.onAction = _ => {
      val fileChooser = new FileChooser
      fileChooser.setTitle("Load File with Sequence of Operations")
      fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
      fileChooser.getExtensionFilters.addAll(new ExtensionFilter("tsv", "*.tsv"))
      val selectedFile = fileChooser.showOpenDialog(stage)
      if (selectedFile != null) {
        for (line <- Source.fromFile(selectedFile.getAbsolutePath).getLines) {
          val tokens = line.split(" ")
          val seqName = tokens(0)
          if (!sequenceMap.contains(seqName)) {
            val fList: ListBuffer[(String, Option[Double])] = ListBuffer[(String, Option[Double])]()
            for (i <- 1 until tokens.size) {
              val operNameConstTokens = tokens(i).split("\\|")
              if (operNameConstTokens.length == 1) {
                val newPair: (String, Option[Double]) = (operNameConstTokens(0), None)
                fList += newPair
              }
              else if (operNameConstTokens.length == 2) {
                val newPair: (String, Option[Double]) = (operNameConstTokens(0), Some(operNameConstTokens(1).toDouble))
                fList += newPair
              }
            }
            if (fList.nonEmpty) {
              val b: Button = createButton(seqName, seqName.length * 15, basicOperationsPaneHeight)
              compositeOperationsPane.add(b, numberOfCompositeOperations, 0)
              numberOfCompositeOperations += 1
              sequenceMap.put(seqName, (fList, b))
              newAddListenersToSequenceButtons()
            }
          } else {
            showFailDialog("Sequence not loaded", "One of the sequences already exists")
          }
        }
      }
    }
    menuOperationRemoveSequences.onAction = _ => {
      for ((key, _) <- sequenceMap) {
        compositeOperationsPane.children.remove(sequenceMap(key)._2)
        numberOfCompositeOperations -= 1
      }
      sequenceMap.clear()
    }
    menuOperationAddComposite.onAction = _ => {
      if (numberOfCompositeOperations >= maxNumberOfCompositeOperations) {
        showFailDialog("Adding not successfull", "Max number of composite operations reached.")
      } else {
        val resButton = showAddComponentDialog(true)
        resButton match {
          case Some(b) =>
            compositeOperationsPane.add(b, numberOfCompositeOperations, 0)
            numberOfCompositeOperations += 1
          case _ =>
        }
      }
    }
    menuOperationAddSequence.onAction = _ => {
      if (numberOfCompositeOperations >= maxNumberOfCompositeOperations) {
        showFailDialog("Adding not successfull", "Max number of composite operations reached.")
      } else {
        val resButton = showAddComponentDialog(false)
        resButton match {
          case Some(b) =>
            compositeOperationsPane.add(b, numberOfCompositeOperations, 0)
            numberOfCompositeOperations += 1
          case _ =>
        }
      }
    }
  }

  def guiShowOpenPictureDialog() {
    val fileChooser = new FileChooser
    fileChooser.setTitle("Open Picture")
    fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
    fileChooser.getExtensionFilters.addAll(new ExtensionFilter("jpg", "*.jpg"), new ExtensionFilter("png", "*.png"),
      new ExtensionFilter("bmp", "*.bmp"), new ExtensionFilter("tsv", "*.tsv"))
    val selectedFile = fileChooser.showOpenDialog(stage)

    if (selectedFile != null) {
      val selectedFilePath = selectedFile.getAbsolutePath
      if (selectedFilePath.substring(selectedFilePath.lastIndexOf(".")).equals(".tsv")) {
        try {
          openPictures.readCompositePicture(selectedFilePath)
        } catch {
          case _: Throwable => showFailDialog("Error during read", "Please check the input file.")
        }
      } else {
        currentPicture = Some(new Picture("file:///" + selectedFilePath))
        openPictures.addPicture(currentPicture)
      }
      refreshViews()
    }
  }

  def guiShowSavePictureDialog(saveComposite: Boolean) {
    currentPicture match {
      case Some(p) =>
        val fileChooser = new FileChooser
        fileChooser.setTitle("Open Picture")
        fileChooser.setInitialDirectory(new File(Paths.get(".").toAbsolutePath.normalize.toString + "/src/images"))
        fileChooser.getExtensionFilters.addAll(new ExtensionFilter("jpg", "*.jpg"), new ExtensionFilter("png", "*.png"),
          new ExtensionFilter("bmp", "*.bmp"), new ExtensionFilter("tsv", "*.tsv"))
        val selectedFile = fileChooser.showSaveDialog(stage)
        if (selectedFile != null) {
          val selectedFilePath = selectedFile.getAbsolutePath
          if (selectedFilePath.substring(selectedFilePath.lastIndexOf(".")).equals(".tsv")) {
            openPictures.saveCompositePicture(selectedFilePath)
          } else {
            if (!saveComposite)
              p.savePicture(selectedFile.getAbsolutePath)
            else
              ImageIO.write(SwingFXUtils.fromFXImage(new Image(openPictures.makeCompositeImage()), null), "png",
                new java.io.File(selectedFile.getAbsolutePath))
          }
        }

      case _ => showFailDialog("No current picture", "There is no picture to be saved.")
    } // No need for refresh
  }

  def showAddComponentDialog(isComposition: Boolean): Option[Button] = {
    val dialog = new Dialog()
    dialog.title = if (isComposition) "Adding a composite operation" else "Adding a sequence of operations"
    dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

    val dialogGrid = new GridPane()
    dialogGrid.setVgap(20)
    dialogGrid.setPrefHeight(500)
    dialogGrid.add(new Label("Please select an operation to add from the list.\n" +
      "Then, add a constant for the operation and click button Add Operation"), 0, 0)

    val middlePane = new GridPane
    middlePane.setVgap(10)
    middlePane.setHgap(10)

    val nameLabel = new Label("Name:")
    val nameTextField = new TextField()

    val constantLabel = new Label("Constant:")
    val constantTextField = new TextField()

    val operationLabel = new Label("Operations:")
    val operationsListView = new ListView[String]()  {
      selectionModel().selectedItem.onChange {
        (_, _, newValue) => {
          constantTextField.disable = if (!fMapArith1.contains(newValue) && !fMapFilter.contains(newValue)) true else false
        }
      }
    }

    if (isComposition)
      operationsListView.items = ObservableBuffer(fMapArith1.keySet.toBuffer.sorted ++ fMapNoConst.keySet.toBuffer.sorted)
    else
      operationsListView.items = ObservableBuffer(fMapArith1.keySet.toBuffer.sorted ++ fMapNoConst.keySet.toBuffer.sorted ++
        fMapPixelDependent.keySet.toBuffer.sorted ++ fMapPositionDependent.keySet.toBuffer.sorted ++
        fMapFilter.keySet.toBuffer.sorted)

    operationsListView.setPrefHeight(170)

    val addButtonLabel = new Label("")
    val addButton = new Button("Add Operation")

    val descriptionLabel = new Label("Result:")
    val descriptionLabelValue = new Label("")
    descriptionLabelValue.setPrefSize(400, 100)
    descriptionLabelValue.wrapText = true

    middlePane.add(nameLabel, 0, 0)
    middlePane.add(nameTextField, 1, 0)
    middlePane.add(operationLabel, 0, 1)
    middlePane.add(operationsListView, 1, 1)
    middlePane.add(constantLabel, 0, 2)
    middlePane.add(constantTextField, 1, 2)
    middlePane.add(addButtonLabel, 0, 3)
    middlePane.add(addButton, 1, 3)
    middlePane.add(descriptionLabel, 0, 4)
    middlePane.add(descriptionLabelValue, 1, 4)

    dialogGrid.add(middlePane, 0, 1)

    dialog.dialogPane().content = dialogGrid

    val fList: ListBuffer[(String, Option[Double])] = ListBuffer[(String, Option[Double])]()

    addButton.onAction = _ => {
      if (nameTextField.text().isEmpty)
        showFailDialog("Saving error", "Please enter a name of new operation.")
      else if (!constantTextField.disabled.get() && constantTextField.text().isEmpty)
        showFailDialog("Adding error", "Please enter a valid constant for your operation.")
      else if (operationsListView.selectionModel().getSelectedItem == null)
        showFailDialog("Adding error", "Please select an operation to compose.")
      else {
        val newName: String = operationsListView.getSelectionModel.getSelectedItem.toString
        val newConst = if (!constantTextField.disabled.get()) Some(constantTextField.text()) else None
        if (!isConstValid(newName, newConst))
          showFailDialog("Wrong constant", "Please check entered constants for requested operation.")
        else {
          val newPair: (String, Option[Double]) = newConst match {
            case Some(con) => (newName, Some(con.toDouble))
            case _ => (newName, None)
          }
          fList += newPair

          val newDesc = if (isComposition)
            operationsListView.selectionModel().getSelectedItem.toString + "(" + (if (descriptionLabelValue.text.isEmpty.get())
              "pixel" else descriptionLabelValue.text().toString) + ", " + constantTextField.text().toString + ")"
          else
            (if (descriptionLabelValue.text.isEmpty.get()) "" else descriptionLabelValue.text().toString + ", ") +
              operationsListView.selectionModel().getSelectedItem.toString + "(" + "pixel, " + constantTextField.text().toString + ")"

          descriptionLabelValue.text = newDesc
        }
      }
    }

    val res = dialog.showAndWait()
    res match {
      case Some(ButtonType.OK) =>
        if (fList.nonEmpty) {
          val funcName = nameTextField.text()
          val b: Button = createButton(funcName, funcName.length * 15, basicOperationsPaneHeight)
          if (isComposition) {
            def composedOperation(pixelValue: Double): Double = repeatedComposeFromList(fList, fList.size - 1)(pixelValue)
            fMapNoConst.put(funcName, (composedOperation, b))
            compositeMap.put(funcName, fList)
            newAddListenersToNoConstButtons() // to all buttons, not so efficient
          } else {
            sequenceMap.put(funcName, (fList, b))
            newAddListenersToSequenceButtons()
          }
          Some(b)
        }
        else None
      case _ => None
    }
  }

  def repeatedComposeFromList(opList: ListBuffer[(String, Option[Double])], n: Int): Double => Double = {
    if (n == 0)
      opList.head._2 match {
        case Some(constDouble) => fMapArith1(opList.head._1)._1(constDouble)
        case _ => fMapNoConst(opList.head._1)._1
      }
    else
      opList(n)._2 match {
        case Some(constDouble) => newCompose(fMapArith1(opList(n)._1)._1(constDouble), repeatedComposeFromList(opList, n - 1))
        case _ => newCompose(fMapNoConst(opList(n)._1)._1, repeatedComposeFromList(opList, n - 1))
      }
  }

  def refreshViews() {
    picturesListView.setItems(openPictures.getPicturesFilenames)
    currentPicture match {
      case Some(p) =>
        imageView.setImage(p.writableImage)
        selectionsListView.setItems(p.getSelectionsAsString)
      case _ =>
        imageView.setImage(new Image(blankImagePath))
        selectionsListView.getItems.clear()
    }
  }

  def showFailDialog(title: String, info: String) {
    val confirmButton = new ButtonType("Confirm", ButtonBar.ButtonData.OKDone)
    val alert = new Alert(AlertType.Information, info, confirmButton)
    alert.setTitle(title)
    alert.setHeaderText("")
    alert.showAndWait()
  }

  def createButton(title: String, width: Int, height: Int): Button = {
    val button = new Button(title)
    button.setPrefSize(width, height)
    button
  }

  def createTextField(width: Int, height: Int): TextField = {
    val tf = new TextField
    tf.setPrefSize(width, height)
    tf
  }

  def createLabel(title: String, width: Int, height: Int): Label = {
    val label = new Label(title)
    label.setPrefSize(width, height)
    label.alignment = Pos.BaselineCenter
    label
  }

  // Listeners
  moveLayerUpButton.onAction = _ => {
    openPictures.movePictureLayerUp(currentPicture)
    refreshViews()
  }

  moveLayerDownButton.onAction = _ => {
    openPictures.movePictureLayerDown(currentPicture)
    refreshViews()
  }

  showCompositePictureButton.onAction = _ => {
    imageView.setImage(openPictures.makeCompositeImage())
  }

  modifyPictureButton.onAction = _ => {
    currentPicture match {
      case Some(p) =>
        if (alphaTextField.text() == null || alphaTextField.text().trim().isEmpty || !isDouble(alphaTextField.text()) ||
          alphaTextField.text().toDouble < 0 || alphaTextField.text().toDouble > 1)
          showFailDialog("Error in text field 'Alpha'", "Please fill text field 'Alpha' with a real number in range [0, 1].")
        else {
          p.isActive = isActivePictureCheckBox.selected.value
          p.transparency = alphaTextField.text().toDouble
        }
      case _ =>
    }
  }

  def addModifySelection(isAdd: Boolean) {
    if (nameTextField.text() == null || nameTextField.text().trim().isEmpty)
      showFailDialog("Error in text field 'Name'", "Please fill text field 'Name' with a string value.")
    else if (upperLeftTextField.text() == null || upperLeftTextField.text().trim().isEmpty || !canReadXY(upperLeftTextField.text()))
      showFailDialog("Error in text field 'Upper Left'", "Please fill text field 'Upper Left' in format (x, y).")
    else if (downRightTextField.text() == null || downRightTextField.text().trim().isEmpty || !canReadXY(downRightTextField.text()))
      showFailDialog("Error in text field 'Down Right'", "Please fill text field 'Down Right' in format (x, y).")
    else {
      currentPicture match {
        case Some(p) =>
          val sel = new Selection(nameTextField.text(), readXY(upperLeftTextField.text()),
            readXY(downRightTextField.text()), isActiveSelectionCheckBox.selected.value)
          if (sel.upperLeft._1 < 0 || sel.upperLeft._2 < 0 || sel.downRight._1 < 0 || sel.downRight._2 < 0 ||
            sel.upperLeft._1 >= sel.downRight._1 || sel.upperLeft._2 >= sel.downRight._2 || sel.upperLeft._1 >= 900 ||
            sel.upperLeft._2 >= 1600 || sel.downRight._1 >= 900 || sel.downRight._2 >= 1600)
            showFailDialog("Error in selection dimensions'", "Please check the input dimensions.")
          else if (sel.name.equals("Basic")) {
            showFailDialog("Basic selection " + (if (isAdd) "adding" else "modification"),
              "Basic selection cannot be " + (if (isAdd) "added" else "modified") + ".")
          } else {
            val res = if (isAdd) p.addSelection(sel) else p.modifySelection(sel)
            if (res) {
              refreshViews()
            } else {
              if (isAdd)
                showFailDialog("Selection not added", "Please check following conditions:\n" +
                  "1) Name of the selection is not 'Basic'.\n" +
                  "3) Name of the selection is not equal to any of the existing names.\n" +
                  "3) Selection does not overlap existing selections.\n")
              else
                showFailDialog("Selection not modified", "Please check following conditions:\n" +
                  "1) Name of the selection is not 'Basic'.\n" +
                  "2) Name of the selection exists in the list of selection.\n" +
                  "3) Selection does not overlap existing selections.\n")
            }
          }
        case _ =>
      }
    }
  }

  addSelectionButton.onAction = _ => {
    addModifySelection(true)
  }

  modifySelectionButton.onAction = _ => {
    addModifySelection(false)
  }

  removeSelectionButton.onAction = _ => {
    currentPicture match {
      case Some(p) =>
        val name: String = nameTextField.text()
        name match {
          case "Basic" => showFailDialog("Basic selection remove", "Basic selection cannot be removed.")
          case _ =>
            p.getSelectionByName(name) match {
              case Some(_) =>
                p.removeSelection(name)
                refreshViews()
              case _ =>
                showFailDialog("Selection not found", "No selection with given name.\nPlease check the text field 'Name'")
            }
        }
      case _ =>
    }
  }

  def clearSelectionOperationComponents() {
    nameTextField.clear()
    upperLeftTextField.clear()
    downRightTextField.clear()
    isActiveSelectionCheckBox.selected = false
  }

  def convertArgbToDoubles(argb: Int): (Double, Double, Double) = {
    val red: Double = ((argb >> 16) & 255).toDouble / 256
    val green: Double = ((argb >> 8) & 255).toDouble / 256
    val blue: Double = (argb & 255).toDouble / 256

    (red, green, blue)
  }

  def newAdd(constValue: Double)(pixelValue: Double): Double = constValue + pixelValue
  def newSub(constValue: Double)(pixelValue: Double): Double = pixelValue - constValue
  def newInvSub(constValue: Double)(pixelValue: Double): Double = constValue - pixelValue
  def newMul(constValue: Double)(pixelValue: Double): Double = constValue * pixelValue
  def newDiv(constValue: Double)(pixelValue: Double): Double = if (constValue != 0) pixelValue / constValue else pixelValue
  def newInvDiv(constValue: Double)(pixelValue: Double): Double = newDiv(constValue)(pixelValue)
  def newPow(constValue: Double)(pixelValue: Double): Double = Math.pow(pixelValue, constValue)
  def newMin(constValue: Double)(pixelValue: Double): Double = Math.min(pixelValue, constValue)
  def newMax(constValue: Double)(pixelValue: Double): Double = Math.max(pixelValue, constValue)
  def newAssign(constValue: Double)(pixelValue: Double): Double = constValue
  def newLog(pixelValue: Double): Double = Math.log(pixelValue)
  def newAbs(pixelValue: Double): Double = Math.abs(pixelValue)
  def newInv(pixelValue: Double): Double = 1 - pixelValue

  def newGreyscale(intPixelValue: Int)(pixelValue: Double): Double = {
    val doubleRgb = convertArgbToDoubles(intPixelValue)
    (doubleRgb._1 + doubleRgb._2 + doubleRgb._3) / 3
  }

  def newOldPhoto(i: Int, j: Int)(pixelValue: Double): Double = {
    val distFromCenter: Double = Math.sqrt(Math.pow(Math.abs(450 - j), 2) + Math.pow(Math.abs(800 - i), 2))
    val darkness: Double = distFromCenter / 917.toDouble
    pixelValue - pixelValue * darkness
  }

  def newPonder(pr: PixelReader, n: Int, x: Int, y: Int, upperLeft: (Int, Int), downRight: (Int, Int)): Int = {
    val startX = if (x - n < upperLeft._1) upperLeft._1 else x - n
    val startY = if (y - n < upperLeft._2) upperLeft._2 else y - n
    val endX = if (x + n > downRight._1) downRight._1 else x + n
    val endY = if (y + n < downRight._2) downRight._2 else y + n

    var sumOfPixels: Int = 0
    var numberOfPixels: Int = 0
    for (i <- startY to endY) {
      for (j <- startX to endX) {
        sumOfPixels += pr.getArgb(i, j)
        numberOfPixels += 1
      }
    }

    if (numberOfPixels != 0)
      sumOfPixels / numberOfPixels
    else
      0
  }

  def newMedian(pr: PixelReader, n: Int, x: Int, y: Int, upperLeft: (Int, Int), downRight: (Int, Int)): Int = {
    val startX = if (x - n < upperLeft._1) upperLeft._1 else x - n
    val startY = if (y - n < upperLeft._2) upperLeft._2 else y - n
    val endX = if (x + n > downRight._1) downRight._1 else x + n
    val endY = if (y + n < downRight._2) downRight._2 else y + n

    val medBuffer: ListBuffer[Int] = new ListBuffer[Int]()
    for (i <- startY to endY) {
      for (j <- startX to endX) {
        medBuffer += pr.getArgb(i, j)
      }
    }

    medBuffer.sortWith(_ > _)(medBuffer.size / 2)
  }

  def newCompose(f: Double => Double, g: Double => Double): Double => Double = pixelValue => f(g(pixelValue))

  val fMapArith1: mutable.HashMap[String, ((Double => Double => Double), Button, TextField)] = mutable.HashMap (
    ("add", (newAdd, addButton, addTextField)),
    ("sub", (newSub, subButton, subTextField)),
    ("invSub", (newInvSub, invSubButton, invSubTextField)),
    ("mul", (newMul, mulButton, mulTextField)),
    ("div", (newDiv, divButton, divTextField)),
    ("invDiv", (newInvDiv, invDivButton, invDivTextField)),
    ("pow", (newPow, powButton, powTextField)),
    ("min", (newMin, minButton, minTextField)),
    ("max", (newMax, maxButton, maxTextField))
  )

  val fMapNoConst: mutable.HashMap[String, (Double => Double, Button)] = mutable.HashMap(
    ("log", (newLog, logButton)),
    ("abs", (newAbs, absButton)),
    ("inv", (newInv, inversionButton))
  )

  val fMapPositionDependent: mutable.HashMap[String, ((Int, Int) => Double => Double, Button)] = mutable.HashMap {
    ("oldPhoto", (newOldPhoto, oldButton))
  }

  val fMapPixelDependent: mutable.HashMap[String, (Int => Double => Double, Button)] = mutable.HashMap {
    ("grey", (newGreyscale, greyscaleButton))
  }

  val fMapFilter: mutable.HashMap[String, (((PixelReader, Int, Int, Int, (Int, Int), (Int, Int)) => Int), Button, TextField)] = mutable.HashMap (
    ("ponder", (newPonder, filterPondButton, filterPondTextField)),
    ("median", (newMedian, filterMedianButton, filterMedianTextField))
  )

  val compositeMap: mutable.HashMap[String, ListBuffer[(String, Option[Double])]] = mutable.HashMap()

  val sequenceMap: mutable.LinkedHashMap[String, (ListBuffer[(String, Option[Double])], Button)] = mutable.LinkedHashMap()

  def operateRGBComponentsOfSinglePixel(func: Double => Double, pixelValue: Int): Int = {
    val rgbs = convertArgbToDoubles(pixelValue)
    val newRed = func(rgbs._1)
    val newGreen = func(rgbs._2)
    val newBlue = func(rgbs._3)
    (255 << 24) + (((newRed - newRed.toInt) * 256).toInt << 16) + (((newGreen - newGreen.toInt) * 256).toInt << 8) +
      ((newBlue - newBlue.toInt) * 256).toInt
  }

  def isConstValid(fName: String, userConst: Option[String]): Boolean = {
    userConst match {
      case Some(uc) =>
        fName match {
          case "add" | "sub" | "invSub" | "mul" | "div" | "invDiv" | "pow" | "min" | "max" =>
            if (!isDouble(uc))
              false
            else
            if (uc.toDouble >= 0 && uc.toDouble <= 5)
              true
            else
              false
          case "fill" =>
            try {
              val tokens = uc.split("\\s")
              val red = tokens(0).toDouble
              val green = tokens(1).toDouble
              val blue = tokens(2).toDouble
              if (red < 0 || green < 0 || blue < 0 || red > 1 || green > 1 || blue > 1)
                false
              else
                true
            } catch {
              case _: Throwable => false
            }
          case _ => true
        }
      case _ => true
    }
  }

  def newWholePictureOperation(opName: String, userConst: Option[String])(picture: Picture): Unit = {
    picture.writableImage.pixelReader match {
      case Some(pr) =>
        val pw = picture.writableImage.pixelWriter
        for (sel <- picture.selections) {
          if (sel.isActive) {
            if (isConstValid(opName, userConst)) {
              for (i <- sel.upperLeft._2 to sel.downRight._2) {
                for (j <- sel.upperLeft._1 to sel.downRight._1) {
                  val newPixelValue = userConst match {
                    case Some(uConst) =>
                      opName match {
                        case "add" | "sub" | "invSub" | "mul" | "div" | "invDiv" | "pow" | "min" | "max" =>
                          operateRGBComponentsOfSinglePixel(fMapArith1(opName)._1(uConst.toDouble), pr.getArgb(i, j))
                        case "median" | "ponder" => fMapFilter(opName)._1(pr, uConst.toInt, i, j, sel.upperLeft, sel.downRight)
                        case "fill" =>
                          val tokens = uConst.split("\\s")
                          (255 << 24) + ((tokens(0).toDouble * 256).toInt << 16) + ((tokens(1).toDouble * 256).toInt << 8) +
                            (tokens(2).toDouble * 256).toInt
                        case _ => pr.getArgb(i, j)
                      }
                    case None =>
                      opName match {
                        case "oldPhoto" =>
                          operateRGBComponentsOfSinglePixel(fMapPositionDependent(opName)._1(i, j), pr.getArgb(i, j))
                        case "grey" =>
                          operateRGBComponentsOfSinglePixel(fMapPixelDependent(opName)._1(pr.getArgb(i, j)), pr.getArgb(i, j))
                        case _ =>
                          if (fMapNoConst.contains(opName))
                            operateRGBComponentsOfSinglePixel(fMapNoConst(opName)._1, pr.getArgb(i, j))
                          else
                            pr.getArgb(i, j)
                      }
                  }
                pw.setArgb(i, j, newPixelValue)
                }
              }
            } else {
              showFailDialog("Wrong constant", "Please check entered constants for requested operation.")
            }
          }
        }
      case _ =>
    }
  }

  def newAddListenersToButtons() {
    for (fname <- fMapArith1.keySet) fMapArith1(fname)._2.onAction = _ => buttonOneConstFunctionArit(fname)
    for (fname <- fMapFilter.keySet) fMapFilter(fname)._2.onAction = _ => buttonOneConstFunctionFilter(fname)

    for (fname <- fMapNoConst.keySet) fMapNoConst(fname)._2.onAction = _ => buttonNoConstFunction(fname)
    for (fname <- fMapPositionDependent.keySet) fMapPositionDependent(fname)._2.onAction = _ => buttonNoConstFunction(fname)
    for (fname <- fMapPixelDependent.keySet) fMapPixelDependent(fname)._2.onAction = _ => buttonNoConstFunction(fname)

    rgbButton.onAction = _ => buttonOneConstFunctionFill()
}

  def buttonNoConstFunction(fname: String) {
    if (operationOnSinglePicture) {
      currentPicture match {
        case Some(p) => newWholePictureOperation(fname, None)(p)
        case _ =>
      }
    } else {
      for (picture <- openPictures.pictures) {
        if (picture.isActive)
          newWholePictureOperation(fname, None)(picture)
      }
    }
    refreshViews()
  }

  def buttonOneConstFunctionArit(fname: String) {
    if (operationOnSinglePicture) {
      currentPicture match {
        case Some(p) => newWholePictureOperation(fname, Some(fMapArith1(fname)._3.text().toString))(p)
        case _ =>
      }
    } else {
      for (picture <- openPictures.pictures) {
        if (picture.isActive)
          newWholePictureOperation(fname, Some(fMapArith1(fname)._3.text().toString))(picture)
      }
    }
    refreshViews()
  }

  def buttonOneConstFunctionFilter(fname: String) {
    if (operationOnSinglePicture) {
      currentPicture match {
        case Some(p) => newWholePictureOperation(fname, Some(fMapFilter(fname)._3.text().toString))(p)
        case _ =>
      }
    } else {
      for (picture <- openPictures.pictures) {
        if (picture.isActive)
          newWholePictureOperation(fname, Some(fMapFilter(fname)._3.text().toString))(picture)
      }
    }
    refreshViews()
  }

  def buttonOneConstFunctionFill() {
    if (operationOnSinglePicture) {
      currentPicture match {
        case Some(p) => newWholePictureOperation("fill", Some(rgbTextField.text().toString))(p)
        case _ =>
      }
    } else {
      for (picture <- openPictures.pictures) {
        if (picture.isActive)
          newWholePictureOperation("fill", Some(rgbTextField.text().toString))(picture)
      }
    }
    refreshViews()
  }

  /* Just util, to be called when adding composite functions */
  def newAddListenersToNoConstButtons() {
    for (fname <- fMapNoConst.keySet) fMapNoConst(fname)._2.onAction = _ => buttonNoConstFunction(fname)
  }

  def newAddListenersToSequenceButtons() {
    for (seqName <- sequenceMap.keySet) {
      sequenceMap(seqName)._2.onAction = _ => {
        for ((operName, operConst) <- sequenceMap(seqName)._1) {
          if (operationOnSinglePicture) {
            currentPicture match {
              case Some(p) => operConst match {
                case Some(con) => newWholePictureOperation(operName, Some(con.toString))(p)
                case _ => newWholePictureOperation(operName, None)(p)
              }
              case _ =>
            }
          } else {
            for (picture <- openPictures.pictures) {
              if (picture.isActive)
                operConst match {
                  case Some(con) => newWholePictureOperation(operName, Some(con.toString))(picture)
                  case _ => newWholePictureOperation(operName, None)(picture)
                }
            }
          }
          refreshViews()
        }
      }
    }
  }

  newAddListenersToButtons()

  stage = new PrimaryStage {
    maximized = true
    title = "Scala Image Processing Tool"
    scene = new Scene {
      val rootPane = new Group()
      val grid = new GridPane
      grid.setVgap(20)

      val operationsPane = new GridPane
      operationsPane.setVgap(10)

      val basicOperationsPane = new GridPane
      basicOperationsPane.setHgap(15)

      compositeOperationsPane.setHgap(5)
      compositeOperationsPane.setPrefHeight(basicOperationsPaneHeight)

      val imagePane = new GridPane
      val imageAndListsPane = new GridPane

      guiAddMenu()

      basicOperationsPane.setPrefHeight(basicOperationsPaneHeight)

      val fillPane = new GridPane
      fillPane.setHgap(5)

      val rgbPane = new GridPane
      rgbPane.add(rgbButton, 0, 0)
      rgbPane.add(rgbTextField, 0, 1)
      fillPane.add(rgbPane, 0, 0)

      basicOperationsPane.add(fillPane, 0, 0)

      val arithmeticPane = new GridPane
      arithmeticPane.setHgap(5)

      val addPane = new GridPane
      addPane.add(addButton, 0, 0)
      addPane.add(addTextField, 0, 1)
      arithmeticPane.add(addPane, 0, 0)

      val subPane = new GridPane
      subPane.add(subButton, 0, 0)
      subPane.add(subTextField, 0, 1)
      arithmeticPane.add(subPane, 1, 0)

      val invSubPane = new GridPane
      invSubPane.add(invSubButton, 0, 0)
      invSubPane.add(invSubTextField, 0, 1)
      arithmeticPane.add(invSubPane, 2, 0)

      val mulPane = new GridPane
      mulPane.add(mulButton, 0, 0)
      mulPane.add(mulTextField, 0, 1)
      arithmeticPane.add(mulPane, 3, 0)

      val divPane = new GridPane
      divPane.add(divButton, 0, 0)
      divPane.add(divTextField, 0, 1)
      arithmeticPane.add(divPane, 4, 0)

      val invDivPane = new GridPane
      invDivPane.add(invDivButton, 0, 0)
      invDivPane.add(invDivTextField, 0, 1)
      arithmeticPane.add(invDivPane, 5, 0)

      basicOperationsPane.add(arithmeticPane, 1, 0)

      val functionsPane = new GridPane
      functionsPane.setHgap(5)

      val powPane = new GridPane
      powPane.add(powButton, 0, 0)
      powPane.add(powTextField, 0, 1)
      functionsPane.add(powPane, 0, 0)

      val logPane = new GridPane
      logPane.add(logButton, 0, 0)
      //logPane.add(logTextField, 0, 1)
      functionsPane.add(logPane, 1, 0)

      val absPane = new GridPane
      absPane.add(absButton, 0, 0)
      //absPane.add(absTextField, 0, 1)
      functionsPane.add(absPane, 2, 0)

      val minPane = new GridPane
      minPane.add(minButton, 0, 0)
      minPane.add(minTextField, 0, 1)
      functionsPane.add(minPane, 3, 0)

      val maxPane = new GridPane
      maxPane.add(maxButton, 0, 0)
      maxPane.add(maxTextField, 0, 1)
      functionsPane.add(maxPane, 4, 0)

      basicOperationsPane.add(functionsPane, 2, 0)

      val predefOpPane = new GridPane
      predefOpPane.setHgap(5)

      predefOpPane.add(inversionButton, 0, 0)
      predefOpPane.add(greyscaleButton, 1, 0)
      predefOpPane.add(oldButton, 2, 0)

      basicOperationsPane.add(predefOpPane, 3, 0)

      val filterPane = new GridPane
      filterPane.setHgap(5)

      val filterMedianPane = new GridPane
      filterMedianPane.add(filterMedianButton, 0, 0)
      filterMedianPane.add(filterMedianTextField, 0, 1)
      filterPane.add(filterMedianPane, 0, 0)

      val filterPondPane = new GridPane
      filterPondPane.add(filterPondButton, 0, 0)
      filterPondPane.add(filterPondTextField, 0, 1)
      filterPane.add(filterPondPane, 1, 0)

      basicOperationsPane.add(filterPane, 4, 0)

      operationsPane.add(basicOperationsPane, 0, 0)
      operationsPane.add(compositeOperationsPane, 0, 1)

      grid.add(operationsPane, 0, 0)

      imagePane.setPrefSize(900, 100)
      imagePane.add(hBox, 0, 0)

      val listsPane = new GridPane
      listsPane.setVgap(20)

      val pictureListAndControlPane = new GridPane
      pictureListAndControlPane.setVgap(5)

      val pictureControlGridPane = new GridPane
      pictureControlGridPane.setHgap(5)
      pictureControlGridPane.add(moveLayerUpButton, 0, 0)
      pictureControlGridPane.add(showCompositePictureButton, 1, 0)
      pictureControlGridPane.add(moveLayerDownButton, 2, 0)
      pictureControlGridPane.alignment = Pos.BaselineCenter

      val pictureInfoGridPane = new GridPane
      pictureInfoGridPane.setHgap(5)
      pictureInfoGridPane.alignment = Pos.BaselineCenter

      val alphaPane = new GridPane
      alphaPane.add(alphaLabel, 0, 0)
      alphaPane.add(alphaTextField, 0, 1)
      pictureInfoGridPane.add(alphaPane, 0, 0)

      val isActivePicturePane = new GridPane
      isActivePicturePane.add(isActivePictureLabel, 0, 0)
      isActivePicturePane.add(isActivePictureCheckBox, 0, 1)
      pictureInfoGridPane.add(isActivePicturePane, 1, 0)

      pictureInfoGridPane.add(modifyPictureButton, 2, 0)
      modifyPictureButton.alignmentInParent = Pos.BottomCenter

      pictureListAndControlPane.add(pictureControlGridPane, 0, 0)
      pictureListAndControlPane.add(pictureInfoGridPane, 0, 1)
      pictureListAndControlPane.add(picturesListView, 0, 2)

      listsPane.add(pictureListAndControlPane, 0, 0)

      val selectionsListAndControlPane = new GridPane
      selectionsListAndControlPane.setVgap(5)

      val selectionsControlPane = new GridPane
      selectionsControlPane.alignment = Pos.BaselineCenter
      selectionsControlPane.setVgap(5)

      val selectionsControlNewPane = new GridPane
      selectionsControlNewPane.setHgap(5)
      selectionsControlNewPane.alignment = Pos.BaselineCenter

      val namePane = new GridPane
      namePane.add(nameLabel, 0, 0)
      namePane.add(nameTextField, 0, 1)
      selectionsControlNewPane.add(namePane, 0, 0)

      val upperLeftPane = new GridPane
      upperLeftPane.add(upperLeftLabel, 0, 0)
      upperLeftPane.add(upperLeftTextField, 0, 1)
      selectionsControlNewPane.add(upperLeftPane, 1, 0)

      val downRightPane = new GridPane
      downRightPane.add(downRightLabel, 0, 0)
      downRightPane.add(downRightTextField, 0, 1)
      selectionsControlNewPane.add(downRightPane, 2, 0)

      val isActivePane = new GridPane
      isActivePane.add(isActiveSelectionLabel, 0, 0)
      isActivePane.add(isActiveSelectionCheckBox, 0, 1)
      selectionsControlNewPane.add(isActivePane, 3, 0)

      selectionsControlPane.add(selectionsControlNewPane, 0, 0)

      val selectionsControlFillPane = new GridPane
      selectionsControlFillPane.alignment = Pos.BaselineCenter
      selectionsControlFillPane.setHgap(5)

      selectionsControlPane.add(selectionsControlFillPane, 0, 1)

      val selectionsControlButtonsPane = new GridPane
      selectionsControlButtonsPane.alignment = Pos.BaselineCenter
      selectionsControlButtonsPane.setHgap(5)

      selectionsControlButtonsPane.add(addSelectionButton, 0, 0)
      selectionsControlButtonsPane.add(modifySelectionButton, 1, 0)
      selectionsControlButtonsPane.add(removeSelectionButton, 2, 0)

      selectionsControlPane.add(selectionsControlButtonsPane, 0, 2)

      selectionsListAndControlPane.add(selectionsControlPane, 0, 0)
      selectionsListAndControlPane.add(selectionsListView, 0, 1)

      listsPane.add(selectionsListAndControlPane, 0, 1)

      imageAndListsPane.add(imagePane, 0, 0)
      imageAndListsPane.add(listsPane, 1, 0)
      imageAndListsPane.setHgap(20)

      grid.setPadding(Insets(40, rightPadding, 0, leftPadding))
      grid.add(imageAndListsPane, 0, 1)

      rootPane.getChildren.add(grid)
      rootPane.getChildren.add(menuBar)

      root = rootPane

      menuBar.prefWidthProperty().setValue(10000)
    }
  }
}