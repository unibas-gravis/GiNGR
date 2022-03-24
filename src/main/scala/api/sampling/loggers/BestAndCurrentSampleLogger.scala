package api.sampling.loggers

import scalismo.sampling.DistributionEvaluator
import scalismo.sampling.loggers.BestSampleLogger

/** keep the best sample so far, warning: changes state */
class BestAndCurrentSampleLogger[A](evaluator: DistributionEvaluator[A]) extends BestSampleLogger[A](evaluator) {

  private case class EvaluatedSample(sample: A, value: Double)

  private var bestState: Option[EvaluatedSample] = None
  private var currentState: Option[A] = None

  override def logState(sample: A): Unit = {
    val value = evaluator.logValue(sample)
    if (bestState.isEmpty || value > bestState.get.value) {
      bestState = Some(EvaluatedSample(sample, value))
    }
    currentState = Some(sample)
  }

  def currentSample(): Option[A] = currentState.map(f => f)

  override def currentBestSample() = bestState.map(_.sample)

  override def currentBestValue() = bestState.map(_.value)

}

object BestAndCurrentSampleLogger {
  def apply[A](evaluator: DistributionEvaluator[A]) = new BestAndCurrentSampleLogger[A](evaluator)
}
