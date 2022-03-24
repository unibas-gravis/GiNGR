package api.registration.config

import java.io.File

import api.registration.utils.PointSequenceConverter
import api.GingrAlgorithm
import api.{CorrespondencePairs, GeneralRegistrationState, GingrConfig, GingrRegistrationState}
import breeze.linalg.{sum, tile, Axis, DenseMatrix, DenseVector}
import scalismo.common.{DiscreteField, DiscreteField3D, PointId, UnstructuredPointsDomain}
import scalismo.geometry.Point.Point3DVectorizer
import scalismo.geometry.{_3D, EuclideanVector, Point}
import scalismo.statisticalmodel.MultivariateNormalDistribution

object CPDCorrespondence {
  val vectorizer: Point.Point3DVectorizer.type = Point3DVectorizer

  def estimate[T](state: CpdRegistrationState): CorrespondencePairs = {
    val refPoints = state.general.fit.pointSet.points.toSeq
    val tarPoints = state.general.target.pointSet.points.toSeq
    val P = state.P
    val P1inv = 1.0 / sum(P, Axis._1)

    val deform = refPoints.zipWithIndex.par.map { case (y, i) =>
      val xscale = tarPoints.zipWithIndex.map { case (x, j) =>
        P1inv(i) * P(i, j) * x.toBreezeVector
      }
      vectorizer.unvectorize(sum(xscale) - y.toBreezeVector).toVector
    }
    val td = refPoints.zip(deform).map { case (p, d) => p + d }
    val corr = state.general.fit.pointSet.pointIds.toSeq.zip(td).map(t => (t._1, t._2)).toIndexedSeq
    CorrespondencePairs(corr)
  }
}

case class CpdRegistrationState(general: GeneralRegistrationState, config: CpdConfiguration) extends GingrRegistrationState[CpdRegistrationState] {
  val P: DenseMatrix[Double] = {
    def gaussKernel(x: Point[_3D], y: Point[_3D], sigma2: Double): Double = {
      math.exp(-(x - y).norm2 / (2.0 * sigma2))
    }
    val refPoints = general.fit.pointSet.points.toSeq
    val tarPoints = general.target.pointSet.points.toSeq
    val M = refPoints.length
    val N = tarPoints.length
    // TODO: Approximate using nyström
    val P: DenseMatrix[Double] = DenseMatrix.zeros[Double](M, N)
    refPoints.zipWithIndex.par.foreach { case (y, i) =>
      tarPoints.zipWithIndex.foreach { case (x, j) =>
        P(i, j) = gaussKernel(x, y, general.sigma2)
      }
    }
    val c = config.w / (1 - config.w) * math.pow((2.0 * math.Pi * general.sigma2), 3.0 / 2.0) * (M.toDouble / N.toDouble)
    val denRow = DenseMatrix(sum(P, Axis._0).t)
    val den = tile(denRow, M, 1) + c

    P /:/ den
  }

  override def updateGeneral(update: GeneralRegistrationState): CpdRegistrationState = this.copy(general = update)
}

object CpdRegistrationState {
  private def computeInitialSigma2(reference: Seq[Point[_3D]], target: Seq[Point[_3D]]): Double = {
    val N = target.length
    val M = reference.length
    val sumDist = reference.flatMap { pm =>
      target.map { pn =>
        (pn - pm).norm2
      }
    }.sum
    sumDist / (3.0 * N * M)
  }

  def apply(general: GeneralRegistrationState, config: CpdConfiguration): CpdRegistrationState = {
    val newGeneral = general.copy(
      sigma2 = config.initialSigma.getOrElse(computeInitialSigma2(general.model.mean.pointSet.points.toSeq, general.target.pointSet.points.toSeq))
    )
    new CpdRegistrationState(
      newGeneral,
      config
    )
  }
}

case class CpdConfiguration(
  override val maxIterations: Int = 100,
  override val threshold: Double = 1e-10,
  override val converged: (GeneralRegistrationState, GeneralRegistrationState, Double) => Boolean =
    (last: GeneralRegistrationState, current: GeneralRegistrationState, threshold: Double) => math.abs(last.sigma2 - current.sigma2) < threshold,
  override val useLandmarkCorrespondence: Boolean = true,
  initialSigma: Option[Double] = None,
  w: Double = 0.0,
  lambda: Double = 1.0
) extends GingrConfig {}

class CpdRegistration(
  override val getCorrespondence: CpdRegistrationState => CorrespondencePairs = (state: CpdRegistrationState) => CPDCorrespondence.estimate(state),
  override val getUncertainty: (PointId, CpdRegistrationState) => MultivariateNormalDistribution = (id: PointId, state: CpdRegistrationState) => {
    val P = state.P
    val P1inv = 1.0 / sum(P, Axis._1)
    MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3) * state.general.sigma2 * state.config.lambda * P1inv(id.id))
  }
) extends GingrAlgorithm[CpdRegistrationState] {
  def name = "CPD"
  val dataConverter: PointSequenceConverter[_3D] = PointSequenceConverter.denseMatrixToPoint3DSequence

  // possibility to override the update function, or just use the base class method?
  override def updateSigma2(current: CpdRegistrationState): Double = {
    val meanUpdate = current.general.fit.pointSet.points.toSeq
    val P = current.P
    val X = dataConverter.toMatrix(current.general.target.pointSet.points.toSeq)
    val TY = dataConverter.toMatrix(meanUpdate)
    val P1 = sum(P, Axis._1)
    val Pt1 = sum(P, Axis._0)
    val Np = sum(P1)

    val xPx: Double = Pt1.t.dot(sum(X *:* X, Axis._1))
    val yPy: Double = P1.t * sum(TY *:* TY, Axis._1)
    val trPXY: Double = sum(TY *:* (P * X))
    val sigma2 = (xPx - 2 * trPXY + yPy) / (Np * 3.0)
    sigma2
  }
}
