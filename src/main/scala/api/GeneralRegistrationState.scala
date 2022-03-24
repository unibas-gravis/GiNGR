package api

import breeze.linalg.{DenseMatrix, DenseVector}
import scalismo.common.PointId
import scalismo.geometry.{_3D, EuclideanVector, Landmark, Point}
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
  override val generatedBy: String = ""
) extends RegistrationState[GeneralRegistrationState] {

  /** Updates the current state with the new fit.
    *
    * @param next
    *   The newly calculated shape / fit.
    */
  override def updateFit(next: TriangleMesh[_3D]): GeneralRegistrationState = this.copy(fit = next)
  override private[api] def updateTranslation(next: EuclideanVector[_3D]): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(translation = next)))
  }
  override private[api] def updateRotation(next: EulerRotation): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(rotation = next)))
  }
  override private[api] def updateRotation(next: Rotation[_3D]): GeneralRegistrationState = {
    val angles = RotationSpace3D.rotMatrixToEulerAngles(next.rotationMatrix)
    val newEuler = EulerRotation(EulerAngles(angles._1, angles._2, angles._3), next.center)
    this.copy(modelParameters = this.modelParameters.copy(pose = this.modelParameters.pose.copy(rotation = newEuler)))
  }
  override private[api] def updateScaling(next: ScaleParameter): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(scale = next))
  }
  override private[api] def updateShapeParameters(next: ShapeParameters): GeneralRegistrationState = {
    this.copy(modelParameters = this.modelParameters.copy(shape = next))
  }
  override private[api] def updateModelParameters(next: ModelFittingParameters): GeneralRegistrationState = this.copy(modelParameters = next)
  override private[api] def updateSigma2(next: Double): GeneralRegistrationState = this.copy(sigma2 = next)
  override private[api] def updateGeneratedBy(next: String): GeneralRegistrationState = this.copy(generatedBy = next)

  lazy val landmarkCorrespondences: IndexedSeq[(PointId, Point[_3D], MultivariateNormalDistribution)] = {
    if (modelLandmarks.nonEmpty && targetLandmarks.nonEmpty) {
      val m = modelLandmarks.get
      val t = targetLandmarks.get
      val commonLmNames = m.map(_.id) intersect t.map(_.id)
      commonLmNames.map { name =>
        val mPoint = m.find(_.id == name).get
        val tPoint = t.find(_.id == name).get
        (
          model.reference.pointSet.findClosestPoint(mPoint.point).id,
          tPoint.point,
          mPoint.uncertainty.getOrElse(MultivariateNormalDistribution(DenseVector.zeros[Double](3), DenseMatrix.eye[Double](3)))
        )
      }.toIndexedSeq
    } else {
      IndexedSeq()
    }
  }
}

object GeneralRegistrationState {
//  def apply(reference: TriangleMesh[_3D], target: TriangleMesh[_3D]): GeneralRegistrationState = {
//    val model: PointDistributionModel[_3D, TriangleMesh] = ???
//    apply(model, target, RigidTransforms)
//  }

  def apply(model: PointDistributionModel[_3D, TriangleMesh], target: TriangleMesh[_3D]): GeneralRegistrationState = {
    apply(model, target, RigidTransforms)
  }

  def apply(model: PointDistributionModel[_3D, TriangleMesh], target: TriangleMesh[_3D], transform: GlobalTranformationType): GeneralRegistrationState = {
    apply(model, Seq(), target, Seq(), transform)
  }

  def apply(
    model: PointDistributionModel[_3D, TriangleMesh],
    modelLandmarks: Seq[Landmark[_3D]],
    target: TriangleMesh[_3D],
    targetLandmarks: Seq[Landmark[_3D]]
  ): GeneralRegistrationState = {
    apply(model, modelLandmarks, target, targetLandmarks, RigidTransforms)
  }

  def apply(
    model: PointDistributionModel[_3D, TriangleMesh],
    modelLandmarks: Seq[Landmark[_3D]],
    target: TriangleMesh[_3D],
    targetLandmarks: Seq[Landmark[_3D]],
    transform: GlobalTranformationType
  ): GeneralRegistrationState = {
    val modelPars = ModelFittingParameters(
      scale = ScaleParameter(1.0),
      pose = PoseParameters(
        translation = EuclideanVector.zeros[_3D],
        rotation = EulerRotation(
          EulerAngles(0.0, 0.0, 0.0),
          Point(0, 0, 0)
        )
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
}
