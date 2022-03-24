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
import other.algorithms.cpd.CPDFactory
import scalismo.common.{DiscreteDomain, DiscreteField, DomainWarp, Vectorizer}
import scalismo.geometry.{NDSpace, Point}

import scala.language.higherKinds

class RigidCPDRegistration[D: NDSpace, DDomain[D] <: DiscreteDomain[D]](template: DDomain[D], lambda: Double = 2, beta: Double = 2, w: Double = 0, max_iterations: Int = 100)(
  implicit
  warper: DomainWarp[D, DDomain],
  vectorizer: Vectorizer[Point[D]],
  pointSequenceConverter: PointSequenceConverter[D]) {
  val cpd = new CPDFactory(template.pointSet.points.toSeq, lambda, beta, w)

  def registrationMethod(targetPoints: Seq[Point[D]]) = cpd.registerRigidly(targetPoints)

  def register(target: DDomain[D]): DDomain[D] = {
    val registrationTask = registrationMethod(target.pointSet.points.toSeq)
    val registration = registrationTask.Registration(max_iterations)
    val warpField = DiscreteField(template, template.pointSet.points.toIndexedSeq.zip(registration).map { case (a, b) => b - a })
    warper.transformWithField(template, warpField)
  }
}

class NonRigidCPDRegistration[D: NDSpace, DDomain[D] <: DiscreteDomain[D]](template: DDomain[D], lambda: Double = 2, beta: Double = 2, w: Double = 0, max_iterations: Int = 100)(
  implicit
  warper: DomainWarp[D, DDomain],
  vectorizer: Vectorizer[Point[D]],
  pointSequenceConverter: PointSequenceConverter[D])
  extends RigidCPDRegistration[D, DDomain](template, lambda, beta, w, max_iterations) {

  override def registrationMethod(targetPoints: Seq[Point[D]]) = cpd.registerNonRigidly(targetPoints)
}

class AffineCPDRegistration[D: NDSpace, DDomain[D] <: DiscreteDomain[D]](template: DDomain[D], lambda: Double = 2, beta: Double = 2, w: Double = 0, max_iterations: Int = 100)(
  implicit
  warper: DomainWarp[D, DDomain],
  vectorizer: Vectorizer[Point[D]],
  pointSequenceConverter: PointSequenceConverter[D])
  extends RigidCPDRegistration[D, DDomain](template, lambda, beta, w, max_iterations) {

  override def registrationMethod(targetPoints: Seq[Point[D]]) = cpd.registerAffine(targetPoints)
}
