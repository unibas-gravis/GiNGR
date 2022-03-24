package api.helper

import api.GingrRegistrationState
import api.sampling.loggers.JSONStateLogger
import scalismo.sampling.loggers.ChainStateLogger
import scalismo.ui.api.PointDistributionModelViewControlsTriangleMesh3D

object CallBackFunctions {

  case class visualLogger[State <: GingrRegistrationState[State]](
    jsonLogger: Option[JSONStateLogger[State]],
    modelView: PointDistributionModelViewControlsTriangleMesh3D,
    modelUpdateFrequency: Int = 10,
    printUpdateFrequency: Int = 100)
    extends ChainStateLogger[State] {
    var counter = 0
    override def logState(sample: State): Unit = {
      counter += 1
      if (counter % modelUpdateFrequency == 0 && counter > 1) {
        println(s"Iteration: ${counter}/${sample.config.maxIterations()} - Sigma: ${sample.general.sigma2}")
        modelView.shapeModelTransformationView.poseTransformationView.transformation = sample.general.modelParameters.rigidTransform
        modelView.shapeModelTransformationView.shapeTransformationView.coefficients = sample.general.modelParameters.shape.parameters
      }
      if (counter % printUpdateFrequency == 0 && counter > 1) {
        jsonLogger.foreach(_.printAcceptInfo())
        jsonLogger.foreach{log => log.filePath.foreach(_ => log.writeLog())}
        RegistrationComparison.evaluateReconstruction2GroundTruthBoundaryAware("", sample.general.fit, sample.general.target)
      }
    }
  }
}
