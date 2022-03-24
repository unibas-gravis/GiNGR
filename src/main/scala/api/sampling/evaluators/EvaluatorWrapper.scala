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
import api.sampling.Evaluator
import scalismo.sampling.DistributionEvaluator

case class EvaluatorWrapper[State <: GingrRegistrationState[State]](probabilistic: Boolean, evaluator: Evaluator[State]) extends DistributionEvaluator[State] {
  private val eval = evaluator.productEvaluator()
  override def logValue(sample: State): Double = {
    if (!probabilistic) {
      0.0
    } else {
      eval.logValue(sample)
    }
  }
}
