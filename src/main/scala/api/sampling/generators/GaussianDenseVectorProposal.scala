package api.sampling.generators

import breeze.linalg.DenseVector
import scalismo.sampling.evaluators.GaussianEvaluator
import scalismo.sampling.{ProposalGenerator, SymmetricTransitionRatio, TransitionProbability}
import scalismo.utils.Random

case class GaussianDenseVectorProposal(sdev: Double)(implicit rnd: Random)
  extends ProposalGenerator[DenseVector[Double]] with SymmetricTransitionRatio[DenseVector[Double]] with TransitionProbability[DenseVector[Double]] {
  override def propose(current: DenseVector[Double]): DenseVector[Double] = {
    require(current.length > 0, "cannot propose change on empty vector")
    current.map { v => v + sdev * rnd.scalaRandom.nextGaussian() }
  }

  override def logTransitionProbability(from: DenseVector[Double], to: DenseVector[Double]): Double = {
    require(from.length > 0, "cannot calculate transition on empty vector")
    require(from.length == to.length, "IndexedSeqs must be of same length")
    to.activeValuesIterator.zip(from.activeValuesIterator).map { case (t, f) => GaussianEvaluator.logDensity(t, f, sdev) }.sum
  }
}
