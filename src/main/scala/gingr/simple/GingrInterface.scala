package gingr.simple

import gingr.api.registration.SimpleRegistrator
import gingr.api.registration.config.{
  CpdConfiguration,
  CpdRegistration,
  CpdRegistrationState,
  IcpConfiguration,
  IcpRegistration,
  IcpRegistrationState
}
import gingr.api.sampling.evaluators.{EvaluationMode, ModelToTargetEvaluation}
import scalismo.geometry.{_3D, Landmark}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.TranslationAfterRotation
import scalismo.utils.Random

import java.io.File

case class GingrInterface(
  model: PointDistributionModel[_3D, TriangleMesh],
  target: TriangleMesh[_3D],
  initialModelParameterTransform: Option[TranslationAfterRotation[_3D]] = None,
  modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
  targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
  evaluatorUncertainty: Double = 1.0,
  evaluatedPoints: Option[Int] = None,
  evaluationMode: EvaluationMode = ModelToTargetEvaluation,
  logFileFittingParameters: Option[File] = None
)(implicit rnd: Random) {
  def CPD(config: CpdConfiguration): SimpleRegistrator[CpdRegistrationState, CpdConfiguration, CpdRegistration] = {
    val algorithm = new CpdRegistration()
    new SimpleRegistrator[CpdRegistrationState, CpdConfiguration, CpdRegistration](
      algorithm = algorithm,
      config = config,
      model = model,
      target = target,
      initialModelParameterTransform = initialModelParameterTransform,
      modelLandmarks = modelLandmarks,
      targetLandmarks = targetLandmarks,
      evaluationMode = evaluationMode,
      evaluatorUncertainty = evaluatorUncertainty,
      evaluatedPoints = evaluatedPoints,
      logFileFittingParameters = logFileFittingParameters
    )
  }

  def ICP(config: IcpConfiguration): SimpleRegistrator[IcpRegistrationState, IcpConfiguration, IcpRegistration] = {
    val algorithm = new IcpRegistration()
    new SimpleRegistrator[IcpRegistrationState, IcpConfiguration, IcpRegistration](
      algorithm = algorithm,
      config = config,
      model = model,
      target = target,
      initialModelParameterTransform = initialModelParameterTransform,
      modelLandmarks = modelLandmarks,
      targetLandmarks = targetLandmarks,
      evaluationMode = evaluationMode,
      evaluatorUncertainty = evaluatorUncertainty,
      evaluatedPoints = evaluatedPoints,
      logFileFittingParameters = logFileFittingParameters
    )
  }
}
