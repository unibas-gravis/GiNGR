//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

import api.NoTransforms
import apps.DemoDatasetLoader
import java.io.File

@main def hello() =
  scalismo.initialize()

  val (model, _) = DemoDatasetLoader.femur.modelGauss()
  val (target, _) = DemoDatasetLoader.femur.target()

  // Run deterministic ICP
  val configDeterministic = new DemoConfigurations(model, target, discretization = 200, maxIterations = 100, probabilistic = false, transform = NoTransforms)
  configDeterministic.ICP()

  // Run probabilistic IPC
  val configProbabilistic = new DemoConfigurations(
    model,
    target,
    discretization = 200,
    maxIterations = 1000,
    probabilistic = true,
    transform = NoTransforms,
    jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingICP.json"))
  )
  configProbabilistic.ICP()