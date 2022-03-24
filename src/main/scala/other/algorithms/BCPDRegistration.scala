package other.algorithms

import api.registration.utils.PointSequenceConverter
import other.algorithms.cpd.BCPD
import scalismo.common.{DiscreteDomain, DiscreteField, DomainWarp, Vectorizer}
import scalismo.geometry.{NDSpace, Point}
import scalismo.kernels.PDKernel

import scala.language.higherKinds

class BCPDRegistration[D: NDSpace, DDomain[D] <: DiscreteDomain[D]](
  template: DDomain[D],
  target: DDomain[D],
  w: Double = 0, // Outlier, [0,1]
  lambda: Double = 2.0, // Noise scaling, R+
  gamma: Double = 1.0, // Initial noise scaling, R+
  k: Double = 1.0, // Dirichlet distribution parameter
  kernel: PDKernel[D], // Positive semi-def kernel
  max_iterations: Int = 100)(implicit warper: DomainWarp[D, DDomain], vectorizer: Vectorizer[Point[D]], pointSequenceConverter: PointSequenceConverter[D]) {
  val cpd = new BCPD(template.pointSet.points.toSeq, target.pointSet.points.toSeq, w, lambda, gamma, k, kernel)

  def register(): DDomain[D] = {
    val registration = cpd.Registration(max_iterations)
    val warpField = DiscreteField(template, template.pointSet.points.toIndexedSeq.zip(registration).map { case (a, b) => b - a })
    warper.transformWithField(template, warpField)
  }
}
