//  $Header
//  $Name

package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.dla.seqloader.GBSeqAttributeResolver;
import org.jax.mgi.shr.dla.seqloader.SequenceRawAttributes;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.KeyNotFoundException;
import org.jax.mgi.dbs.mgd.dao.SEQ_SequenceState;
import org.jax.mgi.dbs.mgd.lookup.TranslationException;

/**
 * @is An object that resolves a SequenceRawAttributes to a SEQ_SequenceState.
 * Reports discrepancies to the validation log.
 * @has
 *   <UL>
 *   <LI>Lookups to resolve attributes - see superclass
 *   <LI>A SEQ_SequenceState
 *   <LI>A SequenceRawAttributes
 *   </UL>
 * @does
 *   <UL>
 *   <LI>Uses lookups and translators to resolve raw sequence attributes
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class GBSeqloadAttributeResolver extends GBSeqAttributeResolver {

    SEQ_SequenceState state;

    /**
    * Constructs a GBSeqAttributeResolver.
    * @assumes Nothing
    * @effects Nothing
    * @param None
    * @throws TranslationException - If a translation error occurs in the type Lookup
    * @throws ConfigException - if there  is an error accessing the
    *         configuration file
    * throws@ DBException - if there is an error accessing the database
    * throws@ CacheException - if there is an error with the
    *         vocabulary cache
    */

    public GBSeqloadAttributeResolver() throws CacheException, ConfigException,
        DBException, TranslationException {
     }

    /**
     * resolves a sets the proper Provider and resolves the
     * SequenceRawAttributes object to a SEQ_SequenceState
     * @assumes Nothing
     * @effects Nothing
     * @param rawAttributes A SequenceRawAttributes object
     * @return sequenceState A SEQ_SequenceState
     * @throws Nothing
     */

    public SEQ_SequenceState resolveAttributes(
        SequenceRawAttributes rawAttributes)  throws KeyNotFoundException,
        TranslationException, DBException, CacheException, ConfigException {

        String newRawProvider = rawAttributes.getProvider() + ":" + rawAttributes.getDivision();
        rawAttributes.setProvider(newRawProvider);

        return super.resolveAttributes(rawAttributes);

    }
}

//  $Log

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
