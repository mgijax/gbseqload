//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.config.InputDataCfg;
import org.jax.mgi.shr.ioutils.RecordDataInterpreter;
import org.jax.mgi.shr.dbutils.dao.BCP_Batch_Stream;
import org.jax.mgi.shr.dla.seqloader.MergeSplitProcessor;
import org.jax.mgi.shr.dla.seqloader.IncremSeqProcessor;
import org.jax.mgi.shr.dla.seqloader.SeqEventDetector;
import org.jax.mgi.shr.dla.seqloader.SequenceInput;
import org.jax.mgi.shr.dla.DLALogger;
import org.jax.mgi.shr.ioutils.InputDataFile;
import org.jax.mgi.shr.ioutils.RecordDataIterator;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.bcp.BCPManager;
import org.jax.mgi.shr.dbutils.dao.SQLStream;
import org.jax.mgi.shr.dbutils.dao.BCP_Batch_Stream;
import org.jax.mgi.shr.exception.MGIException;

/**
 * @is
 * @has
 *   <UL>
 *   <LI>
 *   </UL>
 * @does
 *   <UL>
 *   <LI>
 *   <LI>
 *   <LI>
 *
 *   </UL>
 * @company The Jackson Laboratory
 * @author sc
 * @version 1.0
 */

public class GBSeqloader {
    private SequenceInput seqInput;
    private DLALogger logger;
    private InputDataCfg inDataCfg;
    private InputDataFile inData;
    private GBSequenceInterpreter interpreter;
    private RecordDataIterator iterator;
    private SQLDataManager sqlMgr;
    private BCPManager bcpMgr;
    private SQLStream sqlStream;
    private IncremSeqProcessor seqProcessor;
    private SeqEventDetector seqEventDetector;
    private MergeSplitProcessor mergeSplitProcessor;
    private BCP_Batch_Stream bcpBatchStream;

    /**
     * For each sequence record in the input
     *    create a SequenceInput object
     *    Call GBSequenceProcessor.processSequence(SequenceInput)
     * :Sequence
     * After all sequences processed call
     *     Call mergeSplitProcessor.process()
     */
    public void main() {
        GBSeqloader seqloader = new GBSeqloader();
        try {
            seqloader.initialize();
        }
        catch (MGIException e) {
            System.out.println(e.toString());
        }
    }

    /**
     * what this method does ...
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return
     * @throws Nothing
     */

    private void initialize () throws MGIException {
        // instance of a dataload logger
        this.logger = DLALogger.getInstance();

        // Configurator for the input file
        this.inDataCfg = new InputDataCfg();

        // Input file object
        this.inData = new InputDataFile(inDataCfg);

        // Interpreter for the input file
        //this.interpreter = new GBSequenceInterpreter();

        // Iterator for returning one sequence input object at a time
        this.iterator = inData.getIterator(interpreter);

    }
    /**
     * what this method does ...
     * @assumes Nothing
     * @effects Nothing
     * @param None
     * @return
     * @throws Nothing
     */

    private void load () throws MGIException {
        while (this.iterator.hasNext()) {
            seqInput = (SequenceInput)this.iterator.next();
        }
    }


}
