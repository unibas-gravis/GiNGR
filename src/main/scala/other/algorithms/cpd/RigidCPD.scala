package other.algorithms.cpd

import api.registration.utils.PointSequenceConverter
import breeze.linalg.{*, det, diag, norm, sum, svd, tile, trace, Axis, DenseMatrix, DenseVector}
import breeze.numerics.{abs, pow}
import scalismo.common.Vectorizer
import scalismo.geometry.{NDSpace, Point}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Rigid algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class RigidCPD[D: NDSpace](
  val targetPoints: Seq[Point[D]],
  val cpd: CPDFactory[D]
)(implicit
  val vectorizer: Vectorizer[Point[D]],
  dataConverter: PointSequenceConverter[D]
) {
  import cpd._
  val N: Int = targetPoints.length
  val target: DenseMatrix[Double] = dataConverter.toMatrix(targetPoints)(vectorizer)

  /** Initialize sigma2 - formula in paper fig. 4
    * @param Y
    * @param X
    * @return
    */
  private def initializeGaussianKernel(Y: Seq[Point[D]], X: Seq[Point[D]]): Double = {
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

  def Registration(max_iteration: Int, tolerance: Double = 0.001): Seq[Point[D]] = {
    val sigmaInit = initializeGaussianKernel(templatePoints, targetPoints)

    val fit = (0 until max_iteration).foldLeft((cpd.template, sigmaInit)) { (it, i) =>
      val currentSigma2 = it._2
      println(s"CPD, iteration: ${i}, variance: ${currentSigma2}")
      val iter = Iteration(target, it._1, it._2)
      val TY = iter._1
      val newSigma2 = iter._2
      val diff = abs(newSigma2 - currentSigma2)
      if (diff < tolerance) {
        println("Converged")
        return dataConverter.toPointSequence(TY)(vectorizer)
      } else {
        iter
      }
    }
    dataConverter.toPointSequence(fit._1)(vectorizer)
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
    val c = w / (1 - w) * math.pow((2.0 * math.Pi * sigma2), dim.toDouble / 2.0) * (M.toDouble / N.toDouble)
    val denRow = DenseMatrix(sum(P, Axis._0).t)
    val den = tile(denRow, M, 1) + c

    P /:/ den
  }

  def Maximization(X: DenseMatrix[Double], Y: DenseMatrix[Double], P: DenseMatrix[Double], sigma2: Double): (DenseMatrix[Double], Double) = {
    // Update transform
    val P1: DenseVector[Double] = sum(P, Axis._1)
    val Pt1 = sum(P, Axis._0)
    val Np = sum(P1)

    val muX = 1.0 / Np * X.t * P.t * DenseVector.ones[Double](P.rows)
    val muY = 1.0 / Np * Y.t * P1

    val Xhat = X - DenseVector.ones[Double](N) * muX.t
    val Yhat = Y - DenseVector.ones[Double](M) * muY.t

    val A = Xhat.t * P.t * Yhat
    val svd.SVD(u, _, v) = svd(A)
    val C = DenseVector.ones[Double](dim)
    C(dim - 1) = det(u * v.t)

    val R = u * diag(C) * v
    val s = trace(A.t * R) / trace(Yhat.t * diag(P1) * Yhat)
    val s1 = trace(Xhat.t * diag(Pt1) * Xhat)
    val s2 = s * trace(A.t * R)
    val t = muX - s * R * muY
    val updatedSigma2 = 1 / (Np * dim) * (s1 - s2)
    val TY = s * Y * R.t + DenseVector.ones[Double](M) * t.t

    (TY, updatedSigma2)
  }

}
