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

package gingr.api.sampling.generators

import gingr.api.{GingrRegistrationState, ModelFittingParameters}
import scalismo.sampling.{ProposalGenerator, TransitionProbability}

trait GingrGeneratorWrapper[State <: GingrRegistrationState[State]]
    extends ProposalGenerator[State]
    with TransitionProbability[State] {
  def gingrPropose(current: State): State

  override def propose(current: State): State = {
    val proposedState = gingrPropose(current)
    val fit = ModelFittingParameters.modelInstanceShapePoseScale(
      proposedState.general.model,
      proposedState.general.modelParameters
    )
    proposedState.updateGeneral(
      proposedState.general
        .updateFit(fit)
        .updateIteration()
    )
  }
}
