package other.algorithms

import other.algorithms.icp.ICPFactory
import other.utils.Registrator
import scalismo.common.UnstructuredPoints.Create
import scalismo.common._
import scalismo.geometry.{NDSpace, Point}

import scala.language.higherKinds

class RigidICPRegistration[D: NDSpace, DDomain[A] <: DiscreteDomain[A]](template: DDomain[D], max_iterations: Int = 100)(implicit
  warper: DomainWarp[D, DDomain],
  vectorizer: Vectorizer[Point[D]],
  registration: Registrator[D],
  create: Create[D]) {
  val icp = new ICPFactory[D](UnstructuredPoints(template.pointSet.points.toIndexedSeq))

  def registrationMethod(targetPoints: UnstructuredPoints[D]) = icp.registerRigidly(targetPoints)

  def register(target: DDomain[D]): DDomain[D] = {
    val registrationTask = registrationMethod(UnstructuredPoints(target.pointSet.points.toIndexedSeq))
    val registration = registrationTask.Registration(max_iterations)
    val warpField = DiscreteField(target, target.pointSet.points.toIndexedSeq.zip(registration.points.toIndexedSeq).map { case (a, b) => b - a })
    warper.transformWithField(target, warpField)
  }
}
