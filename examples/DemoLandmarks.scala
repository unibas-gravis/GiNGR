//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

import gingr.simple.DemoDatasetLoader

@main def main() =
  val (model, modelLandmarks) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))
  val (target, targetLandmarks) = DemoDatasetLoader.armadillo.target()

  // Run deterministic CPD without landmarks
  val configNormal = new DemoConfigurations(model, target, discretization = 100, maxIterations = 10)
  configNormal.CPD()

  // Run deterministic CPD with landmarks
  val configLandmarks = new DemoConfigurations(model, target, modelLandmarks = modelLandmarks, targetLandmarks = targetLandmarks, discretization = 100, maxIterations = 10)
  configLandmarks.CPD()