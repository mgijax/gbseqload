package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.timing.Stopwatch;
import org.jax.mgi.shr.dla.input.genbank.GBOrganismChecker;
import org.jax.mgi.shr.dla.loader.seq.SeqLoader;
import org.jax.mgi.shr.dla.input.genbank.GBInputFileNoSeq;
import org.jax.mgi.shr.exception.MGIException;
import java.util.Vector;
import java.util.Iterator;


/**
 * @is an object which extends Seqloader and implements the Seqloader
 * getDataIterator method to set Seqloader's OrganismChecker and
 * RecordDataIterator
 *
 * @has See superclass
 *
 * @does
 * <UL>
 * <LI>implements superclass (Seqloader) getDataIterator to set superclass
 *     OrganismChecker with an EMBLOrganismChecker and create the
 *     superclass RecordDataIterator from an EMBLInputFile
 * <LI>It has an empty implementation of the superclass (DLALoader)
 *     preProcess method
 * <LI>It has an empty implementation of the superclass (Seqloader)
 *     appPostProcess method.
 * </UL>
 * @author sc
 * @version 1.0
 */


public class GBSeqloader extends SeqLoader {

    /**
      * This load has no preprocessing
      * @assumes nothing
      * @effects noting
      * @throws MGIException if errors occur during preprocessing
      */

    protected void preprocess() { }

    /**
     * creates and sets the superclass OrganismChecker and RecordDataIterator
     * with a GBOrganismChecker and creates and creates a GBInputFile
     * with a GBSequenceInterpretor; gets an iterator from the GBInputFile
     * @assumes nothing
     * @effects nothing
     * @throws MGIException
     */
    protected void getDataIterator() throws MGIException {

        // create an organism checker for the interpreter
        GBOrganismChecker oc = new GBOrganismChecker();

        // set oc in the superclass for reporting purposes
        super.organismChecker = oc;

        // Create an GBInputFile
        //GBSequenceInterpreter interp = new GBSequenceInterpreter(oc);
        GBInputFileNoSeq inData = new GBInputFileNoSeq();

        // get an iterator for the GBInputFile with a GBSequenceInterpreter
        super.iterator = inData.getIterator(new GBSequenceInterpreter(oc));
    }

    /**
      * This load has no application specific post processing
      * @assumes nothing
      * @effects noting
      * @throws MGIException if errors occur during preprocessing
      */

   protected void appPostProcess() throws MGIException {

   }
}
