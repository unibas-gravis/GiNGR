package apps.registration

import api.{GeneralRegistrationState, GlobalTranformationType, NoTransforms, RigidTransforms}
import api.registration.config.{CpdConfiguration, CpdRegistration, CpdRegistrationState, IcpConfiguration, IcpRegistration, IcpRegistrationState}
import scalismo.geometry.{_3D, Landmark}
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.utils.Random.implicits.randomGenerator

import java.io.File

class DemoConfigurations(
  model: PointDistributionModel[_3D, TriangleMesh],
  target: TriangleMesh[_3D],
  modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
  targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
  discretization: Int = 100,
  maxIterations: Int = 100,
  probabilistic: Boolean = false,
  transform: GlobalTranformationType = RigidTransforms,
  jsonFile: Option[File] = None
) {

  def CPD(initSigma: Option[Double] = None, threshold: Double = 1e-10, lambda: Double = 1.0): GeneralRegistrationState = {
    val configCPD = CpdConfiguration(maxIterations = maxIterations, threshold = threshold, lambda = lambda, initialSigma = initSigma)
    val algorithmCPD = new CpdRegistration()
    val uiName = if (probabilistic) "CPD-Probabilistic" else "CPD-Deterministic"
    val simpleRegistration = new SimpleRegistrator[CpdRegistrationState, CpdRegistration, CpdConfiguration](
      model = model,
      target = target,
      modelLandmarks = modelLandmarks,
      targetLandmarks = targetLandmarks,
      algorithm = algorithmCPD,
      config = configCPD,
      evaluatorUncertainty = 2.0,
      transform = transform,
      jsonFile = jsonFile,
      name = uiName
    )
    simpleRegistration.runDecimated(modelPoints = discretization, targetPoints = discretization, probabilistic = probabilistic, randomMixture = 0.50).general
  }

  def ICP(initSigma: Double = 1.0, endSigma: Double = 1.0, reverseCorrespondenceDirection: Boolean = false): GeneralRegistrationState = {
    val configICP = IcpConfiguration(maxIterations = maxIterations, initialSigma = initSigma, endSigma = endSigma, reverseCorrespondenceDirection = reverseCorrespondenceDirection)
    val algorithmICP = new IcpRegistration()
    val uiName = if (probabilistic) "ICP-Probabilistic" else "ICP-Deterministic"
    val simpleRegistration = new SimpleRegistrator[IcpRegistrationState, IcpRegistration, IcpConfiguration](
      model,
      target,
      algorithm = algorithmICP,
      config = configICP,
      evaluatorUncertainty = 2.0,
      transform = NoTransforms,
      jsonFile = jsonFile,
      name = uiName
    )

    simpleRegistration.runDecimated(modelPoints = discretization, targetPoints = discretization, probabilistic = probabilistic, randomMixture = 0.50).general
  }
}
