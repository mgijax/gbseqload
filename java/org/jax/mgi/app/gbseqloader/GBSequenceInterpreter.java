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
import org.jax.mgi.shr.dla.seqloader.DateConverter;
import org.jax.mgi.shr.dla.seqloader.AccessionRawAttributes;
import org.jax.mgi.shr.dla.seqloader.RefAssocRawAttributes;
import org.jax.mgi.shr.dla.seqloader.SequenceRawAttributes;
import org.jax.mgi.shr.config.ConfigException;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.stringutil.StringLib;
import org.jax.mgi.dbs.mgd.MolecularSource.MSRawAttributes;


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

    // String constants to find GB seq record TAGS and indicate the current
    // record section we are looking for
    private static String LOCUS = "LOCUS";
    private static String DEFINITION = "DEFINITION";
    private static String ACCESSION = "ACCESSION";
    private static String VERSION = "VERSION";
    private static String ORGANISM = "ORGANISM";
    private static String REFERENCE = "REFERENCE";
    private static String MEDLINE = "MEDLINE";
    private static String PUBMED = "PUBMED";
    private static String FEATURES = "FEATURES";

    // this is the FEATURES sub-section keyword 'source' as opposed to keyword
    // 'SOURCE'
    private static String SOURCE = "source";
    private static String ORIGIN = "ORIGIN";

    // int constants to indicate the current
    // record section we are looking for
    private static int LOCUS_SECTION = 1;
    private static int DEFINITION_SECTION = 2;
    private static int ANOTHER_DEF_LINE = 3;
    private static int ACCESSION_SECTION = 4;
    private static int ANOTHER_ACCESSION_LINE = 5;
    private static int ORGANISM_SECTION = 6;
    private static int REFERENCE_SECTION = 7;
    private static int ANOTHER_REFERENCE_LINE = 8;
    private static int SOURCE_SECTION = 9;
    private static int ANOTHER_SOURCE_LINE = 10;

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

    // raw attributes for a sequence - reused by calling reset()
    private SequenceRawAttributes rawSeq = new SequenceRawAttributes();

    // raw attributes for a sequences source - reused by calling reset()
    private MSRawAttributes ms = new MSRawAttributes();

    // checks a sequence record to see if it an organism we want to load
    private GBOrganismChecker organismChecker;

    // vars that hold sequence record sections for later parsing
    private String locus;
    private StringBuffer definition;
    private StringBuffer accession;
    private String version;
    private String organism;
    private StringBuffer classification;
    private StringBuffer reference;
    private StringBuffer source;

    /**
    * Constructs a GBSequenceInterpreter
    * @assumes Nothing
    * @effects Nothing
    * @param None
    * @throws ConfigException if can't find configuration file
    */

    public GBSequenceInterpreter(GBOrganismChecker oc) throws ConfigException {
        // Create an organism checker for GenBank with the set of deciders
        this.organismChecker = oc;

        // Initialize vars that hold sequence record sections for later parsing
        locus = null;
        definition = new StringBuffer();
        accession  = new StringBuffer();
        version = null;
        organism = null;
        classification  = new StringBuffer();
        reference = new StringBuffer();
        source = new StringBuffer();
    }

    /**
     * Parses a sequence record and  creates a SequenceInput object from
     * Configuration and parsed values
     * @assumes Nothing
     * @effects Nothing
     * @param rcd A sequence record
     * @return A SequenceInput object representing 'rcd'
     * @throws RecordFormatException if we can't parse an attribute because of
     *         record formatting errors
     */

    public Object interpret(String rcd) throws RecordFormatException {

        // the current section we are looking for
        int currentSection = LOCUS_SECTION;

        // re-initialize vars that hold sequence record sections for later parsing
        locus = null;
        definition.setLength(0);
        accession.setLength(0);
        version = null;
        organism = null;
        classification.setLength(0);
        reference.setLength(0);
        source.setLength(0);

        // reset reused instance variables
        sequenceInput.reset();
        rawSeq.reset();
        ms.reset();

        // set the record attribute of the SequenceRawAttributes
        rawSeq.setRecord(rcd);

        // split the record into lines
        StringTokenizer lineSplitter = new StringTokenizer(rcd, SeqloaderConstants.CRT);
        String line;

        // iterate through each line getting individual sections of the sequence
        while (lineSplitter.hasMoreTokens()) {
          // be sure to trim each line so String.startsWith works properly
          line = lineSplitter.nextToken().trim();

          // if we are currently looking for the LOCUS line check to see if
          // 'line' is the LOCUS line
          if (currentSection== LOCUS_SECTION) {
            if (line.startsWith(LOCUS)) {
              // get the LOCUS line
              locus = line;
              currentSection = DEFINITION_SECTION;
            }
          }
          // if we are currently looking for the DEFINITION line check to see
          // if 'line' is the DEFINITION line
          else if (currentSection == DEFINITION_SECTION) {
            if (line.startsWith(DEFINITION)) {
              // get the first DEFINITION line
              definition.append(line);

              // > 1 def line if first line does not end w/PERIOD
              if (!line.endsWith(SeqloaderConstants.PERIOD)) {
                // Now were looking for another definition line
                currentSection = ANOTHER_DEF_LINE;
              }
              else {
                // now we are looking for the ACCESSION line
                currentSection = ACCESSION_SECTION;
              }
            }
          }
          // if we are currently looking for another definition line, then get
          // it
          else if (currentSection == ANOTHER_DEF_LINE) {
            //get another DEFINITION line
            definition.append(SeqloaderConstants.SPC + line);

            // period indicates last def line
            if (line.endsWith(SeqloaderConstants.PERIOD)) {
              // now we are looking for the ACCESSION line
              currentSection = ACCESSION_SECTION;
            }
          }
          // if we are currently looking for the first ACCESSION line check to see
          // if 'line' is the ACCESSION line
          else if (currentSection == ACCESSION_SECTION) {
            if (line.startsWith(ACCESSION)) {
              // get the first ACCESSION line
              accession.append(line + SeqloaderConstants.CRT);
              // now we are looking for another accession line
              currentSection = ANOTHER_ACCESSION_LINE;
            }
          }
          // if we are currently looking for another accession line
          else if (currentSection == ANOTHER_ACCESSION_LINE) {
            // if 'line' is not the VERSION line, it is another accession line
            if (!line.startsWith(VERSION)) {
              accession.append(line + SeqloaderConstants.CRT);
            }
            // if 'line' is the VERSION line get it. Now we are looking for the
            // ORGANISM line
            else {
              // now we are looking for ORGANISM
              version = line;
              currentSection = ORGANISM_SECTION;
            }
          }
          // if we are currently looking for the ORGANISM line check to see if
          // 'line is the ORGANISM line
          else if (currentSection == ORGANISM_SECTION) {
            if (line.startsWith(ORGANISM)) {
              // get the ORGANISM line
              organism = line;

              // now we are looking for the first REFERENCE line
              currentSection = REFERENCE_SECTION;
            }
          }

          // if we are currently looking for the first REFERENCE line check to
          // see if 'line' is the REFERENCE line
          else if (currentSection == REFERENCE_SECTION) {
            if (line.startsWith(REFERENCE)) {
              // get the first REFERENCE line
              reference.append(line + SeqloaderConstants.CRT);
              currentSection = ANOTHER_REFERENCE_LINE;
            }
          }
          // if we are looking for another REFERENCE line
          else if (currentSection == ANOTHER_REFERENCE_LINE) {
            // check to see if 'line' is the FEATURES line
            if (! line.startsWith(FEATURES)){
                reference.append(line + SeqloaderConstants.CRT);
            }
            // if it is the FEATURES line, we are now looking for the
            // first features source line
            else {
              currentSection = SOURCE_SECTION;
            }
          }

          // if we are looking for the first FEATURES source line, check to
          // see if 'line' is a FEATURES source line
          else if (currentSection == SOURCE_SECTION) {
            if (line.startsWith(SOURCE)) {
              // get the first features source line
              source.append(line + SeqloaderConstants.CRT);

              // now we are looking for another features source line
              currentSection = ANOTHER_SOURCE_LINE;
            }
          }
          // if we are looking for another features source line
          else if (currentSection == ANOTHER_SOURCE_LINE) {
            // we only want ONE features source section; if we find another
            // features source line or we find the ORIGIN line we are done
            if (!line.startsWith(SOURCE) && !line.startsWith(ORIGIN)) {
              source.append(line + SeqloaderConstants.CRT);
            }
            else {
              break;
            }
          }
        }

        // set attributes from Configuration (super class hold the values
        // of virtual, provider, and seqStatus)
        rawSeq.setVirtual(virtual);
        rawSeq.setProvider(provider);
        rawSeq.setStatus(seqStatus);

        ///////////////////////////////////////////////////////////////////////
        // Now parse the individual sequence record sections and set values in
        // *RawAttributes objects
        ///////////////////////////////////////////////////////////////////////
        parseLocus(locus);
        parseDefinition(definition.toString());
        parseVersion(version);
        parseOrganism(organism);
        parseSource(source.toString());

        // this method also adds AccessionRawAttributes objects
        // for the primary seqid and any secondary seqids to the SequenceInput object
        parseAccession(accession.toString());

        // this method also adds RefAssocRawAttributes objects to the
        // SequenceInput object
        parseReference(reference.toString());

        // add 'ms' to 'sequenceInput'
        sequenceInput.addMSource(ms);

        // set rawSeq in 'sequenceInput
        sequenceInput.setSeq(rawSeq);

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
        return (organismChecker.checkOrganism(record));
    }

    /**
       * Parses molecular source attributes from the first FEATURES source
       * section of a GenBank sequence record and sets them in MSRawAttributes
       * and SequencRawAttributes objects
       * @assumes Nothing
       * @effects Nothing
       * @param source first FEATURES source section parsed from a GenBank
       * sequence record<BR>
       * FEATURES source section example, note that the first and last lines
       * (FEATURES and ORIGIN are not included in the text of 'source' but added
       *  here for clarity. Note also that this is a contrived source section
       *  used in testing:<BR>
       * <PRE>
       * FEATURES             Location/Qualifiers
       *      source          1..3133
       *                      /organism="Mus musculus"
       *                      /db_xref="taxon:10090"
       *                      /tissue_type="placenta day 20"
       *                      /dev_stage="dpc 14.5"
       *                      /strain="129SJVMus"
       *                      /sex="Female"
       *                      /cell_line="MS-1"
       *       CDS            155..1597
       *                      /codon_start=1
       *                      /product="GATA-2 protein"
       *                      /protein_id="BAA19053.1"
       *                      /db_xref="GI:1754586"
       *                      /translation="MEVAPEQPRWMAHPAVLNAQHPDSHHPGLAHNYMEPAQLLPPDE
       *                      VDVFFNHLDSQGNPYYANPAHARARVSYSPAHARLTGGQMCRPHLLHSPGLPWLDGGK
       *      polyA_site      3133
       *                      /note="16 A nucleotides"
       *      BASE COUNT      681 a    949 c    828 g    675 t
       *      ORIGIN
       * </PRE>
       * @return nothing
       * @throws Nothing
       */

    private void parseSource(String source) throws RecordFormatException {

        // Split the source section into individual lines
        StringTokenizer lineSplitter = new StringTokenizer(
            source, SeqloaderConstants.CRT);
        String line;

        // a qualifier line split into qualifier and value on '='
        ArrayList splitLine = null;

        // the source qualifier
        String qualifier = null;

        // value of the source qualifier
        String value = null;

        while(lineSplitter.hasMoreTokens()) {
            line = lineSplitter.nextToken().trim();
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
    }

    /**
     * Parses sets of MedLine and PubMed ids from all REFERENCE sections in a
     * GenBank sequence record where they exist. Creates a RefAssocRawAttributes
     * object for each id, bundles them in a pair, then sets the pair in the
     * SequenceInput object.
     * If a reference has only one id, the other in the SeqRefAssocPair is null.
     * @assumes Nothing
     * @effects Nothing
     * @param reference All REFERENCE sections parsed from a GenBank sequence record
     * <BR>
     * REFERENCE section example, note that this is a contrived source section
     * used in testing. I added reference ids found in MGI: <BR>
     * <PRE>
     * REFERENCE   1  (bases 1 to 3133)
     *   AUTHORS   Yamamoto,M.
     *   TITLE     Direct Submission
     *   JOURNAL   Submitted (25-DEC-1996) Masayuki Yamamoto, Institute of Basic
     *             Medical Sciences, University of Tsukuba, Molecular and
     *             Developmental Biology; Tennoudai, Tsukuba, Ibaragi 305, Japan
     *             (E-mail:masiya@igaku.md.tsukuba.ac.jp, Tel:81-0298-53-3111,
     *             Fax:81-0298-53-6965)
     * REFERENCE   2  (sites)
     *   AUTHORS   Suwabe,N., Minegishi,N. and Yamamoto,M.
     *   TITLE     mouse GATA-2 cDNA
     *   JOURNAL   Unpublished (1996)
     *   MEDLINE   92239361
     *   PUBMED    1571281
     * </PRE>
     * @return nothing
     * @throws Nothing
     */

    private void parseReference(String reference) {
        // holders for pubmed and medline ids
        String pubmed = null;
        String medline = null;

        // split the REFERENCE section into individual lines
        StringTokenizer lineSplitter = new StringTokenizer(
            reference, SeqloaderConstants.CRT);
        String line;

        // get the first REFERENCE line and throw it away
        line = lineSplitter.nextToken().trim();

        while(lineSplitter.hasMoreTokens()) {
            line = lineSplitter.nextToken().trim();

            // get pubmed/medline id for one reference
            while (!line.startsWith(REFERENCE) ){
                if (line.startsWith(PUBMED)) {
                    pubmed = ( (String) StringLib.split(line).get(1)).trim();
                }
                else if (line.startsWith(MEDLINE)) {
                    medline = ( (String) StringLib.split(line).get(1)).trim();
                }
                if (lineSplitter.hasMoreTokens()) {
                  line = lineSplitter.nextToken().trim();
                }
                // this was the last line of 'reference'
                else {
                  break;
                }
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
     * <BR>
     * ORGANISM line example:<BR>
     * <PRE>
     *   ORGANISM  Mus musculus
     * </PRE>
     * @return nothing
     * @throws Nothing
     */

    private void parseOrganism(String organism) {
        String rawOrganism = organism.substring(9).trim();
        // set the organism field of raw sequence and raw molecular source
        rawSeq.setRawOrganisms(rawOrganism);
        rawSeq.setNumberOfOrganisms(0);
        ms.setOrganism(rawOrganism);
    }

    /**
     * Parses the version number from the VERSION line of a GenBank sequence
     * record. Sets the version in the SequenceRawAttributes object
     * @assumes Nothing
     * @effects Nothing
     * @param version The VERSION line parsed from a GenBank sequence record
     * <BR>
     * VERSION line example:<BR>
     * VERSION     AB000096.1  GI:1754585
     * @return Nothing
     * @throws Nothing
     */

    private void parseVersion(String version) {
        // split the VERSION line into individual fields
        StringTokenizer fieldSplitter = new StringTokenizer(version);
        String field;
        // discard the VERSION tag field
        field = fieldSplitter.nextToken();

        // get the version number by splitting the token on '.' and
        // taking the second token e.g. given AC002397.1 the version we
        // are setting is "1"
        String vers = ((String)StringLib.split(
            fieldSplitter.nextToken(), SeqloaderConstants.PERIOD).get(1)).trim();
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
     * <BR>
     * example of ACCESSION section, note this a contrived section used in testing
     * secondary ids that span multiple lines:<BR>
     * ACCESSION   AB000098 AB000097 AB000096 AB000095 AB000094 AB000093 AB000092
     *             AB000091
     * @return Nothing
     * @throws Nothing
     */

    private void parseAccession(String accession) {
        // split the ACCESSION section into individual lines
        StringTokenizer lineSplitter = new StringTokenizer(
            accession, SeqloaderConstants.CRT);


        String line = lineSplitter.nextToken().trim();

        // split the accession line into individual tokens
        StringTokenizer fieldSplitter = new StringTokenizer(line);
        String field;

        // discard the ACCESSION tag field
        field = fieldSplitter.nextToken();

        // create a primary accession object
        field = fieldSplitter.nextToken().trim();
        createAccession(field, Boolean.TRUE);

        // create 2ndary accessions from first ACCESSION line
         while(fieldSplitter.hasMoreTokens())
         {
             field = fieldSplitter.nextToken().trim();
             // add a secondary accession to SequenceInput
             createAccession(field, Boolean.FALSE);
         }

         // create 2ndary accessions from remaining ACCESSION lines
         while(lineSplitter.hasMoreTokens()) {
             line = lineSplitter.nextToken().trim();
             fieldSplitter = new StringTokenizer(line);
             while(fieldSplitter.hasMoreTokens()) {
                 field = fieldSplitter.nextToken().trim();
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
     * <BR>
     * Example of DEFINITION section, note that this section can span multiple lines
     * in a record, but 'definition' does not contain newlines:
     * DEFINITION  Mus musculus mRNA for GATA-2 protein, complete cds
     * <BR>
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
         //System.out.println(definition.substring(12));
    }

    /**
     * Parses the LOCUS line of a GenBank sequence record. Sets the length,
     * type, division, quality, sequence record date and sequence date in the
     * SequenceRawAttributes object
     * @assumes Nothing
     * @effects Nothing
     * @param locus The LOCUS line of a GenBank sequence record.
     * <BR>
     * Example of LOCUS line:<BR>
     * LOCUS       AB000096                3133 bp     DNA    linear   ROD 05-FEB-1999
     * <BR>
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
     * medline id,  bundles them in a SeqRefAssocPari, then sets the pair in the
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

        AccessionRawAttributes seqid = new AccessionRawAttributes();

        // set attributes of AccessionRawAttributes
        seqid.setAccid(accid);
        seqid.setIsPreferred(preferred);

        // GenBank seqids are public
        seqid.setIsPrivate(Boolean.FALSE);

        // set attributes from Configuration
        seqid.setLogicalDB(seqLogicalDB);
        seqid.setMgiType(seqMGIType);

        // set in SequenceInput
        if(preferred.equals(Boolean.TRUE)) {
            sequenceInput.setPrimaryAcc(seqid);
        }
        else {
            sequenceInput.addSecondary(seqid);
        }
    }
}

//  $Log$
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