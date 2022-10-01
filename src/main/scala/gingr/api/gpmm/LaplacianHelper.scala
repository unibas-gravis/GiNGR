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

package gingr.api.gpmm

import breeze.linalg.DenseMatrix
import gingr.api.gpmm.MatrixHelper.pinv
import scalismo.common.PointId
import scalismo.geometry._3D
import scalismo.mesh.TriangleMesh

case class LaplacianHelper(template: TriangleMesh[_3D]) {
  private val n = template.pointSet.numberOfPoints
  val m         = DenseMatrix.zeros[Double](n, n)

  def deg(i: Int): Int = template.triangulation.adjacentPointsForPoint(PointId(i)).length // get degree of i
  def adj(i: Int, j: Int): Boolean =
    template.triangulation.adjacentPointsForPoint(PointId(j)).contains(PointId(i)) // Check if i is adjecent to j

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
