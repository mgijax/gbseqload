//  $Header$
//  $Name$

package org.jax.mgi.app.gbseqloader;

import org.jax.mgi.shr.timing.Stopwatch;
import org.jax.mgi.shr.dla.seqloader.GBOrganismChecker;
import org.jax.mgi.shr.dla.seqloader.SeqLoader;
import org.jax.mgi.shr.dla.seqloader.GBInputFile;
import org.jax.mgi.shr.exception.MGIException;
import java.util.Vector;
import java.util.Iterator;


/**
 * @is an object which extends Seqloader and implements the Seqloader
 * getRecordDataIterator method to set Seqloader's OrganismChecker and
 * RecordDataIterator
 *
 * @has See superclass
 *
 * @does
 * <UL>
 * <LI>implements superclass (Seqloader) getRecordDataIterator to set superclass
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
    protected void getRecordDataIterator() throws MGIException {

        // create an organism checker for the interpreter
        GBOrganismChecker oc = new GBOrganismChecker();

        // set oc in the superclass for reporting purposes
        super.organismChecker = oc;

        // Create an GBInputFile with a GBSequenceInterpreter
        GBSequenceInterpreter interp = new GBSequenceInterpreter(oc);
        GBInputFile inData = new GBInputFile(interp);

        // get an iterator for the GBInputFile
        super.iterator = inData.getIterator();
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
// $Log
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
