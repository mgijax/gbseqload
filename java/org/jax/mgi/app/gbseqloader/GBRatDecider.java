//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.dla.seqloader.SeqDecider;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is an object that applies this predicate to a the classification section
 * of a GenBank sequence record
 * "Does this classification string represent a rat?"
 * @has
 *   <UL>
 *   <LI>A name
 *   <LI>See also superclass
 *   </UL>
 * @does
 *   <UL>
 *   <LI>Returns true if a classification string represents a rat
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class GBRatDecider extends SeqDecider {
     // name of this decider
     private static String name = "rat";

     // determines if classification is human
     private static GBSeqInterrogator si = new GBSeqInterrogator();

     // answer to the predicate
     private boolean answer;

     /**
      * Constructs a GBRatDecider object
      * @assumes Nothing
      * @effects Nothing
      * @param None
      * @throws Nothing
      */

     public GBRatDecider() {
         super(name);
     }

     /**
      * Determines if 'classification' represents a rat. Counts total
      * classifications processed and total for which the predicate is true.
      * @assumes Nothing
      * @effects Nothing
      * @param classification An organism classification string
      * @return true if this predicate is true for 'classification'
      * @throws MGIException if the sequence interrogator does not support
      *         this decider.
      */


      public boolean isA(String classification) {

          // initialize answer to the predicate to false
          answer = false;

          // apply the predicate
          if (this.si.isOrganism(classification, name)) {
              answer = true;
              incrementTrueCtr();
          }
          incrementAllCtr();
          return answer;
      }
}
//  $Log$
//
  /**************************************************************************
  *
  * Warranty Disclaimer and Copyright Notice
  *
  *  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
  *  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
  *  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
  *  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
  *  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
  *  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
  *
  *  This software and data are provided to enhance knowledge and encourage
  *  progress in the scientific community and are to be used only for research
  *  and educational purposes.  Any reproduction or use for commercial purpose
  *  is prohibited without the prior express written permission of The Jackson
  *  Laboratory.
  *
  * Copyright \251 1996, 1999, 2002, 2003 by The Jackson Laboratory
  *
  * All Rights Reserved
  *
  **************************************************************************/