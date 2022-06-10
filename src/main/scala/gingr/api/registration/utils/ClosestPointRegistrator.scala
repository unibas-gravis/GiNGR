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

package gingr.api.registration.utils

import scalismo.common.{DiscreteDomain, PointId, UnstructuredPointsDomain}
import scalismo.geometry.{EuclideanVector, Point, _3D}
import scalismo.mesh.TriangleMesh

trait ClosestPointRegistrator[DDomain[_3D] <: DiscreteDomain[_3D]] {
  /*
  returns: Seq of point on template to corresponding point on target + a weight identifying the robustness of the closest point (1.0 = robust, 0.0 = not-robust)
           Additionally the average closest point distance is returned
   */
  def closestPointCorrespondence(
      template: DDomain[_3D],
      target: DDomain[_3D]
  ): (Seq[(PointId, Point[_3D], Double)], Double)

  def closestPointCorrespondenceReversal(
      template: DDomain[_3D],
      target: DDomain[_3D]
  ): (Seq[(PointId, Point[_3D], Double)], Double) = {
    val corr = closestPointCorrespondence(target, template)
    val inverted = corr._1.map { case (id, p, w) =>
      (template.pointSet.findClosestPoint(p).id, target.pointSet.point(id), w)
    }
    (inverted, corr._2)
  }
}

object NonRigidClosestPointRegistrator {

  // Todo: Different ICP "flavours", closest point in pointset, closest point on surface, closest point along normal
  // Todo: Swap directions

  def isPointOnBoundary(id: PointId, mesh: TriangleMesh[_3D]): Boolean = {
    mesh.operations.pointIsOnBoundary(id)
  }

  def isNormalDirectionOpposite(n1: EuclideanVector[_3D], n2: EuclideanVector[_3D]): Boolean = {
    // Todo: Add angle hyperparameter - currently it only looks if the vectors are opposite
    (n1.dot(n2)) < 0
  }

  def isClosestPointIntersecting(id: PointId, cp: Point[_3D], mesh: TriangleMesh[_3D]): Boolean = {
    val p = mesh.pointSet.point(id)
    val v = p - cp
    val intersectingPoints = mesh.operations
      .getIntersectionPoints(p, v)
      .filter(f => f != p) // All intersecting points with the closest point vector
    val closestIntersectingPoint =
      if (intersectingPoints.nonEmpty) intersectingPoints.map(ip => (p - ip).norm).min
      else Double.PositiveInfinity // Distance to closest intersecting point on template
    (closestIntersectingPoint < (v).norm)
  }

  object ClosestPointTriangleMesh3D extends ClosestPointRegistrator[TriangleMesh] {
    override def closestPointCorrespondence(
        template: TriangleMesh[_3D],
        target: TriangleMesh[_3D]
    ): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p                     = template.pointSet.point(id)
        val closestPointOnSurface = target.operations.closestPointOnSurface(p)
        val closestPoint          = target.pointSet.findClosestPoint(closestPointOnSurface.point)
        val w =
          if (isPointOnBoundary(closestPoint.id, target)) 0.0
          else if (
            isNormalDirectionOpposite(template.vertexNormals.atPoint(id), target.vertexNormals.atPoint(closestPoint.id))
          ) 0.0
          else if (isClosestPointIntersecting(id, closestPointOnSurface.point, template)) 0.0
          else 1.0
        distance += closestPointOnSurface.distance
        (id, closestPointOnSurface.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

  object ClosestPointAlongNormalTriangleMesh3D extends ClosestPointRegistrator[TriangleMesh] {
    override def closestPointCorrespondence(
        template: TriangleMesh[_3D],
        target: TriangleMesh[_3D]
    ): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val n = template.vertexNormals.atPoint(id)

        val intersectingPoints = target.operations.getIntersectionPoints(p, n).filter(f => f != p)
        val closestPointAlongNormal =
          if (intersectingPoints.nonEmpty) Some(intersectingPoints.minBy(ip => (p - ip).norm)) else None

        val (closestPoint, w) = if (closestPointAlongNormal.nonEmpty) {
          val closestPoint = target.pointSet.findClosestPoint(closestPointAlongNormal.get)
          val weight =
            if (isPointOnBoundary(closestPoint.id, target)) 0.0
            else if (
              isNormalDirectionOpposite(
                template.vertexNormals.atPoint(id),
                target.vertexNormals.atPoint(closestPoint.id)
              )
            ) 0.0
            else if (isClosestPointIntersecting(id, closestPointAlongNormal.get, template)) 0.0
            else 1.0
          (closestPointAlongNormal.get, weight)
        } else (p, 0.0) // return p to avoid influincing the "distance" measure too much
        distance += (p - closestPoint).norm
        (id, closestPoint, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

  object ClosestPointUnstructuredPointsDomain3D extends ClosestPointRegistrator[UnstructuredPointsDomain] {
    override def closestPointCorrespondence(
        template: UnstructuredPointsDomain[_3D],
        target: UnstructuredPointsDomain[_3D]
    ): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p            = template.pointSet.point(id)
        val closestPoint = target.pointSet.findClosestPoint(p)
        val w            = 1.0
        distance += (p - closestPoint.point).norm
        (id, closestPoint.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

  object ClosestPointTriangleMesh3DSimple extends ClosestPointRegistrator[TriangleMesh] {
    override def closestPointCorrespondence(
        template: TriangleMesh[_3D],
        target: TriangleMesh[_3D]
    ): (Seq[(PointId, Point[_3D], Double)], Double) = {
      ClosestPointUnstructuredPointsDomain3D.closestPointCorrespondence(
        UnstructuredPointsDomain(template.pointSet),
        UnstructuredPointsDomain(target.pointSet)
      )
    }
  }

}
