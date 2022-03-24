package other.algorithms.cpd

import api.registration.utils.PointSequenceConverter
import breeze.linalg.{diag, inv, sum, trace, Axis, DenseMatrix, DenseVector, InjectNumericOps}
import scalismo.common._
import scalismo.geometry.{NDSpace, Point}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Affine transformation algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class AffineCPD[D: NDSpace](
  override val targetPoints: Seq[Point[D]],
  override val cpd: CPDFactory[D]
)(implicit
  vectorizer: Vectorizer[Point[D]],
  dataConverter: PointSequenceConverter[D]
) extends RigidCPD[D](targetPoints, cpd) {
  import cpd._
  override def Maximization(X: DenseMatrix[Double], Y: DenseMatrix[Double], P: DenseMatrix[Double], sigma2: Double): (DenseMatrix[Double], Double) = {
    // Update transform
    val P1: DenseVector[Double] = sum(P, Axis._1)
    val Pt1 = sum(P, Axis._0)
    val Np = sum(P1)

    val muX = 1.0 / Np * X.t * P.t * DenseVector.ones[Double](P.rows)
    val muY = 1.0 / Np * Y.t * P1

    val Xhat = X - DenseVector.ones[Double](N) * muX.t
    val Yhat = Y - DenseVector.ones[Double](M) * muY.t

    val B = Xhat.t * P.t * Yhat * inv(Yhat.t * diag(P1) * Yhat)
    val t = muX - B * muY

    val s1 = trace(Xhat.t * diag(Pt1) * Xhat)
    val s2 = trace(Xhat.t * P.t * Yhat * B.t)
    val updatedSigma2 = 1 / (Np * dim) * (s1 - s2)
    val TY = Y * B.t + DenseVector.ones[Double](M) * t.t

    (TY, updatedSigma2)
  }
}
