package api

import scalismo.common.PointId
import scalismo.geometry.{Point, _3D}

case class CorrespondencePairs(pairs: IndexedSeq[(PointId, Point[_3D])])

object CorrespondencePairs {
  def empty(): CorrespondencePairs = new CorrespondencePairs(IndexedSeq())
}