package api

import breeze.linalg.DenseVector
import scalismo.geometry.{_3D, EuclideanVector, Point}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.{Rotation, Scaling, Scaling3D, Translation, TranslationAfterRotation, TranslationAfterScalingAfterRotation}

case class ScaleParameter(s: Double) {
  def parameters: DenseVector[Double] = DenseVector(s)
}

case class EulerAngles(phi: Double, theta: Double, psi: Double) {
  def parameters: DenseVector[Double] = DenseVector(phi, theta, psi)
}

case class EulerRotation(angles: EulerAngles, center: Point[_3D]) {
  val rotation: Rotation[_3D] = Rotation(angles.phi, angles.theta, angles.psi, center)
  def parameters: DenseVector[Double] = DenseVector.vertcat(angles.parameters, center.toBreezeVector)
}

case class PoseParameters(translation: EuclideanVector[_3D], rotation: EulerRotation) {
  def parameters: DenseVector[Double] = {
    DenseVector.vertcat(
      DenseVector.vertcat(
        translation.toBreezeVector,
        rotation.parameters
      )
    )
  }
}

case class ShapeParameters(parameters: DenseVector[Double])

case class ModelFittingParameters(scale: ScaleParameter, pose: PoseParameters, shape: ShapeParameters) {
  val allParameters: DenseVector[Double] = DenseVector.vertcat(scale.parameters, pose.parameters, shape.parameters)

  def rigidTransform: TranslationAfterRotation[_3D] = {
    val translation = Translation(pose.translation)
    val rotation = pose.rotation.rotation
    TranslationAfterRotation(translation, rotation)
  }

  def similarityTransform: TranslationAfterScalingAfterRotation[_3D] = {
    val translation = Translation(pose.translation)
    val rotation = pose.rotation.rotation
    val scaling = Scaling[_3D](scale.s)
    TranslationAfterScalingAfterRotation(translation, scaling, rotation)
  }

  def scaleTransform: Scaling[_3D] = Scaling3D(scale.s)
}

object ModelFittingParameters {
  def apply(modelRank: Int): ModelFittingParameters = {
    val pose = PoseParameters(translation = EuclideanVector(0, 0, 0), rotation = EulerRotation(EulerAngles(0, 0, 0), Point(0, 0, 0)))
    val shape = ShapeParameters(DenseVector.zeros[Double](modelRank))
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def apply(shape: ShapeParameters): ModelFittingParameters = {
    val pose = PoseParameters(translation = EuclideanVector(0, 0, 0), rotation = EulerRotation(EulerAngles(0, 0, 0), Point(0, 0, 0)))
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def apply(pose: PoseParameters, shape: ShapeParameters): ModelFittingParameters = {
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def modelInstanceShapePose(model: PointDistributionModel[_3D, TriangleMesh], pars: ModelFittingParameters): TriangleMesh[_3D] = {
    model.instance(pars.shape.parameters).transform(pars.rigidTransform)
  }

  def modelInstanceShapePoseScale(model: PointDistributionModel[_3D, TriangleMesh], pars: ModelFittingParameters): TriangleMesh[_3D] = {
    val instance = modelInstanceShapePose(model, pars)
    instance.transform(pars.scaleTransform)
  }
}
