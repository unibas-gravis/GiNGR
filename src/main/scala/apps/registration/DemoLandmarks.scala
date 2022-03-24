package apps.registration

import api.RigidTransforms
import apps.DemoDatasetLoader

object DemoLandmarks extends App {
  scalismo.initialize()
  val (model, modelLandmarks) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))
  val (target, targetLandmarks) = DemoDatasetLoader.armadillo.target()

  // Run deterministic CPD without landmarks
  val configNormal = new DemoConfigurations(model, target, discretization = 100, maxIterations = 10)
  configNormal.CPD()

  // Run deterministic CPD with landmarks
  val configLandmarks = new DemoConfigurations(model, target, modelLandmarks = modelLandmarks, targetLandmarks = targetLandmarks, discretization = 100, maxIterations = 10)
  configLandmarks.CPD()
}
