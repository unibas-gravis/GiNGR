package api.registration.utils

import java.io.File

import scalismo.common.interpolation.TriangleMeshInterpolator3D
import scalismo.common.{EuclideanSpace, Field, UnstructuredPoints}
import scalismo.geometry.{EuclideanVector, Point, _3D}
import scalismo.io.MeshIO
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, PointDistributionModel}
import scalismo.ui.api.ScalismoUI

object GPMMHelper {
  // Todo: use implicit to build any kind of model based on the pointSet
  private def maximumPointDistance(template: UnstructuredPoints[_3D]): Double = {
    val p = template.points.toSeq
    p.flatMap { p1 =>
      p.map { p2 => (p1 - p2).norm }
    }.max
  }

  private def minimumPointDistance(template: UnstructuredPoints[_3D]): Double = {
    template.points.toSeq.map { p => (template.findNClosestPoints(p,2).last.point - p).norm }.min
  }

  def automaticGPMMfromTemplate(template: TriangleMesh[_3D], relativeTolerance: Double = 0.1): PointDistributionModel[_3D, TriangleMesh] = {
    println("Constructing GPMM from template")
    val maxDist = maximumPointDistance(template.pointSet)
    val minDist = minimumPointDistance(template.pointSet)
    val sigma1 = maxDist/4
    val sigma2 = maxDist/8
    val sigma3 = minDist*5
    val scale1 = sigma1/2
    val scale2 = sigma2/2
    val scale3 = sigma3/2

    println(s"Maximum distance: ${maxDist}, minimum distance: ${minDist}")
    val k = DiagonalKernel(GaussianKernel[_3D](sigma1) * scale1, 3) +
      DiagonalKernel(GaussianKernel[_3D](sigma2) * scale2, 3) +
      DiagonalKernel(GaussianKernel[_3D](sigma3) * scale3, 3)

    val gp = GaussianProcess[_3D, EuclideanVector[_3D]](k)

    val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(template, gp, relativeTolerance = relativeTolerance, interpolator = TriangleMeshInterpolator3D[EuclideanVector[_3D]]())
    println(s"GPMM rank: ${lowRankGP.rank}")

    PointDistributionModel[_3D, TriangleMesh](template, lowRankGP)
  }
}

//object gpmmTest extends App {
//  scalismo.initialize()
//
//  val template = MeshIO.readMesh(new File("data/femur_reference.stl")).get
//  val gpmm = GPMMHelper.automaticGPMMfromTemplate(template)
//    val ui = ScalismoUI()
//    ui.show(gpmm, "model")
//}