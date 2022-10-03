//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
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

  // Run deterministic CPD
  val configDet = CpdConfiguration(maxIterations = 100, lambda = 1.0)
  val gingrInterface = GingrInterface(model, target, globalTransformationType = NoTransforms, initialModelTransform = None, evaluatorUncertainty = 2.0)
  val cpd = gingrInterface.CPD(configDet)
  val bestDet = cpd.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger))
  ui.show(bestDet.general.fit, "bestDet")

  // Run probabilistic CPD
  logger.reset
  val configPro = CpdConfiguration(maxIterations = 1000, lambda = 1.0)
  val cpdPro = gingrInterface.CPD(configPro)
  val bestPro = cpdPro.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger), probabilistic = true)
  ui.show(bestPro.general.fit, "fitPro")