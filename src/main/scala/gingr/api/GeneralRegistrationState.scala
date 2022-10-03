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

package gingr.api

import breeze.linalg.{DenseMatrix, DenseVector}
import gingr.api.FittingStatuses.FittingStatus
import scalismo.common.PointId
import scalismo.geometry.{EuclideanVector, Landmark, Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.{MultivariateNormalDistribution, PointDistributionModel}
import scalismo.transformations._

case class GeneralRegistrationState(
    override val model: PointDistributionModel[_3D, TriangleMesh],
    override val modelParameters: ModelFittingParameters,
    override val modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
    override val target: TriangleMesh[_3D],
    override val targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
    override val fit: TriangleMesh[_3D],
    override val sigma2: Double = 1.0,
    override val globalTransformation: GlobalTranformationType = RigidTransforms,
    override val stepLength: Double = 1.0,
    override val generatedBy: String = "",
    override val iteration: Int = 0,
    override val status: FittingStatus = FittingStatuses.None
) extends RegistrationState[GeneralRegistrationState] {

  lazy val landmarkCorrespondences: IndexedSeq[(PointId, Point[_3D], MultivariateNormalDistribution)] = {
    if (modelLandmarks.nonEmpty && targetLandmarks.nonEmpty) {
      val m             = modelLandmarks.get
      val t             = targetLandmarks.get
      val commonLmNames = m.map(_.id) intersect t.map(_.id)
      commonLmNames.map { name =>
        val mPoint = m.find(_.id == name).get
        val tPoint = t.find(_.id == name).get
        (
          model.reference.pointSet.findClosestPoint(mPoint.point).id,
          tPoint.point,
          mPoint.uncertainty.getOrElse(
            MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3))
          )
        )
      }.toIndexedSeq
    } else {
      IndexedSeq()
    }
  }

  /** Updates the current state with the new fit.
    *
    * @param next
    *   The newly calculated shape / fit.
    */
  override def updateFit(next: TriangleMesh[_3D]): GeneralRegistrationState = this.copy(fit = next)
  override def updateIteration(): GeneralRegistrationState                  = this.copy(iteration = this.iteration + 1)
  override def clearIteration(): GeneralRegistrationState                   = this.copy(iteration = 0)
  override def updateStatus(next: FittingStatus): GeneralRegistrationState  = this.copy(status = next)

  override private[api] def updateTranslation(next: EuclideanVector[_3D]): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(translation = next)))
  }

  override private[api] def updateRotation(next: EulerRotation): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(rotation = next)))
  }

  override private[api] def updateRotation(next: Rotation[_3D]): GeneralRegistrationState = {
    val angles   = RotationSpace3D.rotMatrixToEulerAngles(next.rotationMatrix)
    val newEuler = EulerRotation(EulerAngles(angles._1, angles._2, angles._3), next.center)
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(rotation = newEuler)))
  }

  override private[api] def updateScaling(next: ScaleParameter): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(scale = next))
  }

  override private[api] def updateShapeParameters(next: ShapeParameters): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(shape = next))
  }

  override private[api] def updateModelParameters(next: ModelFittingParameters): GeneralRegistrationState =
    this.copy(modelParameters = next)

  override private[api] def updateSigma2(next: Double): GeneralRegistrationState = this.copy(sigma2 = next)

  override private[api] def updateGeneratedBy(next: String): GeneralRegistrationState = this.copy(generatedBy = next)

  def printStatus(): Unit = {
    val txt = status match {
      case FittingStatuses.None      => "Initial state - no iterations performed!"
      case FittingStatuses.Converged => s"Fitting converged after ${iteration + 1} accepted iterations!"
      case FittingStatuses.MaxIteration =>
        s"Fitting finished the MaxIterations with (${iteration + 1}) accepted iterations!"
      case FittingStatuses.ModelFlexibilityError =>
        s"Model not flexible enough to compute posterior model - finished after ${iteration + 1} accepted iterations!"
    }
    println(txt)
  }
}

object GeneralRegistrationState {
  def apply(
      model: PointDistributionModel[_3D, TriangleMesh],
      target: TriangleMesh[_3D],
      modelTranform: Option[TranslationAfterRotation[_3D]]
  ): GeneralRegistrationState = {
    apply(model, target, RigidTransforms, modelTranform)
  }

  def apply(
      model: PointDistributionModel[_3D, TriangleMesh],
      target: TriangleMesh[_3D],
      transform: GlobalTranformationType,
      modelTranform: Option[TranslationAfterRotation[_3D]]
  ): GeneralRegistrationState = {
    apply(model, Seq(), target, Seq(), transform, modelTranform)
  }

  def apply(
      model: PointDistributionModel[_3D, TriangleMesh],
      modelLandmarks: Seq[Landmark[_3D]],
      target: TriangleMesh[_3D],
      targetLandmarks: Seq[Landmark[_3D]],
      transform: GlobalTranformationType,
      modelTranform: Option[TranslationAfterRotation[_3D]]
  ): GeneralRegistrationState = {
    val (t, r) = if (modelTranform.isDefined) {
      val initialAngles = RotationSpace3D.rotMatrixToEulerAngles(modelTranform.get.rotation.rotationMatrix)
      (
        modelTranform.get.translation.t,
        EulerRotation(EulerAngles(initialAngles._1, initialAngles._2, initialAngles._3), Point(0, 0, 0))
      )
    } else {
      (
        EuclideanVector.zeros[_3D],
        EulerRotation(
          EulerAngles(0.0, 0.0, 0.0),
          Point(0, 0, 0)
        )
      )
    }

    val modelPars = ModelFittingParameters(
      scale = ScaleParameter(1.0),
      pose = PoseParameters(
        translation = t,
        rotation = r
      ),
      shape = ShapeParameters(DenseVector.zeros[Double](model.rank))
    )
    val fit = ModelFittingParameters.modelInstanceShapePoseScale(model, modelPars)
    val initial =
      new GeneralRegistrationState(
        model = model,
        modelParameters = modelPars,
        modelLandmarks = Option(modelLandmarks),
        target = target,
        targetLandmarks = Option(targetLandmarks),
        fit = fit,
        globalTransformation = transform
      )
    initial
  }

  def apply(
      model: PointDistributionModel[_3D, TriangleMesh],
      modelLandmarks: Seq[Landmark[_3D]],
      target: TriangleMesh[_3D],
      targetLandmarks: Seq[Landmark[_3D]],
      modelTranform: Option[TranslationAfterRotation[_3D]]
  ): GeneralRegistrationState = {
    apply(model, modelLandmarks, target, targetLandmarks, RigidTransforms, modelTranform)
  }
}
