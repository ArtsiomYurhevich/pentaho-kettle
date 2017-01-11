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

package org.pentaho.di.trans.steps.httppost;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;

import com.google.common.io.ByteStreams;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.any23.encoding.TikaEncodingDetector;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaInteger;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.core.util.Assert;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.mock.StepMockHelper;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * User: Dzmitry Stsiapanau Date: 12/2/13 Time: 4:35 PM
 */
public class HTTPPOSTIT {
  class HTTPPOSTHandler extends HTTPPOST {

    Object[] row = new Object[] { "anyData" };
    Object[] outputRow;
    boolean  override;

    public HTTPPOSTHandler( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
      Trans trans, boolean override ) {
      super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
      this.override = override;
    }


    /**
     * In case of getRow, we receive data from previous steps through the input rowset. In case we split the stream, we
     * have to copy the data to the alternate splits: rowsets 1 through n.
     */
    @Override
    public Object[] getRow() throws KettleException {
      return row;
    }

    /**
     * putRow is used to copy a row, to the alternate rowset(s) This should get priority over everything else!
     * (synchronized) If distribute is true, a row is copied only once to the output rowsets, otherwise copies are sent
     * to each rowset!
     *
     * @param row
     *          The row to put to the destination rowset(s).
     * @throws org.pentaho.di.core.exception.KettleStepException
     *
     */
    @Override
    public void putRow( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
      outputRow = row;
    }

    public Object[] getOutputRow() {
      return outputRow;
    }


    @Override
    protected int requestStatusCode( PostMethod post, HostConfiguration hostConfiguration, HttpClient httpPostClient )
            throws IOException {
      if ( override ) {
        return 402;
      } else {
        return super.requestStatusCode( post, hostConfiguration, httpPostClient );
      }

    }

    @Override
    protected InputStreamReader openStream( String encoding, PostMethod post ) throws Exception {
      if ( override ) {
        InputStreamReader mockInputStreamReader = Mockito.mock( InputStreamReader.class );
        when( mockInputStreamReader.read() ).thenReturn( -1 );
        return mockInputStreamReader;
      } else {
        return super.openStream( encoding, post );
      }
    }

    @Override
    protected Header[] searchForHeaders( PostMethod post ) {
      Header[] headers = { new Header( "host", host ) };
      if ( override ) {
        return headers;
      } else {
        return super.searchForHeaders( post );
      }
    }
  }

  public static final String host = "localhost";
  public static final int port = 9998;
  public static final String HTTP_LOCALHOST_9998 = "http://localhost:9998/";
  @InjectMocks
  private StepMockHelper<HTTPPOSTMeta, HTTPPOSTData> stepMockHelper;
  private HttpServer httpServer;

  @BeforeClass
  public static void setupBeforeClass() throws KettleException {
    KettleClientEnvironment.init();
  }

  @Before
  public void setUp() throws Exception {
    stepMockHelper =
      new StepMockHelper<HTTPPOSTMeta, HTTPPOSTData>( "HTTPPOST CLIENT TEST",
        HTTPPOSTMeta.class, HTTPPOSTData.class );
    when( stepMockHelper.logChannelInterfaceFactory.create( any(), any( LoggingObjectInterface.class ) ) ).thenReturn(
      stepMockHelper.logChannelInterface );
    when( stepMockHelper.trans.isRunning() ).thenReturn( true );
    verify( stepMockHelper.trans, never() ).stopAll();
  }

  @After
  public void tearDown() throws Exception {
    httpServer.stop( 5 );

  }

  @Test
  public void test204Answer() throws Exception {
    startHttpServer( get204AnswerHandler() );
    HTTPPOSTData data = new HTTPPOSTData();
    int[] index = { 0, 1 };
    RowMeta meta = new RowMeta();
    meta.addValueMeta( new ValueMetaString( "fieldName" ) );
    meta.addValueMeta( new ValueMetaInteger( "codeFieldName" ) );
    Object[] expectedRow = new Object[] { "", 204L };
    HTTPPOST HTTPPOST = new HTTPPOSTHandler(
            stepMockHelper.stepMeta, data, 0, stepMockHelper.transMeta, stepMockHelper.trans, false );
    RowMetaInterface inputRowMeta = mock( RowMetaInterface.class );
    HTTPPOST.setInputRowMeta( inputRowMeta );
    when( inputRowMeta.clone() ).thenReturn( inputRowMeta );
    when( stepMockHelper.processRowsStepMetaInterface.getUrl() ).thenReturn( HTTP_LOCALHOST_9998 );
    when( stepMockHelper.processRowsStepMetaInterface.getQueryField() ).thenReturn( new String[] {} );
    when( stepMockHelper.processRowsStepMetaInterface.getArgumentField() ).thenReturn( new String[] {} );
    when( stepMockHelper.processRowsStepMetaInterface.getResultCodeFieldName() ).thenReturn( "ResultCodeFieldName" );
    when( stepMockHelper.processRowsStepMetaInterface.getFieldName() ).thenReturn( "ResultFieldName" );
    HTTPPOST.init( stepMockHelper.processRowsStepMetaInterface, data );
    Assert.assertTrue( HTTPPOST.processRow( stepMockHelper.processRowsStepMetaInterface, data ) );
    Object[] out = ( (HTTPPOSTHandler) HTTPPOST ).getOutputRow();
    Assert.assertTrue( meta.equals( out, expectedRow, index ) );
  }

  @Test
  public void testResponseHeader() throws Exception {
    startHttpServer( get204AnswerHandler() );
    HTTPPOSTData data = new HTTPPOSTData();
    int[] index = { 0, 1, 2 };
    RowMeta meta = new RowMeta();
    meta.addValueMeta( new ValueMetaString( "fieldName" ) );
    meta.addValueMeta( new ValueMetaInteger( "codeFieldName" ) );
    meta.addValueMeta( new ValueMetaString( "headerFieldName" ) );
    Object[] expectedRow =
            new Object[] { "", 402L, "{\"host\":\"localhost\"}" };
    HTTPPOST HTTPPOST = new HTTPPOSTHandler(
            stepMockHelper.stepMeta, data, 0, stepMockHelper.transMeta, stepMockHelper.trans, true );
    RowMetaInterface inputRowMeta = mock( RowMetaInterface.class );
    HTTPPOST.setInputRowMeta( inputRowMeta );
    when( inputRowMeta.clone() ).thenReturn( inputRowMeta );
    when( stepMockHelper.processRowsStepMetaInterface.getUrl() ).thenReturn( HTTP_LOCALHOST_9998 );
    when( stepMockHelper.processRowsStepMetaInterface.getQueryField() ).thenReturn( new String[] {} );
    when( stepMockHelper.processRowsStepMetaInterface.getArgumentField() ).thenReturn( new String[] {} );
    when( stepMockHelper.processRowsStepMetaInterface.getResultCodeFieldName() ).thenReturn( "ResultCodeFieldName" );
    when( stepMockHelper.processRowsStepMetaInterface.getFieldName() ).thenReturn( "ResultFieldName" );
    when( stepMockHelper.processRowsStepMetaInterface.getEncoding() ).thenReturn( "UTF-8" );
    when( stepMockHelper.processRowsStepMetaInterface.getResponseHeaderFieldName() ).thenReturn(
            "ResponseHeaderFieldName" );
    HTTPPOST.init( stepMockHelper.processRowsStepMetaInterface, data );
    Assert.assertTrue( HTTPPOST.processRow( stepMockHelper.processRowsStepMetaInterface, data ) );
    Object[] out = ( (HTTPPOSTHandler) HTTPPOST ).getOutputRow();
    Assert.assertTrue( meta.equals( out, expectedRow, index ) );
  }

  @Test
  public void testContentTypeEncodingWasSet() throws Exception {
    startHttpServer( getEncodingCheckingHandler( "UTF-8") );
    HTTPPOSTData data = new HTTPPOSTData();
    RowMeta meta = new RowMeta();
    meta.addValueMeta( new ValueMetaString( "fieldName" ) );
    HTTPPOSTHandler httpPost = new HTTPPOSTHandler(
      stepMockHelper.stepMeta, data, 0, stepMockHelper.transMeta, stepMockHelper.trans, false );
    RowMetaInterface inputRowMeta = mock( RowMetaInterface.class );
    String testString = "test string тест рус ";
    httpPost.setInputRowMeta( inputRowMeta );
    httpPost.row = new Object[] { "test string простите " };
    when( inputRowMeta.clone() ).thenReturn( inputRowMeta );
    when( stepMockHelper.processRowsStepMetaInterface.getUrl() ).thenReturn( HTTP_LOCALHOST_9998 );
    when( stepMockHelper.processRowsStepMetaInterface.getQueryField() ).thenReturn( new String[] {} );
    when( stepMockHelper.processRowsStepMetaInterface.getArgumentField() ).thenReturn( new String[] { "testBodyField" } );
    when( stepMockHelper.processRowsStepMetaInterface.getArgumentParameter() ).thenReturn( new String[] { "testBodyParam" } );
    when( stepMockHelper.processRowsStepMetaInterface.getArgumentHeader() ).thenReturn( new boolean[] { false } );
    when( stepMockHelper.processRowsStepMetaInterface.getFieldName() ).thenReturn( "ResultFieldName" );
    when( stepMockHelper.processRowsStepMetaInterface.getEncoding() ).thenReturn( "UTF-16" );
    when( inputRowMeta.getString( httpPost.row, 0 ) ).thenReturn( testString );
    httpPost.init( stepMockHelper.processRowsStepMetaInterface, data );
    Assert.assertTrue( httpPost.processRow( stepMockHelper.processRowsStepMetaInterface, data ) );
  }

  private void startHttpServer( HttpHandler httpHandler ) throws IOException {
    httpServer = HttpServer.create( new InetSocketAddress( HTTPPOSTIT.host, HTTPPOSTIT.port ), 10 );
    httpServer.createContext( "/", httpHandler );
    httpServer.start();
  }

  private HttpHandler get204AnswerHandler() {
    return httpExchange -> {
      httpExchange.sendResponseHeaders( 204, 0 );
      httpExchange.close();
    };
  }

  private HttpHandler getEncodingCheckingHandler( String expectedEncoding ) {
    return httpExchange -> {
      Assert.assertTrue( checkEncoding( expectedEncoding, httpExchange.getRequestBody() ) );
      httpExchange.sendResponseHeaders( 200, 0 );
      httpExchange.close();
    };
  }

  private boolean checkEncoding( String expectedEncoding, InputStream inputStream )  {
  try {

     byte[] receivedBytes = ByteStreams.toByteArray( inputStream );

    String str1 = new String( receivedBytes, "UTF-8");
    String str2 = new String( receivedBytes, "UTF-16");
    String str3 = new String( receivedBytes, "ISO-8859-2");
    System.out.println( guessCharset( new ByteArrayInputStream( receivedBytes )));
     System.out.println( str1 );
     System.out.println( str2 );
     System.out.println( str3 );
    System.out.println("----------------");
     System.out.println( URLDecoder.decode( str1, "UTF-8") );
     System.out.println( URLDecoder.decode( str1, "UTF-16") );
     System.out.println( URLDecoder.decode( str1, "ISO-8859-2") );
    System.out.println("----------------");
    System.out.println( URLDecoder.decode( str2, "UTF-8") );
    System.out.println( URLDecoder.decode( str2, "UTF-16") );
    System.out.println( URLDecoder.decode( str2, "ISO-8859-2") );
    System.out.println("----------------");
    System.out.println( URLDecoder.decode( str3, "UTF-8") );
    System.out.println( URLDecoder.decode( str3, "UTF-16") );
    System.out.println( URLDecoder.decode( str3, "ISO-8859-2") );

   } catch ( IOException e ) {
     fail( "IO Error" );
   }

   return true;
  }

  public static Charset guessCharset( InputStream is) throws IOException {
    return Charset.forName( new TikaEncodingDetector().guessEncoding( is ));
  }
}
