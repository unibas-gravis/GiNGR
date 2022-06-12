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

import gingr.api.registration.utils.NonRigidClosestPointRegistrator.{
  ClosestPointTriangleMesh3D,
  ClosestPointAlongNormalTriangleMesh3D,
  ClosestPointTriangleMesh3DSimple
}
import breeze.linalg.{DenseMatrix, DenseVector}
import gingr.api.{CorrespondencePairs, GeneralRegistrationState, GingrAlgorithm, GingrConfig, GingrRegistrationState}
import scalismo.common.PointId
import scalismo.statisticalmodel.MultivariateNormalDistribution

sealed trait ICPCorrespondenceMethod

case object TriangularClosestPoint  extends ICPCorrespondenceMethod
case object AlongNormalClosestPoint extends ICPCorrespondenceMethod
case object PointcloudClosestPoint  extends ICPCorrespondenceMethod

object ICPCorrespondence {
  def estimate[T](state: IcpRegistrationState): CorrespondencePairs = {
    val source = state.general.fit
    val target = state.general.target
    val method = state.config.correspondenceMethod match {
      case TriangularClosestPoint  => ClosestPointTriangleMesh3D
      case AlongNormalClosestPoint => ClosestPointAlongNormalTriangleMesh3D
      case PointcloudClosestPoint  => ClosestPointTriangleMesh3DSimple
    }
    val corr =
      if (state.config.reverseCorrespondenceDirection)
        method.closestPointCorrespondenceReversal(source, target)
      else
        method.closestPointCorrespondence(source, target)
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
    reverseCorrespondenceDirection: Boolean = false,
    correspondenceMethod: ICPCorrespondenceMethod = TriangularClosestPoint
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
    math.max(newSigma, current.config.endSigma)
  }
}
