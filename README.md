# GiNGR Framework
GiNGR: Generalized Iterative Non-Rigid Point Cloud and Surface Registration Using Gaussian Process Regression. 

[Link to the paper](https://arxiv.org/pdf/2203.09986.pdf).

BibTex:
```bibtex
@article{madsen2022gingr,
  title={GiNGR: Generalized Iterative Non-Rigid Point Cloud and Surface Registration Using Gaussian Process Regression},
  author={Madsen, Dennis and Aellen, Jonathan and Morel-Forster, Andreas and Vetter, Thomas and L{\"u}thi, Marcel},
  journal={arXiv preprint arXiv:2203.09986},
  year={2022}
}
```

The GiNGR framework allows one to perform non-rigid registration with an iterative algorithm that uses Gaussian Process Regression in each iteration to find its next state.

Existing algorithms can be converted into the GiNGR framework and compared based on three properties:
 
 1. **Kernel function**: how similar should the deformation of neighboring points be - this is determined based on their correlation.
 2. **Correspondence estimation function**: how to calculate/estimate anatomically corresponding points between the moving instance (reference) and the target (e.g., use the closest point as a naive approach).
 3. **Observation uncertainty**: what is the noise assumption of the correspondence estimations?

This framework contains a general library to input these three properties. 

The core part of the GiNGR framework is found in `gingr/api/GingrAlgorithm`, with the `update` function performing one iteration of a GiNGR update.
Different pre-implemented configuration files can be found under `gingr/api/registration/config` for the CPD and the ICP algorithms.

## Installation
To use the GiNGR framework, you can make use of the maven repository version by including the following in your Scala 3 script:

```scala
//> using lib "ch.unibas.cs.gravis::gingr:1.0-RC1"
```

If using SBT, then add the following to your `build.sbt`:

```scala
libraryDependencies += "ch.unibas.cs.gravis" %% "gingr" % "1.0-RC1"
```

Or you can install GiNGR to your local repo (_.ivy2_) by running `sbt publishLocal`.

The examples can be run with [Scala-CLI](https://scala-cli.virtuslab.org/).
To run the first tutorial, first CD into the example directory and run:

```scala
# scala-cli project.scala DemoHelper CreateFemurGPMM.scala
```

Alternatively, the examples can be run with the VSCODE IDE. For installation help, please see https://scalismo.org/docs/Setup/vscode.

After installing VSCODE:

 - Go to the examples folder: `cd examples`
 - Setup Code IDE with `scala-cli setup-ide .`
 - Open the IDE with `code .`
 - Now run the individual examples

## General use
To use GiNGR, one needs to specify the deformation model as a GPMM model, the correspondence estimation function, and the uncertainty update.
### Define the prior model
The creation of the GPMM is separate from the registration step. Look in the `examples` folder where demo scripts have been created to compute and visualize GPMMs for an Armadillo, Bunny and a Femur bone. The deformation model can be evaluated in the UI by sampling from it.
### Configure the registration algorithm
The next step is to define the correspondence and uncertainty estimation update. 
For this, default configurations have been implemented for CPD and ICP. 
Simple Demo applications can be found in `examples/DemoICP` and `examples/DemoCPD`

The demo scripts both perform deterministic and probabilistic registration one after the other. 

### GiNGR state
In each iteration, a new GiNGR state is computed which contains the GPMM model, the current `fit`, the target as well as all the GPMM model parameters (non-rigid and global pose).

### Deterministic vs Probabilistic
The probabilistic implementation is based on the ICP-Proposal repository: https://github.com/unibas-gravis/icp-proposal
#### Visualizing the posterior output from probabilistic fitting
The posterior output from the ICP probabilistic registration of the femur bone can be visualized with `apps/registration/DemoPosteriorVisualizationFemur`.

### Inclusion of Landmarks
In `examples/DemoLandmarks` we compare 10 iterations of CPD for the Armadillo with and without the use of landmarks.

### Multi-resolution fitting
In `examples/DemoMultiResolution` we perform 3 different registrations of GiNGR one after the other. 
First CPD is used on a very coarse mesh (100 vertices), then CPD is used on a medium fine mesh (500 vertices) and finally, ICP is used on a finer mesh (1000 points) to get the fine details of the target mesh.


## Implementation of existing algorithms
In the GiNGR code base, the basic implementations of existing algorithms can also be found for comparison. The algorithms are found under `gingr/other/algorithms`
### CPD: Coherent Point Drift (only Naïve version)
Implementation of the CPD algorithm from https://arxiv.org/pdf/0905.2635.pdf
### BCPD: Bayesian Coherent Point Drift (only Naïve version)
Implementation of the BCPD algorithm from https://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8985307
### Optimal Step ICP (N-ICP-T and N-ICP-A):
Implementation of the non-rigid ICP algorithms from https://gravis.dmi.unibas.ch/publications/2007/CVPR07_Amberg.pdf
