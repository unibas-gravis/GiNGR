package api.sampling.generators

import api.{GingrRegistrationState, ModelFittingParameters}
import scalismo.sampling.{ProposalGenerator, TransitionProbability}

trait GingrGeneratorWrapper[State <: GingrRegistrationState[State]] extends ProposalGenerator[State] with TransitionProbability[State] {
  def gingrPropose(current: State): State

  override def propose(current: State): State = {
    val proposedState = gingrPropose(current)
    val fit = ModelFittingParameters.modelInstanceShapePoseScale(proposedState.general.model, proposedState.general.modelParameters)
    proposedState.updateGeneral(proposedState.general.updateFit(fit))
  }
}
