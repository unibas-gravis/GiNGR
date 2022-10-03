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

package gingr.api.sampling.generators

import gingr.api.GingrRegistrationState
import scalismo.geometry._3D
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.utils.{Memoize, Random}

import scala.util.Try

case class GeneratorWrapperStochastic[State <: GingrRegistrationState[State]](
    update: (State, Boolean) => State,
    cashedPosterior: Memoize[State, Try[PointDistributionModel[_3D, TriangleMesh]]],
    generatedBy: String = "InformedProposal"
)(implicit rnd: Random)
    extends GingrGeneratorWrapper[State] {
  override def gingrPropose(current: State): State = {
    val newState = update(current, true)
    newState.updateGeneral(newState.general.updateGeneratedBy(generatedBy))
  }

  override def logTransitionProbability(from: State, to: State): Double = {
    val posterior = cashedPosterior(from)
    if (posterior.isFailure) {
      Double.NegativeInfinity
    } else {
      val toMesh = if (from.general.stepLength != 1.0) {
        val compensatedTo =
          from.general.modelParameters.shape.parameters + ((to.general.modelParameters.shape.parameters - from.general.modelParameters.shape.parameters) / from.general.stepLength)
        from.general.model.instance(compensatedTo)
      } else from.general.fit
      try {
        val projectedTo = posterior.get.coefficients(toMesh)
        val logpdf      = posterior.get.gp.logpdf(projectedTo)
        logpdf
      } catch {
        case _: Throwable => {
          Double.NegativeInfinity
        }
      }

    }
  }
}
