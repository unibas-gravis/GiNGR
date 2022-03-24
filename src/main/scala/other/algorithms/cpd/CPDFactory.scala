package other.algorithms.cpd

import api.registration.utils.PointSequenceConverter
import breeze.linalg.DenseMatrix
import scalismo.common.Vectorizer
import scalismo.geometry.{NDSpace, Point}

/*
 Implementation of Point Set Registration: Coherent Point Drift
 In this script, only the non-rigid algorithm is implemented. Paper: https://arxiv.org/pdf/0905.2635.pdf
 A python implementation already exists from where parts of the implementation is from: https://github.com/siavashk/pycpd
 */
class CPDFactory[D: NDSpace](
  val templatePoints: Seq[Point[D]],
  val lambda: Double = 2,
  val beta: Double = 2,
  val w: Double = 0
)(implicit
  val vectorizer: Vectorizer[Point[D]],
  dataConverter: PointSequenceConverter[D]
) {
  val M: Int = templatePoints.length // num of reference points
  val dim: Int = vectorizer.dim // dimension
  val G: DenseMatrix[Double] = initializeKernelMatrixG(templatePoints, beta)
  val template: DenseMatrix[Double] = dataConverter.toMatrix(templatePoints)

  require(0.0 <= w && w <= 1.0)
  require(beta > 0)
  require(lambda > 0)
  /** Initialize G matrix - formula in paper fig. 4
    *
    * @param points
    * @param beta
    * @return
    */
  private def initializeKernelMatrixG(
    points: Seq[Point[D]],
    beta: Double
  ): DenseMatrix[Double] = {
    val M = points.length
    val G: DenseMatrix[Double] = DenseMatrix.zeros[Double](M, M)
    (0 until M).map { i =>
      (0 until M).map { j =>
        G(i, j) = math.exp(-(points(i) - points(j)).norm2 / (2 * math.pow(beta, 2)))
      }
    }
    G
  }

  def registerRigidly(targetPoints: Seq[Point[D]]): RigidCPD[D] = {
    new RigidCPD[D](targetPoints, this)
  }

  def registerNonRigidly(targetPoints: Seq[Point[D]]): RigidCPD[D] = {
    new NonRigidCPD[D](targetPoints, this)
  }

  def registerAffine(targetPoints: Seq[Point[D]]): RigidCPD[D] = {
    new AffineCPD[D](targetPoints, this)
  }

}
