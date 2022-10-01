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

package gingr.api.sampling.evaluators

import breeze.linalg.{DenseVector, diag}
import gingr.api.GingrRegistrationState
import scalismo.sampling.DistributionEvaluator
import scalismo.statisticalmodel.MultivariateNormalDistribution

case class ModelEvaluator[State <: GingrRegistrationState[State]](modelrank: Int) extends DistributionEvaluator[State] {
  val mvnormal: MultivariateNormalDistribution =
    MultivariateNormalDistribution(DenseVector.zeros[Double](modelrank), diag(DenseVector.ones[Double](modelrank)))

  override def logValue(theta: State): Double = {
    mvnormal.logpdf(theta.general.modelParameters.shape.parameters)
  }
}
