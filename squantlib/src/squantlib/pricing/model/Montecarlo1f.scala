package squantlib.pricing.model

import squantlib.pricing.randomgenerator.{RandomGenerator, MersenneTwister}
import squantlib.math.statistical.NormSInv


/* Simple Black-Scholes montecarlo pricer for FX
 * FX volatility is constant over time without smile, No rates volatility
 * @param spot 		current underlying price
 * @param ratedom(t)	continuous compounding risk-free rate of domestic pricing currency at time t as number of years
 * @param ratefor(t)	continuous compounding risk-free rate of foreign currency at time t as number of years
 * @param sigma(t)	volatility of the underlying FX
 */

trait Montecarlo1f {
  
  var randomGenerator:RandomGenerator
  
  def reset:Unit

  /* Generates paths.
   * @param eventdates observation dates as number of years
   * @param paths 	Number of Montecarlo paths to be generated
   * @returns Montecarlo paths
   * 
   * CAUTION: Order of event dates are automatically sorted by the function.
   * ie. Order of output paths might not correspond to order of input eventDates if it's not sorted.
  */
  
  def generatePaths(eventDates:List[Double], paths:Int):(List[Double], List[List[Double]])
}

