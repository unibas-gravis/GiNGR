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

import breeze.linalg.DenseMatrix
import scalismo.common.{Domain, EuclideanSpace}
import scalismo.geometry.{Point, _3D}
import scalismo.kernels.{DiagonalKernel, MatrixValuedPDKernel, PDKernel}
import scalismo.mesh.TriangleMesh

object KernelHelper {
  def symmetrizeKernel(kernel: PDKernel[_3D]): MatrixValuedPDKernel[_3D] = {
    val xmirrored = xMirroredKernel3D(kernel)
    val k1        = DiagonalKernel(kernel, 3)
    val k2        = DiagonalKernel(xmirrored * -1f, xmirrored, xmirrored)
    k1 + k2
  }
}

case class xMirroredKernel3D(kernel: PDKernel[_3D]) extends PDKernel[_3D] {
  override def domain: Domain[_3D]                     = kernel.domain
  override def k(x: Point[_3D], y: Point[_3D]): Double = kernel(Point(x(0) * -1.0, x(1), x(2)), y)
}

case class DotProductKernel(kernel: PDKernel[_3D], gamma: Double) extends PDKernel[_3D] {
  override def domain: EuclideanSpace[_3D] = EuclideanSpace[_3D]

  override def k(x: Point[_3D], y: Point[_3D]): Double = {
//    val xh = DenseMatrix(x.toArray :+ gamma)
//    val yh = DenseMatrix(y.toArray :+ gamma)
//    val inner = DenseMatrix.eye[Double](x.dimensionality + 1) * kernel(x, y)
//    val out = xh * inner * yh.t
//    out(0, 0)
    x.toBreezeVector.dot(y.toBreezeVector)
  }
}

case class InverseMultiquadric[_3D](beta: Double) extends PDKernel[_3D] {
  val beta2: Double = beta * beta

  override def domain: EuclideanSpace[_3D] = EuclideanSpace[_3D]

  override def k(x: Point[_3D], y: Point[_3D]): Double = {
    val r = x - y
    1 / scala.math.sqrt(r.norm2 + beta2)
  }
}

case class RationalQuadratic(beta: Double) extends PDKernel[_3D] {
  val beta2: Double = beta * beta

  override def domain: EuclideanSpace[_3D] = EuclideanSpace[_3D]

  override def k(x: Point[_3D], y: Point[_3D]): Double = {
    val r  = x - y
    val nn = r.norm2
    1 - (nn / (nn + beta2))
  }
}

case class LookupKernel(ref: TriangleMesh[_3D], m: DenseMatrix[Double]) extends PDKernel[_3D] {
  override def domain: EuclideanSpace[_3D] = EuclideanSpace[_3D]

  override def k(x: Point[_3D], y: Point[_3D]): Double = {
    val idx = ref.pointSet.findClosestPoint(x).id.id
    val idy = ref.pointSet.findClosestPoint(y).id.id
    m(idx, idy)
  }
}
