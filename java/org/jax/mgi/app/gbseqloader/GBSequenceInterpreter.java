//  $Header
//  $Name

package org.jax.mgi.app.gbseqloader;

import java.util.*;
import java.util.regex.*;
import java.sql.*;

import org.jax.mgi.shr.dla.input.SequenceInterpreter;
import org.jax.mgi.shr.dla.input.SequenceInput;
import org.jax.mgi.shr.dla.loader.seq.SeqloaderConstants;
import org.jax.mgi.dbs.mgd.loads.SeqRefAssoc.SeqRefAssocPair;
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

//  $Log$
//  Revision 1.11.2.2  2004/11/17 14:32:07  sc
//  removed extraneous import
//
//  Revision 1.11.2.1  2004/11/17 13:33:40  sc
//  tr6047 lib_java_dla refactoring
//
//  Revision 1.11  2004/07/12 17:21:28  sc
//  TR5925 set quality to medium if a tpa sequence
//
//  Revision 1.10  2004/04/27 15:46:17  sc
//   moved creation of provider string from GBSeqloadAttributeResolver
//
//  Revision 1.9  2004/03/31 18:54:51  sc
//  comment edit
//
//  Revision 1.8  2004/02/26 21:10:47  sc
//  much code was factored out of this class to lib_java_dla.GBFormatInterpreter.java so that it may be shared with RefSeq
//
//  Revision 1.6  2004/02/17 18:30:45  sc
//  Create new StringBuffers rather than StringBuffer.setLength(0)
//
//  Revision 1.5  2004/02/11 15:51:54  sc
//  moved some local vars to class vars so we dont have to create a new StringBuffer each time (just set its length to 0
//
//  Revision 1.4  2004/02/02 19:33:06  sc
//  Removed code in isValid() that checked for named libraries - vestige of GBSeqloaderIntial.java
//
//  Revision 1.3  2004/01/07 19:01:04  sc
//  updated import statement for StringLib
//
//  Revision 1.2  2003/12/20 16:32:01  sc
//  Changed from code review
//
//  Revision 1.1  2003/12/08 18:50:30  sc
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
*
**************************************************************************/
