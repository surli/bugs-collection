package org.ovirt.engine.core.bll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.ovirt.engine.core.bll.context.CommandContext;
import org.ovirt.engine.core.common.action.VdcActionParametersBase;
import org.ovirt.engine.core.common.businessentities.aaa.DbUser;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogDirector;
import org.ovirt.engine.core.utils.CorrelationIdTracker;
import org.ovirt.engine.core.utils.MockConfigRule;

/** A test case for {@link CommandBase} */
public class CommandBaseTest extends BaseCommandTest {
    @ClassRule
    public static MockConfigRule mcr = new MockConfigRule();

    protected String session = "someSession";

    @Mock
    private AuditLogDirector director;

    @InjectMocks
    private CommandBase<VdcActionParametersBase> command = mock(
            CommandBase.class,
            withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS)
                    .extraInterfaces(RenamedEntityInfoProvider.class)
                    .useConstructor(new VdcActionParametersBase(), CommandContext.createContext(session)));

    @Before
    public void setupEnvironment() {
        CorrelationIdTracker.clean();
        DbUser user = mock(DbUser.class);

        when(engineSessionDao.remove(anyLong())).thenReturn(1);

        sessionDataContainer.setUser(session, user);
    }

    @After
    public void clearEnvironment() {
        CorrelationIdTracker.clean();
        sessionDataContainer.removeSessionOnLogout(session);
    }

    /** Testing the constructor, which adds the user id to the thread local container */
    @Test
    public void testConstructor() {
        DbUser user = mock(DbUser.class);
        when(user.getId()).thenReturn(Guid.EVERYONE);

        // Mock the session
        sessionDataContainer.setUser(session, user);

        // Create a command
        command.postConstruct();

        // Check the session
        assertEquals("wrong user id on command", user.getId(), command.getUserId());
    }

    @Test
    public void logRenamedEntityNotRename() {
        command.logRenamedEntity();
    }

    @Test
    public void logRenamedEntity() {
        RenamedEntityInfoProvider infoProvider = (RenamedEntityInfoProvider) command;
        command.logRenamedEntity();
        when(infoProvider.getEntityOldName()).thenReturn("foo");
        when(infoProvider.getEntityNewName()).thenReturn("bar");
        when(command.getCurrentUser()).thenReturn(mock(DbUser.class));
        command.logRenamedEntity();
        when(infoProvider.getEntityNewName()).thenReturn("bar");
        command.logRenamedEntity();
        when(infoProvider.getEntityOldName()).thenReturn("foo");
        command.logRenamedEntity();
    }

    @Test
    public void testExtractVariableDeclarationsForStaticMsgs() {
        List<String> msgs = Arrays.asList(
                "ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM",
                "IRS_FAILED_RETRIEVING_SNAPSHOT_INFO");

        assertTrue("extractVariableDeclarations didn't return the same static messages",
                CollectionUtils.isEqualCollection(msgs, command.extractVariableDeclarations(msgs)));
    }

    @Test
    public void testExtractVariableDeclarationsForDynamicMsgs() {
        String msg1_1 = "ACTION_TYPE_FAILED_TEMPLATE_IS_USED_FOR_CREATE_VM";
        String msg1_2 = "$VmName Vm1";
        String msg2   = "IRS_FAILED_CREATING_SNAPSHOT";
        String msg3_1 = "ACTION_TYPE_FAILED_VM_SNAPSHOT_HAS_NO_CONFIGURATION";
        String msg3_2 = "$VmName Vm2";
        String msg3_3 = "$SnapshotName Snapshot";
        List<String> appendedMsgs = Arrays.asList(
                new StringBuilder().append(msg1_1).append(msg1_2).toString(),
                msg2,
                new StringBuilder().append(msg3_1).append(msg3_2).append(msg3_3).toString());
        List<String> extractedMsgs = Arrays.asList(msg1_1, msg1_2, msg2, msg3_1, msg3_2, msg3_3);

        assertTrue("extractVariableDeclarations didn't extract the variables as expected",
                CollectionUtils.isEqualCollection(extractedMsgs, command.extractVariableDeclarations(appendedMsgs)));
    }
}
