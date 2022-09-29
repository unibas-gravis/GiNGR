/*
 * Copyright 2022 University of Basel, Graphics and Vision Research Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package gingr.api.registration.config
import gingr.api._
import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.common.PointId
import scalismo.statisticalmodel.MultivariateNormalDistribution

case class TemplateConfiguration(
    override val maxIterations: Int = 1,
    override val threshold: Double = 1e-5,
    override val converged: (GeneralRegistrationState, GeneralRegistrationState, Double) => Boolean =
      (_, _, _) => false,
    override val useLandmarkCorrespondence: Boolean = true
) extends GingrConfig {}

case class TemplateRegistrationState(general: GeneralRegistrationState, config: TemplateConfiguration)
    extends GingrRegistrationState[TemplateRegistrationState] {
  override def updateGeneral(update: GeneralRegistrationState): TemplateRegistrationState = this.copy(general = update)
}

object TemplateRegistrationState {
  def apply(general: GeneralRegistrationState, config: TemplateConfiguration): TemplateRegistrationState = {
    new TemplateRegistrationState(
      general,
      config
    )
  }
}

class TemplateRegistration(
    override val getCorrespondence: TemplateRegistrationState => CorrespondencePairs =
      (state: TemplateRegistrationState) => CorrespondencePairs.empty()
) extends GingrAlgorithm[TemplateRegistrationState, TemplateConfiguration] {
  def name = "Template"
  override val getUncertainty: (PointId, TemplateRegistrationState) => MultivariateNormalDistribution =
    (id: PointId, state: TemplateRegistrationState) =>
      MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3))

  override def initializeState(
      general: GeneralRegistrationState,
      config: TemplateConfiguration
  ): TemplateRegistrationState = {
    TemplateRegistrationState(general, config)
  }
}
