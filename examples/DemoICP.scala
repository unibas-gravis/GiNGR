import DemoHelper.DemoDatasetLoader
import DemoHelper.CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.IcpConfiguration
import gingr.api.registration.config.IcpRegistrationState
import gingr.api.{NoTransforms, RigidTransforms}
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random.implicits._
import java.io.File

@main def DemoICP() =
  val (model, _) = DemoDatasetLoader.femur.modelGauss()
  val (target, _) = DemoDatasetLoader.femur.target()
  // Visualize the data and setup UI callback
  val ui = ScalismoUI("ICP")
  ui.show(target, "target")
  val modelView = ui.show(ui.createGroup("model"), model, "model")
  val logger = visualLogger[IcpRegistrationState](modelView = modelView)

  val gingrInterface = GingrInterface(model, target, evaluatorUncertainty = 5.0, logFileFittingParameters = Option(new File("../data/femur/targetFittingICP.json")))
  // Run deterministic ICP
  val configDet = IcpConfiguration(maxIterations = 100, initialSigma = 1.0, endSigma = 1.0)
  val icpDet = gingrInterface.ICP(configDet)
  val bestDet = icpDet.runDecimated(modelPoints = 100, targetPoints = 100, globalTransformation = NoTransforms, callback = Option(logger))
  bestDet.general.printStatus()
  ui.show(bestDet.general.fit, "bestDet")

  // Run probabilistic IPC
  logger.reset()
  val configPro = IcpConfiguration(maxIterations = 1000, initialSigma = 1.0, endSigma = 1.0)
  val icpPro = gingrInterface.ICP(configPro)
  val bestPro = icpPro.runDecimated(modelPoints = 100, targetPoints = 100, globalTransformation = NoTransforms, callback = Option(logger), probabilistic = true)
  bestPro.general.printStatus()
  ui.show(bestPro.general.fit, "fitPro")