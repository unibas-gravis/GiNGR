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

import api.NoTransforms
import apps.DemoDatasetLoader

import java.io.File

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
