//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.dla.seqloader.SequenceAttributeResolver;
import org.jax.mgi.shr.dla.seqloader.SequenceRawAttributes;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.dbutils.DBException;
import org.jax.mgi.shr.cache.CacheException;
import org.jax.mgi.shr.cache.KeyNotFoundException;
import org.jax.mgi.dbs.mgd.lookup.VocabKeyLookup;
import org.jax.mgi.dbs.mgd.dao.SEQ_SequenceState;
import org.jax.mgi.dbs.mgd.trans.TranslationException;

/**
 * @is An object that resolves raw GenBank sequence attributes. Reports
 * discrepancies to the validation log.
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

public class GBSeqAttributeResolver extends SequenceAttributeResolver {
    private SEQ_SequenceState state;
    private Integer sequenceTypeKey;
    private Integer sequenceQualityKey;
    private Integer sequenceStatusKey;
    private Integer sequenceProviderKey;
    private String division;

    /**
    * Constructs a Genbank Sequence Attribute Resolver.
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

    public GBSeqAttributeResolver() throws CacheException, ConfigException,
        DBException, TranslationException {
           state = new SEQ_SequenceState();
       }

    /**
     * resolves raw sequence attributes and creates a SEQ_SequenceState
     * @assumes Nothing
     * @effects Nothing
     * @param rawAttributes A set of raw sequence attributes
     * @return sequenceState an object representing resolved sequence attributes
     * @throws Nothing
     */

    public SEQ_SequenceState resolveAttributes(
        SequenceRawAttributes rawAttributes)  throws KeyNotFoundException,
        TranslationException, DBException, CacheException, ConfigException {
        //////////////////////////////////
        // lookup all the foreign keys  //
        //////////////////////////////////
        sequenceTypeKey = typeLookup.lookup(rawAttributes.getType());
        sequenceQualityKey = qualityLookup.lookup(rawAttributes.getQuality());
        sequenceStatusKey = statusLookup.lookup(rawAttributes.getStatus());

        // translate division to create provider name
        division = rawAttributes.getDivision();
        if(division.equals("ROD")) {
           division = "Rodent";
        }
        else if(division.equals("PAT")) {
            division = "Patent";
        }
        sequenceProviderKey = providerLookup.lookup(rawAttributes.getProvider()
            + ":" + division);

        // set the foreign keys
        state.setSequenceTypeKey(sequenceTypeKey);
        state.setSequenceQualityKey(sequenceQualityKey);
        state.setSequenceStatusKey(sequenceStatusKey);
        state.setSequenceProviderKey(sequenceProviderKey);

        // copy remaining raw attributes to the sequence state
        state.setLength(new Integer(rawAttributes.getLength()));
        state.setDescription(rawAttributes.getDescription());
        state.setVersion(rawAttributes.getVersion());
        state.setDivision(rawAttributes.getDivision());
        state.setVirtual(rawAttributes.getVirtual());
        state.setRawType(rawAttributes.getType());
        state.setRawLibrary(rawAttributes.getLibrary());
        state.setRawOrganism(rawAttributes.getRawOrganisms());
        state.setRawStrain(rawAttributes.getStrain());
        state.setRawTissue(rawAttributes.getTissue());
        state.setRawAge(rawAttributes.getAge());
        state.setRawSex(rawAttributes.getSex());
        state.setRawCellLine(rawAttributes.getCellLine());
        state.setNumberOfOrganisms(new Integer(rawAttributes.getNumberOfOrganisms()));
        state.setSeqrecordDate(rawAttributes.getSeqRecDate());

        // GenBank sequences do not have sequence dates - set to the record date
        state.setSequenceDate(rawAttributes.getSeqRecDate());

        return state;
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
