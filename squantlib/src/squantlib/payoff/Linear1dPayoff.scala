package squantlib.payoff

import scala.collection.JavaConversions._
import squantlib.util.DisplayUtils._
import squantlib.util.JsonUtils._
import org.codehaus.jackson.JsonNode
import org.codehaus.jackson.map.ObjectMapper
import squantlib.util.VariableInfo

/**
 * Interprets JSON formula specification for a linear formula with cap & floor.
 * JSON format:
 * - {type:"linear1d", variable:string, payoff:formula}, where
 *   formula = {min:double, max:double, mult:double, add:double, description:XXX}
 *   payment for array(i) is min <= mult * variable + add <= max
 */
case class Linear1dPayoff(variable:String, payoff:Linear1dFormula, description:String) extends Payoff {
  
	val variables:Set[String] = if (variable == null) Set.empty else Set(variable)
	 
	override def price(fixings:Map[String, Double]) = 
	  if (fixings contains variable) payoff.price(fixings(variable))
	  else Double.NaN
	
	override def price(fixing:Double)(implicit d:DummyImplicit) = payoff.price(fixing)
	
	override def price = Double.NaN
	
	override def toString:String = payoff.toString(variable)
	
	override def display(isRedemption:Boolean):String = {
	  val varname = VariableInfo.namejpn(variable)
	  val vardisp = (v:Double) => VariableInfo.displayValue(variable, v)
	  
	  if (isRedemption) {
	    "最終参照日の" + varname + "によって、下記の金額が支払われます。" + 
	    sys.props("line.separator") + 
	    (payoff match {
	      case Linear1dFormula(None, None, _, _, _) => "額面 " + (0.0).asPercent
	      case Linear1dFormula(Some(coeff), None, _, _, _) => "額面に対して " + coeff.asDouble + " * " + varname
	      case Linear1dFormula(None, Some(const), _, _, _) => "額面 " + const.asPercent
	      case Linear1dFormula(Some(coeff), Some(const), _, _, _) => "額面 に対して " + coeff.asDouble + " * " + varname + (if(const < 0) " - " else " + ") + math.abs(const).asPercent
	    }) + sys.props("line.separator") + " " + (payoff match {
	      case Linear1dFormula(_, _, Some(c), None, _) => "ただし" + c.asPercent + "を上回りません。"
	      case Linear1dFormula(_, _, None, Some(f), _) => "ただし" + f.asPercent + "を下回りません。"
	      case Linear1dFormula(_, _, Some(c), Some(f), _) => "ただし" + c.asPercent + "を上回らず、" + f.asPercent + "を下回りません。"
	      case Linear1dFormula(_, _, None, None, _) => ""
	    })
	  }
	  
	  else{
	    "利率決定日の" + varname + "によって決定されます。" + 
	    sys.props("line.separator") + " " + 
	    (payoff match {
	      case Linear1dFormula(None, None, _, _, _) => (0.0).asPercent
	      case Linear1dFormula(Some(coeff), None, _, _, _) => coeff.asDouble + " * " + varname
	      case Linear1dFormula(None, Some(const), _, _, _) => const.asPercent
	      case Linear1dFormula(Some(coeff), Some(const), _, _, _) => coeff.asDouble + " * " + varname + (if(const < 0) " - " else " + ") + math.abs(const).asPercent
	    }) + " （年率）" + sys.props("line.separator") + " " + (payoff match {
	      case Linear1dFormula(_, _, Some(c), None, _) => "ただし" + c.asPercent + "を上回りません。"
	      case Linear1dFormula(_, _, None, Some(f), _) => "ただし" + f.asPercent + "を下回りません。"
	      case Linear1dFormula(_, _, Some(c), Some(f), _) => "ただし" + c.asPercent + "を上回らず、" + f.asPercent + "を下回りません。"
	      case Linear1dFormula(_, _, None, None, _) => ""
	    })
	  }
	}
	
	override def jsonString = {
	    
	  val jsonPayoff:java.util.Map[String, Any] = Map(
	      "min" -> payoff.minValue.getOrElse("None"),
	      "max" -> payoff.maxValue.getOrElse("None"),
	      "coeff" -> payoff.coeff.getOrElse("None"),
	      "description" -> payoff.description)
	      
	  val infoMap:java.util.Map[String, Any] = Map(
	      "type" -> "linear1d", 
	      "variable" -> variable, 
	      "payoff" -> jsonPayoff)
	  
	  (new ObjectMapper).writeValueAsString(infoMap)	  
	}
}

object Linear1dPayoff {
  
	def apply(formula:String):Linear1dPayoff = {
	  val variable:String = formula.parseJsonString("variable")	
	  
	  val payoff:Linear1dFormula = formula.jsonNode("payoff") match {
		  case Some(n) => Linear1dFormula(n)
		  case None => null
		}
	  
	  Linear1dPayoff(variable, payoff, null)
	}
	
	def apply(variable:String, payoff:JsonNode):Linear1dPayoff = Linear1dPayoff(variable, Linear1dFormula(payoff), null)
	
	def apply(variable:String, coeff:Option[Double], constant:Option[Double], minValue:Option[Double], maxValue:Option[Double], description:String = null):Linear1dPayoff = 
	  Linear1dPayoff(variable, Linear1dFormula(coeff, constant, minValue, maxValue, description), description)
}

case class Linear1dFormula (val coeff:Option[Double], val constant:Option[Double], val minValue:Option[Double], val maxValue:Option[Double], val description:String) {

	def price(fixing:Double):Double = {
	  var p = (coeff, constant) match {
	    case (None, None) => 0.0
		case (None, Some(c)) => c
		case (Some(x), None) => x * fixing
		case (Some(x), Some(c)) => x * fixing + c
	  }
	  
	  if (minValue.isDefined) p = p.max(minValue.get)
	  if (maxValue.isDefined) p = p.min(maxValue.get)
	  p
	} 
	
	def toString(varname:String) = 
	  linearFormula(coeff, varname, constant) + minValue.asPercentOr("", " >", "") + maxValue.asPercentOr("", " <", "")
									
}

object Linear1dFormula {
  
	def apply(subnode:JsonNode):Linear1dFormula = {
		val minValue:Option[Double] = subnode.parseJsonDouble("min")
		val maxValue:Option[Double] = subnode.parseJsonDouble("max")
		val coeff:Option[Double] = Some(subnode.parseJsonDouble("mult").getOrElse(1.0))
		val constant:Option[Double] = subnode.parseJsonDouble("add")
		val description:String = subnode.parseJsonString("description")
		Linear1dFormula(coeff, constant, minValue, maxValue, description)
	}
	
}