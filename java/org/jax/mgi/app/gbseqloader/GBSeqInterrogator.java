//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import java.util.*;

import org.jax.mgi.shr.exception.MGIException;

    /**
     * @is an object that queries a classification string to determine if
     *     it is for a given organism
     * @has
     *   <UL>
     *   <LI>a mapping structure for mapping controlled vocabulary
     *       to string expressions e.g. "mouse" : "Muridae; Murinae; Mus"
     *   </UL>
     * @does
     *   <UL>
     *   <LI>Given a classification string, e.g.
     *       " Eukaryota; Metazoa; Chordata; Craniata; Vertebrata; Euteleostomi;
               Mammalia; Eutheria; Rodentia; Sciurognathi; Muridae;
               Murinae; Mus." <BR>
             and a controlled vocabulary string, e.g. "mouse",
               determine if the classification is for a mouse.
     *   </UL>
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

public class GBSeqInterrogator {

    /**
     * Determines whether a sequence classification if for a given organism
     * @assumes "organism" is a valid controlled vocabulary for "classification"
     * @effects Nothing
     * @param classification A GenBank sequence classification string
     * @param organism A controlled vocab term for an organism
     * @return true if "classification" is for "organism"
     * @throws Nothing
     */

    public boolean isOrganism (String classification, String organism) {
         // get the string expression that is mapped to 'organism'
         String matchString = (String)expressions.get(organism.toLowerCase());

         // return true if the string expression matches organism of 's'
         if((classification.toLowerCase()).indexOf(matchString) >  -1) {
             return true;
         }
        else {
            return false;
        }
    }

         // a hash map data structure that maps organism controlled vocab
         // to a String expression. All matching is done in lower case.
         private static String MOUSE = "Muridae; Murinae; Mus".toLowerCase();
         private static String RAT = "Rattus".toLowerCase();
         private static String HUMAN = "sapiens".toLowerCase();

         // load HashMap with controlled vocab keys and string expression values
         private static HashMap expressions = new HashMap();
         static {
                 expressions.put("mouse", MOUSE);
                 expressions.put("rat", RAT);
                 expressions.put("human", HUMAN);
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