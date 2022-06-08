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

package gingr.api.registration.utils

import breeze.linalg.DenseMatrix
import scalismo.geometry.{Point, SquareMatrix, _3D}
import scalismo.registration.LandmarkRegistration
import scalismo.transformations.*

case class SimilarityTransformParameters(s: Scaling[_3D], t: Translation[_3D], R: Rotation[_3D]) {
  val simTransform: TranslationAfterScalingAfterRotation[_3D] = TranslationAfterScalingAfterRotation(t, s, R)
  val rigidTransform: TranslationAfterRotation[_3D]           = TranslationAfterRotation(t, R)
  val invSimTransform                                         = simTransform.inverse
  val invRigidTransform                                       = rigidTransform.inverse
  def transform(points: Seq[Point[_3D]]): Seq[Point[_3D]]     = points.map(simTransform)
  def invTransform(points: Seq[Point[_3D]]): Seq[Point[_3D]]  = points.map(invSimTransform)
}
