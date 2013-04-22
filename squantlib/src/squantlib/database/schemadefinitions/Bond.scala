package squantlib.database.schemadefinitions

import java.util.Date
import org.squeryl.annotations.Column
import org.squeryl.KeyedEntity
import squantlib.util.JsonUtils._
import squantlib.payoff.Schedule
import squantlib.setting.initializer._
import org.jquantlib.time.{Date => qlDate, Period => qlPeriod, _}
import org.jquantlib.daycounters._
import scala.collection.JavaConversions._

class Bond(@Column("ID")					override var id: String,
              @Column("REF_NUMBER")			var ref_number: Int,
              @Column("FILING")				var filing: Date,
              @Column("ISSUEDATE")			var issuedate: Date,
              @Column("MATURITY")			var maturity: Date,
              @Column("ISMATURED")			var ismatured: Int,
              @Column("NOMINAL")			var nominal: Option[Double],
              @Column("DENOMINATION")		var denomination: Option[Double],
              @Column("UNDERLYING")			var underlying: String,
              @Column("UNDERLYINGINFO")		var underlyinginfo: String,
              @Column("COUPON")				var coupon: String,
              @Column("COUPON_FREQ")		var coupon_freq: Option[Int],
              @Column("COUPON_PERYR")		var coupon_peryr: Int,
              @Column("DAYCOUNT")			var daycount: String,
              @Column("DAYCOUNT_ADJ")		var daycount_adj: String,
              @Column("PAYMENT_ADJ")		var payment_adj: String,
              @Column("CALENDAR")			var calendar_str: String,
              @Column("INARREARS")			var inarrears: Option[Int],
              @Column("CPNNOTICE")			var cpnnotice: Option[Int],
              @Column("ISSUEPRICE")			var issueprice: Option[Double],
              @Column("REDEMPRICE")			var redemprice: String,
              @Column("REDEMNOTICE")		var redemnotice: Option[Int],
              @Column("CALL")				var call: String,
              @Column("TYPE")				var bondtype: String,
              @Column("INITIALFX")			var initialfx: Double,
              @Column("FIXINGDATE")			var fixingdate: Option[Date],
              @Column("FIXINGS")			var fixings: String,
              @Column("ISIN")				var isin: String,
              @Column("TICKER")				var ticker: String,
              @Column("LEADMANAGER")		var leadmanager: String,
              @Column("BONDNAME")			var bondname: String,
              @Column("SHORTNAME")			var shortname: String,
              @Column("EDINETNAME")			var edinetname: String,
              @Column("DESCRIPTION_JPN")	var description_jpn: String,
              @Column("DESCRIPTION_ENG")	var description_eng: String,
              @Column("CurrencyID")			var currencyid: String,
              @Column("ProductID")			var productid: String,
              @Column("IssuerID")			var issuerid: String,
              @Column("RISKTAGS") 			var risktags: String,
              @Column("SETTINGS") 			var settings: String,
              @Column("FIRSTCOUPON")		var firstcoupon: Option[Double],
              @Column("CURRENTCOUPON")		var currentcoupon:Option[Double],
              @Column("TARGETYIELD")		var targetyield: Option[Double],
              @Column("PRICETAG") 			var pricetag: Option[Int],
              @Column("COMMENT") 			var comment: String,
              @Column("Created")			var created: Option[Date],
              @Column("LastModified")		var lastmodified : Option[Date]
              ) extends StringEntity {
  
  def autoUpdateFields = List("lastmodified","created", "initialfx", "fixings", "shortname", "underlyinginfo", "comment")
  
  def getFieldMap:Map[String, Any] = getObjectFieldMap(this)
  
  def isSameContent(b:Bond):Boolean = compareObjects(this, b, autoUpdateFields)
  
  def currency = Currencies(currencyid)
		
  def calendar:Calendar = if (calendar_str == null) Calendars(currencyid).get
  						else Calendars(calendar_str.split(",").map(_.trim).toSet).getOrElse(Calendars(currencyid).get)
  
  protected def updateFixing(p:String, fixings:Map[String, Any]):String = multipleReplace(p, fixings.map{case (k, v) => ("@" + k, v)})
  						
  def fixedCoupon(fixings:Map[String, Any]):String = updateFixing(coupon, fixings)
  
  def couponList:List[String] = stringList(coupon)
  
  def couponList(fixings:Map[String, Any]):List[String] = stringList(updateFixing(coupon, fixings))
  
  def fixedRedemprice(fixings:Map[String, Any]):String = updateFixing(redemprice, fixings)
  
  def underlyingList:List[String] = stringList(underlying)
  
  def bermudanList:List[Boolean] = booleanList(call, "berm")
  
  def triggerList:List[List[String]] = call.jsonArray.map(_.parseStringList.map(_.orNull))
  
  def fixingList:Map[String, Double] = fixings.parseJsonDoubleFields
  
  def descriptionjpnList:Map[String, String] = description_jpn.parseJsonStringFields
  
  def descriptionengList:Map[String, String] = description_eng.parseJsonStringFields
  
  def schedule:Option[Schedule] = try {
    val period = (this.coupon_freq collect { case f => new qlPeriod(f, TimeUnit.Months)}).orNull
    val issueDate = new qlDate(this.issuedate)
    val maturity = new qlDate(this.maturity)
    val daycount = Daycounters(this.daycount).getOrElse(new Actual365Fixed)
    val calendarAdjust = DayAdjustments.getOrElse(this.daycount_adj, BusinessDayConvention.ModifiedFollowing)
	val paymentAdjust = DayAdjustments.getOrElse(this.payment_adj, BusinessDayConvention.ModifiedFollowing)
	val maturityAdjust = DayAdjustments.getOrElse(this.daycount_adj, BusinessDayConvention.ModifiedFollowing)
	val calendar = this.calendar
	val fixingInArrears = this.inarrears != Some(0)
	val couponNotice:Int = this.cpnnotice.getOrElse(5)
	val rule = DateGeneration.Rule.Backward
	val issuer:String = this.issuerid
	val redemnotice = this.redemnotice.getOrElse(couponNotice)
	
	Some(Schedule(issueDate, maturity, period, calendar, calendarAdjust, paymentAdjust, maturityAdjust, rule, 
	    fixingInArrears, couponNotice, daycount, None, None, true, redemnotice))
  }
  catch { case _:Throwable => None}
  
  def bermudanList(fixings:Map[String, Any] = Map.empty, nbLegs:Int = schedule.size):List[Boolean] = updateFixing(call, fixings).jsonNode match {
  	case Some(b) if b.isArray && b.size == 1 => List.fill(nbLegs - 2)(b.head.parseString == Some("berm")) ++ List(false, false)
	case Some(b) if b isArray => List.fill(nbLegs - 2 - b.size)(false) ++ b.map(_.parseString == Some("berm")).toList ++ List(false, false)
	case _ => List.fill(nbLegs)(false)
  }

  def triggerList(fixings:Map[String, Any] = Map.empty, nbLegs:Int = schedule.size):List[List[Option[Double]]] = updateFixing(call, fixings).jsonNode match {
    case Some(b) if b.isArray && b.size == 1 => 
      List.fill(nbLegs - 2)(if (b.head isArray) b.head.map(_.parseDouble).toList else List.empty) ++ List.fill(2)(List.empty)
    case Some(b) if b isArray => 
      List.fill(nbLegs - b.size - 2)(List.empty) ++ b.map(n => if (n isArray) n.map(_.parseDouble).toList else List.empty) ++ List.fill(2)(List.empty)
    case _ => List.fill(nbLegs)(List.empty)
  }

  def this() = this(
		id = null,
		ref_number = 0,
		filing = new Date,
		issuedate = new Date,
		maturity = new Date,
		ismatured = 0,
		nominal = Some(-9999),
		denomination = Some(-999.0),
		underlying = null,
		underlyinginfo = null,
		coupon = null,
		coupon_freq = Some(9999),
		coupon_peryr = -9999,
		daycount = null,
		daycount_adj = null,
		calendar_str = null, 
		payment_adj = null,
		inarrears = Some(0),
		cpnnotice = Some(0),
		issueprice = Some(-999.0),
		redemprice = null,
		redemnotice = Some(0),
		call = null,
		bondtype = null,
		initialfx = 0.0,
		fixingdate = None,
		fixings = null,
		isin = null,
		ticker = null,
		leadmanager = null,
		bondname = null,
		shortname = null,
		edinetname = null,
		description_jpn = null,
		description_eng = null, 
		currencyid = null,
		productid = null,
		issuerid = null,
		risktags = null,
		settings = null,
		pricetag = Some(-9999),
		firstcoupon = Some(-9999.99),
		currentcoupon = Some(-9999.99),
		targetyield = Some(-9999.99),
		comment = null,
		created = None,
		lastmodified  = None)
 
  override def toString():String = format("%-5s %-15s %-25s %-10s %-15s %-15s", id, issuedate.toString, maturity.toString, coupon, initialfx.toString, created.toString)

  
  
}