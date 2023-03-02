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

package gingr.other.algorithms

import gingr.other.algorithms.cpd.CPDFactory
import scalismo.common.{DiscreteDomain, DiscreteField, DomainWarp, Vectorizer}
import scalismo.geometry.{Point, _3D}

class RigidCPDRegistration[DDomain[_3D] <: DiscreteDomain[_3D]](
    template: DDomain[_3D],
    lambda: Double = 2,
    beta: Double = 2,
    w: Double = 0,
    max_iterations: Int = 100
)(implicit
    warper: DomainWarp[_3D, DDomain],
    vectorizer: Vectorizer[Point[_3D]]
) {
  val cpd = new CPDFactory(template.pointSet.points.toSeq, lambda, beta, w)

  def registrationMethod(targetPoints: Seq[Point[_3D]]) = cpd.registerRigidly(targetPoints)

  def register(target: DDomain[_3D]): DDomain[_3D] = {
    val registrationTask = registrationMethod(target.pointSet.points.toSeq)
    val registration     = registrationTask.Registration(max_iterations)
    val warpField =
      DiscreteField(template, template.pointSet.points.toIndexedSeq.zip(registration).map { case (a, b) => b - a })
    warper.transformWithField(template, warpField)
  }
}

class NonRigidCPDRegistration[DDomain[_3D] <: DiscreteDomain[_3D]](
    template: DDomain[_3D],
    lambda: Double = 2,
    beta: Double = 2,
    w: Double = 0,
    max_iterations: Int = 100
)(implicit
    warper: DomainWarp[_3D, DDomain],
    vectorizer: Vectorizer[Point[_3D]]
) extends RigidCPDRegistration[DDomain](template, lambda, beta, w, max_iterations) {

  override def registrationMethod(targetPoints: Seq[Point[_3D]]) = cpd.registerNonRigidly(targetPoints)
}

class AffineCPDRegistration[DDomain[_3D] <: DiscreteDomain[_3D]](
    template: DDomain[_3D],
    lambda: Double = 2,
    beta: Double = 2,
    w: Double = 0,
    max_iterations: Int = 100
)(implicit
    warper: DomainWarp[_3D, DDomain],
    vectorizer: Vectorizer[Point[_3D]]
) extends RigidCPDRegistration[DDomain](template, lambda, beta, w, max_iterations) {

  override def registrationMethod(targetPoints: Seq[Point[_3D]]) = cpd.registerAffine(targetPoints)
}
