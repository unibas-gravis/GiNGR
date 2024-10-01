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

import scalismo.common.UnstructuredPoints
import scalismo.common.interpolation.TriangleMeshInterpolator3D
import scalismo.geometry.{_3D, EuclideanVector}
import scalismo.kernels.{DiagonalKernel, GaussianKernel}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, PointDistributionModel}

object GPMMHelper {
  private def maximumPointDistance(template: UnstructuredPoints[_3D]): Double = {
    val p = template.points.toSeq
    p.flatMap { p1 =>
      p.map { p2 => (p1 - p2).norm }
    }.max
  }

  private def minimumPointDistance(template: UnstructuredPoints[_3D]): Double = {
    template.points.toSeq.map { p => (template.findNClosestPoints(p, 2).last.point - p).norm }.min
  }

  def automaticGPMMfromTemplate(
    template: TriangleMesh[_3D],
    relativeTolerance: Double = 0.1
  ): PointDistributionModel[_3D, TriangleMesh] = {
    println("Constructing GPMM from template")
    val maxDist = maximumPointDistance(template.pointSet)
    val minDist = minimumPointDistance(template.pointSet)
    val sigma1 = maxDist / 4
    val sigma2 = maxDist / 8
    val sigma3 = minDist * 5
    val scale1 = sigma1 / 2
    val scale2 = sigma2 / 2
    val scale3 = sigma3 / 2

    println(s"Maximum distance: ${maxDist}, minimum distance: ${minDist}")
    val k = DiagonalKernel(GaussianKernel[_3D](sigma1) * scale1, 3) +
      DiagonalKernel(GaussianKernel[_3D](sigma2) * scale2, 3) +
      DiagonalKernel(GaussianKernel[_3D](sigma3) * scale3, 3)

    val gp = GaussianProcess[_3D, EuclideanVector[_3D]](k)

    val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(
      template,
      gp,
      relativeTolerance = relativeTolerance,
      interpolator = TriangleMeshInterpolator3D[EuclideanVector[_3D]]()
    )
    println(s"GPMM rank: ${lowRankGP.rank}")

    PointDistributionModel[_3D, TriangleMesh](template, lowRankGP)
  }
}
