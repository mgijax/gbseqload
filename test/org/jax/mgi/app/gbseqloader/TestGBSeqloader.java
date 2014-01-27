package org.jax.mgi.app.gbseqloader;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.jax.mgi.shr.unitTest.TestManager;
import org.jax.mgi.shr.unitTest.FileUtility;
import org.jax.mgi.shr.dbutils.SQLDataManager;
import org.jax.mgi.shr.dbutils.ResultsNavigator;
import org.jax.mgi.shr.dbutils.DBSchema;
import org.jax.mgi.dbs.mgd.dao.*;

import junit.framework.*;

public class TestGBSeqloader
    extends TestCase {
  private SQLDataManager sqlMgr = null;
  private GBSeqloader gBSeqloader = null;
  private TestManager testMgr = null;
  private ACC_AccessionLookup accLookup = null;
  private PRB_SourceLookup srcLookup = null;
  private SEQ_SequenceLookup seqLookup = null;
  private SEQ_Source_AssocLookup assocLookup = null;
  private DBSchema schema = null;

  public TestGBSeqloader(String name) {
    super(name);
  }

  protected void setUp() throws Exception {
    super.setUp();
    sqlMgr = new SQLDataManager();
    schema = sqlMgr.getDBSchema();
    createTriggers();
    deleteFiles();
    testMgr = new TestManager();
    testMgr.setConfig("SEQ_LOAD_MODE", "incremental");
    testMgr.setConfig("SEQ_LOAD_MOUSE", "true");
    testMgr.setConfig("SEQ_LOAD_HUMAN", "false");
    testMgr.setConfig("SEQ_LOAD_RAT", "false");
    testMgr.setConfig("SEQ_VIRTUAL", "false");
    testMgr.setConfig("SEQ_PROVIDER", "GenBank");
    testMgr.setConfig("SEQ_LOGICALDB", "Sequence DB");
    testMgr.setConfig("MGD_SCP_FILENAME", "mergeSplits");
    testMgr.setConfig("SCP_FILENAME", "updates");
    testMgr.setConfig("SCP_OK_TO_OVERWRITE", "true");
    testMgr.setConfig("MGD_SCP_OUTFILENAME", "mergeSplits");
    testMgr.setConfig("SCP_OUTFILENAME", "updates");
    testMgr.setConfig("SEQ_REPEAT_FILE", "repeats.out");
    testMgr.setConfig("JOBSTREAM", "genbank_load");
    testMgr.setConfig("DLA_LOAD_STREAM",
                      "org.jax.mgi.shr.dbutils.dao.BCP_Script_Stream");
    testMgr.setConfig("BCP_OK_TO_OVERWRITE", "true");
    accLookup = new ACC_AccessionLookup();
    srcLookup = new PRB_SourceLookup();
    seqLookup = new SEQ_SequenceLookup();
    assocLookup = new SEQ_Source_AssocLookup();
    testMgr.resetKey("ACC_Accession");
    testMgr.resetKey("PRB_Source");
    testMgr.resetKey("SEQ_Sequence");
    testMgr.resetKey("SEQ_Source_Assoc");
    gBSeqloader = new GBSeqloader();
  }

  protected void tearDown() throws Exception {
    deleteFiles();
    sqlMgr = null;
    schema = null;
    gBSeqloader = null;
    accLookup = null;
    srcLookup = null;
    seqLookup = null;
    assocLookup = null;
    super.tearDown();
  }

  /**
   * event: add
   * number of sources: 1
   * number of secondary accids: 0
   * number of references: 0
   * source name: anonymous
   * clones cache: full
   * organism: mouse
   * number of repeats: 0
   * @throws Exception
   */
  public void testAdd1() throws Exception
  {
    testMgr.setConfig("INFILE_NAME", "test/single");
    assertEquals(0, testMgr.countObjects("ACC_Accession"));
    assertEquals(0, testMgr.countObjects("PRB_Source"));
    assertEquals(0, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(0, testMgr.countObjects("SEQ_Sequence"));

    gBSeqloader.load();

    // check database against expected results

    dropTriggers();

    ACC_AccessionState accState = new ACC_AccessionState();
    accState.setAccID("AB000096");
    accState.setPrefixPart("AB");
    accState.setObjectKey(new Integer(1));
    accState.setNumericPart(new Integer(96));
    accState.setLogicalDBKey(new Integer(9));
    accState.setMGITypeKey(new Integer(19));
    accState.setPrivateVal(new Boolean(false));
    accState.setPreferred(new Boolean(true));
    accState.setCreatedByKey(new Integer(1));
    accState.setModifiedByKey(new Integer(1));
    ResultsNavigator nav = accLookup.findByState(accState);
    assertTrue(nav.next());
    testMgr.deleteObject((ACC_AccessionDAO)nav.getCurrent());

    SEQ_Source_AssocState assocState = new SEQ_Source_AssocState();
    assocState.setSequenceKey(new Integer(1));
    assocState.setSourceKey(new Integer(1));
    assocState.setCreatedByKey(new Integer(1));
    assocState.setModifiedByKey(new Integer(1));
    nav = assocLookup.findByState(assocState);
    assertTrue(nav.next());
    testMgr.deleteObject((SEQ_Source_AssocDAO)nav.getCurrent());

    PRB_SourceState srcState = new PRB_SourceState();
    srcState.setSegmentTypeKey(new Integer(83333));
    srcState.setVectorKey(new Integer(316369));
    srcState.setOrganismKey(new Integer(1));
    srcState.setStrainKey(new Integer(-1));
    srcState.setTissueKey(new Integer(6932));
    srcState.setGenderKey(new Integer(315167));
    srcState.setCellLineKey(new Integer(316335));
    srcState.setAge("Not Resolved");
    srcState.setAgeMin(new Float(-1.0));
    srcState.setAgeMax(new Float(-1.0));
    srcState.setIsCuratorEdited(new Boolean(false));
    nav = srcLookup.findByState(srcState);
    assertTrue(nav.next());
    testMgr.deleteObject((PRB_SourceDAO)nav.getCurrent());

    SEQ_SequenceState seqState = new SEQ_SequenceState();
    seqState.setSequenceTypeKey(new Integer(316346));
    seqState.setSequenceQualityKey(new Integer(316338));
    seqState.setSequenceStatusKey(new Integer(316342));
    seqState.setSequenceProviderKey(new Integer(316373));
    seqState.setLength(new Integer(3133));
    seqState.setDescription("Mus musculus mRNA for GATA-2 protein, " +
                            "complete cds.");
    seqState.setVersion("2");
    seqState.setDivision("ROD");
    seqState.setVirtual(new Boolean(false));
    seqState.setRawType("mRNA");
    seqState.setRawLibrary(null);
    seqState.setRawTissue("liver");
    seqState.setRawAge("dpc 14.5");
    seqState.setRawOrganism("Mus musculus");
    seqState.setNumberOfOrganisms(new Integer(0));
    nav = seqLookup.findByState(seqState);
    assertTrue(nav.next());
    testMgr.deleteObject((SEQ_SequenceDAO)nav.getCurrent());

    createTriggers();

  }

  /**
   * event: non event
   * number of sources: 1
   * number of secondary accids: 0
   * number of references: 0
   * source name: anonymous
   * clones cache: full
   * organism: mouse
   * number of repeats: 0
   * @throws Exception
   */
  public void testNonEvent1() throws Exception
  {
    testMgr.setConfig("INFILE_NAME", "test/single");
    assertEquals(0, testMgr.countObjects("ACC_Accession"));
    assertEquals(0, testMgr.countObjects("PRB_Source"));
    assertEquals(0, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(0, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();

    // data preperation
    ACC_AccessionState accState = new ACC_AccessionState();
    accState.setAccID("AB000096");
    accState.setPrefixPart("AB");
    accState.setObjectKey(new Integer(1));
    accState.setNumericPart(new Integer(96));
    accState.setLogicalDBKey(new Integer(9));
    accState.setMGITypeKey(new Integer(19));
    accState.setPrivateVal(new Boolean(false));
    accState.setPreferred(new Boolean(true));
    accState.setCreatedByKey(new Integer(1));
    accState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new ACC_AccessionDAO(accState));

    SEQ_Source_AssocState assocState = new SEQ_Source_AssocState();
    assocState.setSequenceKey(new Integer(1));
    assocState.setSourceKey(new Integer(1));
    assocState.setCreatedByKey(new Integer(1));
    assocState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new SEQ_Source_AssocDAO(assocState));

    PRB_SourceState srcState = new PRB_SourceState();
    srcState.setSegmentTypeKey(new Integer(83333));
    srcState.setVectorKey(new Integer(316369));
    srcState.setOrganismKey(new Integer(1));
    srcState.setStrainKey(new Integer(-1));
    srcState.setTissueKey(new Integer(6932));
    srcState.setGenderKey(new Integer(315167));
    srcState.setCellLineKey(new Integer(316335));
    srcState.setAge("Not Resolved");
    srcState.setAgeMin(new Float(-1.0));
    srcState.setAgeMax(new Float(-1.0));
    srcState.setIsCuratorEdited(new Boolean(false));
    testMgr.stageData(new PRB_SourceDAO(srcState));

    SEQ_SequenceState seqState = new SEQ_SequenceState();
    seqState.setSequenceTypeKey(new Integer(316346));
    seqState.setSequenceQualityKey(new Integer(316338));
    seqState.setSequenceStatusKey(new Integer(316342));
    seqState.setSequenceProviderKey(new Integer(316373));
    seqState.setLength(new Integer(3133));
    seqState.setDescription("Mus musculus mRNA for GATA-2 protein, " +
                            "complete cds.");
    seqState.setVersion("2");
    seqState.setDivision("ROD");
    seqState.setVirtual(new Boolean(false));
    seqState.setRawType("mRNA");
    seqState.setRawLibrary(null);
    seqState.setRawTissue("liver");
    seqState.setRawAge("dpc 14.5");
    seqState.setRawOrganism("Mus musculus");
    seqState.setNumberOfOrganisms(new Integer(0));
    seqState.setSeqrecordDate(new Timestamp(new Date().getTime()));
    seqState.setSequenceDate(new Timestamp(new Date().getTime()));
    testMgr.stageData(new SEQ_SequenceDAO(seqState));

    testMgr.commitStage();

    createTriggers();

    gBSeqloader.load();

    // verify that no records changed in the database
    assertTrue(accLookup.findByState(accState).next());
    assertTrue(srcLookup.findByState(srcState).next());
    assertTrue(seqLookup.findByState(seqState).next());
    assertTrue(assocLookup.findByState(assocState).next());

    assertEquals(1, testMgr.countObjects("ACC_Accession"));
    assertEquals(1, testMgr.countObjects("PRB_Source"));
    assertEquals(1, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(1, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();
    testMgr.cleanStage();
    createTriggers();
  }

  /**
   * event: update
   * number of sources: 1
   * number of secondary accids: 0
   * number of references: 0
   * source name: anonymous
   * clones cache: none
   * organism: mouse
   * number of repeats: 0
   * comments: the existing sequence record date is set to Dec 25, 1965 and
   *           the version changes from 1 to 2 which also triggers the
   *           existing sequence date change to the incoming sequence
   *           record date
   * @throws Exception
   */
  public void testUpdate1() throws Exception
  {
    testMgr.setConfig("INFILE_NAME", "test/single");
    assertEquals(0, testMgr.countObjects("ACC_Accession"));
    assertEquals(0, testMgr.countObjects("PRB_Source"));
    assertEquals(0, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(0, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();

    // data preperation
    ACC_AccessionState accState = new ACC_AccessionState();
    accState.setAccID("AB000096");
    accState.setPrefixPart("AB");
    accState.setObjectKey(new Integer(1));
    accState.setNumericPart(new Integer(96));
    accState.setLogicalDBKey(new Integer(9));
    accState.setMGITypeKey(new Integer(19));
    accState.setPrivateVal(new Boolean(false));
    accState.setPreferred(new Boolean(true));
    accState.setCreatedByKey(new Integer(1));
    accState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new ACC_AccessionDAO(accState));

    SEQ_Source_AssocState assocState = new SEQ_Source_AssocState();
    assocState.setSequenceKey(new Integer(1));
    assocState.setSourceKey(new Integer(1));
    assocState.setCreatedByKey(new Integer(1));
    assocState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new SEQ_Source_AssocDAO(assocState));

    PRB_SourceState srcState = new PRB_SourceState();
    srcState.setSegmentTypeKey(new Integer(83333));
    srcState.setVectorKey(new Integer(316369));
    srcState.setOrganismKey(new Integer(1));
    srcState.setStrainKey(new Integer(-1));
    srcState.setTissueKey(new Integer(6932));
    srcState.setGenderKey(new Integer(315167));
    srcState.setCellLineKey(new Integer(316335));
    srcState.setAge("Not Resolved");
    srcState.setAgeMin(new Float(-1.0));
    srcState.setAgeMax(new Float(-1.0));
    srcState.setIsCuratorEdited(new Boolean(false));
    testMgr.stageData(new PRB_SourceDAO(srcState));

    SEQ_SequenceState seqState = new SEQ_SequenceState();
    seqState.setSequenceTypeKey(new Integer(316346));
    seqState.setSequenceQualityKey(new Integer(316338));
    seqState.setSequenceStatusKey(new Integer(316342));
    seqState.setSequenceProviderKey(new Integer(316373));
    seqState.setLength(new Integer(3133));
    seqState.setDescription("Mus musculus mRNA for GATA-2 protein, " +
                            "complete cds.");
    seqState.setVersion("1");
    seqState.setDivision("ROD");
    seqState.setVirtual(new Boolean(false));
    seqState.setRawType("mRNA");
    seqState.setRawLibrary(null);
    seqState.setRawTissue("liver");
    seqState.setRawAge("dpc 14.5");
    seqState.setRawOrganism("Mus musculus");
    seqState.setNumberOfOrganisms(new Integer(0));
    Calendar seqRecDate = new GregorianCalendar(1965, Calendar.DECEMBER, 25);
    seqState.setSeqrecordDate(new Timestamp(seqRecDate.getTime().getTime()));
    seqState.setSequenceDate(new Timestamp(new Date().getTime()));
    testMgr.stageData(new SEQ_SequenceDAO(seqState));

    testMgr.commitStage();

    createTriggers();

    gBSeqloader.load();

    // prepare seqState for querying
    // this is what we expect the program to have done to the data
    seqState.setVersion("2");
    seqRecDate = new GregorianCalendar(2002, Calendar.JANUARY, 16);
    seqState.setSeqrecordDate(new Timestamp(seqRecDate.getTime().getTime()));
    seqState.setSequenceDate(new Timestamp(seqRecDate.getTime().getTime()));
    // do not query on creation date nor modification date
    seqState.setCreationDate(null);
    seqState.setModificationDate(null);

    // verify that this is what is in the database
    assertTrue(accLookup.findByState(accState).next());
    assertTrue(srcLookup.findByState(srcState).next());
    assertTrue(seqLookup.findByState(seqState).next());
    assertTrue(assocLookup.findByState(assocState).next());

    assertEquals(1, testMgr.countObjects("ACC_Accession"));
    assertEquals(1, testMgr.countObjects("PRB_Source"));
    assertEquals(1, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(1, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();
    testMgr.cleanStage();
    createTriggers();
  }

  /**
   * event: update
   * number of sources: 1
   * number of secondary accids: 0
   * number of references: 0
   * source name: anonymous
   * clones cache: full
   * organism: mouse
   * number of repeats: 0
   * comments: the existing sequence record date is set to Dec 25, 1965 and
   *           the sequence type changes from RNA to mRNA (not curator edited)
   *           which causes the existing type to change to the incoming type
   *           and the existing sequence record type changes to the incoming
   * @throws Exception
   */
  public void testUpdate2() throws Exception
  {
    testMgr.setConfig("INFILE_NAME", "test/single");
    assertEquals(0, testMgr.countObjects("ACC_Accession"));
    assertEquals(0, testMgr.countObjects("PRB_Source"));
    assertEquals(0, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(0, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();

    // data preperation
    ACC_AccessionState accState = new ACC_AccessionState();
    accState.setAccID("AB000096");
    accState.setPrefixPart("AB");
    accState.setObjectKey(new Integer(1));
    accState.setNumericPart(new Integer(96));
    accState.setLogicalDBKey(new Integer(9));
    accState.setMGITypeKey(new Integer(19));
    accState.setPrivateVal(new Boolean(false));
    accState.setPreferred(new Boolean(true));
    accState.setCreatedByKey(new Integer(1));
    accState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new ACC_AccessionDAO(accState));

    PRB_SourceState srcState = new PRB_SourceState();
    srcState.setSegmentTypeKey(new Integer(83333));
    srcState.setVectorKey(new Integer(316369));
    srcState.setOrganismKey(new Integer(1));
    srcState.setStrainKey(new Integer(-1));
    srcState.setTissueKey(new Integer(6932));
    srcState.setGenderKey(new Integer(315167));
    srcState.setCellLineKey(new Integer(316335));
    srcState.setAge("Not Resolved");
    srcState.setAgeMin(new Float(-1.0));
    srcState.setAgeMax(new Float(-1.0));
    srcState.setIsCuratorEdited(new Boolean(false));
    testMgr.stageData(new PRB_SourceDAO(srcState));

    SEQ_SequenceState seqState = new SEQ_SequenceState();
    seqState.setSequenceTypeKey(new Integer(316346));
    seqState.setSequenceQualityKey(new Integer(316338));
    seqState.setSequenceStatusKey(new Integer(316342));
    seqState.setSequenceProviderKey(new Integer(316373));
    seqState.setLength(new Integer(3133));
    seqState.setDescription("Mus musculus mRNA for GATA-2 protein, " +
                            "complete cds.");
    seqState.setVersion("2");
    seqState.setDivision("ROD");
    seqState.setVirtual(new Boolean(false));
    seqState.setRawType("DNA");
    seqState.setRawLibrary(null);
    seqState.setRawTissue("liver");
    seqState.setRawAge("dpc 14.5");
    seqState.setRawOrganism("Mus musculus");
    seqState.setNumberOfOrganisms(new Integer(0));
    Calendar seqRecDate = new GregorianCalendar(1965, Calendar.DECEMBER, 25);
    seqState.setSeqrecordDate(new Timestamp(seqRecDate.getTime().getTime()));
    seqState.setSequenceDate(new Timestamp(new Date().getTime()));
    testMgr.stageData(new SEQ_SequenceDAO(seqState));

    SEQ_Source_AssocState assocState = new SEQ_Source_AssocState();
    assocState.setSequenceKey(new Integer(1));
    assocState.setSourceKey(new Integer(1));
    assocState.setCreatedByKey(new Integer(1));
    assocState.setModifiedByKey(new Integer(1));
    testMgr.stageData(new SEQ_Source_AssocDAO(assocState));

    testMgr.commitStage();

    createTriggers();

    gBSeqloader.load();

    // prepare seqState for querying
    // this is what we expect the program to have done to the data
    seqState.setRawType("mRNA");
    seqState.setSequenceTypeKey(new Integer(316346));
    seqRecDate = new GregorianCalendar(2002, Calendar.JANUARY, 16);
    seqState.setSeqrecordDate(new Timestamp(seqRecDate.getTime().getTime()));
    // do not query of sequence date
    seqState.setSequenceDate(null);
    // do not query on creation date nor modification date
    seqState.setCreationDate(null);
    seqState.setModificationDate(null);

    assertTrue(accLookup.findByState(accState).next());
    assertTrue(srcLookup.findByState(srcState).next());
    assertTrue(seqLookup.findByState(seqState).next());
    assertTrue(assocLookup.findByState(assocState).next());

    // should there be a record here since we updated the sequence type?
    assertEquals(1, testMgr.countObjects("ACC_Accession"));
    assertEquals(1, testMgr.countObjects("PRB_Source"));
    assertEquals(1, testMgr.countObjects("SEQ_Source_Assoc"));
    assertEquals(1, testMgr.countObjects("SEQ_Sequence"));

    dropTriggers();
    testMgr.cleanStage();
    createTriggers();
  }


  private void deleteFiles()
  {
    FileUtility.delete("dataLoad.cur.log");
    FileUtility.delete("dataLoad.cur.log.lck");
    FileUtility.delete("dataLoad.diag.log");
    FileUtility.delete("dataLoad.diag.log.lck");
    FileUtility.delete("dataLoad.proc.log");
    FileUtility.delete("dataLoad.proc.log.lck");
    FileUtility.delete("dataLoad.val.log");
    FileUtility.delete("dataLoad.val.log.lck");
    FileUtility.delete("repeats.out");
    FileUtility.delete("ACC_Accession.bcp");
    FileUtility.delete("SEQ_Sequence.bcp");
    FileUtility.delete("SEQ_Source_Assoc.bcp");
    FileUtility.delete("PRB_Source.bcp");
    FileUtility.delete("updates.sql");
    FileUtility.delete("updates.out");
  }

  private void dropTriggers() throws Exception
  {
    schema.dropTriggers("ACC_Accession");
    schema.dropTriggers("SEQ_Source_Assoc");
    schema.dropTriggers("SEQ_Sequence");
    schema.dropTriggers("PRB_Source");
  }

  private void createTriggers() throws Exception
  {
    schema.createTriggers("ACC_Accession");
    schema.createTriggers("SEQ_Source_Assoc");
    schema.createTriggers("SEQ_Sequence");
    schema.createTriggers("PRB_Source");
  }


}
