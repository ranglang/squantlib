/*
 Copyright (C) 2011 Tim Blackler

 This source code is release under the BSD License.

 This file is part of JQuantLib, a free-software/open-source library
 for financial quantitative analysts and developers - http://jquantlib.org/

 JQuantLib is free software: you can redistribute it and/or modify it
 under the terms of the JQuantLib license.  You should have received a
 copy of the license along with this program; if not, please email
 <jquant-devel@lists.sourceforge.net>. The license is also available online at
 <http://www.jquantlib.org/index.php/LICENSE.TXT>.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE.  See the license for more details.

 JQuantLib is based on QuantLib. http://quantlib.org/
 When applicable, the original copyright notice follows this notice.
 */
package org.jquantlib.indexes.inflation;

import org.jquantlib.currencies.Europe.GBPCurrency;
import org.jquantlib.indexes.UKRegion;
import org.jquantlib.indexes.ZeroInflationIndex;

import org.jquantlib.termstructures.ZeroInflationTermStructure;
import org.jquantlib.time.Frequency;
import org.jquantlib.time.Period;
import org.jquantlib.time.TimeUnit;

/**
 * 
 * UK Retail Price Inflation Index
 * 
 * @author Tim Blackler
 *
 */

public class UKRPI extends ZeroInflationIndex {

	public UKRPI(final Frequency frequency,
         	      final boolean revised,
         	      final boolean interpolated) {
		this(frequency, revised, interpolated, null);
		   
	   }
	
    public UKRPI(final Frequency frequency,
            	  final boolean revised,
            	  final boolean interpolated,
            	  final ZeroInflationTermStructure termStructure) {
    	
    	super("RPI",
              new UKRegion(),
              revised,
              interpolated,
              frequency,
              new Period(2, TimeUnit.Months),
              new GBPCurrency(),
              termStructure);
    	
    }

}