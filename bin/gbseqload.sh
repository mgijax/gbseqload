#!/bin/sh
#
#  $Header
#  $Name
#
#  gbseqload.sh
###########################################################################
#
#  Purpose:  This script controls the execution of the GenBank Sequence Load
#
#  Usage:
#
#      gbseqload.sh
#
#  Env Vars:
#
#      See the configuration file
#
#  Inputs:
#
#      - Common configuration file (/usr/local/mgi/etc/common.config.sh)
#      - GenBank load configuration file (gbseqload.config)
#      - One or more GenBank input files 
#
#  Outputs:
#
#      - An archive file
#      - Log files defined by the environment variables ${LOG_PROC},
#        ${LOG_DIAG}, ${LOG_CUR} and ${LOG_VAL}
#      - BCP files for for inserts to each database table to be loaded
#      - SQL script file for updates
#      - Records written to the database tables
#      - Exceptions written to standard error
#      - Configuration and initialization errors are written to a log file
#        for the shell script
#      - QC reports as defined by ${APP_SEQ_QCRPT} and ${APP_MSP_QCRPT}
#
#  Exit Codes:
#
#      0:  Successful completion
#      1:  Fatal error occurred
#      2:  Non-fatal error occurred
#
#  Assumes:  Nothing
#
#  Implementation:  
#
#  Notes:  None
#
###########################################################################

#
#  Set up a log file for the shell script in case there is an error
#  during configuration and initialization.
#
cd `dirname $0`/..
LOG=`pwd`/gbseqload.log
rm -f ${LOG}

#
#  Verify the argument(s) to the shell script.
#
if [ $# -ne 0 ]
then
    echo "Usage: $0" | tee -a ${LOG}
    exit 1
fi

#
#  Establish the configuration file names.
#
CONFIG_COMMON=`pwd`/common.config.sh
CONFIG_LOAD=`pwd`/gbseqload.config
echo ${CONFIG_LOAD}

#
#  Make sure the configuration files are readable.
#
if [ ! -r ${CONFIG_COMMON} ]
then
    echo "Cannot read configuration file: ${CONFIG_COMMON}" | tee -a ${LOG}
    exit 1
fi

if [ ! -r ${CONFIG_LOAD} ]
then
    echo "Cannot read configuration file: ${CONFIG_LOAD}" | tee -a ${LOG}
    exit 1
fi

#
# Source the common configuration files
#
. ${CONFIG_COMMON}

#
# Source the GenBank Load configuration files
#
. ${CONFIG_LOAD}


echo "javaruntime:${JAVARUNTIMEOPTS}"
echo "classpath:${CLASSPATH}"
echo "dbserver:${MGD_DBSERVER}"
echo "database:${MGD_DBNAME}"

#
#  Source the DLA library functions.
#
if [ "${DLAJOBSTREAMFUNC}" != "" ]
then
    if [ -r ${DLAJOBSTREAMFUNC} ]
    then
	. ${DLAJOBSTREAMFUNC}
    else
	echo "Cannot source DLA functions script: ${DLAJOBSTREAMFUNC}"
	exit 1
    fi
else
    echo "Environment variable DLAJOBSTREAMFUNC has not been defined."
    exit 1
fi

#
#  Function that performs cleanup tasks for the job stream prior to
#  termination.
#
shutDown ()
{
    #
    # report location of logs
    #
    echo "\nSee logs at ${LOGDIR}\n" >> ${LOG_PROC}

    #
    # call DLA library function
    #
    postload

    #
    #  Mail the logs to the support staff.
    #
    mailLog "GenBank Load" | tee -a ${LOG}
}

#
# Function that runs to java load
#

run ()
{
    #
    # log time and input files to process
    #
    echo "\n`date`" >> ${LOG_PROC}
    echo "Files read from stdin: ${APP_CAT_METHOD} ${APP_INFILES}" | \
	tee -a ${LOG_DIAG} ${LOG_PROC}
    #
    # run gbseqload
    #
    ${APP_CAT_METHOD}  ${APP_INFILES}  | \
	${JAVA} ${JAVARUNTIMEOPTS} -classpath ${CLASSPATH} \
	-DCONFIG=${CONFIG_COMMON},${CONFIG_LOAD} \
	-DJOBKEY=${JOBKEY} ${DLA_START}

    STAT=$?
    if [ ${STAT} -ne 0 ]
    then
	echo "gbseqload processing failed.  \
	    Return status: ${STAT}" >> ${LOG_PROC}
	shutDown
	exit 1
    fi
    echo "gbseqload completed successfully" >> ${LOG_PROC}
}

##################################################################
# main
##################################################################

#
# createArchive, startLog, getConfigEnv, get job key
#
preload

# if we are processing the non-cums (incremental mode)
# get a set of files, 1 file or set < configured value in MB (compressed)
echo "checking APP_RADAR_INPUT: ${APP_RADAR_INPUT}"
if [ ${APP_RADAR_INPUT} = true -a ${SEQ_LOAD_MODE} = incremental ]
then
    echo 'Getting files to Process' | tee -a ${LOG_DIAG}
    # set the input files to empty string
    APP_INFILES=""

    # get input files 
    APP_INFILES=`${RADARDBUTILSDIR}/bin/getFilesToProcess.csh \
	${RADAR_DBSCHEMADIR} ${JOBSTREAM} ${SEQ_PROVIDER} ${APP_RADAR_MAX}`
    STAT=$?
    if [ ${STAT} -ne 0 ]
    then
	echo "getFilesToProcess.csh failed.  \
	    Return status: ${STAT}" >> ${LOG_PROC}
	shutDown
	exit 1
    fi
    # if no input files report and shutdown gracefully
    if [ "${APP_INFILES}" = "" ]
    then
	echo "No files to process" | tee -a ${LOG_DIAG} ${LOG_PROC}
	shutDown
	exit 0
    fi
    # save to new var, if we are processing repeats APP_INFILES
    # is reassigned and we won't be able to log processed files properly
    FILES_PROCESSED=${APP_INFILES}
    echo 'Done getting files to Process' | tee -a ${LOG_DIAG}
fi
# if we get here then APP_INFILES not set in configuration this is an error
#echo "APP_INFILES=${APP_INFILES}"
if [ "${APP_INFILES}" = "" ]
then
    # set STAT for endJobStream.py called from postload in shutDown
    STAT=1
    echo "APP_RADAR_INPUT=${APP_RADAR_INPUT}. SEQ_LOAD_MODE=${SEQ_LOAD_MODE}. Check that APP_INFILES has been configured. Return status: ${STAT}" | tee -a ${LOG_PROC}
    shutDown
    exit 1
fi

#
# run the load
#
run

#
# run any repeat files if configured to do so
#
ctr=1
if [ ${APP_PROCESS_REPEATS} = true ]
echo "APP_PROCESS_REPEATS=${APP_PROCESS_REPEATS}"
then
    while [ -s ${SEQ_REPEAT_FILE} ]
    # while repeat file exists and is not length 0
    do
	echo "we have a repeat file, rename it for processing"
	# rename the repeat file
	mv ${SEQ_REPEAT_FILE} ${APP_REPEAT_TO_PROCESS}

	# set the cat method
	APP_CAT_METHOD=/usr/bin/cat

	# set the input file name
	APP_INFILES=${APP_REPEAT_TO_PROCESS}
	echo "running repeat file"
	# run the load
	run

	# remove the repeat file we just ran
	#echo "Removing ${APP_REPEAT_TO_PROCESS}"
	#rm ${APP_REPEAT_TO_PROCESS}
	echo "saving repeat file ${APP_REPEAT_TO_PROCESS}.${ctr}"
	mv ${APP_REPEAT_TO_PROCESS} ${APP_REPEAT_TO_PROCESS}.${ctr}
        ctr=`expr ${ctr} + 1`
    done

fi

# if we are processing the non-cums (incremental mode)
# log the non-cums we processed
if [ ${APP_RADAR_INPUT} = true -a ${SEQ_LOAD_MODE} = incremental ]
then
    echo "Logging processed files ${FILES_PROCESSED}" | tee -a ${LOG_DIAG}
    for file in ${FILES_PROCESSED}
    do
	${RADARDBUTILSDIR}/bin/logProcessedFile.csh ${RADAR_DBSCHEMADIR} \
	    ${JOBKEY} ${file}
	STAT=$?
	if [ ${STAT} -ne 0 ]
	then
	    echo "logProcessedFile.csh failed.  \
		Return status: ${STAT}" >> ${LOG_PROC}
	    shutDown
	    exit 1
	fi

    done
    echo 'Done logging processed files' | tee -a ${LOG_DIAG}
fi

#
# run msp qc reports
#
${APP_MSP_QCRPT} ${RADAR_DBSCHEMADIR} ${MGD_DBNAME} ${JOBKEY} ${RPTDIR}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Running MSP QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    shutDown
    exit 1
fi

#
# run seqload qc reports
#
${APP_SEQ_QCRPT} ${RADAR_DBSCHEMADIR} ${MGD_DBNAME} ${JOBKEY} ${RPTDIR}
STAT=$?
if [ ${STAT} -ne 0 ]
then
    echo "Running seqloader QC reports failed.  Return status: ${STAT}" >> ${LOG_PROC}
    shutDown
    exit 1
fi

#
# run postload cleanup and email logs
#
shutDown

exit 0

$Log

###########################################################################
#
# Warranty Disclaimer and Copyright Notice
#
#  THE JACKSON LABORATORY MAKES NO REPRESENTATION ABOUT THE SUITABILITY OR
#  ACCURACY OF THIS SOFTWARE OR DATA FOR ANY PURPOSE, AND MAKES NO WARRANTIES,
#  EITHER EXPRESS OR IMPLIED, INCLUDING MERCHANTABILITY AND FITNESS FOR A
#  PARTICULAR PURPOSE OR THAT THE USE OF THIS SOFTWARE OR DATA WILL NOT
#  INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS, OR OTHER RIGHTS.
#  THE SOFTWARE AND DATA ARE PROVIDED "AS IS".
#
#  This software and data are provided to enhance knowledge and encourage
#  progress in the scientific community and are to be used only for research
#  and educational purposes.  Any reproduction or use for commercial purpose
#  is prohibited without the prior express written permission of The Jackson
#  Laboratory.
#
# Copyright \251 1996, 1999, 2002, 2003 by The Jackson Laboratory
#
# All Rights Reserved
#
###########################################################################
