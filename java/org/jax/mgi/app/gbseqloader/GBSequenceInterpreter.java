//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import java.util.*;
import java.util.regex.*;
import java.sql.*;

import org.jax.mgi.shr.dla.seqloader.SequenceInterpreter;
import org.jax.mgi.shr.dla.seqloader.SequenceInput;
import org.jax.mgi.shr.dla.seqloader.SeqloaderConstants;
import org.jax.mgi.shr.dla.seqloader.SeqRefAssocPair;
import org.jax.mgi.shr.dla.seqloader.OrganismChecker;
import org.jax.mgi.shr.dla.seqloader.DateConverter;
import org.jax.mgi.shr.dla.seqloader.AccessionRawAttributes;
import org.jax.mgi.shr.dla.seqloader.RefAssocRawAttributes;
import org.jax.mgi.shr.dla.seqloader.SequenceRawAttributes;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.StringLib;
import org.jax.mgi.dbs.mgd.AccessionLib;
import org.jax.mgi.dbs.mgd.MolecularSource.MSRawAttributes;


    /**
     * @is An object that parses a GenBank sequence record and obtains values
     *     from a Configurator to create a SequenceInput data object.<BR>
     *     Determines if a GenBank sequence record is valid.
     * @has
     *   <UL>
     *   <LI> A raw sequence object
     *   <LI> A raw accession object for its primary and each secondary id
     *   <LI> A raw reference association object for each reference that has a
     *        PubMed and/or Medline id
     *   <LI> A raw source object
     *   <LI> A set of String constants for parsing
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

public class GBSequenceInterpreter extends SequenceInterpreter {
    //////////////////////////////////////
    // constants for String searching  //
    /////////////////////////////////////
    // temp stuff to rule out sequences with libraries
    private Matcher m;
    private Pattern organismPattern = Pattern.compile("clone_lib=",
        Pattern.MULTILINE);

    // Strings to find GB seq record TAGS
    private static String LOCUS = "LOCUS";
    private static String DEFINITION = "DEFINITION";
    private static String ACCESSION = "ACCESSION";
    private static String VERSION = "VERSION";
    private static String ORGANISM = "ORGANISM";
    private static String REFERENCE = "REFERENCE";
    private static String MEDLINE = "MEDLINE";
    private static String PUBMED = "PUBMED";
    private static String FEATURES = "FEATURES";
    private static String SOURCE = "source";
    private static String ORIGIN = "ORIGIN";

    // Strings to find GB seq record source qualifiers
    private static String LIBRARY = "/clone_lib";
    private static String STRAIN = "/strain";
    private static String TISSUE= "/tissue_type";
    private static String AGE = "/dev_stage";
    private static String SEX = "/sex";
    private static String CELLINE = "/cell_line";

    ///////////////////////////////////////////////
    // A SequenceInput and its parts            //
    //////////////////////////////////////////////

    // An object representing a sequence,
    // its source, references, and accessions
    private SequenceInput sequenceInput = new SequenceInput();

    // raw attributes for a sequence
    private SequenceRawAttributes rawSeq = new SequenceRawAttributes();

    // raw attributes for a sequences source
    private MSRawAttributes ms = new MSRawAttributes();

    // raw attributes for the primary seqid
    private AccessionRawAttributes primary = new AccessionRawAttributes();

    // temp reference reused in createAccession()
    private AccessionRawAttributes tempSeqid;

    // Accession prefix and numeric parts
    private Vector splitAccession;

    //////////////////////
    // parsing flags    //
    //////////////////////
    private boolean moreDefLines;
    private boolean accFound;
    private boolean sourceFound;

    //////////////////////////////////////////////////////////////////
    // vars to hold sections of a sequence record for later parsing //
    //////////////////////////////////////////////////////////////////
    private String locus;
    private StringBuffer definition;
    private StringBuffer accession;
    private String version;
    private String organism;
    private StringBuffer classification;
    private StringBuffer reference;
    private StringBuffer source;

    /////////////////////////////////////////
    // helper vars for parsing the record  //
    /////////////////////////////////////////

    // split a record lines
    private StringTokenizer st;

    // a line in the record
    private String line;

    // split a line into fields; some methods use 'st' and 'tokenizer'
    private StringTokenizer tokenizer;

    // a field from a line
    private String field;

    // Uses a set of deciders to determine if the sequence is from an organism
    // of interest
    OrganismChecker organismChecker;

    /**
    * Constructs a GenBank Sequence interpretor
    * @assumes Nothing
    * @effects Nothing
    * @param None
    * @throws ConfigException if can't find configuration file
    */

    public GBSequenceInterpreter(Vector deciders) throws ConfigException {
        super();

        // initialize all instance variables
        reset();

        // Create an organism checker for GenBank with the set of deciders
        this.organismChecker = new OrganismChecker(deciders, SeqloaderConstants.GENBANK);
    }

    /**
     * Parses a sequence record,  creates a SequenceInput object from
     * Configuration and parsed values
     * @assumes Nothing
     * @effects Nothing
     * @param rcd A sequence record
     * @return A SequenceInput object
     * @throws RecordFormatException if we can't parse an attribute because of
     *         record formatting errors
     */

    public Object interpret(String rcd) throws RecordFormatException {
        // initialize all instance variables
        reset();

        // split the record into lines
        st = new StringTokenizer(rcd, SeqloaderConstants.CRT);

        // iterate through each line getting individual sections of the sequence
        while (st.hasMoreTokens()) {
            line = st.nextToken().trim();

            // get the LOCUS line
            if(line.startsWith(LOCUS)) {
                locus = line;
            }
            // get the first DEFINITION line
            else if(line.startsWith(DEFINITION)) {
                definition.append(line);

                // > 1 def line if first line does not end w/PERIOD; set a flag
                if(! line.endsWith(SeqloaderConstants.PERIOD) ) {
                    moreDefLines = true;
                }
            }
            // get another DEFINITION line
            else if(moreDefLines ) {
                definition.append(SeqloaderConstants.SPC + line);

                // period indicates last def line
                if(line.endsWith(SeqloaderConstants.PERIOD) ) {
                    moreDefLines = false;
                }
            }
            // get the first ACCESSION line
            else if (line.startsWith(ACCESSION)) {
                accession.append(line + SeqloaderConstants.CRT);

                // We have found first ACCESSION line
                accFound = true;
            }
            else if (line.startsWith(VERSION)) {
            // Parse the VERSION line to get the version number
            // The VERSION line indicates the end of the ACCESSION
            //      lines
            // The VERSION line contains two identifiers:
            // 1) The PrimaryAccession.versionNumber
            // 2) The NCBI GI identifier
                version = line;
                accFound = false;
            }
            // get another ACCESSION line
            else if(accFound) {
           // we have the first ACCESSION line but haven't reached
           // VERSION line yet so we have multiple ACCESSION lines
           // Note the test for VERSION must be before this test
                accession.append(line + SeqloaderConstants.CRT);
            }
            // get the entire REFERENCE section
            else if(line.startsWith(REFERENCE)) {
                // add reference lines until we find the FEATURES line
                while (! line.startsWith(FEATURES) && st.hasMoreTokens()){
                    reference.append(line + SeqloaderConstants.CRT);
                    line = st.nextToken().trim();
                }
            }
            // get only ONE source section
            else if(line.startsWith(SOURCE) && ! sourceFound && st.hasMoreTokens()) {
                source.append(line +  SeqloaderConstants.CRT);
                line = st.nextToken().trim();
                // get all the lines for ONE source. We are done when we find
                // another source line or when we find the ORIGIN line
                while(! line.startsWith(SOURCE) && ! line.startsWith(ORIGIN) &&
                      st.hasMoreTokens()) {
                      source.append(line +  SeqloaderConstants.CRT);
                      line = st.nextToken().trim();
                }
                // reset flag so we don't get another source
                sourceFound = true;
            }
            // get the ORGANISM line
            else if (line.startsWith(ORGANISM)) {
                organism = line;
            }
            // source is the last thing we parse; we're done
            if (sourceFound) {
                break;
            }
        }
        // Now parse the individual sequence record sections
        // Note: Order of method calls is important - these methods do two things
        // 1. set attributes of the objects that make up a SequenceInput e.g.
        // set attributes of SequenceRawAttributes, MSRawAttributes, etc
        // 2. set the attributes that make up a SequenceInput e.g.
        // set sequence, molecular source, references, etc
        setSeqFromConfig();
        parseLocus(locus);
        parseDefinition(definition.toString());
        parseAccession(accession.toString());
        parseVersion(version);
        parseOrganism(organism);
        parseSource(source.toString());
        parseReference(reference.toString());
        return sequenceInput;
    }

    /**
     * Determines whether we will load this sequence
     * @assumes Nothing
     * @effects Nothing
     * @param record A GenBank sequence record
     * @return true if we want to load this sequence
     * @throws Nothing
     */

    public boolean isValid(String record) {
        // TEMP for gbseqloader-slim
        m = organismPattern.matcher(record);

        return (! m.find() && organismChecker.checkOrganism(record));
    }

    /**
     * Initializes/resets instance variables
     * @assumes Nothing
     * @effects Nothing
     * @param record A GenBank sequence record
     * @return true if we want to load this sequence
     * @throws Nothing
     */

    private void reset () {
        //////////////////////////////////////////////////////
        // reset the SequenceInput and its parts            //
        //////////////////////////////////////////////////////
        sequenceInput.reset();
        rawSeq.reset();
        ms.reset();
        primary.reset();
        tempSeqid = null;
        splitAccession = null;

        /////////////////////////
        // reset parsing flags //
        /////////////////////////
        moreDefLines = false;
        accFound = false;
        sourceFound = false;

        ////////////////////////////////////////////////////////////////////////
        // reset vars that hold sections of a sequence record for later parsing
        ////////////////////////////////////////////////////////////////////////
        locus = null;
        definition = new StringBuffer();
        accession  = new StringBuffer();
        version = null;
        organism = null;
        classification  = new StringBuffer();
        reference = new StringBuffer();
        source = new StringBuffer();

        /////////////////////////////////////
        // reset helper vars for parsing   //
        /////////////////////////////////////
        st = null;
        line = null;
        tokenizer = null;
        field = null;

    }
    /**
     * Sets sequence attributes from the Configuration file.
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return Nothing
     * @throws Nothing
     */

    private void setSeqFromConfig() {
        rawSeq.setVirtual(virtual);
        rawSeq.setProvider(provider);
        rawSeq.setStatus(seqStatus);
    }


    /**
       * Parses molecular source attributes from the SOURCE section of a
       * GenBank sequence record and creates a MSRawAttributes object. Sets
       * the the MSRawAttributes and SequenceRawAttributes in
       * the SequenceInput
       * @assumes organism has been set on the MSRawAttributes object and
       *  all non-source SequenceRawAttributes have been set
       * @effects Nothing
       * @param source SOURCE section parsed from a GenBank sequence record
       * @return nothing
       * @throws Nothing
       */

    private void parseSource(String source) throws RecordFormatException {

        // Split the source section into individual lines
        st = new StringTokenizer(source, SeqloaderConstants.CRT);

        // a qualifier line split into qualifier and value on '='
        ArrayList splitLine = null;

        // the source qualifier
        String qualifier = null;

        // value of the source qualifier
        String value = null;

        // Set strain, tissue, gender, and cellLine to 'Not Specified'
        // if we find values for them they'll be reset
        ms.setStrain(SeqloaderConstants.NOT_SPECIFIED);
        ms.setTissue(SeqloaderConstants.NOT_SPECIFIED);
        ms.setGender(SeqloaderConstants.NOT_SPECIFIED);
        ms.setCellLine(SeqloaderConstants.NOT_SPECIFIED);

        while(st.hasMoreTokens()) {
            line = st.nextToken();
            // all qualifiers start with '/'
            if (line.startsWith(SeqloaderConstants.SLASH)) {
                // split the qualifier line on '='
                splitLine = StringLib.split(line, SeqloaderConstants.EQUAL);

                // there will always be qualifier, but not necessarily a value
                // go figure ?!
                qualifier = (String) splitLine.get(0);
                if (splitLine.size() == 2) {
                    // get the qualifier
                    value = (String)splitLine.get(1);
                    // remove quotes
                    value = value.replaceAll(SeqloaderConstants.DBL_QUOTE,
                                             SeqloaderConstants.EMPTY_STRING);
                }
                // set source and raw sequence attributes
                if (qualifier.startsWith(LIBRARY)) {
                    ms.setLibraryName(value);
                    rawSeq.setLibrary(value);
                }
                else if (qualifier.startsWith(STRAIN)) {
                    ms.setStrain(value);
                    rawSeq.setStrain(value);
                }
                else if (qualifier.startsWith(TISSUE)) {
                    ms.setTissue(value);
                    rawSeq.setTissue(value);
                }
                else if (qualifier.startsWith(AGE)) {
                    rawSeq.setAge(value);
                }
                else if (qualifier.startsWith(SEX)) {
                    ms.setGender(value);
                    rawSeq.setSex(value);
                }
                else if (qualifier.startsWith(CELLINE)) {
                    ms.setCellLine(value);
                    rawSeq.setCellLine(value);
                }
            }
        }
        // add 'ms' to 'sequenceInput'
        // Note: Assumes that ms organism attribute has been set
         sequenceInput.addMSource(ms);

        // set rawSeq in 'sequenceInput
        // Note: Assumes that all non-source rawSeq attributes have already
        // been set
        sequenceInput.setSeq(rawSeq);
    }

    /**
     * Parses sets of MedLine and PubMed ids from all REFERENCE sections in a
     * GenBank sequence record where they exist. Creates a RefAssocRawAttributes
     * object for each id, bundles them in a pair, then sets the pair in the
     * SequenceInput object.
     * If a reference has only one id, the other in the pair is null.
     * @assumes Nothing
     * @effects Nothing
     * @param reference All REFERENCE sections parsed from a GenBank sequence record
     * @return nothing
     * @throws Nothing
     */

    private void parseReference(String reference) {
        // holders for pubmed and medline ids
        String pubmed = null;
        String medline = null;

        // split the REFERENCE section into individual lines
        st = new StringTokenizer(reference, SeqloaderConstants.CRT);

        // get the first REFERENCE line and throw it away
        line = st.nextToken();

        while(st.hasMoreTokens()) {
            line = st.nextToken().trim();

            // get pubmed/medline id for one reference
            while (!line.startsWith(REFERENCE) && st.hasMoreTokens()){
                if (line.startsWith(PUBMED)) {
                    pubmed = ( (String) StringLib.split(line).get(1)).trim();
                }
                else if (line.startsWith(MEDLINE)) {
                    medline = ( (String) StringLib.split(line).get(1)).trim();
                }
                line = st.nextToken().trim();
            }
            // if we got any ids for this reference create reference objects and
            // add them to SequenceInput object
            if (pubmed != null || medline != null) {
                createReference(pubmed, medline);
                pubmed = null;
                medline = null;
            }
        }
    }

    /**
     * Parses the organism from the ORGANISM line of a GenBank sequence record.
     * Sets the organism in the SequenceRawAttributes and MSRawAttributes objects
     * Sets numberOfOrganisms to 0
     * @assumes Nothing
     * @effects Nothing
     * @param organism The ORGANISM line from a GenBank sequence record
     * @return nothing
     * @throws Nothing
     */

    private void parseOrganism(String organism) {
        // set the organism field of raw sequence and raw molecular source
        rawSeq.setRawOrganisms(organism.substring(9).trim());
        rawSeq.setNumberOfOrganisms(0);
        ms.setOrganism(organism.substring(9).trim());
    }

    /**
     * Parses the version number from the VERSION line of a GenBank sequence
     * record. Sets the version in the SequenceRawAttributes object
     * @assumes Nothing
     * @effects Nothing
     * @param version The VERSION line parsed from a GenBank sequence record
     * @return Nothing
     * @throws Nothing
     */

    private void parseVersion(String version) {
        // split the VERSION line into individual tokens
        tokenizer = new StringTokenizer(version);

        // discard the VERSION tag field
        field = tokenizer.nextToken();

        // get the version number by splitting the token on '.' and
        // taking the second token e.g. given AC002397.1 the version we
        // are setting is "1"
        String vers = ((String)StringLib.split(
            tokenizer.nextToken(), SeqloaderConstants.PERIOD).get(1)).trim();
        rawSeq.setVersion(vers);
    }

    /**
     * Parses seqids from the ACCESSION section of a GenBank sequence record.
     * Creates an AccessionRawAttributes object for each seqid.
     * The first seqid on first line is primary, any remaining seqids
     * are secondary.
     * Sets the Primary and Secondary AccessionRawAttributes in the SequenceInput
     * object
     * @assumes Nothing
     * @effects Nothing
     * @param accession The ACCESSION section of a GenBank sequence record.
     * @return Nothing
     * @throws Nothing
     */

    private void parseAccession(String accession) {
        // split the ACCESSION section into individual lines
        st = new StringTokenizer(accession, SeqloaderConstants.CRT);
        line = st.nextToken();

        // split the line into individual tokens
        tokenizer = new StringTokenizer(line);

        // discard the ACCESSION tag field
        field = tokenizer.nextToken();

        // create a primary accession object
        field = tokenizer.nextToken();
        createAccession(field, Boolean.TRUE);

        // create 2ndary accessions from first ACCESSION line
         while(tokenizer.hasMoreTokens())
         {
             field = tokenizer.nextToken().trim();
             // add a secondary accession to SequenceInput
             createAccession(field, Boolean.FALSE);
         }

         // create 2ndary accessions from remaining ACCESSION lines
         while(st.hasMoreTokens()) {
             line = st.nextToken();
             tokenizer = new StringTokenizer(line);
             while(tokenizer.hasMoreTokens()) {
                 field = tokenizer.nextToken().trim();
                 // add a secondary accession to SequenceInput
                 createAccession(field, Boolean.FALSE);

             }
         }
    }

    /**
     * Parses the definition from the DEFINITION section of a GenBank sequence
     * record. Sets the definition in the SequenceRawAttributes object.
     * Truncates the definition to 255 characters if needed
     * @assumes Nothing
     * @effects Nothing
     * @param definition The definition section of a GenBank sequence record
     * @return Nothing
     * @throws Nothing
     */

    private void parseDefinition(String definition) {
        if (definition.length() > 255) {
            rawSeq.setDescription(definition.substring(12, 255));
        }
        else {
            rawSeq.setDescription(definition.substring(12));
        }
    }

    /**
     * Parses the LOCUS line of a GenBank sequence record. Sets the length,
     * type, division, quality, sequence record date and sequence date in the
     * SequenceRawAttributes object
     * @assumes Nothing
     * @effects Nothing
     * @param locus The LOCUS line of a GenBank sequence record.
     * @return Nothing
     * @throws Nothing
     */

    private void parseLocus(String locus) {
        // unlike other sections of a GenBank record we must use columns as
        // there are a small number of cases where fields abut
        rawSeq.setLength(locus.substring(29, 40).trim());

        // get the sequence type
        rawSeq.setType(locus.substring(47, 53).trim());

        // get the Genbank division code
        rawSeq.setDivision(locus.substring(64, 67));

        // interpret quality from division
        if(locus.substring(64, 67).equals("EST") ||
           locus.substring(64, 67).equals("HTG")) {
            rawSeq.setQuality("Medium");
        }
        else {
            rawSeq.setQuality("High");
        }

        // get the sequence record date
        rawSeq.setSeqRecDate(DateConverter.convertDate(
            locus.substring(68, 79)));
    }

    /**
     * Creates one RefAssocRawAttributes object each for a pubmed id and a
     * medline id,  bundles them in a pair, then sets the pair in the
     * SequenceInput object. If 'pubmed' or 'medline' is null, then the
     * RefAssociationRawAttribute for that id is null
     * @assumes Nothing
     * @effects Nothing
     * @param pubmed Pubmed id for a reference or null
     * @param medline Medline id for the same reference or null
     * @return Nothing
     * @throws Nothing
     */

    private void createReference (String pubmed, String medline) {
        // create a pubmed object
        RefAssocRawAttributes pm = null;
        if(pubmed != null) {
            pm = new RefAssocRawAttributes();
            pm.setRefId(pubmed);
            pm.setRefAssocType(this.refAssocType);
            pm.setMgiType(this.seqMGIType);
        }
        // create a medline object
        RefAssocRawAttributes ml = null;
        if(medline != null) {
            ml = new RefAssocRawAttributes();
            ml.setRefId(medline);
            ml.setRefAssocType(this.refAssocType);
            ml.setMgiType(this.seqMGIType);
        }
        // create a pair and add to the SequenceInput references
        sequenceInput.addRef(new SeqRefAssocPair(pm, ml));
    }

    /**
     * Creates an AccessionRawAttributes object and sets it in the SequenceInput
     * object.
     * @assumes Nothing
     * @effects Nothing
     * @param accid Accession id of a sequence
     * @param preferred If true, 'accid' is primary, else 'accid' is 2ndary
     * @return nothing
     * @throws Nothing
     */

    private void createAccession(String accid, Boolean preferred) {
        // create new object for secondary seqid
        if (preferred != Boolean.TRUE) {
            tempSeqid = new AccessionRawAttributes();
        }
        // reuse object for primary
        else {
            tempSeqid = primary;
            tempSeqid.reset();
        }
        // set attributes of AccessionRawAttributes
        tempSeqid.setAccid(accid);
        tempSeqid.setIsPreferred(preferred);

        // GenBank seqids are public
        tempSeqid.setIsPrivate(Boolean.FALSE);

        // split up the seqid into its prefix and numeric part
        splitAccession = AccessionLib.splitAccID(accid);
        tempSeqid.setPrefixPart((String)splitAccession.get(0));
        tempSeqid.setNumericPart((Integer)splitAccession.get(1));

        // clear for reuse
        splitAccession.clear();

        // set attributes from Configuration
        tempSeqid.setLogicalDB(seqLogicalDB);
        tempSeqid.setMgiType(seqMGIType);

        // set in SequenceInput
        if(preferred.equals(Boolean.TRUE)) {
            sequenceInput.setPrimaryAcc(tempSeqid);
        }
        else {
            sequenceInput.addSecondary(tempSeqid);
        }
    }
}

//  $Log$

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