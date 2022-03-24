package api.registration.utils

import scalismo.common.{DiscreteDomain, PointId, UnstructuredPoints, UnstructuredPointsDomain}
import scalismo.geometry.{_1D, _2D, _3D, EuclideanVector, Point}
import scalismo.mesh.TriangleMesh

trait ClosestPointRegistrator[D, DDomain[D] <: DiscreteDomain[D]] {
  /*
  returns: Seq of point on template to corresponding point on target + a weight identifying the robustness of the closest point (1.0 = robust, 0.0 = not-robust)
           Additionally the average closest point distance is returned
   */
  def closestPointCorrespondence(template: DDomain[D], target: DDomain[D]): (Seq[(PointId, Point[D], Double)], Double)
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
    val intersectingPoints = mesh.operations.getIntersectionPoints(p, v).filter(f => f != p) // All intersecting points with the closest point vector
    val closestIntersectingPoint =
      if (intersectingPoints.nonEmpty) intersectingPoints.map(ip => (p - ip).norm).min else Double.PositiveInfinity // Distance to closest intersecting point on template
    (closestIntersectingPoint < (v).norm)
  }

  object ClosestPointTriangleMesh3D extends ClosestPointRegistrator[_3D, TriangleMesh] {
    override def closestPointCorrespondence(template: TriangleMesh[_3D], target: TriangleMesh[_3D]): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val closestPointOnSurface = target.operations.closestPointOnSurface(p)
        val closestPoint = target.pointSet.findClosestPoint(closestPointOnSurface.point)
        val w =
          if (isPointOnBoundary(closestPoint.id, target)) 0.0
          else if (isNormalDirectionOpposite(template.vertexNormals.atPoint(id), target.vertexNormals.atPoint(closestPoint.id))) 0.0
          else if (isClosestPointIntersecting(id, closestPointOnSurface.point, template)) 0.0
          else 1.0
        distance += closestPointOnSurface.distance
        (id, closestPointOnSurface.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }

    def closestPointCorrespondenceTargetToTemplate(template: TriangleMesh[_3D], target: TriangleMesh[_3D]): (Seq[(PointId, Point[_3D], Double)], Double) = {
      val corr = closestPointCorrespondence(target, template)
      val inverted = corr._1.map { case (id, p, w) =>
        (template.pointSet.findClosestPoint(p).id, target.pointSet.point(id), w)
      }
      (inverted, corr._2)
    }
  }

  object ClosestPointAlongNormalTriangleMesh3D extends ClosestPointRegistrator[_3D, TriangleMesh] {
    override def closestPointCorrespondence(template: TriangleMesh[_3D], target: TriangleMesh[_3D]): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val n = template.vertexNormals.atPoint(id)

        val intersectingPoints = target.operations.getIntersectionPoints(p, n).filter(f => f != p)
        val closestPointAlongNormal = if (intersectingPoints.nonEmpty) Some(intersectingPoints.minBy(ip => (p - ip).norm)) else None

        val (closestPoint, w) = if (closestPointAlongNormal.nonEmpty) {
          val closestPoint = target.pointSet.findClosestPoint(closestPointAlongNormal.get)
          val weight =
            if (isPointOnBoundary(closestPoint.id, target)) 0.0
            else if (isNormalDirectionOpposite(template.vertexNormals.atPoint(id), target.vertexNormals.atPoint(closestPoint.id))) 0.0
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

  object ClosestPointUnstructuredPointsDomain3D extends ClosestPointRegistrator[_3D, UnstructuredPointsDomain] {
    override def closestPointCorrespondence(template: UnstructuredPointsDomain[_3D], target: UnstructuredPointsDomain[_3D]): (Seq[(PointId, Point[_3D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val closestPoint = target.pointSet.findClosestPoint(p)
        val w = 1.0
        distance += (p - closestPoint.point).norm
        (id, closestPoint.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

  object ClosestPointUnstructuredPointsDomain2D extends ClosestPointRegistrator[_2D, UnstructuredPointsDomain] {
    override def closestPointCorrespondence(template: UnstructuredPointsDomain[_2D], target: UnstructuredPointsDomain[_2D]): (Seq[(PointId, Point[_2D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val closestPoint = target.pointSet.findClosestPoint(p)
        val w = 1.0
        distance += (p - closestPoint.point).norm
        (id, closestPoint.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

  object ClosestPointUnstructuredPointsDomain1D extends ClosestPointRegistrator[_1D, UnstructuredPointsDomain] {
    override def closestPointCorrespondence(template: UnstructuredPointsDomain[_1D], target: UnstructuredPointsDomain[_1D]): (Seq[(PointId, Point[_1D], Double)], Double) = {
      var distance = 0.0
      val corr = template.pointSet.pointIds.toSeq.map { id =>
        val p = template.pointSet.point(id)
        val closestPoint = target.pointSet.findClosestPoint(p)
        val w = 1.0
        distance += (p - closestPoint.point).norm
        (id, closestPoint.point, w)
      }
      (corr, distance / template.pointSet.numberOfPoints)
    }
  }

}
