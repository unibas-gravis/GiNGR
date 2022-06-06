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

import api.GingrRegistrationState
import scalismo.sampling.loggers.{AcceptRejectLogger, ChainStateLogger}
import scalismo.sampling.{DistributionEvaluator, ProposalGenerator}

case class EmptyChainStateLogger[State <: GingrRegistrationState[State]]() extends ChainStateLogger[State] {
  override def logState(sample: State): Unit = {}
}

case class EmptyAcceptRejectLogger[State <: GingrRegistrationState[State]]() extends AcceptRejectLogger[State] {
  override def accept(
      current: State,
      sample: State,
      generator: ProposalGenerator[State],
      evaluator: DistributionEvaluator[State]
  ): Unit = {}

  override def reject(
      current: State,
      sample: State,
      generator: ProposalGenerator[State],
      evaluator: DistributionEvaluator[State]
  ): Unit = {}
}
