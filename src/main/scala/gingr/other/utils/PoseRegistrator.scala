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

package gingr.other.utils

import scalismo.common.UnstructuredPoints
import scalismo.geometry.{Point, _3D}
import scalismo.registration.LandmarkRegistration

trait Registrator {
  def register(points: Seq[(Point[_3D], Point[_3D])]): UnstructuredPoints[_3D]
}

object PoseRegistrator {

  implicit object RigidRegistrator3D extends Registrator {
    override def register(points: Seq[(Point[_3D], Point[_3D])]): UnstructuredPoints[_3D] = {
      val t = LandmarkRegistration.rigid3DLandmarkRegistration(points, Point(0, 0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }

  implicit object AffineRegistrator3D extends Registrator {
    override def register(points: Seq[(Point[_3D], Point[_3D])]): UnstructuredPoints[_3D] = {
      val t = LandmarkRegistration.similarity3DLandmarkRegistration(points, Point(0, 0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }
}
