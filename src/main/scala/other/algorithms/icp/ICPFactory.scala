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
