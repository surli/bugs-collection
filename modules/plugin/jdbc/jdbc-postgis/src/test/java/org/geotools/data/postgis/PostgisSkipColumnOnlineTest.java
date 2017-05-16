package org.geotools.data.postgis;

import org.geotools.jdbc.JDBCSkipColumnOnlineTest;
import org.geotools.jdbc.JDBCSkipColumnTestSetup;

/**
 * 
 *
 * @source $URL$
 */
public class PostgisSkipColumnOnlineTest extends JDBCSkipColumnOnlineTest {

    @Override
    protected JDBCSkipColumnTestSetup createTestSetup() {
        return new PostgisSkipColumnTestSetup();
    }

}
