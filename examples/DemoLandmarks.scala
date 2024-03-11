import DemoHelper.DemoDatasetLoader
import DemoHelper.CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.CpdConfiguration
import gingr.api.registration.config.CpdRegistrationState
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random.implicits._

@main def DemoLandmarks() =
  val (model, modelLandmarks) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))
  val (target, targetLandmarks) = DemoDatasetLoader.armadillo.target()
  // Visualize the data and setup UI callback
  val ui = ScalismoUI("CPD")
  ui.show(target, "target")
  val modelView = ui.show(ui.createGroup("model"), model, "model")
  val logger = visualLogger[CpdRegistrationState](modelView = modelView)

  val configDet = CpdConfiguration(maxIterations = 30)
  // Run deterministic CPD without landmarks
  val gingrInterface = GingrInterface(model, target, evaluatorUncertainty = 2.0)
  val cpd = gingrInterface.CPD(configDet)
  val result = cpd.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger))
  result.general.printStatus()
  ui.show(result.general.fit, "NoLMs")

  // Run deterministic CPD with landmarks
  logger.reset()
  val gingrInterfaceLM = GingrInterface(model, target, modelLandmarks = modelLandmarks, targetLandmarks = targetLandmarks, evaluatorUncertainty = 2.0)
  val cpdLM = gingrInterfaceLM.CPD(configDet)
  val resultLM = cpdLM.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger))
  resultLM.general.printStatus()
  ui.show(resultLM.general.fit, "WithLMs")