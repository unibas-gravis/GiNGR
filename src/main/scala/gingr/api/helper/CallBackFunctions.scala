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

package gingr.api.helper

import gingr.api.GingrRegistrationState
import gingr.api.sampling.loggers.JSONStateLogger
import scalismo.sampling.loggers.ChainStateLogger
import scalismo.ui.api.PointDistributionModelViewControlsTriangleMesh3D

object CallBackFunctions {

  case class visualLogger[State <: GingrRegistrationState[State]](
      jsonLogger: Option[JSONStateLogger[State]],
      modelView: PointDistributionModelViewControlsTriangleMesh3D,
      modelUpdateFrequency: Int = 10,
      printUpdateFrequency: Int = 100
  ) extends ChainStateLogger[State] {
    var counter = 0
    override def logState(sample: State): Unit = {
      counter += 1
      if (counter % modelUpdateFrequency == 0 && counter > 1) {
        println(s"Iteration: ${counter}/${sample.config.maxIterations()} - Sigma: ${sample.general.sigma2}")
        modelView.shapeModelTransformationView.poseTransformationView.transformation =
          sample.general.modelParameters.rigidTransform
        modelView.shapeModelTransformationView.shapeTransformationView.coefficients =
          sample.general.modelParameters.shape.parameters
      }
      if (counter % printUpdateFrequency == 0 && counter > 1) {
        jsonLogger.foreach(_.printAcceptInfo())
        jsonLogger.foreach { log => log.filePath.foreach(_ => log.writeLog()) }
        RegistrationComparison.evaluateReconstruction2GroundTruthBoundaryAware(
          id = "",
          reconstruction = sample.general.fit,
          groundTruth = sample.general.target
        )
      }
    }
  }
}
