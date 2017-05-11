package org.ovirt.engine.core.bll.memory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.ovirt.engine.core.common.businessentities.StorageDomain;
import org.ovirt.engine.core.common.businessentities.storage.DiskImage;
import org.ovirt.engine.core.compat.Guid;

@RunWith(MockitoJUnitRunner.class)
public class MemoryStorageHandlerTest {

    private StorageDomain validStorageDomain1;
    private StorageDomain validStorageDomain2;
    private StorageDomain validStorageDomain3;
    private StorageDomain invalidStorageDomain1;
    private StorageDomain invalidStorageDomain2;
    private StorageDomain invalidStorageDomain3;
    private List<DiskImage> memoryDisks;
    private List<DiskImage> vmDisks;

    @Spy
    private MemoryStorageHandler memoryStorageHandler;

    @Before
    public void setUp() {
        memoryDisks = new LinkedList<>();
        vmDisks = new LinkedList<>();
        initStorageDomains();
        initFilters();
        initComparators();
    }

    @Test
    public void filterAllDomainsExceptForTheFirstOne() {
        filterAllStorageDomainsExceptOne(Arrays.asList(
                validStorageDomain1, invalidStorageDomain1, invalidStorageDomain2), validStorageDomain1);
    }

    @Test
    public void filterAllDomainsExceptForTheSecondOne() {
        filterAllStorageDomainsExceptOne(Arrays.asList(
                invalidStorageDomain1, validStorageDomain1, invalidStorageDomain2), validStorageDomain1);
    }

    @Test
    public void filterAllDomainsExceptForTheThirdOne() {
        filterAllStorageDomainsExceptOne(Arrays.asList(
                invalidStorageDomain1, invalidStorageDomain2, validStorageDomain1), validStorageDomain1);
    }

    @Test
    public void filterAllDomains() {
        List<StorageDomain> filteredStorageDomains = memoryStorageHandler.filterStorageDomains(
                Arrays.asList(invalidStorageDomain1, invalidStorageDomain2, invalidStorageDomain3), memoryDisks);
        assertTrue(filteredStorageDomains.isEmpty());
    }

    @Test
    public void sortStorageDomainsWithSingleComparator() {
        sortStorageDomains(Arrays.asList(validStorageDomain1, validStorageDomain3),
                Arrays.asList(validStorageDomain3, validStorageDomain1));
    }

    @Test
    public void sortStorageDomainsWithWithMultipleComparators() {
        sortStorageDomains(Arrays.asList(validStorageDomain1, validStorageDomain2, validStorageDomain3),
                Arrays.asList(validStorageDomain3, validStorageDomain1, validStorageDomain2));
    }

    @Test
    public void testFindStorageDomainForMemoryWithSingleDomain() {
        verifyDomainForMemory(Collections.singletonList(validStorageDomain1), validStorageDomain1);
    }

    @Test
    public void testFindStorageDomainForMemory() {
        verifyDomainForMemory(
                Arrays.asList(invalidStorageDomain1, validStorageDomain1, invalidStorageDomain2,
                        validStorageDomain2, invalidStorageDomain3, validStorageDomain3), validStorageDomain3);
    }

    @Test
    public void testFindStorageDomainForMemoryWithEmptyDomainsList() {
        verifyNoDomainForMemory(Collections.emptyList());
    }

    @Test
    public void testFindStorageDomainForMemoryWithSingleInvalidDomain() {
        verifyNoDomainForMemory(Collections.singletonList(invalidStorageDomain1));
    }

    private void initStorageDomains() {
        validStorageDomain1 = initStorageDomain();
        validStorageDomain2 = initStorageDomain();
        validStorageDomain3 = initStorageDomain();
        invalidStorageDomain1 = initStorageDomain();
        invalidStorageDomain2 = initStorageDomain();
        invalidStorageDomain3 = initStorageDomain();
    }

    private void initFilters() {
        List<Predicate<StorageDomain>> storageDomainFilters = Arrays.asList(
                d -> !d.equals(invalidStorageDomain1),
                d -> !d.equals(invalidStorageDomain2),
                d -> !d.equals(invalidStorageDomain3));
        doReturn(storageDomainFilters).when(memoryStorageHandler).getStorageDomainFilters(memoryDisks);
    }

    private void initComparators() {
        List<Comparator<StorageDomain>> comparators = Arrays.asList(
                Comparator.comparing((StorageDomain sd) -> sd.equals(validStorageDomain2)),
                Comparator.comparing((StorageDomain sd) -> sd.equals(validStorageDomain3)).reversed());
        doReturn(comparators).when(memoryStorageHandler).getStorageDomainComparators(anyList());
    }

    private StorageDomain initStorageDomain() {
        StorageDomain storageDomain = new StorageDomain();
        storageDomain.setId(Guid.newGuid());
        return storageDomain;
    }

    private void filterAllStorageDomainsExceptOne(List<StorageDomain> storageDomains,
            StorageDomain expectedStorageDomain) {
        List<StorageDomain> filteredStorageDomains = new ArrayList<>(
                memoryStorageHandler.filterStorageDomains(storageDomains, memoryDisks));
        assertEquals(filteredStorageDomains, Collections.singletonList(expectedStorageDomain));
    }

    private void verifyDomainForMemory(List<StorageDomain> storageDomains, StorageDomain expectedStorageDomain) {
        StorageDomain storageDomain =
                memoryStorageHandler.findStorageDomainForMemory(storageDomains, memoryDisks, vmDisks);
        assertEquals(expectedStorageDomain, storageDomain);
    }

    private void verifyNoDomainForMemory(List<StorageDomain> storageDomains) {
        StorageDomain storageDomain =
                memoryStorageHandler.findStorageDomainForMemory(storageDomains, memoryDisks, vmDisks);
        assertNull(storageDomain);
    }

    private void sortStorageDomains(List<StorageDomain> domainsInPool, List<StorageDomain> expectedSortedList) {
        memoryStorageHandler.sortStorageDomains(domainsInPool, vmDisks);
        assertEquals(domainsInPool, expectedSortedList);
    }
}
