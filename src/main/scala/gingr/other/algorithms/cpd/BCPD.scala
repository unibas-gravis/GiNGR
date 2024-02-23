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
import breeze.linalg.{det, diag, kron, pinv, sum, svd, tile, trace, Axis, DenseMatrix, DenseVector}
import breeze.numerics.{abs, digamma}
import scalismo.common.Vectorizer
import scalismo.geometry.{_3D, Point}
import scalismo.kernels.PDKernel
import scalismo.statisticalmodel.MultivariateNormalDistribution

// Similarity transformation parameters
case class similarityTransformationParameters(
  sigma: DenseMatrix[Double],
  s: Double,
  R: DenseMatrix[Double],
  t: DenseVector[Double],
  sigma2: Double,
  alpha: DenseVector[Double]
)

/*
 Implementation of Bayesian Coherent Point Drift (BCPD)
 Paper: https://ieeexplore.ieee.org/stamp/stamp.jsp?arnumber=8985307
 */
class BCPD(
  val templatePoints: Seq[Point[_3D]],
  val targetPoints: Seq[Point[_3D]],
  val w: Double, // Outlier, [0,1]
  val lambda: Double, // Noise scaling, R+
  val gamma: Double, // Initial noise scaling, R+
  val k: Double,
  val kernel: PDKernel[_3D] // Positive semi-def kernel
)(implicit
  val vectorizer: Vectorizer[Point[_3D]]
) {
  require(0.0 <= w && w <= 1.0)
  require(lambda > 0)
  require(gamma > 0)

  val M: Int = templatePoints.length // num of reference points
  val N: Int = targetPoints.length // num of target points
  val dim: Int = vectorizer.dim // dimension
  val G: DenseMatrix[Double] = initializeKernelMatrixG(templatePoints, kernel)
  val Ginv: DenseMatrix[Double] = pinv(G)
  val GinvLambda: DenseMatrix[Double] = Ginv.*(lambda)
  val X: DenseVector[Double] = PointSequenceConverter.toVector(targetPoints)
  val Y: DenseVector[Double] = PointSequenceConverter.toVector(templatePoints)

  // Helper unit matrices
  val D1Vec: DenseMatrix[Double] = DenseVector.ones[Double](dim).toDenseMatrix
  val Dmat: DenseMatrix[Double] = DenseMatrix.eye[Double](dim)

  /**
   * Initialize G matrix - formula in paper fig. 4
   *
   * @param points
   *   // * @param kernel
   * @return
   */
  private def initializeKernelMatrixG(
    points: Seq[Point[_3D]],
    kernel: PDKernel[_3D]
  ): DenseMatrix[Double] = {
    val G: DenseMatrix[Double] = DenseMatrix.zeros[Double](M, M)
    (0 until M).map { i =>
      (0 until M).map { j =>
        G(i, j) = kernel(points(i), points(j))
      }
    }
    G
  }

  /**
   * Initialize sigma2 - formula in paper fig. 4
   *
   * @param Y
   * @param X
   * @return
   */
  private def computeSigma2init(Y: Seq[Point[_3D]], X: Seq[Point[_3D]]): Double = {
    val M = Y.length
    val N = X.length
    val dim = vectorizer.dim
    val s: Double = (0 until M).flatMap { m =>
      (0 until N).map { n =>
        (Y(m) - X(n)).norm2
      }
    }.sum
    s / (dim * N * M)
  }

  private def printTransformation(pars: similarityTransformationParameters) = {
    println(s"Final transformation, s: ${pars.s}, t: ${pars.t}, \n R: ${pars.R}")
  }

  def Registration(max_iteration: Int, tolerance: Double = 0.000001): Seq[Point[_3D]] = {
    val sigma2Init = gamma * computeSigma2init(templatePoints, targetPoints)
    val simParsInit = similarityTransformationParameters(
      sigma = DenseMatrix.eye[Double](M),
      s = 1.0,
      R = DenseMatrix.eye[Double](dim),
      t = DenseVector.zeros[Double](dim),
      sigma2 = sigma2Init,
      alpha = DenseVector.ones[Double](M) / M.toDouble
    )

    var fit = (templatePoints, simParsInit)
    var i = 0
    var converged = false

    while (i < max_iteration && !converged) {
      val currentPars = fit._2
      println(s"CPD, iteration: ${i}, variance: ${currentPars.sigma2}")
      val iter = Iteration(fit._1, fit._2)
      val TY = iter._1
      val newPars = iter._2
      val diff = math.abs(newPars.sigma2 - currentPars.sigma2)

      if (diff < tolerance) {
        println("Converged")
        printTransformation(iter._2)
        converged = true
      } else {
        fit = iter
        i += 1
      }
    }

    printTransformation(fit._2)
    fit._1
  }

  // TranslationAfterScalingAfterRotation
  private def vectorTransform(v: DenseVector[Double], pars: similarityTransformationParameters): DenseVector[Double] = {
    val tmp = (kron(DenseMatrix.eye[Double](M), pars.R) * v) +
      (kron(DenseVector.ones[Double](M).toDenseMatrix, pars.t.toDenseMatrix).toDenseVector)
    tmp.map(p => p * pars.s)
  }

  // RotationAfterScalingAfterTranslation
  private def vectorInvTransform(
    v: DenseVector[Double],
    pars: similarityTransformationParameters
  ): DenseVector[Double] = {
    val sinv = 1.0 / pars.s
    val tmp = (v + (kron(DenseVector.ones[Double](M).toDenseMatrix, pars.t.toDenseMatrix * (-1.0)).toDenseVector))
    (kron(DenseMatrix.eye[Double](M), pinv(pars.R))) *
      (tmp.map(p => p * sinv))
  }

  private def computeP(yPoints: Seq[Point[_3D]], pars: similarityTransformationParameters): DenseMatrix[Double] = {
    val Phi = DenseMatrix.zeros[Double](M, N)
    (0 until M).map { m =>
      val mvnd =
        MultivariateNormalDistribution(vectorizer.vectorize(yPoints(m)), DenseMatrix.eye[Double](dim) * pars.sigma2)
      val e = math.exp(-pars.s / (2 * pars.sigma2) * trace(DenseMatrix.eye[Double](dim).*(pars.sigma(m, m))))
      (0 until N).map { n =>
        Phi(m, n) = mvnd.pdf(vectorizer.vectorize(targetPoints(n))) * e * pars.alpha(m)
      }
    }
    val Pinit = Phi.copy * (1 - w)
    // TODO: Fix outlier distribution (section 4.3.4)
    val c = w * 1.0 / N.toDouble // * pout(x_n) see 4.3.4 (1/V)
    val denRow = DenseMatrix(sum(Pinit, Axis._0).t) * (1 - w) + c
    val den = tile(denRow, M, 1)

    Pinit /:/ den
  }

  /**
   * One iteration of BCPD
   *
   * @param YhatPoints
   *   // template points (M)
   * @param pars
   *   // Similarity transformation parameters
   * @return
   */
  def Iteration(
    YhatPoints: Seq[Point[_3D]],
    pars: similarityTransformationParameters
  ): (Seq[Point[_3D]], similarityTransformationParameters) = {
    // Update P and related terms
    val P = computeP(YhatPoints, pars)

    val v = sum(P, Axis._1) // R^M Estimated number of target points matched with each source point
    val v_ = sum(P, Axis._0).t.copy // R^N Posterior probability that x_n is a non-outlier
    val Nhat = sum(v_)

    val Pkron = kron(P, Dmat)
    val vkron = kron(v.toDenseMatrix, D1Vec).toDenseVector
    val v_kron = kron(v_.toDenseMatrix, D1Vec).toDenseVector
    val xhat = pinv(diag(vkron)) * Pkron * X
    val xhatTinv = vectorInvTransform(xhat, pars)

    // Update Local deformations
    val s2divsigma2 = (math.pow(pars.s, 2) / pars.sigma2)
    val SigmaInv = GinvLambda + diag(v) * s2divsigma2
    val Sigma = pinv(SigmaInv)
    val SigmaKron = kron(Sigma, Dmat)
    val vhat = SigmaKron.*(s2divsigma2) * diag(vkron) * (xhatTinv - Y)
    val uhat = Y + vhat

    val alpha = v.map(f => math.exp(digamma(k + f) - digamma(k * M + Nhat)))

    // Update Similarity transform
    val xMean = sum((0 until M).map(m => xhat(m * dim until m * dim + dim).*(v(m)))) / Nhat
    val uMean = sum((0 until M).map(m => uhat(m * dim until m * dim + dim).*(v(m)))) / Nhat
    val sigma2bar = sum((0 until M).map(m => v(m) * Sigma(m, m))) / Nhat

    val Sxu = sum((0 until M).map { m =>
      val xm = xhat(m * dim until m * dim + dim)
      val um = uhat(m * dim until m * dim + dim)
      (xm - xMean).*(v(m)) * (um - uMean).t
    }) / Nhat

    val Suu = sum((0 until M).map { m =>
      val um = uhat(m * dim until m * dim + dim)
      (um - uMean).*(v(m)) * (um - uMean).t + DenseMatrix.eye[Double](dim) * sigma2bar
    }) / Nhat

    val svd.SVD(phi, _, psiT) = svd(Sxu)
    val diagphipsi = DenseVector.ones[Double](dim)
    diagphipsi(dim - 1) = det(phi * psiT)

    val R = phi * diag(diagphipsi) * psiT
    val s = trace(R * Sxu) / trace(Suu)
    val t = xMean - R.*(s) * uMean

    val newYhat = vectorTransform(Y + vhat, pars)

    val sXX = X.t * diag(v_kron) * X
    val sXY = X.t * Pkron.t * newYhat
    val sYY = newYhat.t * diag(vkron) * newYhat
    val sC = pars.sigma2 * sigma2bar
    val newSigma2 =
      (sXX - 2 * sXY + sYY + sC) / (Nhat * dim) // TODO: Should sC be added to all or included in the parenthesis as currently?

    val newPars =
      similarityTransformationParameters(sigma = Sigma, s = s, R = R, t = t, sigma2 = newSigma2, alpha = alpha)

    (PointSequenceConverter.toPointSequence(newYhat), newPars)
  }
}
