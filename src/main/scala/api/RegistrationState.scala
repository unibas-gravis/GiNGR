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

package api

import scalismo.geometry.{EuclideanVector, Landmark, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.Rotation

trait RegistrationState[T] {
  def model(): PointDistributionModel[_3D, TriangleMesh] // Prior statistical mesh model
  def modelParameters(): ModelFittingParameters // parameters of the current fitting state in the model
  def modelLandmarks(): Option[Seq[Landmark[_3D]]] // Landmarks on the model
  def target(): TriangleMesh[_3D] // Target mesh
  def targetLandmarks(): Option[Seq[Landmark[_3D]]] // Landmarks on the target
  def fit(): TriangleMesh[_3D] // Current fit based on model parameters, global alignment and scaling
  def sigma2(): Double // Global uncertainty parameter
  def globalTransformation(): GlobalTranformationType // Type of global transformation (none, rigid, similarity)
  def stepLength(): Double // Step length of a single registration step (0.0 to 1.0)
  def generatedBy(): String // Name of generator that produced the State

  /** Updates the current state with the new fit.
    *
    * @param next
    *   The newly calculated shape / fit.
    */
  private[api] def updateFit(next: TriangleMesh[_3D]): T
//  private[api] def updateAlignment(next: TranslationAfterRotation[_3D]): T
  private[api] def updateTranslation(next: EuclideanVector[_3D]): T
  private[api] def updateRotation(next: EulerRotation): T
  private[api] def updateRotation(next: Rotation[_3D]): T
  private[api] def updateScaling(next: ScaleParameter): T
  private[api] def updateShapeParameters(next: ShapeParameters): T
  private[api] def updateModelParameters(next: ModelFittingParameters): T
  private[api] def updateSigma2(next: Double): T
  private[api] def updateGeneratedBy(next: String): T
}
