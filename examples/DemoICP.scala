//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
import DemoDatasetLoader.*
import CallBackFunctions.{visualLogger}
import gingr.simple.GingrInterface
import gingr.api.registration.config.IcpConfiguration
import gingr.api.registration.config.IcpRegistrationState
import gingr.api.NoTransforms
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

  // Run deterministic CPD
  val gingrInterface = GingrInterface(model, target, initialModelTransform = None)
  val icpDeterministicConfig = IcpConfiguration(maxIterations = 100)
  val icp = gingrInterface.ICP(icpDeterministicConfig)
  icp.runDecimated(modelPoints = 100, targetPoints = 100, callback = Option(logger))

//   // Run probabilistic IPC
//   val configProbabilistic = new DemoConfigurations(
//     model,
//     target,
//     discretization = 200,
//     maxIterations = 1000,
//     probabilistic = true,
//     transform = NoTransforms,
//     jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingICP.json"))
//   )
//   configProbabilistic.ICP()