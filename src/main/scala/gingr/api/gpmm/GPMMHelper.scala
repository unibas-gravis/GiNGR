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

package gingr.api.gpmm

import breeze.linalg.sum
import scalismo.common.DiscreteField.ScalarMeshField
import scalismo.common._
import scalismo.common.interpolation.NearestNeighborInterpolator
import scalismo.geometry.{_3D, EuclideanVector}
import scalismo.kernels.{DiagonalKernel, GaussianKernel, MatrixValuedPDKernel}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{GaussianProcess, LowRankGaussianProcess, PointDistributionModel}

trait GPMM[DDomain[_3D] <: DiscreteDomain[_3D]] {
  def construct(
    reference: DDomain[_3D],
    kernel: MatrixValuedPDKernel[_3D],
    relativeTolerance: Double = 0.01
  ): PointDistributionModel[_3D, DDomain]
}

object GPMM {

  implicit object gpmm3Dtriangle extends GPMM[TriangleMesh] {
    override def construct(
      reference: TriangleMesh[_3D],
      kernel: MatrixValuedPDKernel[_3D],
      relativeTolerance: Double
    ): PointDistributionModel[_3D, TriangleMesh] = {
      val gp = GaussianProcess[_3D, EuclideanVector[_3D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(
        reference,
        gp,
        relativeTolerance = relativeTolerance,
        interpolator = NearestNeighborInterpolator()
      )
      PointDistributionModel[_3D, TriangleMesh](reference, lowRankGP)
    }
  }

  implicit object gpmm3Dpoints extends GPMM[UnstructuredPointsDomain] {
    override def construct(
      reference: UnstructuredPointsDomain[_3D],
      kernel: MatrixValuedPDKernel[_3D],
      relativeTolerance: Double
    ): PointDistributionModel[_3D, UnstructuredPointsDomain] = {
      val gp = GaussianProcess[_3D, EuclideanVector[_3D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(
        reference,
        gp,
        relativeTolerance = relativeTolerance,
        interpolator = NearestNeighborInterpolator()
      )
      PointDistributionModel[_3D, UnstructuredPointsDomain](reference, lowRankGP)
    }
  }

}

case class PointSetHelper[DDomain[_3D] <: DiscreteDomain[_3D]](reference: DDomain[_3D]) {

  // TODO: Need to compute these valus faster for large pointsets
  def maximumPointDistance(pointSet: PointSet[_3D]): Double = {
    val p = pointSet.points.toSeq
    p.flatMap { p1 =>
      p.map { p2 => (p1 - p2).norm }
    }.max
  }

  def minimumPointDistance(pointSet: PointSet[_3D]): Double = {
    pointSet.points.toSeq.map { p => (pointSet.findNClosestPoints(p, 2).last.point - p).norm }.min
  }

  def boundingBoxSize(pointSet: PointSet[_3D]): Double = {
    pointSet.boundingBox.volume
  }
}

case class GaussianKernelParameters(sigma: Double, scaling: Double)

case class GPMMTriangleMesh3D(reference: TriangleMesh[_3D], relativeTolerance: Double)(implicit
  gpmm: GPMM[TriangleMesh]
) {
  def Gaussian(sigma: Double, scaling: Double): PointDistributionModel[_3D, TriangleMesh] = {
    val kernel = GaussianKernel[_3D](sigma) * scaling
    gpmm.construct(reference, DiagonalKernel(kernel, 3), relativeTolerance)
  }
  def GaussianDot(sigma: Double, scaling: Double): PointDistributionModel[_3D, TriangleMesh] = {
    val kernel = DotProductKernel(GaussianKernel[_3D](sigma), 1.0) * scaling
    gpmm.construct(reference, DiagonalKernel(kernel, 3), relativeTolerance)
  }
  def GaussianSymmetry(sigma: Double, scaling: Double): PointDistributionModel[_3D, TriangleMesh] = {
    val kernel = GaussianKernel[_3D](sigma) * scaling
    val symmKernel = KernelHelper.symmetrizeKernel(kernel)
    gpmm.construct(reference, symmKernel, relativeTolerance)
  }

  def GaussianMixture(pars: Seq[GaussianKernelParameters]): PointDistributionModel[_3D, TriangleMesh] = {
    val kernels = pars.map(p => GaussianKernel[_3D](p.sigma) * p.scaling)
    val kernel = kernels.tail.foldLeft(kernels.head)(_ + _)
    gpmm.construct(reference, DiagonalKernel(kernel, 3), relativeTolerance)
  }

  def AutomaticGaussian(): PointDistributionModel[_3D, TriangleMesh] = {
    val refPoints = reference.pointSet
    val psHelper = PointSetHelper[TriangleMesh](reference)
    val maxDist = psHelper.maximumPointDistance(refPoints)
    GaussianMixture(
      Seq(
        GaussianKernelParameters(maxDist / 4.0, maxDist / 8.0),
        GaussianKernelParameters(maxDist / 8.0, maxDist / 16.0)
      )
    )
  }

  def InverseLaplacian(scaling: Double): PointDistributionModel[_3D, TriangleMesh] = {
    val m = LaplacianHelper(reference).inverseLaplacianMatrix()
    val kernel = DiagonalKernel(LookupKernel(reference, m) * scaling, 3)
    gpmm.construct(reference, kernel, relativeTolerance)
  }

  def InverseLaplacianDot(scaling: Double, gamma: Double): PointDistributionModel[_3D, TriangleMesh] = {
    val m = LaplacianHelper(reference).inverseLaplacianMatrix()
    val kernel = DiagonalKernel(DotProductKernel(LookupKernel(reference, m), gamma) * scaling, 3)
    gpmm.construct(reference, kernel, relativeTolerance)
  }

  def computeDistanceAbsMesh(
    model: PointDistributionModel[_3D, TriangleMesh],
    lmId: PointId
  ): ScalarMeshField[Double] = {
    val dist: Array[Double] = model.reference.pointSet.pointIds.toSeq.map { pid =>
      val cov = model.gp.cov(lmId, pid)
      sum((0 until 3).map(i => math.abs(cov(i, i))))
    }.toArray
    ScalarMeshField[Double](model.reference, dist)
  }

}
