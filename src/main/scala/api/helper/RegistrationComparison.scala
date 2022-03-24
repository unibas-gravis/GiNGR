package api.helper

import scalismo.geometry._3D
import scalismo.mesh.{MeshMetrics, TriangleMesh, TriangleMesh3D}

object RegistrationComparison {

  def maxDistance(m1: TriangleMesh[_3D], m2: TriangleMesh[_3D]): Double = {
    def allDistsBetweenMeshes(mm1: TriangleMesh[_3D], mm2: TriangleMesh[_3D]): Iterator[Double] = {
      for (ptM1 <- mm1.pointSet.points) yield {
        val cpM2 = mm2.operations.closestPointOnSurface(ptM1).point
        (ptM1 - cpM2).norm
      }
    }

    val d1 = allDistsBetweenMeshes(m1, m2)
    d1.max
  }

  def evaluateReconstruction2GroundTruth(id: String, reconstruction: TriangleMesh3D, groundTruth: TriangleMesh3D): (Double, Double, Double) = {
    val avgDist2Surf = MeshMetrics.avgDistance(reconstruction, groundTruth)

    val hausdorffDistance = MeshMetrics.hausdorffDistance(reconstruction, groundTruth)
    val md = maxDistance(reconstruction, groundTruth)
    println(s"ID: ${id} average2surface: ${avgDist2Surf} max: ${md}, hausdorff: ${hausdorffDistance}")
    (avgDist2Surf, md, hausdorffDistance)
  }

  def evaluateReconstruction2GroundTruthDouble(id: String, reconstruction: TriangleMesh3D, groundTruth: TriangleMesh3D): (Double, Double) = {
    val avgDist2Surf = (MeshMetrics.avgDistance(reconstruction, groundTruth) + MeshMetrics.avgDistance(groundTruth, reconstruction)) / 2.0

    val hausdorffDistance = MeshMetrics.hausdorffDistance(reconstruction, groundTruth)
    println(s"ID: ${id} average2surface: ${avgDist2Surf} hausdorff: ${hausdorffDistance}")
    (avgDist2Surf, hausdorffDistance)
  }

  private def avgDistanceBoundaryAware(m1: TriangleMesh[_3D], m2: TriangleMesh[_3D]): (Double, Double) = {

    val pointsOnSample = m1.pointSet.points
    val dists = for (p <- pointsOnSample) yield {
      val pTarget = m2.operations.closestPointOnSurface(p).point
      val pTargetId = m2.pointSet.findClosestPoint(pTarget).id
      if (m2.operations.pointIsOnBoundary(pTargetId)) None
      else Option((pTarget - p).norm)
    }
    val filteredDists = dists.toIndexedSeq.filter(f => f.nonEmpty).flatten
    (filteredDists.sum / filteredDists.size, filteredDists.max)
  }

  def evaluateReconstruction2GroundTruthBoundaryAware(id: String, reconstruction: TriangleMesh3D, groundTruth: TriangleMesh3D): (Double, Double) = {
    val (avgDist2Surf1, maxDist2Surf1) = avgDistanceBoundaryAware(reconstruction, groundTruth)
    val (avgDist2Surf2, maxDist2Surf2) = avgDistanceBoundaryAware(groundTruth, reconstruction)
    val avgDist2Surf = (avgDist2Surf1 + avgDist2Surf2) / 2.0
    val maxDist2Surf = math.max(maxDist2Surf1, maxDist2Surf2)
    println(s"ID: ${id} average2surface: ${avgDist2Surf} max: ${maxDist2Surf}")
    (avgDist2Surf, maxDist2Surf)
  }

}
