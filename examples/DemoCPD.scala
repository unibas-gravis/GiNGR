//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

import gingr.api.NoTransforms
import gingr.simple.DemoDatasetLoader
import java.io.File

@main def main() =
  val (model, _) = DemoDatasetLoader.femur.modelGauss()
  val (target, _) = DemoDatasetLoader.femur.target()

  // Run deterministic CPD
  val configDeterministic = new DemoConfigurations(model, target, discretization = 100, maxIterations = 100, probabilistic = false, transform = NoTransforms)
  configDeterministic.CPD()

  // Run probabilistic CPD
  val configProbabilistic = new DemoConfigurations(
    model,
    target,
    discretization = 100,
    maxIterations = 1000,
    probabilistic = true,
    transform = NoTransforms,
    jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingCPD.json"))
  )
  configProbabilistic.CPD()
