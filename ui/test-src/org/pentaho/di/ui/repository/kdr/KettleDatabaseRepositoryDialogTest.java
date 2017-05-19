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
package org.pentaho.di.ui.repository.kdr;


import org.eclipse.swt.widgets.Shell;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.repository.RepositoriesMeta;

import org.mockito.Mockito;
import org.pentaho.di.repository.kdr.KettleDatabaseRepositoryMeta;

public class KettleDatabaseRepositoryDialogTest {

  Shell shell;

  @Before
  public void setup() throws Exception {
    shell = Mockito.mock( Shell.class );
  }

  @Test
  public void testIsDatabaseWithNameExist() throws Exception {
    RepositoriesMeta repositories = new RepositoriesMeta();
    DatabaseMeta databaseMeta1 = new DatabaseMeta();
    databaseMeta1.setName( "TestDB1" );
    repositories.addDatabase( databaseMeta1 );
    DatabaseMeta databaseMeta2 = new DatabaseMeta();
    databaseMeta2.setName( "TestDB2" );
    repositories.addDatabase( databaseMeta2 );

    KettleDatabaseRepositoryDialog dialog = new KettleDatabaseRepositoryDialog( shell, 0, new KettleDatabaseRepositoryMeta(), repositories );

    Mockito.when( repositories.nrDatabases() ).thenReturn( 2 );
    Mockito.when( repositories.getDatabase( 0 ) ).thenReturn( databaseMeta1 );
    Mockito.when( repositories.getDatabase( 1 ) ).thenReturn( databaseMeta2 );


    //existing databases
    Assert.assertFalse( dialog.isDatabaseWithNameExist( databaseMeta1, false ) );
    databaseMeta2.setName( "TestDB1" );
    Assert.assertTrue( dialog.isDatabaseWithNameExist( databaseMeta2, false ) );

    //new databases
    DatabaseMeta databaseMeta3 = new DatabaseMeta();
    databaseMeta3.setName( "TestDB3" );
    Assert.assertFalse( dialog.isDatabaseWithNameExist( databaseMeta3, true ) );
    databaseMeta3.setName( "TestDB1" );
    Assert.assertTrue( dialog.isDatabaseWithNameExist( databaseMeta3, true ) );
  }

}
