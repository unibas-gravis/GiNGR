package api.sampling.evaluators

import api.GingrRegistrationState
import scalismo.sampling.DistributionEvaluator

case class AcceptAllEvaluator[State <: GingrRegistrationState[State]]() extends DistributionEvaluator[State] with EvaluationCaching[State] {
  override def computeLogValue(sample: State): Double = 0.0
}
