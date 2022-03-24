package api.sampling

import api.GingrRegistrationState
import api.sampling.evaluators.{AcceptAllEvaluator, EvaluationMode, IndependentPointDistanceEvaluator, ModelEvaluator, ModelToTargetEvaluation}
import scalismo.sampling.DistributionEvaluator
import scalismo.sampling.evaluators.ProductEvaluator

case class EvaluatorIdentifier[A](name: String, evaluator: DistributionEvaluator[A])

trait Evaluator[A] {
  def evaluator: Seq[EvaluatorIdentifier[A]]
  def productEvaluator(): DistributionEvaluator[A] = new ProductEvaluator[A](evaluator.map(_.evaluator))
}

case class AcceptAll[State <: GingrRegistrationState[State]]() extends Evaluator[State] {
  override def evaluator: Seq[EvaluatorIdentifier[State]] = {
    Seq(
      EvaluatorIdentifier(name = "AcceptAll", evaluator = AcceptAllEvaluator[State]())
    )
  }
}

case class IndependtPoints[State <: GingrRegistrationState[State]](
  state: State,
  uncertainty: Double,
  mode: EvaluationMode = ModelToTargetEvaluation,
  evaluatedPoints: Option[Int] = None)
  extends Evaluator[State] {
  private val likelihoodModel = breeze.stats.distributions.Gaussian(0, uncertainty)

  override def evaluator: Seq[EvaluatorIdentifier[State]] = {
    Seq(
      EvaluatorIdentifier(name = "Prior", evaluator = ModelEvaluator[State](state.general.model.rank)),
      EvaluatorIdentifier(name = "Distance", evaluator = IndependentPointDistanceEvaluator[State](state, likelihoodModel, mode, evaluatedPoints))
    )
  }
}
