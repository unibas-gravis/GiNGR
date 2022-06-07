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

import gingr.other.algorithms.icp.ICPFactory
import gingr.other.utils.Registrator
import scalismo.common.UnstructuredPoints.Create
import scalismo.common.*
import scalismo.geometry.{NDSpace, Point}

class RigidICPRegistration[D: NDSpace, DDomain[A] <: DiscreteDomain[A]](
    template: DDomain[D],
    max_iterations: Int = 100
)(implicit
    warper: DomainWarp[D, DDomain],
    vectorizer: Vectorizer[Point[D]],
    registration: Registrator[D],
    create: Create[D]
) {
  val icp = new ICPFactory[D](UnstructuredPoints(template.pointSet.points.toIndexedSeq))

  def registrationMethod(targetPoints: UnstructuredPoints[D]) = icp.registerRigidly(targetPoints)

  def register(target: DDomain[D]): DDomain[D] = {
    val registrationTask = registrationMethod(UnstructuredPoints(target.pointSet.points.toIndexedSeq))
    val registration     = registrationTask.Registration(max_iterations)
    val warpField = DiscreteField(
      target,
      target.pointSet.points.toIndexedSeq.zip(registration.points.toIndexedSeq).map { case (a, b) => b - a }
    )
    warper.transformWithField(target, warpField)
  }
}
