/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2002-2016 by Pentaho : http://www.pentaho.com
 *
 *******************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package org.pentaho.di.trans.steps.joinrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.owasp.encoder.Encode;
import org.pentaho.di.core.BlockingRowSet;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.SingleRowRowSet;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaBoolean;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.RowStepCollector;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.RowListener;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.StepMetaInterface;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Denis Mashukov
 */
//@RunWith( PowerMockRunner.class )
public class JoinRowsTest {

  private StepMetaInterface meta;
  private JoinRowsData data;

  @Before
  public void setUp() throws Exception {
    meta = new JoinRowsMeta();
    data = new JoinRowsData();
  }

  @After
  public void tearDown() {
    meta = null;
    data = null;
  }

  /**
   * BACKLOG-8520 Check that method call does't throw an error NullPointerException.
   */
  @Test
  public void checkThatMethodPerformedWithoutError() throws Exception {
    getJoinRows().dispose( meta, data );
  }

  @Test
  public void disposeDataFiles() throws Exception {
    File mockFile1 = Mockito.mock( File.class );
    File mockFile2 = Mockito.mock( File.class );
    data.file = new File[]{ null, mockFile1, mockFile2 };
    getJoinRows().dispose( meta, data );
    verify( mockFile1, times( 1 ) ).delete();
    verify( mockFile2, times( 1 ) ).delete();
  }

  private JoinRows getJoinRows() throws Exception {
    StepMeta stepMeta = new StepMeta();
    TransMeta transMeta = new TransMeta();
    Trans trans = new Trans( transMeta );

    transMeta.clear();
    transMeta.addStep( stepMeta );
    transMeta.setStep( 0, stepMeta );
    stepMeta.setName( "test" );
    trans.setLog( Mockito.mock( LogChannelInterface.class ) );
    trans.prepareExecution( null );
    trans.startThreads();

    return new JoinRows( stepMeta, null, 0, transMeta, trans );
  }

  @Test
//  @PrepareForTest( { File.class } )
  public void testGetRowData() throws Exception {
    JoinRowsMeta joinRowsMeta = new JoinRowsMeta();
    joinRowsMeta.setMainStepname( "main step name" );
    joinRowsMeta.setPrefix( "out" );

    JoinRowsData joinRowsData = new JoinRowsData();


    StepMeta stepMeta = mock( StepMeta.class );
    TransMeta transMeta = mock( TransMeta.class );
    Trans trans = mock( Trans.class );
//    PowerMockito.mockStatic( File.class );
//    File file  = createTempFile();
//    when( File.createTempFile( eq( joinRowsMeta.getPrefix() ), eq( ".tmp" ), anyObject() ) ).thenReturn( createTempFile());


//    JoinRows joinRows = new JoinRows( stepMeta, joinRowsData, 0, transMeta, trans );
    JoinRows joinRows = getJoinRows();
    joinRows.init( joinRowsMeta, joinRowsData );


    List<RowSet> rowSets = new ArrayList<>();
    rowSets.add( getRowSetWithData( 5, "main --", true ) );
    rowSets.add( getRowSetWithData( 5, "secondary --", false ) );
    joinRows.setInputRowSets( rowSets );

    RowStepCollector rowStepCollector = new RowStepCollector();
    joinRows.addRowListener( rowStepCollector );

    joinRows.processRow( joinRowsMeta, joinRowsData );


  }


  BlockingRowSet getRowSetWithData( int size, String dataPrefix, boolean isMainStep ) {
    BlockingRowSet blockingRowSet = new BlockingRowSet( size );
    RowMeta rowMeta = new RowMeta();
    ValueMetaInterface valueMetaString = new ValueMetaString( dataPrefix + " first value name" );
    ValueMetaInterface valueMetaInteger = new ValueMetaString( dataPrefix + " second value name" );
    ValueMetaInterface valueMetaBoolean = new ValueMetaString( dataPrefix + " third value name" );
    rowMeta.addValueMeta( valueMetaString );
    rowMeta.addValueMeta( valueMetaInteger );
    rowMeta.addValueMeta( valueMetaBoolean );
    blockingRowSet.setRowMeta( rowMeta );
    for( int i = 0; i < size; i++ ) {
      Object[] rowData =  new Object[ 3 ];
      rowData[0] = dataPrefix + i;
      rowData[1] = dataPrefix + i;
      rowData[2] = dataPrefix + i;
      blockingRowSet.putRow( rowMeta, rowData );
    }

    if ( isMainStep ) {
      blockingRowSet.setThreadNameFromToCopy( "main step name", 0, null, 0 );
    } else {
      blockingRowSet.setThreadNameFromToCopy( "secondary step name", 0, null, 0 );
    }
    return blockingRowSet;
  }

  File createTempFile() throws Exception {
    return Files.createTempFile( "PDI_tmp", ".tmp" ).toFile();
  }
}
