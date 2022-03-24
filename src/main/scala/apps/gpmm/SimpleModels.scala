package apps.gpmm

import api.gpmm.GPMMTriangleMesh3D
import scalismo.geometry._3D
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel

sealed trait WhichKernel {
  val name: String
  val printpars: String
}
case class InvLapKernel(scaling: Double) extends WhichKernel {
  override val name: String = "InvLap"
  override val printpars: String = scaling.toString
}
case class InvLapDotKernel(scaling: Double, gamma: Double) extends WhichKernel {
  override val name: String = "InvLapDot"
  override val printpars: String = scaling.toString + "_" + gamma.toString
}
case class GaussKernel(scaling: Double, sigma: Double) extends WhichKernel {
  override val name: String = "Gauss"
  override val printpars: String = scaling.toString + "_" + sigma.toString
}
case class GaussMixKernel() extends WhichKernel {
  override val name: String = "GaussMix"
  override val printpars: String = ""
}
case class GaussDotKernel(scaling: Double, sigma: Double) extends WhichKernel {
  override val name: String = "GaussDot"
  override val printpars: String = scaling.toString + "_" + sigma.toString
}
case class GaussMirrorKernel(scaling: Double, sigma: Double) extends WhichKernel {
  override val name: String = "GaussMirror"
  override val printpars: String = scaling.toString + "_" + sigma.toString
}

object SimpleTriangleModels3D {

  def create(reference: TriangleMesh[_3D], kernelSelect: WhichKernel, relativeTolerance: Double = 0.01): PointDistributionModel[_3D, TriangleMesh] = {
    val model = kernelSelect match {
      case InvLapKernel(s) => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).InverseLaplacian(scaling = s)
      case InvLapDotKernel(s, g) => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).InverseLaplacianDot(scaling = s, gamma = g)
      case GaussKernel(s, sigma) => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).Gaussian(sigma = sigma, scaling = s)
      case GaussMixKernel() => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).AutomaticGaussian()
      case GaussDotKernel(s, sigma) => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).GaussianDot(sigma = sigma, scaling = s)
      case GaussMirrorKernel(s, sigma) => GPMMTriangleMesh3D(reference, relativeTolerance = relativeTolerance).GaussianSymmetry(sigma = sigma, s)
    }
    model
  }
}
