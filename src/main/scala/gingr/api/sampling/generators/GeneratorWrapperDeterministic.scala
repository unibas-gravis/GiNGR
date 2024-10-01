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

import gingr.api.GingrRegistrationState
import scalismo.utils.Random

case class GeneratorWrapperDeterministic[State <: GingrRegistrationState[State]](
  update: (State, Boolean) => State,
  generatedBy: String = "Deterministic"
)(implicit rnd: Random)
    extends GingrGeneratorWrapper[State] {
  override def gingrPropose(current: State): State = {
    val newState = update(current, false)
    newState.updateGeneral(
      newState.general
        .updateGeneratedBy(generatedBy)
    )
  }
  override def logTransitionProbability(from: State, to: State): Double = {
    Double.NegativeInfinity
  }
}
