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

import breeze.linalg.DenseVector
import scalismo.geometry.{EuclideanVector, Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations._
import spray.json.DefaultJsonProtocol._
import spray.json.{RootJsonFormat, _}

import java.io.{BufferedWriter, File, FileWriter, IOException}
import scala.io.Source

case class ScaleParameter(s: Double) {
  def parameters: DenseVector[Double] = DenseVector(s)
}

case class EulerAngles(phi: Double, theta: Double, psi: Double) {
  def parameters: DenseVector[Double] = DenseVector(phi, theta, psi)
}

case class EulerRotation(angles: EulerAngles, center: Point[_3D]) {
  def rotation: Rotation[_3D]         = Rotation(angles.phi, angles.theta, angles.psi, center)
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
  def allParameters: DenseVector[Double] = DenseVector.vertcat(scale.parameters, pose.parameters, shape.parameters)

  def rigidTransform: TranslationAfterRotation[_3D] = {
    val translation = Translation(pose.translation)
    val rotation    = pose.rotation.rotation
    TranslationAfterRotation(translation, rotation)
  }

  def similarityTransform: TranslationAfterScalingAfterRotation[_3D] = {
    val translation = Translation(pose.translation)
    val rotation    = pose.rotation.rotation
    val scaling     = Scaling[_3D](scale.s)
    TranslationAfterScalingAfterRotation(translation, scaling, rotation)
  }

  def scaleTransform: Scaling[_3D] = Scaling3D(scale.s)
}
object ModelFittingParametersJson {
  implicit object RootJsDenseVectorFormat extends RootJsonFormat[DenseVector[Double]] {
    def write(value: DenseVector[Double]): JsValue = value.toArray.toJson
    def read(value: JsValue): DenseVector[Double] = value match {
      case x: JsArray => DenseVector[Double](x.convertTo[Array[Double]])
      case _          => deserializationError("JSON array expected")
    }
  }
  implicit object RootJsPoint3DFormat extends RootJsonFormat[Point[_3D]] {
    def write(value: Point[_3D]): JsValue = value.toArray.toJson
    def read(value: JsValue): Point[_3D] = value match {
      case x: JsArray => Point[_3D](x.convertTo[Array[Double]])
      case _          => deserializationError("JSON array expected")
    }
  }

  implicit object RootJsEuclideanVector3DFormat extends RootJsonFormat[EuclideanVector[_3D]] {
    def write(value: EuclideanVector[_3D]): JsValue = value.toArray.toJson
    def read(value: JsValue): EuclideanVector[_3D] = value match {
      case x: JsArray => EuclideanVector[_3D](x.convertTo[Array[Double]])
      case _          => deserializationError("JSON array expected")
    }
  }

  implicit val myJsonShapePars: RootJsonFormat[ShapeParameters]        = jsonFormat1(ShapeParameters.apply)
  implicit val myJsonAngPars: RootJsonFormat[EulerAngles]              = jsonFormat3(EulerAngles.apply)
  implicit val myJsonRotPars: RootJsonFormat[EulerRotation]            = jsonFormat2(EulerRotation.apply)
  implicit val myJsonPosePars: RootJsonFormat[PoseParameters]          = jsonFormat2(PoseParameters.apply)
  implicit val myJsonScalePars: RootJsonFormat[ScaleParameter]         = jsonFormat1(ScaleParameter.apply)
  implicit val myJsonModelPars: RootJsonFormat[ModelFittingParameters] = jsonFormat3(ModelFittingParameters.apply)
}

object ModelFittingParameters {
  import ModelFittingParametersJson._
  def apply(modelRank: Int): ModelFittingParameters = {
    val pose = PoseParameters(
      translation = EuclideanVector(0, 0, 0),
      rotation = EulerRotation(EulerAngles(0, 0, 0), Point(0, 0, 0))
    )
    val shape = ShapeParameters(DenseVector.zeros[Double](modelRank))
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def apply(shape: ShapeParameters): ModelFittingParameters = {
    val pose = PoseParameters(
      translation = EuclideanVector(0, 0, 0),
      rotation = EulerRotation(EulerAngles(0, 0, 0), Point(0, 0, 0))
    )
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def apply(pose: PoseParameters, shape: ShapeParameters): ModelFittingParameters = {
    ModelFittingParameters(ScaleParameter(1.0), pose, shape)
  }

  def modelInstanceShapePose(
      model: PointDistributionModel[_3D, TriangleMesh],
      pars: ModelFittingParameters
  ): TriangleMesh[_3D] = {
    model.instance(pars.shape.parameters).transform(pars.rigidTransform)
  }

  def modelInstanceShapePoseScale(
      model: PointDistributionModel[_3D, TriangleMesh],
      pars: ModelFittingParameters
  ): TriangleMesh[_3D] = {
    val instance = modelInstanceShapePose(model, pars)
    instance.transform(pars.scaleTransform)
  }

  def save(pars: ModelFittingParameters, file: File): Unit = {
    try {
      val writer = new BufferedWriter(new FileWriter(file))
      writer.write(pars.toJson.prettyPrint)
      writer.close()
    } catch {
      case e: Exception => throw new IOException("Writing JSON log file failed!")
    }
  }

  def load(file: File): ModelFittingParameters = {
    val src  = Source.fromFile(file.toString)
    val data = src.mkString.parseJson.convertTo[ModelFittingParameters]
    src.close()
    data
  }
}
