package other.utils

import scalismo.common.UnstructuredPoints
import scalismo.geometry.{_2D, _3D, Point}
import scalismo.registration.LandmarkRegistration

trait Registrator[D] {
  def register(points: Seq[(Point[D], Point[D])]): UnstructuredPoints[D]
}

object PoseRegistrator {

  implicit object RigidRegistrator3D extends Registrator[_3D] {
    override def register(points: Seq[(Point[_3D], Point[_3D])]): UnstructuredPoints[_3D] = {
      val t = LandmarkRegistration.rigid3DLandmarkRegistration(points, Point(0, 0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }

  implicit object AffineRegistrator3D extends Registrator[_3D] {
    override def register(points: Seq[(Point[_3D], Point[_3D])]): UnstructuredPoints[_3D] = {
      val t = LandmarkRegistration.similarity3DLandmarkRegistration(points, Point(0, 0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }

  implicit object RigidRegistrator2D extends Registrator[_2D] {
    override def register(points: Seq[(Point[_2D], Point[_2D])]): UnstructuredPoints[_2D] = {
      val t = LandmarkRegistration.rigid2DLandmarkRegistration(points, Point(0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }

  implicit object AffineRegistrator2D extends Registrator[_2D] {
    override def register(points: Seq[(Point[_2D], Point[_2D])]): UnstructuredPoints[_2D] = {
      val t = LandmarkRegistration.similarity2DLandmarkRegistration(points, Point(0, 0))
      UnstructuredPoints(points.map { case (p, _) => t.f(p) }.toIndexedSeq)
    }
  }

}
