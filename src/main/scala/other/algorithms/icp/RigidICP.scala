package other.algorithms.icp

import breeze.numerics.abs
import other.utils.Registrator
import scalismo.common.{PointId, UnstructuredPoints, Vectorizer}
import scalismo.geometry.{NDSpace, Point}

private[icp] class RigidICP[D: NDSpace](
  val targetPoints: UnstructuredPoints[D],
  val icp: ICPFactory[D]
)(implicit
  val vectorizer: Vectorizer[Point[D]],
  registrator: Registrator[D]
) {
  require(vectorizer.dim == 2 || vectorizer.dim == 3)

  def Registration(max_iteration: Int, tolerance: Double = 0.001): UnstructuredPoints[D] = {
    val sigmaInit = 0.0

    val fit = (0 until max_iteration).foldLeft((icp.templatePoints, sigmaInit)) { (it, i) =>
      val iter = Iteration(it._1, targetPoints)
      val distance = iter._2
      println(s"ICP, iteration: ${i}, distance: ${distance}")
      val TY = iter._1
      val diff = abs(distance - it._2)
      if (diff < tolerance) {
        println("Converged")
        return TY
      } else {
        iter
      }
    }
    fit._1
  }

  private def attributeCorrespondences(template: UnstructuredPoints[D], target: UnstructuredPoints[D]): (Seq[(Point[D], Point[D])], Double) = {
    val ptIds = template.pointIds.toIndexedSeq
    var distance = 0.0
    val corr = ptIds.map { id: PointId =>
      val pt = template.point(id)
      val closestPointOnMesh2 = target.findClosestPoint(pt).point
      distance += (pt - closestPointOnMesh2).norm
      (pt, closestPointOnMesh2)
    }
    (corr, distance / ptIds.length)
  }

  def Iteration(template: UnstructuredPoints[D], target: UnstructuredPoints[D]): (UnstructuredPoints[D], Double) = {
    val (correspondences, distance) = attributeCorrespondences(template, target)
    val registeredPoints = registrator.register(correspondences)
    (registeredPoints, distance)
  }

}
