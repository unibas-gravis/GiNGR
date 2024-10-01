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

package gingr.api.registration

import gingr.api.helper.RegistrationComparison
import gingr.api.sampling.IndependentPoints
import gingr.api.sampling.evaluators.{EvaluationMode, ModelToTargetEvaluation}
import gingr.api.sampling.loggers.JSONStateLogger
import gingr.api.{
  FittingStatuses,
  GeneralRegistrationState,
  GingrAlgorithm,
  GingrConfig,
  GingrRegistrationState,
  GlobalTranformationType,
  ModelFittingParameters,
  ProbabilisticSettings,
  RigidTransforms
}
import scalismo.common.interpolation.NearestNeighborInterpolator
import scalismo.geometry.{_3D, Landmark}
import scalismo.mesh.TriangleMesh
import scalismo.sampling.loggers.ChainStateLogger
import scalismo.statisticalmodel.PointDistributionModel
import scalismo.transformations.TranslationAfterRotation
import scalismo.utils.Random

import java.io.File

class SimpleRegistrator[State <: GingrRegistrationState[State], Config <: GingrConfig, Algorithm <: GingrAlgorithm[
  State,
  Config
]](
  algorithm: Algorithm,
  config: Config,
  model: PointDistributionModel[_3D, TriangleMesh],
  target: TriangleMesh[_3D],
  initialModelParameterTransform: Option[TranslationAfterRotation[_3D]] = None,
  modelLandmarks: Option[Seq[Landmark[_3D]]] = None,
  targetLandmarks: Option[Seq[Landmark[_3D]]] = None,
  evaluationMode: EvaluationMode = ModelToTargetEvaluation,
  evaluatorUncertainty: Double = 1.0,
  evaluatedPoints: Option[Int] = None,
  logFileFittingParameters: Option[File] = None
)(implicit rnd: Random) {

  def runDecimated(
    modelPoints: Int,
    targetPoints: Int,
    generalState: Option[GeneralRegistrationState] = None,
    globalTransformation: GlobalTranformationType = RigidTransforms,
    probabilistic: Boolean = false,
    randomMixture: Double = 0.5,
    callback: Option[ChainStateLogger[State]] = None
  ): State = {
    val initState = decimateState(generalState, globalTransformation, modelPoints, targetPoints)
    run(Option(initState), globalTransformation, probabilistic, randomMixture, callback)
  }

  private def combineStates(generalState: GeneralRegistrationState): State = {
    algorithm.initializeState(
      generalState
        .clearIteration()
        .updateStatus(FittingStatuses.None),
      config
    )
  }

  private def decimateState(
    generalState: Option[GeneralRegistrationState],
    globalTransformation: GlobalTranformationType,
    modelPoints: Int,
    targetPoints: Int
  ): GeneralRegistrationState = {
    val newRef = model.reference.operations.decimate(modelPoints)
    val decimatedModel = model.newReference(newRef, NearestNeighborInterpolator())
    val decimatedTarget = target.operations.decimate(targetPoints)
    val initState =
      if (generalState.isDefined) combineStates(generalState.get)
      else createInitialState(decimatedModel, decimatedTarget, globalTransformation, initialModelParameterTransform)
    initState
      .updateGeneral(
        initState.general.copy(
          model = decimatedModel,
          target = decimatedTarget,
          fit = ModelFittingParameters.modelInstanceShapePoseScale(decimatedModel, initState.general.modelParameters)
        )
      )
      .general
  }

  def createInitialState(
    model: PointDistributionModel[_3D, TriangleMesh],
    target: TriangleMesh[_3D],
    globalTransformation: GlobalTranformationType,
    modelTranform: Option[TranslationAfterRotation[_3D]] = None
  ): State = {
    val generalState: GeneralRegistrationState =
      if (modelLandmarks.nonEmpty && targetLandmarks.nonEmpty)
        GeneralRegistrationState(
          model,
          modelLandmarks.get,
          target,
          targetLandmarks.get,
          globalTransformation,
          modelTranform
        )
      else
        GeneralRegistrationState(model, target, globalTransformation, modelTranform)
    algorithm.initializeState(generalState, config)
  }

  def run(
    generalState: Option[GeneralRegistrationState] = None,
    globalTransformation: GlobalTranformationType = RigidTransforms,
    probabilistic: Boolean = false,
    randomMixture: Double = 0.5,
    callback: Option[ChainStateLogger[State]] = None
  ): State = {
    val state =
      if (generalState.isDefined) combineStates(generalState.get)
      else createInitialState(model, target, globalTransformation, initialModelParameterTransform)
    val evaluator: IndependentPoints[State] = IndependentPoints(
      state = state,
      uncertainty = evaluatorUncertainty,
      mode = evaluationMode,
      evaluatedPoints = evaluatedPoints
    )
    val jsonLogger = if (probabilistic) Some(JSONStateLogger(evaluator, logFileFittingParameters)) else None
    val finalState = algorithm.run(
      initialState = state,
      acceptRejectLogger = jsonLogger,
      callBackLogger = callback,
      probabilisticSettings =
        if (probabilistic) Some(ProbabilisticSettings[State](evaluator, randomMixture = randomMixture)) else None
    )
    val fit = ModelFittingParameters.modelInstanceShapePoseScale(model, finalState.general.modelParameters)
    jsonLogger.foreach(_.printAcceptInfo())
    jsonLogger.foreach(_.writeLog())
    println("Final registration with full resolution meshes:")
    RegistrationComparison.evaluateReconstruction2GroundTruthBoundaryAware("", fit, target)
    finalState.updateGeneral(finalState.general.updateFit(fit))
  }
}
