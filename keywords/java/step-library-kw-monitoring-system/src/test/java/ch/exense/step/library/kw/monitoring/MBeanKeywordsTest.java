/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ch.exense.step.library.kw.monitoring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import step.functions.io.Output;
import step.handlers.javahandler.KeywordRunner;
import step.handlers.javahandler.KeywordRunner.ExecutionContext;

public class MBeanKeywordsTest {

	protected ExecutionContext ctx;

	@Before
	public void setUp() {
		ctx = KeywordRunner.getExecutionContext(MBeanKeywords.class);
	}

	@After
	public void tearDown() {
		ctx.close();
	}

	@Test
	public void testHealthStats() throws Exception {
		JsonObject input = Json.createObjectBuilder().build();
		Output<JsonObject> output = ctx.run("HealthStats", input.toString());
		assertEquals(7, output.getMeasures().size());
		assertTrue(output.getPayload().containsKey(MBeanKeywords.FREE_PHYSICAL_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.FREE_SWAP_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.HEAP_MEMORY_USAGE_MAX));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.HEAP_MEMORY_USAGE_USED));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.SYSTEM_CPU_LOAD));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.TOTAL_PHYSICAL_MEMORY_SIZE));
		assertTrue(output.getPayload().containsKey(MBeanKeywords.TOTAL_SWAP_SPACE_SIZE));
	}
}
