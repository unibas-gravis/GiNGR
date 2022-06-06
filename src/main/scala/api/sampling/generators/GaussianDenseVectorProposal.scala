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

import breeze.linalg.DenseVector
import scalismo.sampling.evaluators.GaussianEvaluator
import scalismo.sampling.{ProposalGenerator, SymmetricTransitionRatio, TransitionProbability}
import scalismo.utils.Random

case class GaussianDenseVectorProposal(sdev: Double)(implicit rnd: Random)
    extends ProposalGenerator[DenseVector[Double]]
    with SymmetricTransitionRatio[DenseVector[Double]]
    with TransitionProbability[DenseVector[Double]] {
  override def propose(current: DenseVector[Double]): DenseVector[Double] = {
    require(current.length > 0, "cannot propose change on empty vector")
    current.map { v => v + sdev * rnd.scalaRandom.nextGaussian() }
  }

  override def logTransitionProbability(from: DenseVector[Double], to: DenseVector[Double]): Double = {
    require(from.length > 0, "cannot calculate transition on empty vector")
    require(from.length == to.length, "IndexedSeqs must be of same length")
    to.activeValuesIterator
      .zip(from.activeValuesIterator)
      .map { case (t, f) => GaussianEvaluator.logDensity(t, f, sdev) }
      .sum
  }
}
