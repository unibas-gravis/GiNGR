//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

import gingr.simple.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

@main def main() =
  // Reduce decimation points for lower-appromation and faster computation
  val (model, _) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))
//  val (model, _) = DemoDatasetLoader.armadillo.modelInvLap(Some(1000), scaling = 30, fullResolutionReturn = true)
//  val (model, _) = DemoDatasetLoader.armadillo.modelInvLapDot(Some(1000), scaling = 0.04, fullResolutionReturn = true)

  val modelTruncated = model.truncate(100)
  val ui             = ScalismoUI()
  ui.show(modelTruncated, "model")