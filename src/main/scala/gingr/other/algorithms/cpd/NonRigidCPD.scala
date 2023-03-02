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

import breeze.linalg.{Axis, DenseMatrix, DenseVector, diag, inv, sum}
import scalismo.common.Vectorizer
import scalismo.geometry.{Point, _3D}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Non-Rigid algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class NonRigidCPD(
    override val targetPoints: Seq[Point[_3D]],
    override val cpd: CPDFactory
)(implicit
    vectorizer: Vectorizer[Point[_3D]]
) extends RigidCPD(targetPoints, cpd) {
  import cpd._

  def computeSigma2(template: Seq[Point[_3D]], target: Seq[Point[_3D]]): Double = {
    val sumDist = template.toIndexedSeq.flatMap { pm =>
      target.toIndexedSeq.map { pn =>
        (pn - pm).norm2
      }
    }.sum
    sumDist / (3 * template.length * target.length)
  }

  override def Maximization(
      X: DenseMatrix[Double],
      Y: DenseMatrix[Double],
      P: DenseMatrix[Double],
      sigma2: Double
  ): (DenseMatrix[Double], Double) = {
    // Update transform
    val P1: DenseVector[Double] = sum(P, Axis._1)
    val Pt1                     = sum(P, Axis._0)
    val Np                      = sum(P1)

    val myG       = G
    val diagP1inv = inv(diag(P1))
    val PX        = P * X

    val A: DenseMatrix[Double] = G + lambda * sigma2 * diagP1inv
    val B: DenseMatrix[Double] = diagP1inv * PX - Y

    val W = A \ B
    // Update Point Cloud
    val deform = myG * W
    val TY     = Y + deform

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

//    val Xtarget: Seq[Point[_3D]] = dataConverter.toPointSequence(X)(vectorizer)
//    val Ytemp   = dataConverter.toPointSequence(TY)(vectorizer)
//
//    val updatedSigma2 = computeSigma2(Xtarget, Ytemp)

    (TY, updatedSigma2)
  }
}
