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

package api.sampling

import api.GingrRegistrationState
import api.sampling.evaluators._
import breeze.stats.distributions.Rand.FixedSeed.randBasis
import scalismo.sampling.DistributionEvaluator
import scalismo.sampling.evaluators.ProductEvaluator

case class EvaluatorIdentifier[A](name: String, evaluator: DistributionEvaluator[A])

trait Evaluator[A] {
  def evaluator: Seq[EvaluatorIdentifier[A]]
  def productEvaluator(): DistributionEvaluator[A] = new ProductEvaluator[A](evaluator.map(_.evaluator))
}

case class AcceptAll[State <: GingrRegistrationState[State]]() extends Evaluator[State] {
  override def evaluator: Seq[EvaluatorIdentifier[State]] = {
    Seq(
      EvaluatorIdentifier(name = "AcceptAll", evaluator = AcceptAllEvaluator[State]())
    )
  }
}

case class IndependentPoints[State <: GingrRegistrationState[State]](
    state: State,
    uncertainty: Double,
    mode: EvaluationMode = ModelToTargetEvaluation,
    evaluatedPoints: Option[Int] = None
) extends Evaluator[State] {
  private val likelihoodModel = breeze.stats.distributions.Gaussian(0, uncertainty)

  override def evaluator: Seq[EvaluatorIdentifier[State]] = {
    Seq(
      EvaluatorIdentifier(name = "Prior", evaluator = ModelEvaluator[State](state.general.model.rank)),
      EvaluatorIdentifier(
        name = "Distance",
        evaluator = IndependentPointDistanceEvaluator[State](state, likelihoodModel, mode, evaluatedPoints)
      )
    )
  }
}
