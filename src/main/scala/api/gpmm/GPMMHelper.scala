package api.gpmm

import breeze.linalg.{diag, sum, trace, DenseMatrix}
import scalismo.common.DiscreteField.ScalarMeshField
import scalismo.common.interpolation.{NearestNeighborInterpolator, TriangleMeshInterpolator3D}
import scalismo.common.{DiscreteDomain, PointId, PointSet, ScalarMeshField, UnstructuredPoints, UnstructuredPointsDomain}
import scalismo.geometry.{_1D, _2D, _3D, EuclideanVector, NDSpace}
import scalismo.kernels.{DiagonalKernel, GaussianKernel, MatrixValuedPDKernel}
import scalismo.mesh.TriangleMesh
import scalismo.numerics.PivotedCholesky
import scalismo.numerics.PivotedCholesky.NumberOfEigenfunctions
import scalismo.statisticalmodel.{DiscreteLowRankGaussianProcess, GaussianProcess, LowRankGaussianProcess, PointDistributionModel}

trait GPMM[D, DDomain[D] <: DiscreteDomain[D]] {
  def construct(reference: DDomain[D], kernel: MatrixValuedPDKernel[D], relativeTolerance: Double = 0.01): PointDistributionModel[D, DDomain]
}

object GPMM {

  implicit object gpmm3Dtriangle extends GPMM[_3D, TriangleMesh] {
    override def construct(reference: TriangleMesh[_3D], kernel: MatrixValuedPDKernel[_3D], relativeTolerance: Double): PointDistributionModel[_3D, TriangleMesh] = {
      val gp = GaussianProcess[_3D, EuclideanVector[_3D]](kernel)
      val lr = LowRankGaussianProcess
      //      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = TriangleMeshInterpolator3D())
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = NearestNeighborInterpolator())
      PointDistributionModel[_3D, TriangleMesh](reference, lowRankGP)
    }
  }

  implicit object gpmm2Dtriangle extends GPMM[_2D, TriangleMesh] {
    override def construct(reference: TriangleMesh[_2D], kernel: MatrixValuedPDKernel[_2D], relativeTolerance: Double): PointDistributionModel[_2D, TriangleMesh] = {
      val gp = GaussianProcess[_2D, EuclideanVector[_2D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = NearestNeighborInterpolator())
      PointDistributionModel[_2D, TriangleMesh](reference, lowRankGP)
    }
  }

  implicit object gpmm1Dpoints extends GPMM[_1D, UnstructuredPointsDomain] {
    override def construct(
      reference: UnstructuredPointsDomain[_1D],
      kernel: MatrixValuedPDKernel[_1D],
      relativeTolerance: Double): PointDistributionModel[_1D, UnstructuredPointsDomain] = {
      val gp = GaussianProcess[_1D, EuclideanVector[_1D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = NearestNeighborInterpolator())
      PointDistributionModel[_1D, UnstructuredPointsDomain](reference, lowRankGP)
    }
  }

  implicit object gpmm2Dpoints extends GPMM[_2D, UnstructuredPointsDomain] {
    override def construct(
      reference: UnstructuredPointsDomain[_2D],
      kernel: MatrixValuedPDKernel[_2D],
      relativeTolerance: Double): PointDistributionModel[_2D, UnstructuredPointsDomain] = {
      val gp = GaussianProcess[_2D, EuclideanVector[_2D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = NearestNeighborInterpolator())
      PointDistributionModel[_2D, UnstructuredPointsDomain](reference, lowRankGP)
    }
  }

  implicit object gpmm3Dpoints extends GPMM[_3D, UnstructuredPointsDomain] {
    override def construct(
      reference: UnstructuredPointsDomain[_3D],
      kernel: MatrixValuedPDKernel[_3D],
      relativeTolerance: Double): PointDistributionModel[_3D, UnstructuredPointsDomain] = {
      val gp = GaussianProcess[_3D, EuclideanVector[_3D]](kernel)
      val lowRankGP = LowRankGaussianProcess.approximateGPCholesky(reference, gp, relativeTolerance = relativeTolerance, interpolator = NearestNeighborInterpolator())
      PointDistributionModel[_3D, UnstructuredPointsDomain](reference, lowRankGP)
    }
  }

}

case class PointSetHelper[D, DDomain[D] <: DiscreteDomain[D]](reference: DDomain[D]) {

  // TODO: Need to compute these valus faster for large pointsets
  def maximumPointDistance(pointSet: PointSet[D]): Double = {
    val p = pointSet.points.toSeq
    p.flatMap { p1 =>
      p.map { p2 => (p1 - p2).norm }
    }.max
  }

  def minimumPointDistance(pointSet: PointSet[D]): Double = {
    pointSet.points.toSeq.map { p => (pointSet.findNClosestPoints(p, 2).last.point - p).norm }.min
  }

  def boundingBoxSize(pointSet: PointSet[D]): Double = {
    pointSet.boundingBox.volume
  }
}

case class GaussianKernelParameters(sigma: Double, scaling: Double)

case class GPMMTriangleMesh3D(reference: TriangleMesh[_3D], relativeTolerance: Double)(implicit gpmm: GPMM[_3D, TriangleMesh]) {
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
    val psHelper = PointSetHelper[_3D, TriangleMesh](reference)
    val maxDist = psHelper.maximumPointDistance(refPoints)
//    val minDist = psHelper.minimumPointDistance(refPoints)
    GaussianMixture(
      Seq(
        GaussianKernelParameters(maxDist / 4.0, maxDist / 8.0),
        GaussianKernelParameters(maxDist / 8.0, maxDist / 16.0)
//        GaussianKernelParameters(maxDist/16.0, maxDist/32.0),
      ))
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

  def computeDistanceAbsMesh(model: PointDistributionModel[_3D, TriangleMesh], lmId: PointId): ScalarMeshField[Double] = {
    val dist: Array[Double] = model.reference.pointSet.pointIds.toSeq.map { pid =>
      val cov = model.gp.cov(lmId, pid)
      sum((0 until 3).map(i => math.abs(cov(i, i))))
    }.toArray
    ScalarMeshField[Double](model.reference, dist)
  }

}
