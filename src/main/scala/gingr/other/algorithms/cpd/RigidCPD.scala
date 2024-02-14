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

package gingr.other.algorithms.cpd

import gingr.api.registration.utils.PointSequenceConverter
import breeze.linalg.{Axis, DenseMatrix, DenseVector, det, diag, norm, svd, tile, trace, *}
import breeze.numerics.{abs, pow}
import scalismo.common.Vectorizer
import scalismo.geometry.{Point, _3D}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Rigid algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class RigidCPD(
    val targetPoints: Seq[Point[_3D]],
    val cpd: CPDFactory
)(implicit
    val vectorizer: Vectorizer[Point[_3D]]
) {
  import cpd._
  val N: Int                      = targetPoints.length
  val target: DenseMatrix[Double] = PointSequenceConverter.toMatrix(targetPoints)(vectorizer)

  /** Initialize sigma2 - formula in paper fig. 4
    * @param Y
    * @param X
    * @return
    */
  private def initializeGaussianKernel(Y: Seq[Point[_3D]], X: Seq[Point[_3D]]): Double = {
    val M   = Y.length
    val N   = X.length
    val dim = vectorizer.dim
    val s: Double = (0 until M).flatMap { m =>
      (0 until N).map { n =>
        (Y(m) - X(n)).norm2
      }
    }.sum
    s / (dim * N * M)
  }

  def Registration(max_iteration: Int, tolerance: Double = 0.001): Seq[Point[_3D]] = {
    val sigmaInit = initializeGaussianKernel(templatePoints, targetPoints)

    var fit = (cpd.template, sigmaInit)
    var i = 0
    var converged = false

    while (i < max_iteration && !converged) {
      val currentSigma2 = fit._2
      println(s"CPD, iteration: ${i}, variance: ${currentSigma2}")
      val iter = Iteration(target, fit._1, fit._2)
      val TY = iter._1
      val newSigma2 = iter._2
      val diff = abs(newSigma2 - currentSigma2)
      if (diff < tolerance) {
        println("Converged")
        fit = (TY, newSigma2)
        converged = true
      } else {
        fit = (TY, newSigma2)
        i += 1
      }
    }

    PointSequenceConverter.toPointSequence(fit._1)(vectorizer)
  }

  def Iteration(X: DenseMatrix[Double], Y: DenseMatrix[Double], sigma2: Double): (DenseMatrix[Double], Double) = {
    val P = Expectation(X, Y, sigma2)
    Maximization(X, Y, P, sigma2)
  }

  def Expectation(X: DenseMatrix[Double], Y: DenseMatrix[Double], sigma2: Double): DenseMatrix[Double] = {
    // TODO: Do matrix substraction with broadcasting instead
    val P: DenseMatrix[Double] = Y(*, ::).map { m =>
      val vec = X(*, ::).map { n =>
        val y = m.copy
        val x = n.copy
        math.exp(-pow(norm(x - y), 2) / (2 * sigma2))
      }
      vec
    }
    val c      = w / (1 - w) * math.pow((2.0 * math.Pi * sigma2), dim.toDouble / 2.0) * (M.toDouble / N.toDouble)
    val denRow = DenseMatrix(breeze.linalg.sum(P, Axis._0).t)
    val den    = tile(denRow, M, 1) + c

    P /:/ den
  }

  def Maximization(
      X: DenseMatrix[Double],
      Y: DenseMatrix[Double],
      P: DenseMatrix[Double],
      sigma2: Double
  ): (DenseMatrix[Double], Double) = {
    // Update transform
    val P1: DenseVector[Double] = breeze.linalg.sum(P, Axis._1)
    val Pt1                     = breeze.linalg.sum(P, Axis._0)
    val Np                      = breeze.linalg.sum(P1)

    val muX = 1.0 / Np * X.t * P.t * DenseVector.ones[Double](P.rows)
    val muY = 1.0 / Np * Y.t * P1

    val Xhat = X - DenseVector.ones[Double](N) * muX.t
    val Yhat = Y - DenseVector.ones[Double](M) * muY.t

    val A                = Xhat.t * P.t * Yhat
    val svd.SVD(u, _, v) = svd(A)
    val C                = DenseVector.ones[Double](dim)
    C(dim - 1) = det(u * v.t)

    val R             = u * diag(C) * v
    val s             = trace(A.t * R) / trace(Yhat.t * diag(P1) * Yhat)
    val s1            = trace(Xhat.t * diag(Pt1) * Xhat)
    val s2            = s * trace(A.t * R)
    val t             = muX - s * R * muY
    val updatedSigma2 = 1 / (Np * dim) * (s1 - s2)
    val TY            = s * Y * R.t + DenseVector.ones[Double](M) * t.t

    (TY, updatedSigma2)
  }

}
