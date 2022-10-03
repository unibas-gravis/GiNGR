//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
import DemoHelper.DemoDatasetLoader
import DemoHelper.CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.{CpdConfiguration, IcpConfiguration}
import gingr.api.registration.config.{CpdRegistrationState, IcpRegistrationState}
import gingr.api.{NoTransforms, RigidTransforms}
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random.implicits._
import scalismo.transformations.TranslationAfterRotation
import scalismo.transformations.Translation
import scalismo.geometry.EuclideanVector
import scalismo.transformations.Rotation
import scalismo.geometry.Point
import scalismo.utils.Random.implicits._
@main def DemoMultiResolution() =
  val rigidOffset = TranslationAfterRotation(Translation(EuclideanVector(50, 50, 50)), Rotation(0.1, 0.1, 0.1, Point(0, 0, 0)))

  val (model, _) = DemoDatasetLoader.bunny.modelGauss()
  val (target, _) = DemoDatasetLoader.bunny.target(offset = rigidOffset)
  // Visualize the data and setup UI callback
  val ui = ScalismoUI("MultiResolution")
  ui.show(target, "target")
  ui.show(model.mean, "initial Model")
  val modelView = ui.show(ui.createGroup("model"), model, "model")
  val loggerCPD = visualLogger[CpdRegistrationState](modelView = modelView)
  val loggerICP = visualLogger[IcpRegistrationState](modelView = modelView)

  // Run deterministic fitting
  val gingrInterface = GingrInterface(model, target)

  val configCPD1 = CpdConfiguration(maxIterations = 50)
  val cpd1 = gingrInterface.CPD(configCPD1)

  val fitCoarse = cpd1.runDecimated(modelPoints = 100, targetPoints = 100, globalTransformation = RigidTransforms, callback = Option(loggerCPD))
  fitCoarse.general.printStatus()
  ui.show(fitCoarse.general.fit, "Coarse")

  loggerCPD.reset()
  val configCPD2 = CpdConfiguration(maxIterations = 50, initialSigma = Option(fitCoarse.general.sigma2))
  val cpd2 = gingrInterface.CPD(configCPD2) 
  val fitMedium = cpd2.runDecimated(modelPoints = 500, targetPoints = 500, generalState = Option(fitCoarse.general), globalTransformation = RigidTransforms, callback = Option(loggerCPD))
  fitMedium.general.printStatus()
  ui.show(fitMedium.general.fit, "Medium")
  
  val configICP = IcpConfiguration(maxIterations = 100, initialSigma = 2.0, endSigma = 0.01)
  val icp = gingrInterface.ICP(configICP)
  val fitFine = icp.runDecimated(modelPoints = 1000, targetPoints = 1000, generalState = Option(fitMedium.general), globalTransformation = NoTransforms, callback = Option(loggerICP))
  fitFine.general.printStatus()
  ui.show(fitFine.general.fit, "Fine")