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

package api.registration.utils

import breeze.linalg.{CSCMatrix, DenseMatrix, DenseVector}
import scalismo.common.{PointId, Vectorizer}
import scalismo.geometry.Point.{Point1DVectorizer, Point2DVectorizer, Point3DVectorizer}
import scalismo.geometry._

import scala.collection.parallel.CollectionConverters._

trait PointSequenceConverter[D] {
  def toPointSequence(m: DenseMatrix[Double])(implicit vectorizer: Vectorizer[Point[D]]): Seq[Point[D]]
  def toPointSequence(v: DenseVector[Double])(implicit vectorizer: Vectorizer[Point[D]]): Seq[Point[D]]

  def toMatrix(points: Seq[Point[D]])(implicit vectorizer: Vectorizer[Point[D]]): DenseMatrix[Double] = {
    val dim: Int = vectorizer.dim
    val m = DenseMatrix.zeros[Double](points.length, dim)
    points.zipWithIndex.par.foreach { case (p, i) =>
      m(i, ::) := vectorizer.vectorize(p).t
    }
    m
  }

  // Vectorize with x_1x, x_1y, x_1z, x_2x ...
  def toVector(points: Seq[Point[D]])(implicit vectorizer: Vectorizer[Point[D]]): DenseVector[Double] = {
    DenseVector(points.flatMap { p =>
      vectorizer.vectorize(p).toArray
    }.toArray)
  }

  def toLMMatrix(points: Seq[Point[D]], pointIds: Seq[PointId], n: Int)(implicit vectorizer: Vectorizer[Point[D]]): CSCMatrix[Double] = {
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
}

object PointSequenceConverter {

  def matrixTo1Dpoints(m: DenseMatrix[Double]): IndexedSeq[Point[_1D]] = {
    (0 until m.rows).map { r =>
      Point1D(x = m(r, 0))
    }
  }

  def matrixTo2Dpoints(m: DenseMatrix[Double]): IndexedSeq[Point[_2D]] = {
    (0 until m.rows).map { r =>
      Point2D(x = m(r, 0), y = m(r, 1))
    }
  }

  def matrixTo3Dpoints(m: DenseMatrix[Double]): IndexedSeq[Point[_3D]] = {
    (0 until m.rows).map { r =>
      Point3D(x = m(r, 0), y = m(r, 1), z = m(r, 2))
    }
  }

  def vectorTo1Dpoints(v: DenseVector[Double]): IndexedSeq[Point[_1D]] = {
    v.map { i =>
      Point1D(i)
    }.toArray.toIndexedSeq
  }

  def vectorTo2Dpoints(v: DenseVector[Double]): IndexedSeq[Point[_2D]] = {
    (0 until v.length / 2).map { i =>
      Point2D(x = v(i * 2), y = v(i * 2 + 1))
    }
  }

  def vectorTo3Dpoints(v: DenseVector[Double]): IndexedSeq[Point[_3D]] = {
    (0 until v.length / 3).map { i =>
      Point3D(x = v(i * 3), y = v(i * 3 + 1), z = v(i * 3 + 2))
    }
  }

  implicit object denseMatrixToPoint1DSequence extends PointSequenceConverter[_1D] {
    implicit val vectorizer: Point.Point1DVectorizer.type = Point1DVectorizer

    override def toPointSequence(m: DenseMatrix[Double])(implicit vectorizer: Vectorizer[Point[_1D]]): Seq[Point[_1D]] = {
      matrixTo1Dpoints(m)
    }

    override def toPointSequence(v: DenseVector[Double])(implicit vectorizer: Vectorizer[Point[_1D]]): Seq[Point[_1D]] = {
      vectorTo1Dpoints(v)
    }
  }

  implicit object denseMatrixToPoint2DSequence extends PointSequenceConverter[_2D] {
    implicit val vectorizer: Point.Point2DVectorizer.type = Point2DVectorizer

    override def toPointSequence(m: DenseMatrix[Double])(implicit vectorizer: Vectorizer[Point[_2D]]): Seq[Point[_2D]] = {
      matrixTo2Dpoints(m)
    }

    override def toPointSequence(v: DenseVector[Double])(implicit vectorizer: Vectorizer[Point[_2D]]): Seq[Point[_2D]] = {
      vectorTo2Dpoints(v)
    }
  }

  implicit object denseMatrixToPoint3DSequence extends PointSequenceConverter[_3D] {
    implicit val vectorizer: Point.Point3DVectorizer.type = Point3DVectorizer

    override def toPointSequence(m: DenseMatrix[Double])(implicit vectorizer: Vectorizer[Point[_3D]]): Seq[Point[_3D]] = {
      matrixTo3Dpoints(m)
    }

    override def toPointSequence(v: DenseVector[Double])(implicit vectorizer: Vectorizer[Point[_3D]]): Seq[Point[_3D]] = {
      vectorTo3Dpoints(v)
    }
  }

}
