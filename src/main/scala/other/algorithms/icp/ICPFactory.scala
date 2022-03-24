package other.algorithms.icp

import other.utils.Registrator
import scalismo.common.{UnstructuredPoints, Vectorizer}
import scalismo.geometry.{NDSpace, Point}

/*
 Implementation of Point Set Registration: Iterative closest points todo: check and update rest of the comment
 In this script, only the non-rigid algorithm is implemented. Paper: https://arxiv.org/pdf/0905.2635.pdf
 A python implementation already exists from where parts of the implementation is from: https://github.com/siavashk/pycpd
 */
class ICPFactory[D: NDSpace](
  val templatePoints: UnstructuredPoints[D]
)(implicit
  val vectorizer: Vectorizer[Point[D]],
  registrator: Registrator[D]
) {
  def registerRigidly(targetPoints: UnstructuredPoints[D]): RigidICP[D] = {
    new RigidICP[D](targetPoints, this)
  }
}
