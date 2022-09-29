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
import gingr.api.{GlobalTranformationType, RigidTransforms}
import gingr.api.sampling.evaluators.{EvaluationMode, ModelToTargetEvaluation}
import scalismo.geometry.{Landmark, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.utils.Random

import java.io.File

class Interface(
    model: PointDistributionModel[_3D, TriangleMesh],
    target: TriangleMesh[_3D],
    modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
    targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
    transform: GlobalTranformationType = RigidTransforms,
    evaluatorUncertainty: Double = 1.0,
    evaluatedPoints: Option[Int] = None,
    evaluationMode: EvaluationMode = ModelToTargetEvaluation,
    jsonFile: Option[File] = None
)(implicit rnd: Random) {

  def CPD(config: CpdConfiguration): SimpleRegistrator[CpdRegistrationState, CpdConfiguration, CpdRegistration] = {
    val algorithm = new CpdRegistration()
    new SimpleRegistrator[CpdRegistrationState, CpdConfiguration, CpdRegistration](
      model,
      target,
      modelLandmarks,
      targetLandmarks,
      algorithm,
      config,
      transform,
      evaluatorUncertainty,
      evaluatedPoints,
      evaluationMode,
      jsonFile
    )
  }

  def ICP(config: IcpConfiguration): SimpleRegistrator[IcpRegistrationState, IcpConfiguration, IcpRegistration] = {
    val algorithm = new IcpRegistration()
    new SimpleRegistrator[IcpRegistrationState, IcpConfiguration, IcpRegistration](
      model,
      target,
      modelLandmarks,
      targetLandmarks,
      algorithm,
      config,
      transform,
      evaluatorUncertainty,
      evaluatedPoints,
      evaluationMode,
      jsonFile
    )
  }

}
