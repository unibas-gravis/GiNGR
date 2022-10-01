// //> using scala "3"
// //> using lib "ch.unibas.cs.gravis::gingr:0.1.0-SNAPSHOT"
// //> using lib "ch.unibas.cs.gravis::scalismo-ui:0.91.0"

// import gingr.api.ModelFittingParameters
// import gingr.api.helper.{LogHelper, PosteriorHelper}
// import gingr.api.sampling.loggers.JSONStateLogger
// import gingr.simple.DemoDatasetLoader
// import scalismo.ui.api.ScalismoUI
// import java.awt.Color
// import java.io.File

// @main def main() =
//   val burnInPhase = 100

//   val (model, _) = DemoDatasetLoader.femur.modelGauss()
//   val (target, _) = DemoDatasetLoader.femur.target()

//   val jsonFile = new File("data/femur/targetFittingICP.json")
//   val fullLog = JSONStateLogger.loadLog(jsonFile)

//   val logSamples = LogHelper.samplesFromLog(fullLog, takeEveryN = 50, total = 10000, burnInPhase)
//   println(s"Number of samples from log: ${logSamples.length}/${fullLog.length - burnInPhase}")
//   val logShapes = LogHelper.logSamples2shapes(model, logSamples.map(_._1))

//   val bestLogInstance = JSONStateLogger.getBestStateFromLog(fullLog)
//   val bestModelParameters = JSONStateLogger.jsonFormatToModelFittingParameters(bestLogInstance)
//   val best = ModelFittingParameters.modelInstanceShapePoseScale(model, bestModelParameters)

//   val colorMap_normalVariance = PosteriorHelper.computeDistanceMapFromMeshesNormal(logShapes, best)
//   val colorMap_posteriorEstimate = PosteriorHelper.computeDistanceMapFromMeshesTotal(logShapes, best)

//   val ui = ScalismoUI(s"Posterior visualization")
//   val modelGroup = ui.createGroup("model")
//   val targetGroup = ui.createGroup("target")
//   val colorGroup = ui.createGroup("color")
//   val showModel = ui.show(modelGroup, model, "model")
//   showModel.referenceView.opacity = 0.0
//   val showTarget = ui.show(targetGroup, target, "target")
//   showTarget.color = Color.YELLOW
//   showTarget.opacity = 0.0
//   ui.show(colorGroup, colorMap_posteriorEstimate, "posterior")
//   ui.show(colorGroup, colorMap_normalVariance, "normal")
