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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import com.sun.management.OperatingSystemMXBean;

import step.core.accessors.Attribute;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

@SuppressWarnings("restriction")
@Attribute(key="project", value="@system")
public class MBeanKeywords extends AbstractKeyword {

	protected static final String HEAP_MEMORY_USAGE_MAX = "HeapMemoryUsageMax";
	protected static final String HEAP_MEMORY_USAGE_USED = "HeapMemoryUsageUsed";
	protected static final String TOTAL_SWAP_SPACE_SIZE = "TotalSwapSpaceSize";
	protected static final String FREE_SWAP_MEMORY_SIZE = "FreeSwapMemorySize";
	protected static final String TOTAL_PHYSICAL_MEMORY_SIZE = "TotalPhysicalMemorySize";
	protected static final String FREE_PHYSICAL_MEMORY_SIZE = "FreePhysicalMemorySize";
	protected static final String SYSTEM_CPU_LOAD = "SystemCpuLoad";

	@Keyword(name = "HealthStats", schema = "{\"properties\":{}}")
	public void getHealthStats() throws Exception {
		OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
		long systemCpuLoadPercentage = (long) (systemCpuLoad * 100l);
		System.out.println(systemCpuLoadPercentage);
		addMeasureAndOutput(SYSTEM_CPU_LOAD, systemCpuLoadPercentage);

		long freePhysicalMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getFreePhysicalMemorySize());
		addMeasureAndOutput(FREE_PHYSICAL_MEMORY_SIZE, freePhysicalMemorySize);
		
		long totalPhysicalMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getTotalPhysicalMemorySize());
		addMeasureAndOutput(TOTAL_PHYSICAL_MEMORY_SIZE, totalPhysicalMemorySize);

		long freeSwapMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getFreeSwapSpaceSize());
		addMeasureAndOutput(FREE_SWAP_MEMORY_SIZE, freeSwapMemorySize);

		long totalSwapSpaceSize = fromBytesToMegaBytes(operatingSystemMXBean.getTotalSwapSpaceSize());
		addMeasureAndOutput(TOTAL_SWAP_SPACE_SIZE, totalSwapSpaceSize);
		
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		addMeasureAndOutput(HEAP_MEMORY_USAGE_USED, fromBytesToMegaBytes(heapMemoryUsage.getUsed()));
		addMeasureAndOutput(HEAP_MEMORY_USAGE_MAX, fromBytesToMegaBytes(heapMemoryUsage.getMax()));
	}
	
	protected Long fromBytesToMegaBytes(long bytesValue) {
		return bytesValue / 1048576;
	}

	protected void addMeasureAndOutput(String measureName, long systemCpuLoadPercentage) {
		output.addMeasure(measureName, systemCpuLoadPercentage);
		output.add(measureName, Long.toString(systemCpuLoadPercentage));
	}
}
