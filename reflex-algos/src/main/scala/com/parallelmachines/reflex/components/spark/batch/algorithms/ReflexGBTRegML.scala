package com.parallelmachines.reflex.components.spark.batch.algorithms

import com.parallelmachines.reflex.components.spark.batch.algorithms.MlMethod.MlMethodType
import com.parallelmachines.reflex.components.{ComponentAttribute, FeaturesColComponentAttribute, LabelColComponentAttribute, PredictionColComponentAttribute}
import org.apache.spark.ml.PipelineStage
import org.apache.spark.ml.regression.GBTRegressor
import com.parallelmachines.reflex.common.constants.McenterTags


class ReflexGBTRegML extends ReflexSparkMLAlgoBase {
  override val label: String = "Gradient-boosted Tree Regression Training"
  override lazy val defaultModelName: String = "Gradient-boosted Tree Regression"
  override val description: String = "Gradient-boosted Tree Regression Batch Training"
  override val version: String = "1.0.0"
  addTags(McenterTags.explainable)

  val gbtr = new GBTRegressor()


  val tempSharedPath = ComponentAttribute("tempSharedPath", "",
    "temp Shared Path", "Temporary shared path for model transfer, " +
      "paths with prefix file:// or hdfs://", optional = true)
  val significantFeaturesNumber = ComponentAttribute("significantFeaturesNumber", 0,
    "significant Features Number", "Number of significant features in Feature Importance vector. " +
      "0 indicates not presenting feature importance.. (>= 0) (Default: 0)",
    optional = true).setValidator(x => x >= 0)
  val maxDepth = ComponentAttribute("maxDepth", 5, "Maximum Depth", "Maximum depth of the tree. (>= 0)" +
    " E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes." +
    "(Default: 5)", optional = true).setValidator(x => x >= 0)
  val minInstancesPerNode = ComponentAttribute("minInstancesPerNode", 1, "Minimum Instances Per Node",
    "Minimum number of instances each child must have after split. If a split causes the left or" +
      " right child to have fewer than minInstancesPerNode, the split will be discarded as invalid." +
      " Should be >= 1. (Default: 1)", optional = true).setValidator(x => x >= 1)
  val minInfoGain = ComponentAttribute("minInfoGain", 0.0, "Minimum Information Gain", "Minimum information gain" +
    " for a split to be considered at a tree node.  Should be >= 0. (Default: 0)", optional = true)
    .setValidator(x => x >= 0.0)
  val maxMemoryInMB = ComponentAttribute("maxMemoryInMB", 256, "Maximum Memory In MB", "" +
    "Maximum memory in MB allocated to histogram aggregation.  Should be >= 256MB." +
    "(Default: 256)", optional = true).setValidator(x => x >= 256)
  val cacheNodeIds = ComponentAttribute("cacheNodeIds", false, "Cache Node Ids", "If false, the " +
    "algorithm will pass trees to executors to match instances with nodes. If true, " +
    "the algorithm will cache node IDs for each instance. Caching can speed up training of " +
    "deeper trees. (Default: false)", optional = true)
  val checkpointInterval = ComponentAttribute("checkpointInterval", 10, "Checkpoint Interval",
    "Specifies how often to checkpoint the cached node IDs. E.g. 10 means that the cache will" +
      " get checkpointed every 10 iterations. This is only used if cacheNodeIds is true and " +
      "if the checkpoint directory is set in [[org.apache.spark.SparkContext]]. Must be at" +
      " least 1. (Default: 10)", optional = true).setValidator(x => x >= 1)
  val seed = ComponentAttribute("seed", "Random", "Seed", "random seed (A number). (Default: Random) ", optional = true)
  val maxBins = ComponentAttribute("maxBins", 32, "Maximum number of Bins", "Maximum number of bins used for" +
    " splitting features (Default: 32). Must be >=2", optional = true).setValidator(x => x >= 2)
  val subsamplingRate = ComponentAttribute("subsamplingRate", 1.0, "Sub-sampling Rate",
    "Fraction of the training data used for learning each decision tree, in range (0, 1]." +
    " (Default: 1)", optional = true).setValidator(x => (x > 0.0) & (x <= 1.0))
  val maxIter = ComponentAttribute("maxIter", 10, "Maximum Iterations", "maximum number of " +
    "iterations (>= 0). (Default: 10)", optional = true).setValidator(x => x > 0.0)
  val stepSize = ComponentAttribute("stepSize", 0.1, "Step Size", "Step size (a.k.a. learning rate) " +
    "in interval (0, 1] for shrinking the contribution of each estimator. " +
    "(Default: 0.1)", optional = true).setValidator(x => (x > 0.0) & (x <= 1.0))
  val lossType = ComponentAttribute("lossType", "squared", "Loss Type", "Loss function which GBT" +
    " tries to minimize. (Options: 'squared'(Default), 'absolute')",optional = true)
  lossType.setOptions(List[(String, String)](("squared", "squared"), ("absolute", "absolute")))

  val labelCol = LabelColComponentAttribute()
  val featuresCol = FeaturesColComponentAttribute()
  val predictionCol = PredictionColComponentAttribute() //prediction column produced in transform

  attrPack.add(tempSharedPath, significantFeaturesNumber, maxDepth, maxBins, subsamplingRate, seed, maxIter, stepSize, lossType,
    labelCol, featuresCol, predictionCol, minInstancesPerNode, minInfoGain, maxMemoryInMB,
    cacheNodeIds, checkpointInterval)

  override val mlType: MlMethodType = MlMethod.Regression

  override def getLabelColumnName: Option[String] = Some(labelCol.value)

  override def configure(paramMap: Map[String, Any]): Unit = {
    super.configure(paramMap)

    if (paramMap.contains(tempSharedPath.key)) {
      this.tempSharedPathStr = tempSharedPath.value
    }
    if (paramMap.contains(significantFeaturesNumber.key)) {
      this.significantFeatures = significantFeaturesNumber.value
    }
    if (paramMap.contains(maxDepth.key)) {
      gbtr.setMaxDepth(maxDepth.value)
    }
    if (paramMap.contains(maxBins.key)) {
      gbtr.setMaxBins(maxBins.value)
    }
    if (paramMap.contains(minInstancesPerNode.key)) {
      gbtr.setMinInstancesPerNode(minInstancesPerNode.value)
    }
    if (paramMap.contains(minInfoGain.key)) {
      gbtr.setMinInfoGain(minInfoGain.value)
    }
    if (paramMap.contains(maxMemoryInMB.key)) {
      gbtr.setMaxMemoryInMB(maxMemoryInMB.value)
    }
    if (paramMap.contains(cacheNodeIds.key)) {
      gbtr.setCacheNodeIds(cacheNodeIds.value)
    }
    if (paramMap.contains(checkpointInterval.key)) {
      gbtr.setCheckpointInterval(checkpointInterval.value)
    }
    if (paramMap.contains(seed.key)) {
      if (seed.value != "Random"){
        gbtr.setSeed(seed.value.toLong)
      }
    }
    if (paramMap.contains(subsamplingRate.key)) {
      gbtr.setSubsamplingRate(subsamplingRate.value)
    }
    if (paramMap.contains(maxIter.key)) {
      gbtr.setMaxIter(maxIter.value)
    }
    if (paramMap.contains(stepSize.key)) {
      gbtr.setStepSize(stepSize.value)
    }
    if (paramMap.contains(lossType.key)) {
      gbtr.setLossType(lossType.value)
    }
    if (paramMap.contains(featuresCol.key)) {
      gbtr.setFeaturesCol(featuresCol.value)
    }
    if (paramMap.contains(labelCol.key)) {
      gbtr.setLabelCol(labelCol.value)
    }
    if (paramMap.contains(predictionCol.key)) {
      gbtr.setPredictionCol(predictionCol.value)
    }
  }



  override def getAlgoStage(): PipelineStage = {
    this.featuresColName = featuresCol.value
    this.supportFeatureImportance = true

    gbtr
  }
}
