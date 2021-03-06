/*
 * Copyright 2007 Daniel Spiewak
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 *	    http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.ao.schema;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import test.schema.Company;
import test.schema.CompanyAddressInfo;
import test.schema.Person;
import test.schema.PersonLegalDefence;
import test.schema.PersonSuit;

/**
 * @author Daniel Spiewak
 */
public class PluralizedNameConverterTest {
	private PluralizedNameConverter converter;

	@Before
	public void setUp() throws Exception {
		converter = new PluralizedNameConverter();
	}

	@Test
	public void testAddClassMapping() {
		converter.addClassMapping(Person.class, "rowdy_one");
		assertEquals("rowdy_one", converter.getName(Person.class));
		
		converter.addClassMapping(PersonSuit.class, "unfair_procedings");
		assertEquals("unfair_procedings", converter.getName(PersonSuit.class));
	}

	@Test
	public void testGetName() {
		assertEquals("people", converter.getName(Person.class));
		assertEquals("companies", converter.getName(Company.class));
		assertEquals("personSuits", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("companyAddressInfo", converter.getName(CompanyAddressInfo.class));
	}

	@Test
	public void testGetNameUnderscore() {
		converter = new PluralizedNameConverter(new UnderscoreTableNameConverter(true));
		
		assertEquals("PEOPLE", converter.getName(Person.class));
		assertEquals("COMPANIES", converter.getName(Company.class));
		assertEquals("PERSON_SUITS", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("COMPANY_ADDRESS_INFO", converter.getName(CompanyAddressInfo.class));
	}
	
	/**
	 * Tests explicit patterns, regular expression patterns, pattern overriding
	 * and pattern addition order.
	 */
	@Test
	public void testAddPatternMapping() {
		converter.addPatternMapping("person", "persons");
		converter.addPatternMapping("personDefence", "somethingBadHappened");
		converter.addPatternMapping("company", "company");
		converter.addPatternMapping("(.+)any", "{1}anies");
		
		assertEquals("persons", converter.getName(Person.class));
		assertEquals("companies", converter.getName(Company.class));
		assertEquals("personSuits", converter.getName(PersonSuit.class));
		assertEquals("personDefence", converter.getName(PersonLegalDefence.class));
		assertEquals("companyAddressInfo", converter.getName(CompanyAddressInfo.class));
	}
	
	@Test
	public void testPluralization() throws IOException {
		Properties data = new Properties();
		InputStream is = getClass().getResourceAsStream("/net/java/ao/schema/englishPluralTest.properties");
		
		try {
			data.load(is);
		} finally{
			is.close();
		}
		
		for (Object key : data.keySet()) {
			assertEquals(data.getProperty(key.toString()), converter.processName(key.toString()));
		}
	}
}
