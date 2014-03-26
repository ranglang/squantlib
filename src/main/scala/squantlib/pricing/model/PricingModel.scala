package squantlib.pricing.model

import squantlib.schedule.{CalculationPeriod, Schedule, ScheduledPayoffs}
import squantlib.schedule.payoff.{Payoffs, Payoff}
import squantlib.util.Date
import squantlib.model.rates.DiscountCurve
import squantlib.pricing.mcengine.MontecarloEngine
import scala.collection.mutable.{SynchronizedMap, WeakHashMap}
import scala.annotation.tailrec

trait PricingModel {
  
  /*	
   * Number of Montecarlo paths
   */
  var mcPaths:Int = 100000
  
  /*	
   * Returns montecarlo paths
   */
  def modelPaths(paths:Int = mcPaths):List[List[Double]] = List.empty
  
  /*	
   * Type of price ("MODEL", "PUBLISHED", etc)
   */
  val priceType:String
  
  /*	
   * Returns forward underlyings
   */
  def modelForward(paths:Int = mcPaths):List[Double] = List.empty
  

  /*	
   * Target payoffs
   */
  val scheduledPayoffs:ScheduledPayoffs
  
  /*	
   * Pricing function to be overridden. Result is annual rate without discount.
   */
  def calculatePrice:List[Double]
  
  /*  
   * Trigger probability function to be overridden. Result is call probabilities.
   */
  def triggerProbabilities:List[Double] = List.empty
  
  /*  
   * Store trigger information in the model.
   */
  def updateTriggerProbabilities:Unit = if (!scheduledPayoffs.calls.forall(c => c.isEmpty)) {
    val probs = triggerProbabilities
    if (probs.isEmpty) modelOutput("exercise_probability", null) else modelOutput("exercise_probability", probs)
  }
  
  /*	
   * Returns forward value of each coupon. Result is annual rate without discount.
   */
  def forwardLegs:List[(CalculationPeriod, Double)] = scheduledPayoffs.schedule.toList zip calculatePrice

  /*	
   * Returns price per payment legs.
   */
  def discountedPriceLegs(curve:DiscountCurve):List[(CalculationPeriod, Double)] = forwardLegs.map{case (c, p) => (c, p * c.coefficient(curve))}
	
  /*
   * Returns present value of the future cashflow.
   * Override this function if the price is different from model calculated price.
   */
  def price(curve:DiscountCurve):Option[Double] = {
    val result = discountedPriceLegs(curve).unzip._2.sum + optionPrice.getOrElse(0.0)
    if (result.isNaN || result.isInfinity) None else Some(result)
  }
  
  /*
   * Returns present value of the future cashflow.
   * Override this function if the price is different from model calculated price, and where discount curve is not necessary (eg. published price)
   */
  def price:Option[Double] = None
  
  /*
   * Price used for greeks calculation.
   */
  def modelPrice(curve:DiscountCurve):Option[Double] = price(curve)
  
  /*
   * Returns the model after pre-calibration.
   */
  def calibrate:PricingModel = this

  /*
   * Returns callable option price. Override if the product is callable.
   */
  val optionPrice:Option[Double] = None
  
  /*
   * Returns calculation information
   */
  def priceInfo:String = ""
    
  /*
   * Returns pricing engine
   */
  val mcEngine:Option[MontecarloEngine] = None
    
  def generatePaths(paths:Int):List[Any] = List.empty
  
  
  /*
   * cache to store intermediary values used in the model
   */
  val modelCache= new WeakHashMap[String, Any] with SynchronizedMap[String, Any]
  
  def getCache[A](k:String):Option[A] = modelCache.get(k).collect{case v => v.asInstanceOf[A]}
  def getOrUpdateCache[A](k:String, f: => A):A = modelCache.getOrElseUpdate(k, f).asInstanceOf[A]
  def clearCache = modelCache.clear
  
  def concatList(data:List[List[Double]]):List[Double] = 
    if (data.isEmpty) List.empty[Double]
    else concatListRec(data, List.fill(data.head.size)(0.0))
  
  @tailrec private def concatListRec(data:List[List[Double]], result:List[Double]):List[Double] = data match {
    case Nil => result
    case h::t => concatListRec(t, (result, h).zipped.map(_ + _))
  }
  
  /*
   * model output capacity
   */
  var modelOutput:(String, List[Any]) => Unit = (_, _) => println("missing model output setting")
  
}


