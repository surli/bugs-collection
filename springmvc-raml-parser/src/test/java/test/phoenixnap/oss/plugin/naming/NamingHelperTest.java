/*
 * Copyright 2002-2017 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package test.phoenixnap.oss.plugin.naming;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.phoenixnap.oss.ramlapisync.naming.NamingHelper;
import com.phoenixnap.oss.ramlapisync.raml.RamlModelFactoryOfFactories;
import com.phoenixnap.oss.ramlapisync.raml.RamlResource;

import test.phoenixnap.oss.plugin.naming.testclasses.CamelCaseTest;
import test.phoenixnap.oss.plugin.naming.testclasses.ServicesControllerImpl;

/**
 * Unit tests for the NamingHelper class
 * 
 * @author Kurt Paris
 * @since 0.0.1
 *
 */
public class NamingHelperTest {

	@Test
	public void test_convertClassName_Success() {
		assertEquals("Should not kill all names", "services",
				NamingHelper.convertClassName(ServicesControllerImpl.class));
		assertEquals("CamelCaseCheck", "camelCaseTest", NamingHelper.convertClassName(CamelCaseTest.class));
	}
	
	@Test
	public void test_getResourceName_Success() {
		RamlResource testResource = RamlModelFactoryOfFactories.createRamlModelFactoryV08().createRamlResource();
		
		testResource.setRelativeUri("/service_thingy");
		assertEquals("Should deal with underscores", "ServiceThingy", NamingHelper.getResourceName(testResource, true));
		
		testResource.setRelativeUri("/quotes");
		assertEquals("Should deal with plural", "Quote", NamingHelper.getResourceName(testResource, true));
		
		testResource.setRelativeUri("/2342quotes");
		assertEquals("Should deal with invalid java identifiers", "_2342quote", NamingHelper.getResourceName(testResource, true));
		
		testResource.setRelativeUri("/;qu%ot'es");
		assertEquals("Should deal with invalid java characters", "quote", NamingHelper.getResourceName(testResource, true));
	}
	
	@Test
	public void test_getAllResourcesNames_Success() {

		String url = "/services/things";
		
		assertEquals("Should deal with unlimited depth", "ServicesThings", NamingHelper.getAllResourcesNames(url, false, -1));
		assertEquals("Should deal with unlimited depth and singularization", "ServiceThing", NamingHelper.getAllResourcesNames(url, true, -1));
		assertEquals("Should deal with depth=1", "Things", NamingHelper.getAllResourcesNames(url, false, 1));
		assertEquals("Should deal with depth=1 and singularization", "Thing", NamingHelper.getAllResourcesNames(url, true, 1));
		assertEquals("Should deal with depth=2", "ServicesThings", NamingHelper.getAllResourcesNames(url, false, 2));
		assertEquals("Should deal with depth=2 and singularization", "ServiceThing", NamingHelper.getAllResourcesNames(url, true, 2));
		
		url = "/services/things/quotes";
		
		assertEquals("Should deal with unlimited depth", "ServicesThingsQuotes", NamingHelper.getAllResourcesNames(url, false, -1));
		assertEquals("Should deal with unlimited depth and singularization", "ServiceThingQuote", NamingHelper.getAllResourcesNames(url, true, -1));
	}

	@Test
	public void test_cleanLeadingAndTrailingNewLineAndChars_Empty() {
		assertEquals("EmptyString", "", NamingHelper.cleanLeadingAndTrailingNewLineAndChars(""));
		assertEquals("Null", null, NamingHelper.cleanLeadingAndTrailingNewLineAndChars(null));
	}

	@Test
	public void test_cleanLeadingAndTrailingNewLineAndChars_AsIs() {
		assertEquals("Ignore anything except trailing chars", "asdasd asdasd",
				NamingHelper.cleanLeadingAndTrailingNewLineAndChars("asdasd asdasd"));
	}

	@Test
	public void test_cleanLeadingAndTrailingNewLineAndChars_Cleanup() {
		assertEquals("Clean Leading", "asdasd",
				NamingHelper.cleanLeadingAndTrailingNewLineAndChars("   -*\n\t     asdasd"));
		assertEquals("Clean Trailing", "asdasd",
				NamingHelper.cleanLeadingAndTrailingNewLineAndChars("asdasd   -*\n\t     "));
		assertEquals("Clean Both", "asdasd",
				NamingHelper.cleanLeadingAndTrailingNewLineAndChars("   -*\n\t     asdasd   -*\n\t     "));
	}
	
	@Test
	public void test_resolveProperties_Success() {
		assertEquals("Should ignore non properties", "/one/two/{three}", NamingHelper.resolveProperties("/one/two/{three}"));
		assertEquals("Should use defaults if available", "/one/{two}/three", NamingHelper.resolveProperties("/one/{two}/${something:three}"));
		assertEquals("Should use defaults if available", "/one:8080/two/three/{four}/five/", NamingHelper.resolveProperties("/one:8080/two/${something:three}/{four}/five/"));
		assertEquals("Should use defaults if available", "/one:8080/two/{three}/four/five", NamingHelper.resolveProperties("/one:8080/two/${something:{three}}/four${something:/five}"));
		assertEquals("Should use defaults if available", "/one:8080/two/three/four/five/", NamingHelper.resolveProperties("/one:8080/two/${something:three}/four/${something:five}/"));		
		assertEquals("Should resolve to name if none available", "/one/two/three", NamingHelper.resolveProperties("/one/two/${three}"));
		assertEquals("Should resolve to name if none available", "/one:8080/two/three/four/five/", NamingHelper.resolveProperties("/one:8080/two/${three}/four/${five}/"));
		System.setProperty("three", "3");
		assertEquals("Should resolve property if available", "/one:8080/two/3/four/five/", NamingHelper.resolveProperties("/one:8080/two/${three}/four/${five}/"));
		assertEquals("All of the above", "/one:8080/two/3/four/five/", NamingHelper.resolveProperties("/one:8080/two/${three}/${four:four}/${five}/"));
	}
	
	@Test
	public void test_convertTypeToQualifier_Success() {
		assertEquals("Should deal with simple standards cleanly", "AsJson", NamingHelper.convertContentTypeToQualifier("application/json"));
		assertEquals("Should deal with simple standards cleanly", "AsBinary", NamingHelper.convertContentTypeToQualifier("application/octet-stream"));
		assertEquals("Should deal with simple standards cleanly", "AsText", NamingHelper.convertContentTypeToQualifier("text/plain"));
		assertEquals("Should deal with simple standards cleanly", "AsText", NamingHelper.convertContentTypeToQualifier("text/html"));
		
		assertEquals("Should deal extract versions", "V1", NamingHelper.convertContentTypeToQualifier("application/v1+json"));
		assertEquals("Should deal extract versions", "V1", NamingHelper.convertContentTypeToQualifier("application/asdasdasdv1asdsad+json"));
		assertEquals("Should deal extract versions", "V1_2", NamingHelper.convertContentTypeToQualifier("application/asdasdasdv1.2asdsad+json"));
		
		assertEquals("Should deal extract versions", "_StuffAsJson", NamingHelper.convertContentTypeToQualifier("application/stuff+json"));
		
	}
	
	@Test
	public void test_enumNaming_Success() {
		assertEquals("SOMETHING_WORDY", NamingHelper.cleanNameForJavaEnum("somethingWordy"));
		assertEquals("SOMETHING_WORDY", NamingHelper.cleanNameForJavaEnum("SomethingWordy"));
		assertEquals("SOMETHING_WORDY", NamingHelper.cleanNameForJavaEnum("something Wordy"));
		assertEquals("SOMETHING_WORDY", NamingHelper.cleanNameForJavaEnum("SOMETHING WORDY"));
		assertEquals("SOMETHING_WORDY", NamingHelper.cleanNameForJavaEnum("something wordy"));
		assertEquals("SOME_THING_WORDY", NamingHelper.cleanNameForJavaEnum("some@#%(@%thing wordy"));
		assertEquals("AB_CDEF", NamingHelper.cleanNameForJavaEnum("ABCdef"));
		assertEquals("ABC", NamingHelper.cleanNameForJavaEnum("ABC"));
		assertEquals("A_B_CD", NamingHelper.cleanNameForJavaEnum("a b CD"));
		assertEquals("A_B_C_D", NamingHelper.cleanNameForJavaEnum("a b cD"));
		
		assertEquals("S_WORD", NamingHelper.cleanNameForJavaEnum("sWORD"));
		assertEquals("S_WORD", NamingHelper.cleanNameForJavaEnum("sWord"));
	}

}
