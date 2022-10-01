//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
import DemoDatasetLoader.*
import CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.CpdConfiguration
import gingr.api.registration.config.CpdRegistrationState
import gingr.api.NoTransforms
import scalismo.ui.api.ScalismoUI
import scalismo.utils.Random.implicits._
import java.io.File

@main def DemoCPD() =
  val (model, _) = DemoDatasetLoader.femur.modelGauss()
  val (target, _) = DemoDatasetLoader.femur.target()
  // Visualize the data and setup UI callback
  val ui = ScalismoUI("CPD")
  ui.show(target, "target")
  val modelView = ui.show(ui.createGroup("model"), model, "model")
  val logger = visualLogger[CpdRegistrationState](modelView = modelView)

  // Run deterministic CPD
  val gingrInterface = GingrInterface(model, target, initialModelTransform = None)
  val cpdDeterministicConfig = CpdConfiguration(maxIterations = 100)
  val cpd = gingrInterface.CPD(cpdDeterministicConfig)
  cpd.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger))

  // Run probabilistic CPD
//   val configProbabilistic = new DemoConfigurations(
//     model,
//     target,
//     discretization = 100,
//     maxIterations = 1000,
//     probabilistic = true,
//     transform = NoTransforms,
//     jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingCPD.json"))
//   )
//   configProbabilistic.CPD()
