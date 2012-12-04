package squantlib.database.schemadefinitions

import java.util.Date
import org.squeryl.annotations.Column
import org.squeryl.KeyedEntity
import squantlib.setting.initializer.Calendars
import org.jquantlib.time.Calendar


class Bond(@Column("ID")					var id: String,
              @Column("REF_NUMBER")			var ref_number: Int,
              @Column("FILING")				var filing: Date,
              @Column("ISSUEDATE")			var issuedate: Date,
              @Column("MATURITY")			var maturity: Date,
              @Column("NOMINAL")			var nominal: Double,
              @Column("DENOMINATION")		var denomination: Option[Double],
              @Column("COUPON")				var coupon: String,
              @Column("COUPON_FREQ")		var coupon_freq: Option[Int],
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
              @Column("ISIN")				var isin: String,
              @Column("TICKER")				var ticker: String,
              @Column("DESCRIPTION_JPN")	var description_jpn: String,
              @Column("DESCRIPTION_ENG")	var description_eng: String,
              @Column("CurrencyID")			var currencyid: String,
              @Column("ProductID")			var productid: String,
              @Column("IssuerID")			var issuerid: String,
              @Column("RISKTAGS") 			var risktags: String,
              @Column("SETTINGS") 			var settings: String,
              @Column("Created")			var created: Option[Date],
              @Column("LastModified")		var lastmodified : Option[Date]
              ) extends KeyedEntity[String] {

  
  def calendar:Calendar = {
    val cdrlist:Set[String] = calendar_str.split(",").map(_.trim).toSet
    Calendars(cdrlist).getOrElse(Calendars(currencyid).get)
  }
  
  def this() = this(
		id = null,
		ref_number = 0,
		filing = new Date,
		issuedate = new Date,
		maturity = new Date,
		nominal = 0.0,
		denomination = Some(-999.0),
		coupon = null,
		coupon_freq = Some(9999),
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
		isin = null,
		ticker = null,
		description_jpn = null,
		description_eng = null,
		currencyid = null,
		productid = null,
		issuerid = null,
		risktags = null,
		settings = null,
		created = None,
		lastmodified  = None)
 
  override def toString():String = format("%-5s %-15s %-25s %-10s %-15s %-15s", id, issuedate.toString, maturity.toString, coupon, initialfx.toString, created.toString)
  
}