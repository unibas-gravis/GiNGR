package api.gpmm

import breeze.linalg.{sum, DenseMatrix, DenseVector}
import scalismo.common.EuclideanSpace
import scalismo.geometry.{_3D, Point}
import scalismo.kernels.{DiagonalKernel, MatrixValuedPDKernel, PDKernel}
import scalismo.mesh.TriangleMesh

object KernelHelper {
  def symmetrizeKernel(kernel: PDKernel[_3D]): MatrixValuedPDKernel[_3D] = {
    val xmirrored = xMirroredKernel3D(kernel)
    val k1 = DiagonalKernel(kernel, 3)
    val k2 = DiagonalKernel(xmirrored * -1f, xmirrored, xmirrored)
    k1 + k2
  }
}

case class xMirroredKernel3D(kernel: PDKernel[_3D]) extends PDKernel[_3D] {
  override def domain = kernel.domain
  override def k(x: Point[_3D], y: Point[_3D]) = kernel(Point(x(0) * -1.0, x(1), x(2)), y)
}

case class DotProductKernel[D](kernel: PDKernel[D], gamma: Double) extends PDKernel[D] {
  override def domain: EuclideanSpace[D] = EuclideanSpace[D]

  override def k(x: Point[D], y: Point[D]): Double = {
//    val xh = DenseMatrix(x.toArray :+ gamma)
//    val yh = DenseMatrix(y.toArray :+ gamma)
//    val inner = DenseMatrix.eye[Double](x.dimensionality + 1) * kernel(x, y)
//    val out = xh * inner * yh.t
//    out(0, 0)
    x.toBreezeVector.dot(y.toBreezeVector)
  }
}

case class InverseMultiquadric[D](beta: Double) extends PDKernel[D] {
  val beta2: Double = beta * beta

  override def domain: EuclideanSpace[D] = EuclideanSpace[D]

  override def k(x: Point[D], y: Point[D]): Double = {
    val r = x - y
    1 / scala.math.sqrt(r.norm2 + beta2)
  }
}

case class RationalQuadratic[D](beta: Double) extends PDKernel[D] {
  val beta2: Double = beta * beta

  override def domain: EuclideanSpace[D] = EuclideanSpace[D]

  override def k(x: Point[D], y: Point[D]): Double = {
    val r = x - y
    val nn = r.norm2
    1 - (nn / (nn + beta2))
  }
}

case class LookupKernel[D](ref: TriangleMesh[D], m: DenseMatrix[Double]) extends PDKernel[D] {
  override def domain: EuclideanSpace[D] = EuclideanSpace[D]

  override def k(x: Point[D], y: Point[D]): Double = {
    val idx = ref.pointSet.findClosestPoint(x).id.id
    val idy = ref.pointSet.findClosestPoint(y).id.id
    m(idx, idy)
  }
}
