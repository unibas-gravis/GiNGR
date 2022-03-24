package api.registration.utils

import breeze.linalg.DenseMatrix
import scalismo.common._
import scalismo.geometry.Point.{Point1DVectorizer, Point2DVectorizer, Point3DVectorizer}
import scalismo.geometry._
import scalismo.mesh._

trait DiscreteDomainConverter[D, DDomain[D] <: DiscreteDomain[D]] {
  def denseMatrixToDomain(
      mat: DenseMatrix[Double],
      reference: DDomain[D]
  )(implicit pointSequenceConverter: PointSequenceConverter[D]): DDomain[D]

  def toMatrix(dc: DDomain[D])(implicit vectorizer: Vectorizer[Point[D]], pointSequenceConverter: PointSequenceConverter[D]): DenseMatrix[Double] = {
    pointSequenceConverter.toMatrix(dc.pointSet.points.toSeq)
  }
}

object DiscreteDomainConverter {

  implicit object denseMatrixToPointDomain1D extends DiscreteDomainConverter[_1D, UnstructuredPointsDomain] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: UnstructuredPointsDomain[_1D])(
        implicit pointSequenceConverter: PointSequenceConverter[_1D]): UnstructuredPointsDomain[_1D] = {
      UnstructuredPointsDomain.Create.CreateUnstructuredPointsDomain1D.create(pointSequenceConverter.toPointSequence(mat).toIndexedSeq)
    }
  }

  implicit object denseMatrixToPointDomain2D extends DiscreteDomainConverter[_2D, UnstructuredPointsDomain] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: UnstructuredPointsDomain[_2D])(
        implicit pointSequenceConverter: PointSequenceConverter[_2D]): UnstructuredPointsDomain[_2D] = {
      UnstructuredPointsDomain.Create.CreateUnstructuredPointsDomain2D.create(pointSequenceConverter.toPointSequence(mat).toIndexedSeq)
    }
  }

  implicit object denseMatrixToPointDomain3D extends DiscreteDomainConverter[_3D, UnstructuredPointsDomain] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: UnstructuredPointsDomain[_3D])(
        implicit pointSequenceConverter: PointSequenceConverter[_3D]): UnstructuredPointsDomain[_3D] = {
      UnstructuredPointsDomain.Create.CreateUnstructuredPointsDomain3D.create(pointSequenceConverter.toPointSequence(mat).toIndexedSeq)
    }
  }

  implicit object denseMatrixToTriangleMesh2D extends DiscreteDomainConverter[_2D, TriangleMesh] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: TriangleMesh[_2D])(
        implicit pointSequenceConverter: PointSequenceConverter[_2D]): TriangleMesh[_2D] = {
      TriangleMesh2D(UnstructuredPoints2D(pointSequenceConverter.toPointSequence(mat).toIndexedSeq), reference.triangulation)
    }
  }

  implicit object denseMatrixToTriangleMesh3D extends DiscreteDomainConverter[_3D, TriangleMesh] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: TriangleMesh[_3D])(
        implicit pointSequenceConverter: PointSequenceConverter[_3D]): TriangleMesh[_3D] = {
      TriangleMesh3D(UnstructuredPoints3D(pointSequenceConverter.toPointSequence(mat).toIndexedSeq), reference.triangulation)
    }
  }

  implicit object denseMatrixToTetrahedralMesh3D extends DiscreteDomainConverter[_3D, TetrahedralMesh] {
    override def denseMatrixToDomain(mat: DenseMatrix[Double], reference: TetrahedralMesh[_3D])(
        implicit pointSequenceConverter: PointSequenceConverter[_3D]): TetrahedralMesh[_3D] = {
      TetrahedralMesh3D(pointSequenceConverter.toPointSequence(mat).toIndexedSeq, reference.tetrahedralization)
    }
  }

}
