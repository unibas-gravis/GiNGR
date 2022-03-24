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

import api.gpmm.GPMMTriangleMesh3D
import apps.DemoDatasetLoader
import scalismo.ui.api.ScalismoUI

object VisualizeGPMMCorrelation extends App {
  scalismo.initialize()

  val (model, lms) = DemoDatasetLoader.armadillo.modelGauss(Some(10000))

  // Choose landmark to compute correlation
  val refLm = lms.get.find(f => f.id == "J").get
  val refId = model.reference.pointSet.findClosestPoint(refLm.point).id

  val gpmmHelp = GPMMTriangleMesh3D(model.reference, relativeTolerance = 0.0)
  val color = gpmmHelp.computeDistanceAbsMesh(model, refId)

  val ui = ScalismoUI()
  ui.show(color, "Correlation")
  ui.show(refLm, "landmarks")

}
