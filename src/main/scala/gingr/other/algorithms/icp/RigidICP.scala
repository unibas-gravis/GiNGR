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

package gingr.other.algorithms.icp

import breeze.numerics.abs
import gingr.other.utils.Registrator
import scalismo.common.{PointId, UnstructuredPoints, Vectorizer}
import scalismo.geometry.{_3D, Point}

private[icp] class RigidICP(
  val targetPoints: UnstructuredPoints[_3D],
  val icp: ICPFactory
)(implicit
  val vectorizer: Vectorizer[Point[_3D]],
  registrator: Registrator
) {
  require(vectorizer.dim == 2 || vectorizer.dim == 3)

  def Registration(max_iteration: Int, tolerance: Double = 0.001): UnstructuredPoints[_3D] = {
    val sigmaInit = 0.0

    var fit = (icp.templatePoints, sigmaInit)
    var i = 0
    var converged = false

    while (i < max_iteration && !converged) {
      val iter = Iteration(fit._1, targetPoints)
      val distance = iter._2
      println(s"ICP, iteration: $i, distance: $distance")
      val TY = iter._1
      val diff = abs(distance - fit._2)
      if (diff < tolerance) {
        println("Converged")
        fit = (TY, distance)
        converged = true
      } else {
        fit = iter
      }
      i += 1
    }

    fit._1
  }

  private def attributeCorrespondences(
    template: UnstructuredPoints[_3D],
    target: UnstructuredPoints[_3D]
  ): (Seq[(Point[_3D], Point[_3D])], Double) = {
    val ptIds = template.pointIds.toIndexedSeq
    var distance = 0.0
    val corr = ptIds.map { (id: PointId) =>
      val pt = template.point(id)
      val closestPointOnMesh2 = target.findClosestPoint(pt).point
      distance += (pt - closestPointOnMesh2).norm
      (pt, closestPointOnMesh2)
    }
    (corr, distance / ptIds.length)
  }

  def Iteration(
    template: UnstructuredPoints[_3D],
    target: UnstructuredPoints[_3D]
  ): (UnstructuredPoints[_3D], Double) = {
    val (correspondences, distance) = attributeCorrespondences(template, target)
    val registeredPoints = registrator.register(correspondences)
    (registeredPoints, distance)
  }

}
