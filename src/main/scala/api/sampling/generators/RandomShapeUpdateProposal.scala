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

package api.sampling.generators

import api.{GingrRegistrationState, ShapeParameters}
import scalismo.utils.Random

case class RandomShapeUpdateProposal[State <: GingrRegistrationState[State]](
    stdev: Double,
    generatedBy: String = "RandomShapeUpdateProposal"
)(implicit random: Random)
    extends GingrGeneratorWrapper[State] {

  private val generator = GaussianDenseVectorProposal(stdev)

  override def gingrPropose(theta: State): State = {
    val currentCoeffs = theta.general.modelParameters.shape.parameters
    val updatedCoeffs = generator.propose(currentCoeffs)
    theta.updateGeneral(
      theta.general.updateShapeParameters(ShapeParameters(updatedCoeffs)).updateGeneratedBy(generatedBy)
    )
  }

  override def logTransitionProbability(from: State, to: State): Double = {
    if (to.general.modelParameters.copy(shape = from.general.modelParameters.shape) != from.general.modelParameters) {
      Double.NegativeInfinity
    } else {
      generator.logTransitionProbability(
        from.general.modelParameters.shape.parameters,
        to.general.modelParameters.shape.parameters
      )
    }
  }
}
