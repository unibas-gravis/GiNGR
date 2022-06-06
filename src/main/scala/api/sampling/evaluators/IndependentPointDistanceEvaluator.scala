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

package api.sampling.evaluators

import api.GingrRegistrationState
import breeze.stats.distributions.ContinuousDistr
import scalismo.common.PointId
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh3D
import scalismo.sampling.DistributionEvaluator

trait EvaluationMode

case object ModelToTargetEvaluation extends EvaluationMode

case object TargetToModelEvaluation extends EvaluationMode

case object SymmetricEvaluation extends EvaluationMode

case class IndependentPointDistanceEvaluator[State <: GingrRegistrationState[State]](
    sample: State,
    likelihoodModel: ContinuousDistr[Double],
    evaluationMode: EvaluationMode,
    numberOfPointsForComparison: Option[Int]
) extends DistributionEvaluator[State]
    with EvaluationCaching[State] {

  private val instance = sample.general.fit
  private val target   = sample.general.target

  private val (instanceDecimated, targetDecimated) = numberOfPointsForComparison match {
    case Some(num) => (instance.operations.decimate(num), target.operations.decimate(num))
    case _         => (instance, target)
  }

  private val randomPointsOnTarget: IndexedSeq[Point[_3D]] = targetDecimated.pointSet.points.toIndexedSeq
  private val randomPointIdsOnModel: IndexedSeq[PointId]   = instanceDecimated.pointSet.pointIds.toIndexedSeq

  def distModelToTarget(modelSample: TriangleMesh3D): Double = {
    val pointsOnSample = randomPointIdsOnModel.map(modelSample.pointSet.point)
    val dists = for (pt <- pointsOnSample) yield {
      likelihoodModel.logPdf((target.operations.closestPointOnSurface(pt).point - pt).norm)
    }
    dists.sum
  }

  def distTargetToModel(modelSample: TriangleMesh3D): Double = {
    val dists = for (pt <- randomPointsOnTarget) yield {
      likelihoodModel.logPdf((modelSample.operations.closestPointOnSurface(pt).point - pt).norm)
    }
    dists.sum
  }

  def computeLogValue(sample: State): Double = {

    val currentSample = sample.general.fit
    val dist = evaluationMode match {
      case ModelToTargetEvaluation => distModelToTarget(currentSample)
      case TargetToModelEvaluation => distTargetToModel(currentSample)
      case SymmetricEvaluation     => 0.5 * distModelToTarget(currentSample) + 0.5 * distTargetToModel(currentSample)
    }
    dist
  }
}
