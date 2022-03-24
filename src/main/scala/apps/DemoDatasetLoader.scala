package apps

import apps.gpmm.{GaussDotKernel, GaussKernel, GaussMirrorKernel, GaussMixKernel, InvLapDotKernel, InvLapKernel, SimpleTriangleModels3D, WhichKernel}
import java.io.File

import scalismo.common.interpolation.TriangleMeshInterpolator3D
import scalismo.geometry.{_3D, Landmark, Point}
import scalismo.io.{LandmarkIO, MeshIO, StatisticalModelIO}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.{TranslationAfterRotation, TranslationAfterRotationSpace3D}

trait DataSetLoader {
  def name: String
  def path: File
  def json: File = new File(path, "probabilisticFitting.json")
  def defaultGaussSigma: Double = 1.0
  def defaultGaussScaling: Double = 1.0
  def defaultInvLapScaling: Double = 1.0
  def defaultInvLapDotScaling: Double = 1.0
  def defaultInvLapDotGamma: Double = 1.0
  def relativeTolerance: Double = 0.01
  def defaultTargetOffset: TranslationAfterRotation[_3D] = TranslationAfterRotationSpace3D(Point(0, 0, 0)).identityTransformation
  def reference(): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]])
  def reference(decimate: Option[Int] = None): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
    val (ref, lm) = reference()
    val dec =
      if (decimate.nonEmpty)
        ref.operations.decimate(decimate.get)
      else ref
    (dec, lm)
  }
  def target(offset: TranslationAfterRotation[_3D]): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]])
  def model(decimate: Option[Int] = None, kernelSelect: WhichKernel, fullResolutionReturn: Boolean): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    val (ref, lm) = reference(decimate)
    val decstring = if (decimate.nonEmpty) decimate.get.toString else "full"
    val modelFile = new File(path, s"${name}_dec-${decstring}_${kernelSelect.name}_${kernelSelect.printpars}.h5")
    val mdec = StatisticalModelIO.readStatisticalTriangleMeshModel3D(modelFile).getOrElse {
      println(s"Model file does not exist, creating: ${modelFile}")
      val m = SimpleTriangleModels3D.create(ref, kernelSelect, relativeTolerance = relativeTolerance)
      StatisticalModelIO.writeStatisticalTriangleMeshModel3D(m, modelFile).get
      m
    }
    val model =
      if (fullResolutionReturn) {
        val (fullref, _) = reference()
        mdec.newReference(fullref, TriangleMeshInterpolator3D())
      } else mdec
    (model, lm)
  }
  def modelGauss(
    decimate: Option[Int] = None,
    fullResolutionReturn: Boolean = true,
    scaling: Double = defaultGaussScaling,
    sigma: Double = defaultGaussSigma): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, GaussKernel(scaling, sigma), fullResolutionReturn)
  }
  def modelGaussMix(decimate: Option[Int] = None, fullResolutionReturn: Boolean = true): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, GaussMixKernel(), fullResolutionReturn)
  }
  def modelGaussDot(
    decimate: Option[Int] = None,
    fullResolutionReturn: Boolean = true,
    scaling: Double = defaultGaussScaling,
    sigma: Double = defaultGaussSigma): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, GaussDotKernel(scaling / 1000, sigma), fullResolutionReturn)
  }
  def modelGaussMirror(
    decimate: Option[Int] = None,
    fullResolutionReturn: Boolean = true,
    scaling: Double = defaultGaussScaling,
    sigma: Double = defaultGaussSigma): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, GaussMirrorKernel(scaling, sigma), fullResolutionReturn)
  }
  def modelInvLap(
    decimate: Option[Int] = None,
    fullResolutionReturn: Boolean = true,
    scaling: Double = defaultInvLapScaling): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, InvLapKernel(scaling), fullResolutionReturn)
  }
  def modelInvLapDot(
    decimate: Option[Int] = None,
    fullResolutionReturn: Boolean = true,
    scaling: Double = defaultInvLapDotScaling,
    gamma: Double = defaultInvLapDotGamma): (PointDistributionModel[_3D, TriangleMesh], Option[Seq[Landmark[_3D]]]) = {
    model(decimate, InvLapDotKernel(scaling, gamma), fullResolutionReturn)
  }
}

object DemoDatasetLoader {
  val dataPath = new File("data")
  case object femur extends DataSetLoader {
    override def name: String = "femur"
    override def path: File = new File(dataPath, name)
    override val defaultGaussSigma = 70.0
    override val defaultGaussScaling = 50.0
    override def reference(): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "femur.stl")).get
      val landmarks = LandmarkIO.readLandmarksJson3D(new File(path, "femur.json")).get
      (reference, Some(landmarks))
    }
    override def target(offset: TranslationAfterRotation[_3D] = defaultTargetOffset): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "femur_target.stl")).get
      val landmarks = LandmarkIO.readLandmarksJson3D(new File(path, "femur_target.json")).get
      (reference, Some(landmarks))
    }
  }

  case object armadillo extends DataSetLoader {
    override def name: String = "armadillo"
    override def path: File = new File(dataPath, name)
    override val defaultGaussSigma = 50.0
    override val defaultGaussScaling = 20.0
    override val defaultInvLapDotScaling = 0.02
    override def reference(): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "armadillo.ply")).get
      val landmarks = LandmarkIO.readLandmarksJson3D(new File(path, "armadillo.json")).get
      (reference, Some(landmarks))
    }
    override def target(offset: TranslationAfterRotation[_3D] = defaultTargetOffset): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "armadillo_karate.ply")).get
      val landmarks = LandmarkIO.readLandmarksJson3D(new File(path, "armadillo_karate.json")).get
      (reference, Some(landmarks))
    }
  }

  case object bunny extends DataSetLoader {
    override def name: String = "bunny"
    override def path: File = new File(dataPath, name)
    override val defaultGaussSigma = 40.0
    override val defaultGaussScaling = 20.0
    override def reference(): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "bunny.ply")).get
      val landmarks = None
      (reference, landmarks)
    }
    override def target(offset: TranslationAfterRotation[_3D] = defaultTargetOffset): (TriangleMesh[_3D], Option[Seq[Landmark[_3D]]]) = {
      val reference = MeshIO.readMesh(new File(path, "target.ply")).get
      val landmarks = None
      (reference, landmarks)
    }
  }
}
