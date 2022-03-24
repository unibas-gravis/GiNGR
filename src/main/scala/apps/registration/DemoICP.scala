package apps.registration

import api.NoTransforms
import apps.DemoDatasetLoader
import scalismo.geometry.{EuclideanVector, Point}
import scalismo.transformations.{Rotation, Translation, TranslationAfterRotation}
import scalismo.utils.Random.implicits.randomGenerator

import java.io.File

object DemoICP extends App {
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
    jsonFile = Some(new File(DemoDatasetLoader.dataPath, "femur/targetFittingICP.json")))
  configProbabilistic.ICP()
}
