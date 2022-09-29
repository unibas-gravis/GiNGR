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

package gingr.api

import gingr.api.sampling.evaluators.EvaluatorWrapper
import gingr.api.sampling.generators.{GeneratorWrapperDeterministic, GeneratorWrapperStochastic}
import gingr.api.sampling.loggers.BestAndCurrentSampleLogger
import gingr.api.sampling.{AcceptAll, Evaluator, Generator}
import scalismo.common.PointId
import scalismo.geometry.{Point, _3D}
import scalismo.mesh.TriangleMesh
import scalismo.registration.LandmarkRegistration
import scalismo.sampling.algorithms.MetropolisHastings
import scalismo.sampling.loggers.ChainStateLogger.implicits._
import scalismo.sampling.loggers.{AcceptRejectLogger, ChainStateLogger, ChainStateLoggerContainer}
import scalismo.sampling.proposals.MixtureProposal
import scalismo.sampling.proposals.MixtureProposal.implicits._
import scalismo.sampling.{ProposalGenerator, TransitionProbability}
import scalismo.statisticalmodel.{MultivariateNormalDistribution, PointDistributionModel}
import scalismo.transformations.{
  Scaling,
  TranslationAfterRotation,
  TranslationAfterScalingAfterRotation,
  TranslationAfterScalingAfterRotationSpace3D
}
import scalismo.utils.{Memoize, Random}

case class ProbabilisticSettings[State <: GingrRegistrationState[State]](
    evaluators: Evaluator[State],
    randomMixture: Double = 0.5
) {
  require(randomMixture >= 0.0 && randomMixture <= 1.0)
}

trait GingrConfig {
  def maxIterations: Int
  def threshold: Double
  def converged: (GeneralRegistrationState, GeneralRegistrationState, Double) => Boolean
  def useLandmarkCorrespondence: Boolean
}

trait GingrRegistrationState[State] {
  def general: GeneralRegistrationState
  def config: GingrConfig
  def updateGeneral(update: GeneralRegistrationState): State
}

trait GingrAlgorithm[State <: GingrRegistrationState[State], config <: GingrConfig] {
  val getCorrespondence: State => CorrespondencePairs
  val getUncertainty: (PointId, State) => MultivariateNormalDistribution
  private val cashedPosterior = Memoize(computePosterior, 10)

  def name: String

  def initializeState(general: GeneralRegistrationState, config: config): State

  def estimateRigidTransform(
      current: TriangleMesh[_3D],
      update: IndexedSeq[(PointId, Point[_3D])]
  ): TranslationAfterScalingAfterRotation[_3D] = {
    val pairs = update.map(f => (current.pointSet.point(f._1), f._2))
    val t     = LandmarkRegistration.rigid3DLandmarkRegistration(pairs, Point(0, 0, 0))
    TranslationAfterScalingAfterRotation(t.translation, Scaling(1.0), t.rotation)
  }

  def estimateSimilarityTransform(
      current: TriangleMesh[_3D],
      update: IndexedSeq[(PointId, Point[_3D])]
  ): TranslationAfterScalingAfterRotation[_3D] = {
    val pairs = update.map(f => (current.pointSet.point(f._1), f._2))
    LandmarkRegistration.similarity3DLandmarkRegistration(pairs, Point(0, 0, 0))
  }

  def instance(model: PointDistributionModel[_3D, TriangleMesh], state: GeneralRegistrationState): TriangleMesh[_3D] = {
    model.instance(state.modelParameters.shape.parameters).transform(state.modelParameters.similarityTransform)
  }

  /** Runs the actual registration with the provided configuration through the passed parameters.
    *
    * @param initialState
    *   State from which the registration is started.
    * @param callBackLogger
    *   Logger to provide call back functionality to user after each iteration
    * @param acceptRejectLogger
    *   Logger to use for advanced file logging
    * @param probabilisticSettings
    *   Evaluator to be used if probabilistic registration is set
    * @param generators
    *   Pass in external generators to use
    * @param rnd
    *   Implicit random number generator.
    * @return
    *   Returns the best sample of the chain given the evaluator..
    */
  def run(
      initialState: State,
      acceptRejectLogger: Option[AcceptRejectLogger[State]],
      callBackLogger: Option[ChainStateLogger[State]],
      probabilisticSettings: Option[ProbabilisticSettings[State]],
      generators: Option[ProposalGenerator[State] with TransitionProbability[State]] = None
  )(implicit rnd: Random): State = {
    val evaluator             = probabilisticSettings.getOrElse(ProbabilisticSettings(AcceptAll(), randomMixture = 0.0))
    val registrationEvaluator = EvaluatorWrapper(probabilisticSettings.nonEmpty, evaluator.evaluators)
    val registrationGenerator = generatorCombined(probabilisticSettings, generators)
    val bestSampleLogger      = BestAndCurrentSampleLogger[State](registrationEvaluator)
    val logs =
      if (callBackLogger.isDefined) ChainStateLoggerContainer(Seq(callBackLogger.get, bestSampleLogger))
      else bestSampleLogger
    val mhChain = MetropolisHastings[State](registrationGenerator, registrationEvaluator)
    acceptRejectLogger.foreach(_.accept(initialState, initialState, registrationGenerator, registrationEvaluator))

    val states =
      if (acceptRejectLogger.isDefined) mhChain.iterator(initialState, acceptRejectLogger.get).loggedWith(logs)
      else mhChain.iterator(initialState).loggedWith(logs)

    // we need to query if there is a next element, otherwise due to laziness the chain is not calculated
    var currentState: Option[GeneralRegistrationState] = None
    states
      .take(initialState.config.maxIterations)
      .dropWhile { state =>
        if (probabilisticSettings.isEmpty) {
          val converged =
            if (currentState.nonEmpty) state.config.converged(currentState.get, state.general, state.config.threshold)
            else false
          currentState = Some(state.general)
          if (converged) println(s"Registration converged")
          !converged
        } else true
      }
      .hasNext

    // If probabilistic: select the sample with the best posterior value. If deterministic: select the last sample.
    val fit = probabilisticSettings match {
      case Some(_) => bestSampleLogger.currentBestSample().get
      case _       => bestSampleLogger.currentSample().get
    }
    fit
  }

  def generatorCombined(
      probabilisticSettings: Option[ProbabilisticSettings[State]],
      mixing: Option[ProposalGenerator[State] with TransitionProbability[State]]
  )(implicit rnd: Random): ProposalGenerator[State] with TransitionProbability[State] = {
    probabilisticSettings match {
      case Some(setting) =>
        val mix = mixing.getOrElse(
          new Generator().DefaultRandom()
        ) // Use passed in generator or use default random generator to mix with
        val informedGenerator = GeneratorWrapperStochastic(update, cashedPosterior, name)
        MixtureProposal(setting.randomMixture *: mix + (1.0 - setting.randomMixture) *: informedGenerator)
      case _ => GeneratorWrapperDeterministic(update, name)
    }
  }

  def update(current: State, probabilistic: Boolean)(implicit rnd: Random): State = {
    try {
      val posterior            = cashedPosterior(current)
      val shapeproposal        = if (!probabilistic) posterior.mean else posterior.sample()
      val transformedModelInit = current.general.model.transform(current.general.modelParameters.rigidTransform)

      val newCoefficients          = transformedModelInit.coefficients(shapeproposal)
      val currentShapeCoefficients = current.general.modelParameters.shape.parameters
      val newCoefficientsCombined =
        currentShapeCoefficients + (newCoefficients - currentShapeCoefficients) * current.general.stepLength

      val newshape = transformedModelInit.instance(newCoefficientsCombined)

      val currentFitNoTransform = current.general.model.instance(current.general.modelParameters.shape.parameters)

      // If global transform is set, then extract rigid part from non-rigid to be explained by global pose parameters
      val globalTransform: TranslationAfterScalingAfterRotation[_3D] = current.general.globalTransformation match {
        case SimilarityTransforms => estimateSimilarityTransform(currentFitNoTransform, newshape)
        case RigidTransforms      => estimateRigidTransform(currentFitNoTransform, newshape)
        case _                    => TranslationAfterScalingAfterRotationSpace3D(Point(0, 0, 0)).identityTransformation
      }
      val newGlobalAlignment: TranslationAfterRotation[_3D] =
        TranslationAfterRotation(globalTransform.translation, globalTransform.rotation)
      val transformedModel = current.general.model.transform(newGlobalAlignment)
      val alpha            = transformedModel.coefficients(newshape)

      val general = current.general
        .updateTranslation(newGlobalAlignment.translation.t)
        .updateRotation(newGlobalAlignment.rotation)
        .updateScaling(ScaleParameter(globalTransform.scaling.s))
        .updateShapeParameters(ShapeParameters(alpha))
      val newState  = current.updateGeneral(general)
      val newSigma2 = updateSigma2(newState)
      newState.updateGeneral(newState.general.updateSigma2(newSigma2))
    } catch {
      case _: Throwable => current
    }
  }

  def updateSigma2(current: State): Double = {
    current.general.sigma2
  }

  def estimateRigidTransform(
      current: TriangleMesh[_3D],
      update: TriangleMesh[_3D]
  ): TranslationAfterScalingAfterRotation[_3D] = {
    val t = LandmarkRegistration.rigid3DLandmarkRegistration(
      current.pointSet.points.toSeq.zip(update.pointSet.points.toSeq),
      Point(0, 0, 0)
    )
    TranslationAfterScalingAfterRotation(t.translation, Scaling(1.0), t.rotation)
  }

  def estimateSimilarityTransform(
      current: TriangleMesh[_3D],
      update: TriangleMesh[_3D]
  ): TranslationAfterScalingAfterRotation[_3D] = {
    LandmarkRegistration.similarity3DLandmarkRegistration(
      current.pointSet.points.toSeq.zip(update.pointSet.points.toSeq),
      Point(0, 0, 0)
    )
  }

  private def computePosterior(current: State): PointDistributionModel[_3D, TriangleMesh] = {
    val correspondences = getCorrespondence(current)
    val correspondencesWithUncertainty = correspondences.pairs.map { pair =>
      val (pid, point) = pair
      val uncertainty  = getUncertainty(pid, current)
      (pid, point, uncertainty)
    }

    val observationsWithUncertainty = if (current.config.useLandmarkCorrespondence) {
      // note: I would propose to remove the estimated correspondences for given landmarks
      val landmarksToUse = current.general.landmarkCorrespondences.map(_._1).toSet
      val filteredCorrespondencesWithUncertainty = correspondencesWithUncertainty.filter { case (pid, _, _) =>
        !landmarksToUse.contains(pid)
      }
      filteredCorrespondencesWithUncertainty ++ current.general.landmarkCorrespondences
    } else {
      correspondencesWithUncertainty
    }
    // Need to realign model first
    current.general.model
      .transform(current.general.modelParameters.rigidTransform)
      .posterior(observationsWithUncertainty)
  }
}
