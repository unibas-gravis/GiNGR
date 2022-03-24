package apps.registration

import java.io.File

import api.{NoTransforms, RigidTransforms}
import apps.DemoDatasetLoader
import scalismo.geometry.{EuclideanVector, Point}
import scalismo.transformations.{Rotation, Translation, TranslationAfterRotation}

object DemoCPD extends App {
  scalismo.initialize()

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
    jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingCPD.json")))
  configProbabilistic.CPD()
}
