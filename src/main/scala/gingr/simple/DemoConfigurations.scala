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

package gingr.simple

import gingr.api.registration.config.*
import gingr.api.{GeneralRegistrationState, GlobalTranformationType, NoTransforms, RigidTransforms}
import scalismo.geometry.{Landmark, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.utils.Random.implicits.randomGenerator

import java.io.File

class DemoConfigurations(
    model: PointDistributionModel[_3D, TriangleMesh],
    target: TriangleMesh[_3D],
    modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
    targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
    discretization: Int = 100,
    maxIterations: Int = 100,
    probabilistic: Boolean = false,
    transform: GlobalTranformationType = RigidTransforms,
    jsonFile: Option[File] = None
) {

  def CPD(
      initSigma: Option[Double] = None,
      threshold: Double = 1e-10,
      lambda: Double = 1.0
  ): GeneralRegistrationState = {
    val configCPD =
      CpdConfiguration(maxIterations = maxIterations, threshold = threshold, lambda = lambda, initialSigma = initSigma)
    val algorithmCPD = new CpdRegistration()
    val uiName       = if (probabilistic) "CPD-Probabilistic" else "CPD-Deterministic"
    val simpleRegistration = new SimpleRegistrator[CpdRegistrationState, CpdRegistration, CpdConfiguration](
      model = model,
      target = target,
      modelLandmarks = modelLandmarks,
      targetLandmarks = targetLandmarks,
      algorithm = algorithmCPD,
      config = configCPD,
      evaluatorUncertainty = 2.0,
      transform = transform,
      jsonFile = jsonFile,
      name = uiName
    )
    simpleRegistration
      .runDecimated(
        modelPoints = discretization,
        targetPoints = discretization,
        probabilistic = probabilistic,
        randomMixture = 0.50
      )
      .general
  }

  def ICP(
      initSigma: Double = 1.0,
      endSigma: Double = 1.0,
      reverseCorrespondenceDirection: Boolean = false
  ): GeneralRegistrationState = {
    val configICP = IcpConfiguration(
      maxIterations = maxIterations,
      initialSigma = initSigma,
      endSigma = endSigma,
      reverseCorrespondenceDirection = reverseCorrespondenceDirection
    )
    val algorithmICP = new IcpRegistration()
    val uiName       = if (probabilistic) "ICP-Probabilistic" else "ICP-Deterministic"
    val simpleRegistration = new SimpleRegistrator[IcpRegistrationState, IcpRegistration, IcpConfiguration](
      model,
      target,
      algorithm = algorithmICP,
      config = configICP,
      evaluatorUncertainty = 2.0,
      transform = NoTransforms,
      jsonFile = jsonFile,
      name = uiName
    )

    simpleRegistration
      .runDecimated(
        modelPoints = discretization,
        targetPoints = discretization,
        probabilistic = probabilistic,
        randomMixture = 0.50
      )
      .general
  }
}
