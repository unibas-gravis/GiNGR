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

package other.algorithms

import api.registration.utils.PointSequenceConverter
import other.algorithms.cpd.BCPD
import scalismo.common.{DiscreteDomain, DiscreteField, DomainWarp, Vectorizer}
import scalismo.geometry.{NDSpace, Point}
import scalismo.kernels.PDKernel

import scala.language.higherKinds

class BCPDRegistration[D: NDSpace, DDomain[D] <: DiscreteDomain[D]](
  template: DDomain[D],
  target: DDomain[D],
  w: Double = 0, // Outlier, [0,1]
  lambda: Double = 2.0, // Noise scaling, R+
  gamma: Double = 1.0, // Initial noise scaling, R+
  k: Double = 1.0, // Dirichlet distribution parameter
  kernel: PDKernel[D], // Positive semi-def kernel
  max_iterations: Int = 100)(implicit warper: DomainWarp[D, DDomain], vectorizer: Vectorizer[Point[D]], pointSequenceConverter: PointSequenceConverter[D]) {
  val cpd = new BCPD(template.pointSet.points.toSeq, target.pointSet.points.toSeq, w, lambda, gamma, k, kernel)

  def register(): DDomain[D] = {
    val registration = cpd.Registration(max_iterations)
    val warpField = DiscreteField(template, template.pointSet.points.toIndexedSeq.zip(registration).map { case (a, b) => b - a })
    warper.transformWithField(template, warpField)
  }
}
