package com.alibaba.dcm.agent;

import com.alibaba.dcm.DnsCacheManipulator;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Jerry Lee (oldratlee at gmail dot com)
 */
public class DcmAgentTest {
    private File outputFile;
    private String outputFilePath;

    @Before
    public void setUp() throws Exception {
        outputFile = new File("target/output.log");
        FileUtils.deleteQuietly(outputFile);
        FileUtils.touch(outputFile);
        assertTrue(outputFile.length() == 0);
        System.out.println("Prepared output file: " + outputFile.getAbsolutePath());

        outputFilePath = outputFile.getAbsolutePath();

        DnsCacheManipulator.clearDnsCache();
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("============================================");
        System.out.println("Agent Output File Content");
        System.out.println("============================================");
        final String text = FileUtils.readFileToString(outputFile);
        System.out.println(text);
    }

    @Test
    public void test_agentmain_empty() throws Exception {
        DcmAgent.agentmain("   ");
    }

    @Test
    public void test_agentmain_file() throws Exception {
        DcmAgent.agentmain("file " + outputFilePath);

        final List<String> content = FileUtils.readLines(outputFile);
        assertThat(content.get(0), containsString("No action in agent argument, do nothing!"));
    }

    @Test
    public void test_agentmain_set() throws Exception {
        DcmAgent.agentmain("set baidu.com 1.2.3.4");
        assertEquals("1.2.3.4", DnsCacheManipulator.getDnsCache("baidu.com").getIp());
    }

    @Test
    public void test_agentmain_set_toFile() throws Exception {
        DcmAgent.agentmain("set baidu.com 1.2.3.4 file " + outputFilePath);
        assertEquals("1.2.3.4", DnsCacheManipulator.getDnsCache("baidu.com").getIp());

        final List<String> content = FileUtils.readLines(outputFile);
        assertEquals(DcmAgent.DCM_AGENT_SUCCESS_MARK_LINE, content.get(content.size() - 1));
    }

    @Test
    public void test_agentmain_set_MultiIp() throws Exception {
        DcmAgent.agentmain("set baidu.com 1.1.1.1 2.2.2.2");
        assertArrayEquals(new String[]{"1.1.1.1", "2.2.2.2"}, DnsCacheManipulator.getDnsCache("baidu.com").getIps());
    }

    @Test
    public void test_agentmain_get() throws Exception {
        DnsCacheManipulator.setDnsCache("baidu.com", "3.3.3.3");
        DcmAgent.agentmain("get baidu.com");
    }

    @Test
    public void test_agentmain_rm() throws Exception {
        DnsCacheManipulator.setDnsCache("baidu.com", "3.3.3.3");
        DcmAgent.agentmain("rm baidu.com");
        
        assertNull(DnsCacheManipulator.getDnsCache("baidu.com"));
    }

    @Test
    public void test_agentmain_rm_withFile() throws Exception {
        DnsCacheManipulator.setDnsCache("baidu.com", "3.3.3.3");
        assertNotNull(DnsCacheManipulator.getDnsCache("baidu.com"));
        DcmAgent.agentmain("rm  baidu.com file " + outputFilePath);

        assertNull(DnsCacheManipulator.getDnsCache("baidu.com"));
    }

    @Test
    public void test_agentmain_list() throws Exception {
        DcmAgent.agentmain("   list  ");
    }

    @Test
    public void test_agentmain_clear() throws Exception {
        DnsCacheManipulator.setDnsCache("baidu.com", "3.3.3.3");
        DcmAgent.agentmain("   clear  ");
        assertEquals(0, DnsCacheManipulator.listDnsCache().size());
    }

    @Test
    public void test_agentmain_setPolicy() throws Exception {
        DcmAgent.agentmain("   setPolicy    345  ");
        assertEquals(345, DnsCacheManipulator.getDnsCachePolicy());
    }

    @Test
    public void test_agentmain_getPolicy() throws Exception {
        DnsCacheManipulator.setDnsCachePolicy(456);
        DcmAgent.agentmain("   getPolicy     ");
        assertEquals(456, DnsCacheManipulator.getDnsCachePolicy());
    }

    @Test
    public void test_agentmain_setNegativePolicy() throws Exception {
        DcmAgent.agentmain("   setNegativePolicy 42 ");
        assertEquals(42, DnsCacheManipulator.getDnsNegativeCachePolicy());
    }

    @Test
    public void test_agentmain_getNegativePolicy() throws Exception {
        DnsCacheManipulator.setDnsNegativeCachePolicy(45);
        DcmAgent.agentmain("   getNegativePolicy");
        assertEquals(45, DnsCacheManipulator.getDnsNegativeCachePolicy());
    }

    @Test
    public void test_agentmain_skipNoActionArguments() throws Exception {
        DcmAgent.agentmain("  arg1  arg2   ");
    }

    @Test
    public void test_agentmain_actionNeedMoreArgument() throws Exception {
        DnsCacheManipulator.setDnsNegativeCachePolicy(1110);

        DcmAgent.agentmain("  setNegativePolicy     file " + outputFilePath);

        assertEquals(1110, DnsCacheManipulator.getDnsNegativeCachePolicy());

        final List<String> content = FileUtils.readLines(outputFile);
        assertThat(content.get(0), containsString("Error to do action setNegativePolicy"));
        assertThat(content.get(0), containsString("action setNegativePolicy need more argument!"));
    }

    @Test
    public void test_agentmain_actionTooMoreArgument() throws Exception {
        DnsCacheManipulator.setDnsNegativeCachePolicy(1111);

        DcmAgent.agentmain("  setNegativePolicy 737 HaHa  file " + outputFilePath);

        assertEquals(1111, DnsCacheManipulator.getDnsNegativeCachePolicy());

        final List<String> content = FileUtils.readLines(outputFile);
        assertThat(content.get(0), containsString("Error to do action setNegativePolicy 737 HaHa"));
        assertThat(content.get(0), containsString("Too more arguments for Action setNegativePolicy! arguments: [737, HaHa]"));
    }

    @Test
    public void test_agentmain_unknownAction() throws Exception {
        DcmAgent.agentmain("  unknownAction  arg1  arg2   file " + outputFilePath);

        final List<String> content = FileUtils.readLines(outputFile);
        assertThat(content.get(0), containsString("No action in agent argument, do nothing!"));
    }
}
