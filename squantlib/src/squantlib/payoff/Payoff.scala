package squantlib.payoff

import squantlib.util.DisplayUtils._
import squantlib.util.JsonUtils._

trait Payoff{
	
	val variables:Set[String]
	
	def price(fixings:Map[String, Double]):Double
	
	def price(fixing:Double) (implicit d:DummyImplicit):Double 
	
	def price:Double
	
	def toString:String
	
}

object Payoff {
  
	def apply(formula:String):Payoff =
	  payoffType(formula) match {
	    case "fixed" => FixedPayoff(formula)
		case "leps1d" => LEPS1dPayoff(formula)
		case "linear1d" => Linear1dPayoff(formula)
		case _ => GeneralPayoff(formula).remodelize
	  }
  
	def payoffType(formula:String):String = formula match {
	  case f if f.parseDouble.isDefined => "fixed"
	  case f if f.startsWith("leps") => "leps1d"
	  case f => formula.parseJsonString("type")
	  }
}

