package apps.gpmm

import apps.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

object CreateFemurGPMM extends App {
  scalismo.initialize()

  // Reduce decimation points for lower-appromation and faster computation
  val (model, _) = DemoDatasetLoader.femur.modelGauss()

  val ui = ScalismoUI()
  ui.show(model, "model")
}
