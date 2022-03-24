package apps.gpmm

import apps.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

object CreateBunnyGPMM extends App {
  scalismo.initialize()

  // Reduce decimation points for lower-appromation and faster computation
  val (model, _) = DemoDatasetLoader.bunny.modelGauss()

  val ui = ScalismoUI()
  ui.show(model, "model")
}
