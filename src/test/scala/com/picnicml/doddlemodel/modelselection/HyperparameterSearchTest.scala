package com.picnicml.doddlemodel.modelselection

import com.picnicml.doddlemodel.data.{loadBreastCancerDataset, shuffleDataset}
import com.picnicml.doddlemodel.linear.LogisticRegression
import com.picnicml.doddlemodel.linear.LogisticRegression.ev
import com.picnicml.doddlemodel.metrics.accuracy
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Random

class HyperparameterSearchTest extends FlatSpec with Matchers {

  "Hyperparameter search" should "return the best model from available candidates" in {
    val (x, y) = loadBreastCancerDataset

    implicit val rand: Random = new Random(42)
    val (xShuffled, yShuffled) = shuffleDataset(x, y)
    val trIndices = 0 until 400
    val teIndices = 400 until x.rows
    val (xTr, yTr) = (xShuffled(trIndices, ::), yShuffled(trIndices))
    val (xTe, yTe) = (xShuffled(teIndices, ::), yShuffled(teIndices))

    val cv: CrossValidation = CrossValidation(metric = accuracy, dataSplitter = KFoldSplitter(numFolds = 5))
    val search = HyperparameterSearch(numIterations = 100, crossValidation = cv)
    val grid = (0 until 100).toIterator.map(_.toDouble)

    val underfittedModel = ev.fit(LogisticRegression(lambda = 99.0), xTr, yTr)
    val bestModel = search.bestOf(xTr, yTr) {
      LogisticRegression(lambda = grid.next)
    }

    accuracy(yTe, ev.predict(bestModel, xTe)) > accuracy(yTe, ev.predict(underfittedModel, xTe)) shouldBe true
  }
}
