package net.squantlib.util

import scala.annotation.tailrec
import DisplayUtils._
import net.squantlib.util.initializer._

case class FixingInformation(
  currencyId:String,
  paymentCurrencyId: String,
  var tbd:Option[Double],
  var minRange:Option[Double],
  var maxRange:Option[Double],
  var initialFixing:Map[String, Double],
  fixingPageInformation: List[Map[String, String]]
) {
  
  def initialFixingFull:Map[String, Double] = {
    val inv:Map[String, Double] = initialFixing.withFilter{case (k, v) => k.size == 6}.map{case (k, v) => (((k takeRight 3) + (k take 3)), (if (v == 0.0) 0.0 else 1.0 / v))}.toMap
    if (inv.isEmpty) initialFixing else initialFixing ++ inv
  }
    
  def all:Map[String, Double] = tbd match {
    case Some(c) => initialFixingFull.updated("tbd", c)
    case None => initialFixingFull
  }
    
  def update(p:String):String = multipleReplace(p, all.map{case (k, v) => ("@" + k, v)})
  
  def updateInitial(p:String):String = multipleReplace(p, initialFixing.map{case (k, v) => ("@" + k, v)})
  
  @tailrec private def multipleReplace(s:String, replacements:Map[String, Double]):String = 
    if (s == null) null
    else replacements.headOption match {
      case None => s
      case Some((k, v)) => multipleReplace(s.replace(k, v.toString), replacements - k)
    }
  
  def updateCompute(p:String):Option[Double] = FormulaParser.calculate(update(p))
  
  /*
   * Fixing Information Accessor
   */
    
  def currentPercent(decimal:Int):String = tbd.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def currentDouble(decimal:Int):String = tbd.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
  
  def minRangePercent(decimal:Int):String = minRange.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def minRangeDouble(decimal:Int):String = minRange.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
    
  def maxRangePercent(decimal:Int):String = maxRange.collect{case v => v.asPercent(decimal)}.getOrElse("未定")
    
  def maxRangeDouble(decimal:Int):String = maxRange.collect{case v => v.asDouble(decimal)}.getOrElse("未定")
    
  def rangePercent(decimal:Int):String = (minRange, maxRange) match {
    case (min, max) if min.isDefined || max.isDefined => s"[${min.collect{case v => v.asPercent(decimal)}.getOrElse("")}～${max.collect{case v => v.asPercent(decimal)}.getOrElse("")}]"
    case _ => ""
  }
    
  def rangeDouble(decimal:Int):String = (minRange, maxRange) match {
    case (min, max) if min.isDefined || max.isDefined => s"[${min.collect{case v => v.asDouble(decimal)}.getOrElse("")}～${max.collect{case v => v.asDouble(decimal)}.getOrElse("")}]"
    case _ => ""
  }

  val underlyingAssetIds:Map[String, FixingPage] = fixingPageInformation.map(pageInfo => {
    val bidOffer:Option[String] = pageInfo.get("bidoffer") match {
      case Some(b) if b == "bid" || b == "offer" => Some(b)
      case _ => None
    }

    (
      pageInfo.getOrElse("underlying", "missingUnderlying"),
      FixingPage(
        underlying = pageInfo.getOrElse("underlying", "missingUnderlying"),
        page = pageInfo.get("page"), //getOrElse("page", "missingPage"),
        bidOffer = bidOffer,
        time = pageInfo.get("time"),
        country = pageInfo.get("country"),
        priceType = pageInfo.get("price_type").getOrElse("close"),
        initialPriceType = pageInfo.get("initial_price_type").getOrElse("close")
      )
    )
  }).toMap

  def getUnderlyingFixing(ul: String):UnderlyingFixingInfo = {
    underlyingAssetIds.get(ul) match {
      case Some(v) => UnderlyingFixingInfo(Set(v), (vs) => vs.get(v))
      case _  if Currencies.isForex(ul) =>
        val ccy1 = ul.take(3)
        val ccy2 = ul.takeRight(3)
        (underlyingAssetIds.find{case (k, v) => k.contains(ccy1)}, underlyingAssetIds.find{case (k, v) => k.contains(ccy2)}) match {

          case (Some((fxul1, p1)), Some((fxul2, p2))) if fxul1 == fxul2 =>
            UnderlyingFixingInfo(Set(p1), ulFixings => (ulFixings.get(p1).collect{case v => 1.0 / v}))

          case (Some((fxul1, p1)), Some((fxul2, p2))) if fxul1.replace(ccy1, "") == fxul2.replace(ccy2, "") =>
            UnderlyingFixingInfo(Set(p1, p2), ulFixings => (ulFixings.get(p1), ulFixings.get(p2)) match {
              case (Some(fx1), Some(fx2)) =>
                val v1 = if (fxul1.take(3) == ccy1) fx1 else 1.0 / fx1
                val v2 = if (fxul2.take(3) == ccy2) fx2 else 1.0 / fx2
                Some(v1 / v2)
              case _ => None
            })

          case _ => UnderlyingFixingInfo(Set.empty, (vs) => None)
        }
      case _ => UnderlyingFixingInfo(Set.empty, (vs) => None)
    }
  }


}
  
object FixingInformation {
  
  def empty(currencyId:String, paymentCurrencyId:String) = FixingInformation(currencyId, paymentCurrencyId, None, None, None, Map.empty, List.empty)
  
}


case class UnderlyingFixingInfo(
  fixingPages:Set[FixingPage],
  priceCalculator:Map[FixingPage, Double] => Option[Double]
 ) {

  def getPrice(underlyingPrices:Map[FixingPage, Double]):Option[Double] = {
    if (fixingPages.forall(s => underlyingPrices.get(s).collect { case v => !v.isNaN }.getOrElse(false))) {
      priceCalculator(underlyingPrices)
    }
    else {
      None
    }
  }

  def getPriceFromFixings(pagePrices:Map[String, Double], isInitialFixing:Boolean):Option[Double] = {

//    val underlyingPrices:Map[FixingPage, Double] = pagePrices.map{case (ul, v) =>
//      fixingPages.find(p => p.pageList.contains(ul)) match {
//        case Some(fixingPage) => Some((fixingPage, v))
//        case _ => None
//      }
//    }.flatMap{case s => s}.toMap

    val underlyingPrices:Map[FixingPage, Double] =
      fixingPages.map(fixingPage => {fixingPage.getPrice(pagePrices, isInitialFixing) match {
        case Some(v) => Some((fixingPage, v))
        case _ => None
      }}).flatMap{case s => s}.toMap

    getPrice(underlyingPrices)
  }

}

case class FixingPage(
  underlying:String,
  page:Option[String],
  bidOffer:Option[String],
  time:Option[String],
  country:Option[String],
  priceType:String,
  initialPriceType:String
) {

  val pageFull:Option[String] = page match {
    case Some(p) if Set(time, country).exists(_.isDefined) => Some(underlying + ":" + p + ":" + List(bidOffer, time, country).flatMap(s => s).mkString(":"))
    case _ => None
  }

  val pageWithBidoffer:Option[String] = page match {
    case Some(p) => bidOffer.collect{case bo => underlying + ":" + p + ":" + bo}
    case _ => None
  }

  val pageOnly:Option[String] = page.collect{case p => underlying + ":" + p}

  val timeOnly:Option[String] = (time, country) match {
    case (Some(t), Some(c)) => Some(underlying + "::" + t + ":" + c)
    case _ => None
  }

  def pageWithPriceType(isInitialFixing:Boolean):Option[String] = {
    val refPriceType = (if (isInitialFixing) initialPriceType else priceType)

    if (refPriceType == "close") None
    else Some(underlying + ":" + refPriceType.toUpperCase)
  }

//  val pageList:List[String] = List(pageFull, pageWithBidoffer, pageOnly).flatMap(s => s)

  val basePageList:List[String] = List(pageFull, pageWithBidoffer, pageOnly, timeOnly).flatMap(s => s)

  def pageList(isInitialFixing:Boolean):List[String] = pageWithPriceType(isInitialFixing) match {
    case Some(p) => p :: basePageList
    case None => basePageList
  }

  val allPageSet:Set[String] = (pageList(true) ++ pageList(false)).toSet

  override def toString = pageList(false).mkString(", ")

  def getPrice(pagePrices:Map[String, Double], isInitialFixing:Boolean):Option[Double] = {
    pageList(isInitialFixing).map(pageName => pagePrices.get(pageName)).flatMap(s => s).headOption
  }

  //  val pageList:List[String] = if (defaultPage == fallback) List(defaultPage) else List(defaultPage, fallback)

}
