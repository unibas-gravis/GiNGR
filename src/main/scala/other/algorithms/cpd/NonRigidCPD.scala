package other.algorithms.cpd

import api.registration.utils.PointSequenceConverter
import breeze.linalg.{diag, inv, sum, Axis, DenseMatrix, DenseVector}
import scalismo.common.Vectorizer
import scalismo.geometry.{NDSpace, Point}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Non-Rigid algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class NonRigidCPD[D: NDSpace](
  override val targetPoints: Seq[Point[D]],
  override val cpd: CPDFactory[D]
)(implicit
  vectorizer: Vectorizer[Point[D]],
  dataConverter: PointSequenceConverter[D]
) extends RigidCPD[D](targetPoints, cpd) {
  import cpd._

  def computeSigma2(template: Seq[Point[D]], target: Seq[Point[D]]): Double = {
    val sumDist = template.toIndexedSeq.flatMap { pm =>
      target.toIndexedSeq.map { pn =>
        (pn - pm).norm2
      }
    }.sum
    sumDist / (3 * template.length * target.length)
  }

  override def Maximization(X: DenseMatrix[Double], Y: DenseMatrix[Double], P: DenseMatrix[Double], sigma2: Double): (DenseMatrix[Double], Double) = {
    // Update transform
    val P1: DenseVector[Double] = sum(P, Axis._1)
    val Pt1 = sum(P, Axis._0)
    val Np = sum(P1)

    val myG = G
    val diagP1inv = inv(diag(P1))
    val PX = P * X

    val A: DenseMatrix[Double] = G + lambda * sigma2 * diagP1inv
    val B: DenseMatrix[Double] = diagP1inv * PX - Y

    val W = A \ B
    // Update Point Cloud
    val deform = myG * W
    val TY = Y + deform

    // Update variance
    /*
        Update the variance of the mixture model using the new estimate of the deformable transformation.
        See the update rule for sigma2 in Eq. 23 of of https://arxiv.org/pdf/0905.2635.pdf.
     */
    // The original CPD paper does not explicitly calculate the objective functional.
    // This functional will include terms from both the negative log-likelihood and
    // the Gaussian kernel used for regularization.
    val xPx: Double = Pt1.t.dot(sum(X *:* X, Axis._1))
    val yPy: Double = P1.t * sum(TY *:* TY, Axis._1)

    val trPXY: Double = sum(TY *:* (P * X))

    val updatedSigma2 = (xPx - 2 * trPXY + yPy) / (Np * dim)

//    val Xtarget: Seq[Point[D]] = dataConverter.toPointSequence(X)(vectorizer)
//    val Ytemp   = dataConverter.toPointSequence(TY)(vectorizer)
//
//    val updatedSigma2 = computeSigma2(Xtarget, Ytemp)

    (TY, updatedSigma2)
  }
}
