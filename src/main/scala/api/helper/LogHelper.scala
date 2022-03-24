package api.helper

import api.ModelFittingParameters
import api.sampling.loggers.{JSONStateLogger, jsonLogFormat}
import scalismo.geometry._3D
import scalismo.mesh.TriangleMesh
import scalismo.statisticalmodel.PointDistributionModel

import scala.annotation.tailrec

object LogHelper {

  def samplesFromLog(log: IndexedSeq[jsonLogFormat], takeEveryN: Int = 50, total: Int = 100, burnIn: Int = 0): IndexedSeq[(jsonLogFormat, Int)] = {
    @tailrec
    def getLogIndex(i: Int): Int = {
      if (log(i).status) i
      else getLogIndex(i - 1)
    }
    println("Log length: " + log.length)
    val indexes = (burnIn until math.min(log.length, total) by takeEveryN).map(i => getLogIndex(i))
    val filtered = indexes.map(i => (log(i), i))
    filtered.take(math.min(total, filtered.length))
  }

  def logSamples2shapes(model: PointDistributionModel[_3D, TriangleMesh], log: IndexedSeq[jsonLogFormat]): IndexedSeq[TriangleMesh[_3D]] = {
    log.map { l =>
      val modelPars = JSONStateLogger.jsonFormatToModelFittingParameters(l)
      ModelFittingParameters.modelInstanceShapePoseScale(model, modelPars)
    }
  }
}
