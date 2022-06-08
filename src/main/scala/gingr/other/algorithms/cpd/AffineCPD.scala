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
import breeze.linalg.{Axis, DenseMatrix, DenseVector, InjectNumericOps, diag, inv, sum, trace}
import scalismo.common.*
import scalismo.geometry.{NDSpace, Point, _3D}

/*
 Implementation of Point Set Registration: Coherent Point Drift (CPD) - Affine transformation algorithm
 Paper: https://arxiv.org/pdf/0905.2635.pdf
 */
private[cpd] class AffineCPD(
    override val targetPoints: Seq[Point[_3D]],
    override val cpd: CPDFactory
)(implicit
    vectorizer: Vectorizer[Point[_3D]]
) extends RigidCPD(targetPoints, cpd) {
  import cpd._
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

    val muX = 1.0 / Np * X.t * P.t * DenseVector.ones[Double](P.rows)
    val muY = 1.0 / Np * Y.t * P1

    val Xhat = X - DenseVector.ones[Double](N) * muX.t
    val Yhat = Y - DenseVector.ones[Double](M) * muY.t

    val B = Xhat.t * P.t * Yhat * inv(Yhat.t * diag(P1) * Yhat)
    val t = muX - B * muY

    val s1            = trace(Xhat.t * diag(Pt1) * Xhat)
    val s2            = trace(Xhat.t * P.t * Yhat * B.t)
    val updatedSigma2 = 1 / (Np * dim) * (s1 - s2)
    val TY            = Y * B.t + DenseVector.ones[Double](M) * t.t

    (TY, updatedSigma2)
  }
}
