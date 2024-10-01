import DemoHelper.DemoDatasetLoader
import DemoHelper.CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.CpdConfiguration
import gingr.api.registration.config.CpdRegistrationState
import gingr.api.{NoTransforms, RigidTransforms}
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random.implicits._

@main def DemoCPD() =
  val (model, _) = DemoDatasetLoader.femur.modelGauss()
  val (target, _) = DemoDatasetLoader.femur.target()
  // Visualize the data and setup UI callback
  val ui = ScalismoUI("CPD")
  ui.show(target, "target")
  val modelView = ui.show(ui.createGroup("model"), model, "model")
  val logger = visualLogger[CpdRegistrationState](modelView = modelView)

  val gingrInterface = GingrInterface(model, target, evaluatorUncertainty = 5.0)
  // Run deterministic CPD
  val configDet = CpdConfiguration(maxIterations = 100, initialSigma = Option(1))
  val cpd = gingrInterface.CPD(configDet)
  val bestDet = cpd.runDecimated(modelPoints = 100, targetPoints = 100, globalTransformation = NoTransforms, callback = Option(logger))
  bestDet.general.printStatus()
  ui.show(bestDet.general.fit, "bestDet")

  // Run probabilistic CPD
  logger.reset()
  val configPro = CpdConfiguration(maxIterations = 1000, initialSigma = Option(1))
  val cpdPro = gingrInterface.CPD(configPro)
  val bestPro = cpdPro.runDecimated(modelPoints = 100, targetPoints = 100, globalTransformation = NoTransforms, callback = Option(logger), probabilistic = true)
  bestPro.general.printStatus()
  ui.show(bestPro.general.fit, "fitPro")