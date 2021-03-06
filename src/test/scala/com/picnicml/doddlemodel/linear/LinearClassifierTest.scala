package com.picnicml.doddlemodel.linear

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.numerics.sigmoid
import com.picnicml.doddlemodel.data.{Features, RealVector, Simplex, Target}
import com.picnicml.doddlemodel.linear.typeclasses.LinearClassifier
import org.scalatest.{FlatSpec, Matchers, OptionValues}

case class DummyLinearClassifier(numClasses: Option[Int], w: Option[RealVector])

class LinearClassifierTest extends FlatSpec with Matchers with OptionValues {

  val ev: LinearClassifier[DummyLinearClassifier] = new LinearClassifier[DummyLinearClassifier] {

    override def numClasses(model: DummyLinearClassifier): Option[Int] = model.numClasses

    override protected def w(model: DummyLinearClassifier): Option[RealVector] = model.w

    override protected[doddlemodel] def copy(model: DummyLinearClassifier, numClasses: Int): DummyLinearClassifier =
      model.copy(numClasses = Some(numClasses))

    override protected def copy(model: DummyLinearClassifier, w: RealVector): DummyLinearClassifier =
      model.copy(w = Some(w))

    override protected def predictStateless(model: DummyLinearClassifier, w: RealVector, x: Features): Target =
      x * w

    override protected def predictProbaStateless(model: DummyLinearClassifier, w: RealVector, x: Features): Simplex =
      sigmoid(x * w).asDenseMatrix.t

    override protected[linear] def lossStateless(model: DummyLinearClassifier,
                                                 w: RealVector, x: Features, y: Target): Double = 0

    override protected[linear] def lossGradStateless(model: DummyLinearClassifier,
                                                     w: RealVector, x: Features, y: Target): RealVector = w
  }

  private val x = DenseMatrix.rand[Double](10, 5)
  private val y = DenseVector.vertcat(DenseVector.zeros[Double](5), DenseVector.ones[Double](5))
  private val model = DummyLinearClassifier(None, None)

  "Linear classifier" should "throw an exception when using fit, predict on trained, untrained models" in {
    an [IllegalArgumentException] should be thrownBy ev.predict(model, x)
    val trainedModel = ev.fit(model, x, y)
    an [IllegalArgumentException] should be thrownBy ev.fit(trainedModel, x, y)
  }

  it should "implement predictor functions" in {
    ev.isFitted(model) shouldBe false
    val trainedModel = ev.fit(model, x, y)
    ev.isFitted(trainedModel) shouldBe true
    val yPred = ev.predict(trainedModel, x)
    yPred.length shouldEqual y.length
  }

  it should "set the correct number of classes after fit" in {
    ev.numClasses(model).isEmpty shouldBe true
    val trainedModel = ev.fit(model, x, y)
    ev.numClasses(trainedModel).value shouldBe 2
  }

  it should "throw an exception if fitting a model with an invalid target variable" in {
    val invalidCategoricalY = DenseVector.zeros[Double](10)
    an [IllegalArgumentException] should be thrownBy ev.fit(model, x, invalidCategoricalY)
    val invalidRealY = DenseVector.rand[Double](10)
    an [IllegalArgumentException] should be thrownBy ev.fit(model, x, invalidRealY)
  }
}
