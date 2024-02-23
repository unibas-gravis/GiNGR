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

package gingr.api.registration.utils

import breeze.linalg.{CSCMatrix, DenseMatrix, DenseVector}
import scalismo.common.{PointId, Vectorizer}
import scalismo.geometry.Point.Point3DVectorizer
import scalismo.geometry.{_3D, Point, Point3D}

import scala.collection.parallel.CollectionConverters.ImmutableSeqIsParallelizable

object PointSequenceConverter {
  val vectorizer: Point.Point3DVectorizer.type = Point3DVectorizer

  def toMatrix(points: Seq[Point[_3D]])(implicit vectorizer: Vectorizer[Point[_3D]]): DenseMatrix[Double] = {
    val dim: Int = vectorizer.dim
    val m = DenseMatrix.zeros[Double](points.length, dim)
    points.zipWithIndex.par.foreach { case (p, i) =>
      m(i, ::) := vectorizer.vectorize(p).t
    }
    m
  }

  def toLMMatrix(points: Seq[Point[_3D]], pointIds: Seq[PointId], n: Int)(implicit
    vectorizer: Vectorizer[Point[_3D]]
  ): CSCMatrix[Double] = {
    val nPoints = points.size
    val dim: Int = vectorizer.dim
    val csc = CSCMatrix.zeros[Double](nPoints, dim)
    points.zip(pointIds).zipWithIndex.foreach { case ((p, pid), i) =>
      val v = vectorizer.vectorize(p)
      for (j <- 0 until dim) {
        csc(i, j) = v(j)
      }
    }
    csc
  }

  // Vectorize with x_1x, x_1y, x_1z, x_2x ...
  def toVector(points: Seq[Point[_3D]])(implicit vectorizer: Vectorizer[Point[_3D]]): DenseVector[Double] = {
    DenseVector(points.flatMap { p =>
      vectorizer.vectorize(p).toArray
    }.toArray)
  }

  def matrixTo3Dpoints(m: DenseMatrix[Double]): IndexedSeq[Point[_3D]] = {
    (0 until m.rows).map { r =>
      Point3D(x = m(r, 0), y = m(r, 1), z = m(r, 2))
    }
  }

  def vectorTo3Dpoints(v: DenseVector[Double]): IndexedSeq[Point[_3D]] = {
    (0 until v.length / 3).map { i =>
      Point3D(x = v(i * 3), y = v(i * 3 + 1), z = v(i * 3 + 2))
    }
  }

  def toPointSequence(m: DenseMatrix[Double])(implicit vectorizer: Vectorizer[Point[_3D]]): Seq[Point[_3D]] = {
    matrixTo3Dpoints(m)
  }

  def toPointSequence(
    v: DenseVector[Double]
  )(implicit vectorizer: Vectorizer[Point[_3D]]): Seq[Point[_3D]] = {
    vectorTo3Dpoints(v)
  }

}
