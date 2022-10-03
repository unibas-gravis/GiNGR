//> using scala "3"
//> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
//> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.2"
package DemoHelper

import gingr.api.GingrRegistrationState
import gingr.api.sampling.loggers.JSONStateLogger
import scalismo.sampling.loggers.ChainStateLogger
import gingr.api.helper.RegistrationComparison
import scalismo.ui.api.PointDistributionModelViewControlsTriangleMesh3D

object CallBackFunctions:
  case class visualLogger[State <: GingrRegistrationState[State]](
    modelView: PointDistributionModelViewControlsTriangleMesh3D,
    updateFrequency: Int = 10,
  ) extends ChainStateLogger[State] {
    var counter = 0
    def reset = counter = 0
    override def logState(sample: State): Unit = {
      counter += 1
      if (counter % updateFrequency == 0 && counter > 1) {
        println(s"Iteration: ${counter}/${sample.config.maxIterations} - Sigma: ${sample.general.sigma2}")
        modelView.shapeModelTransformationView.poseTransformationView.transformation =
            sample.general.modelParameters.rigidTransform
        modelView.shapeModelTransformationView.shapeTransformationView.coefficients =
            sample.general.modelParameters.shape.parameters
        }
    }
  }
