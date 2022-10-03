//> using scala "3"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
import DemoHelper.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

@main def CreateBunnyGPMM() =
    // Reduce decimation points for lower-appromation and faster computation
    val (model, _) = DemoDatasetLoader.bunny.modelGauss()

    val ui = ScalismoUI()
    ui.show(model, "model")