/*! ******************************************************************************
 *
 * Pentaho Data Integration
 *
 * Copyright (C) 2017-2017 by Pentaho : http://www.pentaho.com
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
package org.pentaho.di.www;


import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.pentaho.di.core.logging.LogChannelInterface;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.trans.TransMeta;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Collections;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class RunTransServletTest {

  @Mock
  HttpServletRequest mockRequest;
  @Mock
  HttpServletResponse mockResponse;
  @Mock
  LogChannelInterface logChannelInterface;
  @Mock
  TransformationMap mockTransformationMap;
  @Mock
  SlaveServerConfig slaveServerConfig;
  @Mock
  Repository repository;
  @Mock
  RepositoryDirectoryInterface repositoryDirectoryInterface;
  @Mock
  ObjectId transformationId;
  @Mock
  TransMeta transMeta;

  String transName = "/home/test/testTransName";


  RunTransServlet runTransServlet;

  @Before
  public void setup() throws Exception {
    runTransServlet = new RunTransServlet( mockTransformationMap );
    when( mockRequest.getContextPath() ).thenReturn( RunTransServlet.CONTEXT_PATH );
    when( mockRequest.getParameter( eq( "trans" ) ) ).thenReturn( transName );
    runTransServlet.log = logChannelInterface;
    when( mockTransformationMap.getSlaveServerConfig() ).thenReturn( slaveServerConfig );
    when( slaveServerConfig.getRepository() ).thenReturn( repository );
    when( repository.loadRepositoryDirectoryTree() ).thenReturn( repositoryDirectoryInterface );
    when( repositoryDirectoryInterface.findDirectory( eq( "/home/test") ) ).thenReturn( repositoryDirectoryInterface );
    when( repository.getTransformationID( eq( "testTransName" ), any( RepositoryDirectoryInterface.class ) ) ).thenReturn( transformationId );
    when( repository.loadTransformation( transformationId, null ) ).thenReturn( transMeta );
    when( mockRequest.getParameterNames() ).thenReturn( Collections.emptyEnumeration() );
    when( transMeta.listVariables() ).thenReturn( new String[]{} );
    when( transMeta.listParameters() ).thenReturn( new String[]{} );

  }

  @Test
  public void testGet() throws Exception {
  runTransServlet.doGet( mockRequest, mockResponse );
  }

}
