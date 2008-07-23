package org.jax.mgi.app.gbseqloader;

import java.util.*;
import java.util.regex.*;
import java.sql.*;

import org.jax.mgi.shr.dla.input.SequenceInterpreter;
import org.jax.mgi.shr.dla.input.SequenceInput;
import org.jax.mgi.shr.dla.loader.seq.SeqloaderConstants;
import org.jax.mgi.shr.dla.input.DateConverter;
import org.jax.mgi.dbs.mgd.loads.Acc.AccessionRawAttributes;
import org.jax.mgi.dbs.mgd.loads.SeqRefAssoc.RefAssocRawAttributes;
import org.jax.mgi.dbs.mgd.loads.Seq.SequenceRawAttributes;
import org.jax.mgi.shr.dla.input.genbank.GBFormatInterpreter;
import org.jax.mgi.shr.dla.input.genbank.GBOrganismChecker;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.stringutil.StringLib;
import org.jax.mgi.dbs.mgd.loads.SeqSrc.MSRawAttributes;

    /**
     * @is An object that parses a GenBank sequence record and obtains values
     *     from a Configurator to create a SequenceInput data object.<BR>
     *     Determines if a GenBank sequence record is valid.
     * @has
     *   <UL>
     *   <LI>A SequenceInput object into which it bundles:
     *   <LI>A SequenceRawAttributes object
     *   <LI>An AccessionRawAttributes object for its primary seqid
     *   <LI>One AccessionRawAttributes object for each secondary seqid
     *   <LI> A RefAssocRawAttributes object for each reference that has a
     *        PubMed and/or Medline id
     *   <LI> A MSRawAttributes
     *   <LI> See also superclass
     *   </UL>
     * @does
     *   <UL>
     *   <LI>Determines if a GenBank sequence record is valid
     *   <LI>Parses a GenBank sequence record
     *   </UL>
     * @company The Jackson Laboratory
     * @author sc
     * @version 1.0
     */

public class GBSequenceInterpreter extends GBFormatInterpreter {

    private SequenceInput seqInput;

    public GBSequenceInterpreter(GBOrganismChecker oc) throws ConfigException {
        super(oc);
    }
    /**
     * Parses a sequence record and  creates a SequenceInput object from
     * Configuration and parsed values. Sets sequence Quality for GenBank
     * sequences by division
     * @assumes Nothing
     * @effects Nothing
     * @param rcd A sequence record
     * @return A SequenceInput object representing 'rcd'
     * @throws RecordFormatException if we can't parse an attribute because of
     *         record formatting errors
     */

    public Object interpret(String rcd) throws RecordFormatException {
        // call superclass to parse the record and get config
	seqInput = (SequenceInput)super.interpret(rcd);

        // if this is a Third Party Annotation Sequence - set quality to medium
        // If this is EST, HTG, or STS division - seq quality to medium
        // otherwise set quality to high
        String keyword = seqInput.getSeq().getMisc();
        String division = seqInput.getSeq().getDivision();
	if (division.equals("EST") || division.equals("HTG") ||
		division.equals("STS") ||
                keyword.indexOf(SeqloaderConstants.TPA) != -1)	{
	    seqInput.getSeq().setQuality(SeqloaderConstants.MED_QUAL);
        }
        else {
            seqInput.getSeq().setQuality(SeqloaderConstants.HIGH_QUAL);
        }

        // use provider and division to set the provider
        String provider = seqInput.getSeq().getProvider();
        seqInput.getSeq().setProvider(provider + ":" + division);

        // return the SequenceInput object with quality set
        return seqInput;
    }
}
