package api.registration.utils

import breeze.linalg.DenseMatrix
import scalismo.geometry.{_2D, _3D, Point, SquareMatrix}
import scalismo.registration.LandmarkRegistration
import scalismo.transformations._

case class SimilarityTransformParameters[D](s: Scaling[D], t: Translation[D], R: Rotation[D]) {
  val simTransform: TranslationAfterScalingAfterRotation[D] = TranslationAfterScalingAfterRotation(t, s, R)
  val rigidTransform: TranslationAfterRotation[D] = TranslationAfterRotation(t, R)
  val invSimTransform: RotationAfterScalingAfterTranslation[D] = simTransform.inverse
  val invRigidTransform: RotationAfterTranslation[D] = rigidTransform.inverse

  def transform(points: Seq[Point[D]]): Seq[Point[D]] = points.map(simTransform)
  def invTransform(points: Seq[Point[D]]): Seq[Point[D]] = points.map(invSimTransform)
}

trait TransformationHelper[D] {
  def getSimilarityTransform(source: Seq[Point[D]], target: Seq[Point[D]]): SimilarityTransformParameters[D]
  def getRigidTransform(source: Seq[Point[D]], target: Seq[Point[D]]): SimilarityTransformParameters[D]
  def zeroRotationInitialization: Rotation[D]
  def rotationMatrixToEuler(m: DenseMatrix[Double]): Rotation[D]
}

object TransformationHelper {
  implicit object similarityTransform3D extends TransformationHelper[_3D] {
    override def getSimilarityTransform(source: Seq[Point[_3D]], target: Seq[Point[_3D]]): SimilarityTransformParameters[_3D] = {
      require(source.length == target.length)
      val reg = LandmarkRegistration.similarity3DLandmarkRegistration(source.zip(target), Point(0, 0, 0))
      SimilarityTransformParameters(reg.scaling, reg.translation, reg.rotation)
    }

    override def getRigidTransform(source: Seq[Point[_3D]], target: Seq[Point[_3D]]): SimilarityTransformParameters[_3D] = {
      require(source.length == target.length)
      val reg = LandmarkRegistration.rigid3DLandmarkRegistration(source.zip(target), Point(0, 0, 0))
      SimilarityTransformParameters(Scaling(1.0), reg.translation, reg.rotation)
    }
    override def zeroRotationInitialization: Rotation[_3D] = Rotation(0.0, 0.0, 0.0, Point(0, 0, 0))

    override def rotationMatrixToEuler(m: DenseMatrix[Double]): Rotation[_3D] = {
      val d = new DenseMatrix(3, 3, m.toArray.map(_.toDouble))
      val s = SquareMatrix[_3D](d.toArray)
      Rotation3D(s, Point(0, 0, 0))
    }
  }
  implicit object similarityTransform2D extends TransformationHelper[_2D] {
    override def getSimilarityTransform(source: Seq[Point[_2D]], target: Seq[Point[_2D]]): SimilarityTransformParameters[_2D] = {
      require(source.length == target.length)
      val reg = LandmarkRegistration.similarity2DLandmarkRegistration(source.zip(target), Point(0, 0))
      SimilarityTransformParameters(reg.scaling, reg.translation, reg.rotation)
    }

    override def getRigidTransform(source: Seq[Point[_2D]], target: Seq[Point[_2D]]): SimilarityTransformParameters[_2D] = {
      require(source.length == target.length)
      val reg = LandmarkRegistration.rigid2DLandmarkRegistration(source.zip(target), Point(0, 0))
      SimilarityTransformParameters(Scaling(1.0), reg.translation, reg.rotation)
    }

    override def zeroRotationInitialization: Rotation[_2D] = Rotation(0.0, Point(0, 0))

    override def rotationMatrixToEuler(m: DenseMatrix[Double]): Rotation[_2D] = Rotation2D(0, Point(0, 0)) // TODO: Need to be implemented
  }
}
