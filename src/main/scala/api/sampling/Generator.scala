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
import api.sampling.generators.{GaussianAxisRotationProposal, GaussianAxisTranslationProposal, PitchAxis, RandomShapeUpdateProposal, RollAxis, YawAxis}
import scalismo.sampling.{ProposalGenerator, TransitionProbability}
import scalismo.sampling.proposals.MixtureProposal
import scalismo.utils.Random
import scalismo.sampling.proposals.MixtureProposal.implicits._

class Generator[State <: GingrRegistrationState[State]](implicit rnd: Random) {

  val defaultTranslation = 0.1
  val defaultRotation = 0.01

  def RandomShape(steps: Seq[Double] = Seq(1.0, 0.1, 0.01)): ProposalGenerator[State] with TransitionProbability[State] = {
    val gen = steps.map { d => (1.0 / steps.length.toDouble, RandomShapeUpdateProposal[State](d, generatedBy = s"RandomShape-${d}")) }
    MixtureProposal.fromProposalsWithTransition(gen: _*)
  }

  def RandomRotation(
    rotYaw: Double = defaultRotation,
    rotPitch: Double = defaultRotation,
    rotRoll: Double = defaultRotation): ProposalGenerator[State] with TransitionProbability[State] = {
    MixtureProposal(
      0.5 *: GaussianAxisRotationProposal[State](rotYaw, YawAxis, generatedBy = s"RotationYaw-${rotYaw}") +
        0.5 *: GaussianAxisRotationProposal[State](rotPitch, PitchAxis, generatedBy = s"RotationPitch-${rotPitch}") +
        0.5 *: GaussianAxisRotationProposal[State](rotRoll, RollAxis, generatedBy = s"RotationRoll-${rotRoll}")
    )
  }

  def RandomTranslation(
    transX: Double = defaultTranslation,
    transY: Double = defaultTranslation,
    transZ: Double = defaultTranslation): ProposalGenerator[State] with TransitionProbability[State] = {
    MixtureProposal(
      0.5 *: GaussianAxisTranslationProposal[State](transX, 0, generatedBy = s"TranslationX-${transX}") +
        0.5 *: GaussianAxisTranslationProposal[State](transY, 1, generatedBy = s"TranslationY-${transY}") +
        0.5 *: GaussianAxisTranslationProposal[State](transZ, 2, generatedBy = s"TranslationZ-${transZ}")
    )
  }

  def RandomPose(
    rotYaw: Double = defaultRotation,
    rotPitch: Double = defaultRotation,
    rotRoll: Double = defaultRotation,
    transX: Double = defaultTranslation,
    transY: Double = defaultTranslation,
    transZ: Double = defaultTranslation): ProposalGenerator[State] with TransitionProbability[State] = {
    MixtureProposal(
      0.5 *: RandomRotation(rotYaw, rotPitch, rotRoll) +
        0.5 *: RandomTranslation(transX, transY, transZ)
    )
  }

  def DefaultRandom(): ProposalGenerator[State] with TransitionProbability[State] = {
    MixtureProposal(
      0.5 *: RandomPose() +
        0.5 *: RandomShape()
    )
  }
}
