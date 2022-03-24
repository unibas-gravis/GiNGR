package api.sampling.evaluators

import api.GingrRegistrationState
import scalismo.sampling.DistributionEvaluator
import scalismo.utils.Memoize

/**
  * This trait can be mixe in with an DistributionEvalutor, to enable caching of the values
  */
trait EvaluationCaching[State <: GingrRegistrationState[State]] {
  self: DistributionEvaluator[State] =>

  def computeLogValue(sample: State): Double


  private val computeLogValueMemoized = Memoize(computeLogValue, 3)

  override def logValue(sample: State): Double = {
    computeLogValueMemoized(sample)
  }

}

