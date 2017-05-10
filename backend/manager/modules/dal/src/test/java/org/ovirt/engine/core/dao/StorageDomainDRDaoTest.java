package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.StorageDomainDR;

public class StorageDomainDRDaoTest extends BaseDaoTestCase {

    private StorageDomainDRDao dao;
    private StorageDomainDR storageDomainDR;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        dao = dbFacade.getStorageDomainDRDao();

        storageDomainDR = new StorageDomainDR();
        storageDomainDR.setStorageDomainId(FixturesTool.POSIX_STORAGE_DOMAIN_ID);
        storageDomainDR.setGeoRepSessionId(FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        storageDomainDR.setScheduleCronExpression("0 30 22 * * ? *");
        storageDomainDR.setJobId("qrtzjob1");
    }

    @Test
    public void testGetStorageDomainDR() {
        StorageDomainDR result = dao.get(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(storageDomainDR , result);
    }

    @Test
    public void testSaveorUpdate() {
        storageDomainDR.setScheduleCronExpression(null);
        dao.saveOrUpdate(storageDomainDR);

        StorageDomainDR result = dao.get(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(storageDomainDR , result);
    }

    @Test
    public void testGetStorageDomainDRs() {
        List<StorageDomainDR> result = dao.getAllForStorageDomain(FixturesTool.POSIX_STORAGE_DOMAIN_ID);
        assertEquals(1, result.size());
    }

    @Test
    public void testGetStorageDomainDRWithGeoRep() {
        List<StorageDomainDR> result = dao.getWithGeoRepSession(FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(1, result.size());
    }

    @Test
    public void testUpdate() {
        storageDomainDR.setJobId("qrtzjob2");
        dao.update(storageDomainDR);
        StorageDomainDR result = dao.get(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(storageDomainDR, result);
    }

    @Test
    public void testRemoveAndSave() {
        dao.remove(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        StorageDomainDR result = dao.get(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(null, result);
        dao.save(storageDomainDR);
        result = dao.get(FixturesTool.POSIX_STORAGE_DOMAIN_ID, FixturesTool.GLUSTER_GEOREP_SESSION_ID2);
        assertEquals(storageDomainDR, result);
    }
}
