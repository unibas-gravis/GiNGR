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

package gingr.api.registration.config

import api.registration.utils.NonRigidClosestPointRegistrator.ClosestPointTriangleMesh3D
import breeze.linalg.{DenseMatrix, DenseVector}
import gingr.api.{CorrespondencePairs, GeneralRegistrationState, GingrAlgorithm, GingrConfig, GingrRegistrationState}
import scalismo.common.PointId
import scalismo.statisticalmodel.MultivariateNormalDistribution

object ICPCorrespondence {
  def estimate[T](state: IcpRegistrationState): CorrespondencePairs = {
    val source = state.general.fit
    val target = state.general.target
    val corr =
      if (state.config.reverseCorrespondenceDirection)
        ClosestPointTriangleMesh3D.closestPointCorrespondenceTargetToTemplate(source, target)
      else
        ClosestPointTriangleMesh3D.closestPointCorrespondence(source, target)
    CorrespondencePairs(pairs = corr._1.filter(_._3 == 1.0).map(f => (f._1, f._2)).toIndexedSeq)
  }
}

case class IcpConfiguration(
    override val maxIterations: Int = 100,
    override val threshold: Double = 1e-10,
    override val converged: (GeneralRegistrationState, GeneralRegistrationState, Double) => Boolean =
      (last: GeneralRegistrationState, current: GeneralRegistrationState, threshold: Double) => false,
    override val useLandmarkCorrespondence: Boolean = true,
    initialSigma: Double = 100.0,
    endSigma: Double = 1.0,
    reverseCorrespondenceDirection: Boolean = false
) extends GingrConfig {
  val sigmaStep: Double = (initialSigma - endSigma) / maxIterations.toDouble
}

case class IcpRegistrationState(general: GeneralRegistrationState, config: IcpConfiguration)
    extends GingrRegistrationState[IcpRegistrationState] {
  override def updateGeneral(update: GeneralRegistrationState): IcpRegistrationState = this.copy(general = update)
}

object IcpRegistrationState {
  def apply(general: GeneralRegistrationState, config: IcpConfiguration): IcpRegistrationState = {
    val newGeneral = general.copy(
//      maxIterations = config.maxIterations,
      sigma2 = config.initialSigma
//      converged = false
    )
    new IcpRegistrationState(
      newGeneral,
      config
    )
  }
}

class IcpRegistration(
    override val getCorrespondence: IcpRegistrationState => CorrespondencePairs = (state: IcpRegistrationState) =>
      ICPCorrespondence.estimate(state),
    override val getUncertainty: (PointId, IcpRegistrationState) => MultivariateNormalDistribution =
      (id: PointId, state: IcpRegistrationState) =>
        MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3) * state.general.sigma2)
) extends GingrAlgorithm[IcpRegistrationState] {
  def name = "ICP"
  // possibility to override the update function, or just use the base class method?
  override def updateSigma2(current: IcpRegistrationState): Double = {
    val newSigma = current.general.sigma2 - current.config.sigmaStep
    newSigma
  }
}
