package api.sampling.evaluators

import api.GingrRegistrationState
import breeze.linalg.{diag, DenseVector}
import scalismo.sampling.DistributionEvaluator
import scalismo.statisticalmodel.MultivariateNormalDistribution

case class ModelEvaluator[State <: GingrRegistrationState[State]](modelrank: Int) extends DistributionEvaluator[State] {
  val mvnormal: MultivariateNormalDistribution = MultivariateNormalDistribution(DenseVector.zeros[Double](modelrank), diag(DenseVector.ones[Double](modelrank)))

  override def logValue(theta: State): Double = {
    mvnormal.logpdf(theta.general.modelParameters.shape.parameters)
  }
}
