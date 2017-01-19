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

package org.pentaho.di.trans.steps.syslog;

import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.mock.StepMockHelper;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SyslogMessageConcurrentTest {

  private StepMockHelper<SyslogMessageMeta, SyslogMessageData> stepMockHelper;

    @Before
    public void setUp() throws Exception {
      stepMockHelper =
                new StepMockHelper<SyslogMessageMeta, SyslogMessageData>( "SYSLOG_MESSAGE TEST", SyslogMessageMeta.class,
                        SyslogMessageData.class );
      when( stepMockHelper.logChannelInterfaceFactory.create( any(), any( LoggingObjectInterface.class ) ) ).thenReturn(
                stepMockHelper.logChannelInterface );

    }

    @Test
    public void concurrentSyslogTasks() throws Exception {
      SyslogMessage syslogMessage = new SyslogMessageTask( stepMockHelper.stepMeta, stepMockHelper.stepDataInterface, 0, stepMockHelper.transMeta,
                        stepMockHelper.trans );
        SyslogMessageData data = new SyslogMessageData();
        when( stepMockHelper.processRowsStepMetaInterface.getServerName() ).thenReturn( "localhost" );
        when( stepMockHelper.processRowsStepMetaInterface.getMessageFieldName() ).thenReturn( "message field" );
        when( stepMockHelper.processRowsStepMetaInterface.getPort() ).thenReturn( "9988" );
        when( stepMockHelper.processRowsStepMetaInterface.getPriority() ).thenReturn( "ERROR" );
        syslogMessage.init( stepMockHelper.processRowsStepMetaInterface, data );
        RowMetaInterface inputRowMeta = mock( RowMetaInterface.class );
        when( inputRowMeta.indexOfValue( any() ) ).thenReturn( 0 );
        when( inputRowMeta.getString( any(), eq( 0 ) ) ).thenReturn( "message value" );
        syslogMessage.setInputRowMeta( inputRowMeta );
        syslogMessage.processRow( stepMockHelper.processRowsStepMetaInterface, data );



    }


    private class SyslogMessageTask extends SyslogMessage implements Runnable {

        public SyslogMessageTask( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta, Trans trans) {
            super( stepMeta, stepDataInterface, copyNr, transMeta, trans);
        }

        @Override
        public void run() {

        }

        @Override
        public Object[] getRow() throws KettleException {
            return new Object[]{ "message" };
        }
    }

}
