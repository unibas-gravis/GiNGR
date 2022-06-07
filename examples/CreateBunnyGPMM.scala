//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

import apps.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

@main def hello() =
 scalismo.initialize()

 // Reduce decimation points for lower-appromation and faster computation
 val (model, _) = DemoDatasetLoader.bunny.modelGauss()

 val ui = ScalismoUI()
 ui.show(model, "model")