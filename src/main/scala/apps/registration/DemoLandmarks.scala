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
