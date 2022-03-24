package api.sampling.loggers

import api.GingrRegistrationState
import scalismo.sampling.{DistributionEvaluator, ProposalGenerator}
import scalismo.sampling.loggers.{AcceptRejectLogger, ChainStateLogger}

case class EmptyChainStateLogger[State <: GingrRegistrationState[State]]() extends ChainStateLogger[State] {
  override def logState(sample: State): Unit = {}
}

case class EmptyAcceptRejectLogger[State <: GingrRegistrationState[State]]() extends AcceptRejectLogger[State] {
  override def accept(current: State, sample: State, generator: ProposalGenerator[State], evaluator: DistributionEvaluator[State]): Unit = {}

  override def reject(current: State, sample: State, generator: ProposalGenerator[State], evaluator: DistributionEvaluator[State]): Unit = {}
}
