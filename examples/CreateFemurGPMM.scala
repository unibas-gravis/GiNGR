import DemoHelper.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

@main def CreateFemurGPMM() =
    // Reduce decimation points for lower-appromation and faster computation
    val (model, _) = DemoDatasetLoader.femur.modelGauss()
    val ui = ScalismoUI()
    ui.show(model, "model")