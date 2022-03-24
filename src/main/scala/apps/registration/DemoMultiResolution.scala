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

import api.GeneralRegistrationState
import api.registration.config._
import apps.DemoDatasetLoader
import scalismo.geometry.{_3D, EuclideanVector, Point}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.{Rotation, Translation, TranslationAfterRotation}
import scalismo.utils.Random.implicits.randomGenerator

object DemoMultiResolution extends App {
  scalismo.initialize()

  def fit(model: PointDistributionModel[_3D, TriangleMesh], target: TriangleMesh[_3D], initCPD: Int, mediumCPD: Int, finalICP: Int): GeneralRegistrationState = {
    val configCPD = CpdConfiguration(maxIterations = 50, threshold = 1e-10, lambda = 1.0)
    val algorithmCPD = new CpdRegistration()

    val simpleRegistrationCPD = new SimpleRegistrator[CpdRegistrationState, CpdRegistration, CpdConfiguration](
      model,
      target,
      algorithm = algorithmCPD,
      config = configCPD
    )

    val coarseFit = simpleRegistrationCPD.runDecimated(modelPoints = initCPD, targetPoints = initCPD)

    val mediumFit = simpleRegistrationCPD.runDecimated(modelPoints = mediumCPD, targetPoints = mediumCPD, state = Some(coarseFit))

    val configICP = IcpConfiguration(maxIterations = 100, threshold = 1e-10, initialSigma = mediumFit.general.sigma2, endSigma = mediumFit.general.sigma2 / 10.0)
    val algorithmICP = new IcpRegistration()

    val simpleRegistrationICP = new SimpleRegistrator[IcpRegistrationState, IcpRegistration, IcpConfiguration](
      model,
      target,
      algorithm = algorithmICP,
      config = configICP
    )

    val initICPstate = IcpRegistrationState.apply(mediumFit.general, configICP)
    val detailedFit = simpleRegistrationICP.runDecimated(modelPoints = finalICP, targetPoints = finalICP, state = Some(initICPstate))
    detailedFit.general
  }

  val rigidOffset = TranslationAfterRotation(Translation(EuclideanVector(50, 50, 50)), Rotation(0.1, 0.1, 0.1, Point(0, 0, 0)))
  val (model, _) = DemoDatasetLoader.bunny.modelGauss()
  val (target, _) = DemoDatasetLoader.bunny.target(offset = rigidOffset)
  fit(model, target, 100, 500, 1000)
}
