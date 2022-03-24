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
