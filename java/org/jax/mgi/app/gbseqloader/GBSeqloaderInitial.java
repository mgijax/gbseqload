package org.jax.mgi.app.gbseqloader;

import java.io.*;
import java.util.*;

import org.jax.mgi.shr.dla.seqloader.SequenceInput;
import org.jax.mgi.shr.dla.seqloader.Sequence;
import org.jax.mgi.shr.dla.seqloader.SeqRefAssocPair;
import org.jax.mgi.shr.dla.seqloader.SeqRefAssocProcessor;
import org.jax.mgi.shr.dla.seqloader.AccAttributeResolver;
import org.jax.mgi.shr.dla.seqloader.AccessionRawAttributes;
import org.jax.mgi.shr.dla.seqloader.SequenceRawAttributes;
import org.jax.mgi.shr.dla.seqloader.SeqDecider;
import org.jax.mgi.shr.dla.seqloader.RefAssocRawAttributes;
import org.jax.mgi.shr.dla.seqloader.SeqRefAssocPair;
import org.jax.mgi.shr.dla.DLALogger;
import org.jax.mgi.shr.dla.DLAException;
import org.jax.mgi.shr.dla.DLAExceptionHandler;
import org.jax.mgi.shr.exception.MGIException;
import org.jax.mgi.shr.cache.KeyNotFoundException;
import org.jax.mgi.shr.config.InputDataCfg;
import org.jax.mgi.shr.config.BCPManagerCfg;
import org.jax.mgi.shr.ioutils.InputDataFile;
import org.jax.mgi.shr.ioutils.RecordDataIterator;
import org.jax.mgi.shr.ioutils.RecordFormatException;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.SQLDataManagerFactory;
import org.jax.mgi.shr.dbutils.bcp.BCPManager;
//import org.jax.mgi.shr.dbutils.dao.BCP_Batch_Stream;
import org.jax.mgi.shr.dbutils.dao.BCP_Inline_Stream;
import org.jax.mgi.dbs.SchemaConstants;
import org.jax.mgi.dbs.mgd.dao.*;
import org.jax.mgi.dbs.mgd.MolecularSource.MSRawAttributes;
import org.jax.mgi.dbs.mgd.MolecularSource.MSProcessorSC;
//import org.jax.mgi.dbs.mgd.MolecularSource.MSProcessor;
import org.jax.mgi.dbs.mgd.MolecularSource.MolecularSource;
import org.jax.mgi.dbs.mgd.MolecularSource.MSException;

// TEMP so can set segmentType and vector for adds
import org.jax.mgi.dbs.mgd.MolecularSource.MolecularSourceSC;

public class GBSeqloaderInitial {

    // Checks a record and determines if the sequence is from an organism
    // we want to load
    private GBOrganismChecker organismChecker;

    // Interpretor for GenBank format sequence records
    private GBSequenceInterpreter interp = null;

    // An input data file object for the input file.
    private InputDataFile inData = null;

    // An iterator that gets one sequence record at a time.
    private RecordDataIterator iter = null;

    // Instance of the dataload logger for sending messages to the log files.
    private DLALogger logger = null;

    // An SQL data manager for providing a connection to the database
    private SQLDataManager sqlMgr = null;

    // A bcp manager for handling bcp inserts for an SQL stream.
    private BCPManager bcpMgr = null;

    // A stream for handling DAO objects.
    private BCP_Inline_Stream stream = null;
    //private BCP_Batch_Stream stream = null;

    // resolves GenBank sequence attributes to MGI values
    private GBSeqAttributeResolver seqResolver = null;

    // resolves accession attributes to MGI values
    private AccAttributeResolver accResolver = null;

    // Resolves sequence reference attributes to MGI values
    private SeqRefAssocProcessor refAssocProcessor = null;

    // The MGI_Reference_Assoc state for a given reference
    private MGI_Reference_AssocState refAssocState = null;

    // A compound object containing DAO's for a sequence,
    // its refs, source associations, and seqids
    private Sequence sequence;

    // resolves molecular source for a sequence
    private MSProcessorSC msProcessor;
    //private MSProcessor msProcessor;
    // An object containing the DAO for a Molecular Source
    private MolecularSource msSource;

    // TEMP class - the Molecular Source that has set capability on ageMin,
    // ageMax, and isCuratorEdited
    private MolecularSourceSC msSourceSC;
    //private MolecularSource msSource;

    public static void main(String[] args) {
        DLAException e1 = null;
        DLAExceptionHandler eh = null;
        GBSeqloaderInitial d = new GBSeqloaderInitial();

        // instantiate objects and initialize variables
        try {
            d.initialize();
        }
        catch (MGIException e) {
            e1 = new DLAException("Sequence loader initialization failed", false);
            e1.setParent(e);
            eh = new DLAExceptionHandler();
            eh.handleException(e1);
            System.out.println(e1.getMessage());
            System.exit(1);
        }

        // load sequences
        try {
            d.load();
        }
        catch (MGIException e) {
            e1 = new DLAException("Sequence loader failed", false);
            e1.setParent(e);
            eh = new DLAExceptionHandler();
            eh.handleException(e1);
            System.out.println(e1.getMessage());
            System.exit(1);
        }
    }

    /**
     * Instantiates objects for the sequence load
     * @assumes Nothing
     * @effects Initializes all instance variables
     * @param None
     * @return
     * @throws MGIException if any objects cannot be instantiated
     */

    private void initialize ()  throws MGIException {
        // Create an instance of the dataload logger.
        logger = DLALogger.getInstance();
        logger.logpInfo("Perform initialization", false);
        logger.logdInfo("Perform initialization",true);

        // an InputDataFile has a Configurator from which itgets its file name
        inData =  new InputDataFile();

        // create an organism checker to pass to the interpreter
        organismChecker = new GBOrganismChecker();

        // Create an interpretor and get an iterator that uses that interpreter
        interp = new GBSequenceInterpreter(organismChecker);
        iter = inData.getIterator(interp);

        // Create a SQLDataManager for the MGD database from the factory.
        sqlMgr = SQLDataManagerFactory.getShared(SchemaConstants.MGD);

        // Create a bcp manager that has been configured for the MGD database.
        bcpMgr = new BCPManager(new BCPManagerCfg("MGD"));

        // Provide the bcp manager with the SQL data manager and the logger.
        bcpMgr.setSQLDataManager(sqlMgr);
        bcpMgr.setLogger(logger);

        // Create a stream for handling DAO objects.
        //stream = new BCP_Batch_Stream(sqlMgr, bcpMgr);
        stream = new BCP_Inline_Stream(sqlMgr, bcpMgr);

        // Create a GenBank Sequence Attribute Resolver
        seqResolver = new GBSeqAttributeResolver();

        // Create an Accession Attribute Resolver
        accResolver = new AccAttributeResolver();

        // Create a Reference Association Processor
        refAssocProcessor = new SeqRefAssocProcessor();

        // Create a Molecular Source Processor
        msProcessor = new MSProcessorSC();
        //msProcessor = new MSProcessor();
    }
    /**
    * Iterates through sequence records, interpreting then resolving them to MGI
    * values. Writes valid seqs to bcp files and sql scripts.
    * Executes bcp and SQL scripts
    * @assumes Nothing
    * @effects Writes to bcp and sql script files, and executes them. Adds and
    * updates data in the database
    * @param None
    * @return 0 exit code if success
    * @throws MGIException if there is an error
    */

    private void load () throws MGIException {
        logger.logpInfo("Iterating through sequences", false);
        logger.logdInfo("Iterating through sequences", true);

        // the sequence state returned from the sequence resolver
        SEQ_SequenceState sequenceState;

        // Compound object holding raw data for a sequence, its references, seqids,
        // and source returned from the InputDataFile iterator
        SequenceInput si;

        // number of valid sequences WITHOUT processing errors:
        int passedCtr = 0;

        // number of valid sequences WITH processing errors
        int errCtr = 0;

        while (iter.hasNext()) {
            //interpret the sequence, if record format errors log and go to next sequence
            try {
                si = (SequenceInput) iter.next();
            }
            catch (RecordFormatException e) {
                logger.logdErr(e.getMessage());
                errCtr++;
                continue;
            }
            // resolve raw sequence
            try {
                sequenceState = seqResolver.resolveAttributes(si.getSeq());
            }
            // if resolving errors log and go to next sequence, all other
            // exceptions thrown as MGIException out to main
            catch (KeyNotFoundException e) {
                logger.logdErr(e.getMessage());
                errCtr++;
                continue;
            }

            // create the compound sequence
            sequence = new Sequence(sequenceState, stream);

            // resolve primary accession attributes and set the accession state
            // in the Sequence
            // Note: Exceptions thrown resolving accessions are thrown
            // out to main; indicates a bad LogicalDB value in the config file
            sequence.setAccPrimary(
                accResolver.resolveAttributes(
                    si.getPrimaryAcc(), sequence.getSequenceKey()));
            logger.logdDebug("Primary: " +
                ((MSRawAttributes)si.getMSources().get(0)).getOrganism() + " " +
                    si.getPrimaryAcc().getAccID(), false);

            // resolve secondary accessions and set the accession states in the
            // Sequence
            // Note: Exceptions thrown resolving accessions are thrown
            // out to main; indicates a bad LogicalDB value in the config file
            Iterator i = si.getSecondary().iterator();
            while(i.hasNext()) {
                AccessionRawAttributes ara = (AccessionRawAttributes)i.next();
                logger.logdDebug("Secondary: " +
                        (ara).getAccID(), false);
                sequence.addAccSecondary(
                    accResolver.resolveAttributes(ara,
                        sequence.getSequenceKey()));
            }

            // resolve sequence reference associations and set the states
            // in the Sequence
            // Note: Exceptions thrown resolving reference associations are
            // are thrown out to main; indicates a logicalDB other than MEDLINE
            // or PubMed
            i = si.getRefs().iterator();
            while(i.hasNext()) {
                refAssocState = refAssocProcessor.process(
                    (SeqRefAssocPair)i.next(),
                        sequence.getSequenceKey());

                // null if reference not in MGI
                if(refAssocState != null) {
                    sequence.addRefAssoc(refAssocState);
                }
            }
            // resolve and process Molecular Source then create SEQ_Source
            // associations
            i = si.getMSources().iterator();
            try {

                while (i.hasNext()) {

                    // should get the primary accid from 'sequence' when we implement
                    // the copy in sequence.getAccPrimary()

                    // process the molecular source
                    msSource = msProcessor.processNewSeqSrc(
                        si.getPrimaryAcc().getAccID(), (MSRawAttributes) i.next());

                    // TEMP: wrap it in class that allows us to set ageMin,
                    // ageMax, and isCuratorEdited
                    msSourceSC = new MolecularSourceSC(msSource);
                    msSourceSC.setCuratorEdited(Boolean.FALSE);
                    msSourceSC.setageMinAgeMax(new Float( -1));

                    // create a new source association state
                    SEQ_Source_AssocState sourceAssocState = new SEQ_Source_AssocState();

                    // set the sequence key and set in the source association
                    sourceAssocState.setSequenceKey(sequence.getSequenceKey());

                    // set the source key and set it in the source association
                    sourceAssocState.setSourceKey(msSourceSC.getMSKey());

                    // set the source association in the Sequence
                    sequence.addSeqSrcAssoc(sourceAssocState);

                    // this is temporary - we assume insertion of all source;
                    // here we'll need to should check an attribute
                    // of the MSSource to see if it needs to be inserted
                    msSourceSC.insert(stream);
                }
            }
            // if there is an error processing the source, we skip this sequence
            catch (MSException e) {
                logger.logdErr(e.getMessage());
                errCtr++;
                continue;
            }
            sequence.sendToStream();
            if (passedCtr > 0 && passedCtr%10000 == 0)
                logger.logdInfo("Processed " + passedCtr + " input records",false);
            passedCtr++;
        }
        // report count of total sequences processed
        // note until we remove the check for named library this total will
        // reflect only anonymous source sequences
        Vector v = organismChecker.getDeciderCounts();
        Iterator i = v.iterator();
        while (i.hasNext()) {
               logger.logpInfo((String)i.next(), false);
        }


        // report count of the valid sequences NOT processed because of errors
        logger.logpInfo("Total valid sequences with errors: " + errCtr, false);

        // close the stream; this executes bcp, batch, sql scripts etc.
        logger.logdInfo("Executing bcp and sql script files", false);
        stream.close();

        // close the logger, remove this when writing to log from the jobstream
        logger.close();
    }
}
