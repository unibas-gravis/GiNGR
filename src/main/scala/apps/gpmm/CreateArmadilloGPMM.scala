/*
 * Copyright 2022 University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package apps.gpmm

import apps.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

object CreateArmadilloGPMM extends App {
  scalismo.initialize()

  // Reduce decimation points for lower-appromation and faster computation
  val (model, _) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))
//  val (model, _) = DemoDatasetLoader.armadillo.modelInvLap(Some(1000), scaling = 30, fullResolutionReturn = true)
//  val (model, _) = DemoDatasetLoader.armadillo.modelInvLapDot(Some(1000), scaling = 0.04, fullResolutionReturn = true)

  val modelTruncated = model.truncate(100)
  val ui = ScalismoUI()
  ui.show(modelTruncated, "model")
}
