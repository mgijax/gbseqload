//  $Header
//  $Name

package org.jax.mgi.app.gbseqloader;

import java.util.*;
import java.util.regex.*;

import org.jax.mgi.shr.dla.seqloader.SeqDecider;
import org.jax.mgi.shr.dla.seqloader.SeqloaderConstants;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is An object that, given a sequence record and a set of deciders representing
 *     organisms determines if the any of the deciders are true for the sequence
 *     record e.g. Given three deciders, mouse, human, and rat, determines if the
 *     sequence record is a mouse, or a human, or rat.
 * @has
 *   <UL>
 *   <LI>A sequence record
 *   <LI>A set of deciders; each a predicate to identify a given organism
 *   </UL>
 * @does
 *   <UL>
 *   <LI>Finds the classification section of a sequence record
 *   <LI>Queries each decider; Determines if the classification is for an
 *       organism represented by a decider
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class GBOrganismChecker {
    // the expression with which to find the classification section
    private String expression;

    // expression as a regular expression
    private Pattern organismPattern;

    // a regular expression matcher for organismPattern
    private Matcher m;

    // true if any decider returns true
    private boolean isA;

    // the set of organism deciders to query
    private Vector deciders;

    // the current decider being queried
    private SeqDecider currentDecider;

    /**
    * Constructs an OrganismChecker for a given provider with a set of
    * deciders
    * @assumes nothing
    * @effects nothing
    * @param deciders A set of predicates to identify a set of organisms
    * @param provider The sequence provider to determine the regex for
    *        finding the classification in a sequence record.
    * @throws An exception if there are no deciders or unsupported provider
    */

    public GBOrganismChecker (Vector deciders, String provider) {

        this.deciders = deciders;
        if(provider.equals(SeqloaderConstants.GENBANK)) {
            expression = SeqloaderConstants.GBCLASSIFICATION;
        }
        else if (provider.equals(SeqloaderConstants.EMBL)) {
            // Fill this in
        }
        else {
            //throw an exception
        }
           organismPattern = Pattern.compile(expression, Pattern.MULTILINE);
    }

    /**
    * Determines if a sequence record is an organism
    * represented by the set of deciders
    * @assumes Nothing
    * @effects Nothing
    * @param None
    * @return true if sequence record is an organism represented by one of
    *         the deciders.
    * @throws Nothing
    */

    public boolean checkOrganism(String record) {
        // find the classification section of this record
        m = organismPattern.matcher(record);

        // reset - if we don't find the classification section,
        // the record is invalid
        isA = false;
        if (m.find() == true) {
            // Determine if we are interested in this sequence
            Iterator i = deciders.iterator();
            while (i.hasNext()) {
                currentDecider = (SeqDecider)i.next();
                // m.group(1) is the classification
                if(currentDecider.isA(m.group(1))) {
                    isA = true;
                    break;
                }
            }
        }
        return isA;
    }
}

//  $Log$
//  Revision 1.1  2003/12/08 18:40:37  sc
//  initial commit
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
**************************************************************************/
