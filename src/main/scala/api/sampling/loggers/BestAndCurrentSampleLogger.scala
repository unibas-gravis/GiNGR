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

package api.sampling.loggers

import scalismo.sampling.DistributionEvaluator
import scalismo.sampling.loggers.BestSampleLogger

/** keep the best sample so far, warning: changes state */
class BestAndCurrentSampleLogger[A](evaluator: DistributionEvaluator[A]) extends BestSampleLogger[A](evaluator) {

  private case class EvaluatedSample(sample: A, value: Double)

  private var bestState: Option[EvaluatedSample] = None
  private var currentState: Option[A]            = None

  override def logState(sample: A): Unit = {
    val value = evaluator.logValue(sample)
    if (bestState.isEmpty || value > bestState.get.value) {
      bestState = Some(EvaluatedSample(sample, value))
    }
    currentState = Some(sample)
  }

  def currentSample(): Option[A] = currentState.map(f => f)

  override def currentBestSample() = bestState.map(_.sample)

  override def currentBestValue() = bestState.map(_.value)

}

object BestAndCurrentSampleLogger {
  def apply[A](evaluator: DistributionEvaluator[A]) = new BestAndCurrentSampleLogger[A](evaluator)
}
