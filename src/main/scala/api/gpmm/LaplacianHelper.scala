package api.gpmm

import api.gpmm.MatrixHelper.pinv
import breeze.linalg.DenseMatrix
import scalismo.common.PointId
import scalismo.geometry._3D
import scalismo.mesh.TriangleMesh

case class LaplacianHelper(template: TriangleMesh[_3D]) {
  private val n = template.pointSet.numberOfPoints
  val m = DenseMatrix.zeros[Double](n, n)

  def deg(i: Int): Int = template.triangulation.adjacentPointsForPoint(PointId(i)).length // get degree of i
  def adj(i: Int, j: Int): Boolean = template.triangulation.adjacentPointsForPoint(PointId(j)).contains(PointId(i)) // Check if i is adjecent to j

  def laplacianMatrix(inverse: Boolean = false): DenseMatrix[Double] = {
    (0 until n).map { i =>
      (0 until n).map { j =>
        m(i, j) = if (i == j) deg(i).toDouble else if (adj(i, j)) -1.0 else 0.0
      }
    }
    if (inverse) pinv(m) else m
  }

  def inverseLaplacianMatrix(): DenseMatrix[Double] = {
    pinv(laplacianMatrix())
  }

  def laplacianNormalizedMatrix(inverse: Boolean = false): DenseMatrix[Double] = {
    (0 until n).map { i =>
      (0 until n).map { j =>
        m(i, j) = if (i == j && deg(i) != 0) 1.0 else if (adj(i, j)) -1.0 / math.sqrt(deg(i) * deg(j)) else 0.0
      }
    }
    if (inverse) pinv(m) else m
  }
}
