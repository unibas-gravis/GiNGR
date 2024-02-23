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

import breeze.linalg.DenseMatrix
import scalismo.common.{
  DiscreteDomain,
  UnstructuredPoints,
  UnstructuredPointsDomain,
  UnstructuredPointsDomain3D,
  Vectorizer
}
import scalismo.geometry.{_3D, Point}
import scalismo.mesh.{TetrahedralMesh, TetrahedralMesh3D, TriangleMesh, TriangleMesh3D}

trait DiscreteDomainConverter[DDomain[_3D] <: DiscreteDomain[_3D]] {
  def denseMatrixToDomain(
    mat: DenseMatrix[Double],
    reference: DDomain[_3D]
  ): DDomain[_3D]

  def toMatrix(dc: DDomain[_3D])(implicit
    vectorizer: Vectorizer[Point[_3D]]
  ): DenseMatrix[Double] = {
    PointSequenceConverter.toMatrix(dc.pointSet.points.toSeq)
  }
}

object DiscreteDomainConverter {

  implicit object denseMatrixToPointDomain3D extends DiscreteDomainConverter[UnstructuredPointsDomain] {
    override def denseMatrixToDomain(
      mat: DenseMatrix[Double],
      reference: UnstructuredPointsDomain[_3D]
    ): UnstructuredPointsDomain[_3D] = {
      UnstructuredPointsDomain3D(PointSequenceConverter.toPointSequence(mat).toIndexedSeq)
    }
  }

  implicit object denseMatrixToTriangleMesh3D extends DiscreteDomainConverter[TriangleMesh] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: TriangleMesh[_3D]): TriangleMesh[_3D] = {
      TriangleMesh3D(
        UnstructuredPoints(PointSequenceConverter.toPointSequence(mat).toIndexedSeq),
        reference.triangulation
      )
    }
  }

  implicit object denseMatrixToTetrahedralMesh3D extends DiscreteDomainConverter[TetrahedralMesh] {
    override def denseMatrixToDomain(
      mat: DenseMatrix[Double],
      reference: TetrahedralMesh[_3D]
    ): TetrahedralMesh[_3D] = {
      TetrahedralMesh3D(PointSequenceConverter.toPointSequence(mat).toIndexedSeq, reference.tetrahedralization)
    }
  }

}
