package api.registration.config

import api.{GeneralRegistrationState, GingrConfig, GingrRegistrationState}

trait StateHandler[State <: GingrRegistrationState[State], config <: GingrConfig] {
  def initialize(general: GeneralRegistrationState, config: config): State
}

object StateHandler {
  implicit object StateHandlerCPD extends StateHandler[CpdRegistrationState, CpdConfiguration] {
    override def initialize(general: GeneralRegistrationState, config: CpdConfiguration): CpdRegistrationState = {
      CpdRegistrationState(general, config)
    }
  }

  implicit object StateHandlerICP extends StateHandler[IcpRegistrationState, IcpConfiguration] {
    override def initialize(general: GeneralRegistrationState, config: IcpConfiguration): IcpRegistrationState = {
      IcpRegistrationState(general, config)
    }
  }
}
