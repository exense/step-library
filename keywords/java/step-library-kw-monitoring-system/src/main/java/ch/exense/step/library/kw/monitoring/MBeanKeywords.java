/*******************************************************************************
 * Copyright 2021 exense GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ch.exense.step.library.kw.monitoring;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import ch.exense.step.library.commons.BusinessException;
import com.sun.management.OperatingSystemMXBean;

import step.core.accessors.Attribute;
import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

import javax.swing.filechooser.FileSystemView;

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
	protected static final String SYSTEM_FILESYSTEM_NAME = "FilesystemName";
	protected static final String SYSTEM_FILESYSTEM_FREE = "FilesystemFreeSpace";
	protected static final String SYSTEM_FILESYSTEM_USABLE = "FilesystemUsableSpace";
	protected static final String SYSTEM_FILESYSTEM_TOTAL = "FilesystemTotalSpace";

	@Keyword(name = "HealthAlerting", schema = "{\"properties\":{}}")
	public void checksHealthStats() throws Exception {
		Map<String, Long> metrics = getMetrics();

		input.forEach((inputName, inputValueString) -> {
			long inputValue = Long.getLong(String.valueOf(inputValueString));
			long metricValue = metrics.get(inputName);

			if (metrics.containsKey(inputName)) {
				if (metricValue < inputValue) {
					throw new BusinessException(String.format("Metric '{0}' was '{1}', lower than '{2}'",inputName,metricValue,inputValue));
				}
			}
		});
	}

	@Keyword(name = "HealthStats", schema = "{\"properties\":{}}")
	public void getHealthStats() throws Exception {
		getMetrics().forEach((measureName, measureValue) -> {
			output.addMeasure(measureName, measureValue);
			output.add(measureName, Long.toString(measureValue));
		});
	}

	private Map<String,Long> getMetrics() {

		Map<String,Long> metrics = new HashMap<>();

		OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		double systemCpuLoad = operatingSystemMXBean.getSystemCpuLoad();
		long systemCpuLoadPercentage = (long) (systemCpuLoad * 100L);

		metrics.put(SYSTEM_CPU_LOAD, systemCpuLoadPercentage);

		long freePhysicalMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getFreePhysicalMemorySize());
		metrics.put(FREE_PHYSICAL_MEMORY_SIZE, freePhysicalMemorySize);
		
		long totalPhysicalMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getTotalPhysicalMemorySize());
		metrics.put(TOTAL_PHYSICAL_MEMORY_SIZE, totalPhysicalMemorySize);

		long freeSwapMemorySize = fromBytesToMegaBytes(operatingSystemMXBean.getFreeSwapSpaceSize());
		metrics.put(FREE_SWAP_MEMORY_SIZE, freeSwapMemorySize);

		long totalSwapSpaceSize = fromBytesToMegaBytes(operatingSystemMXBean.getTotalSwapSpaceSize());
		metrics.put(TOTAL_SWAP_SPACE_SIZE, totalSwapSpaceSize);
		
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		metrics.put(HEAP_MEMORY_USAGE_USED, fromBytesToMegaBytes(heapMemoryUsage.getUsed()));
		metrics.put(HEAP_MEMORY_USAGE_MAX, fromBytesToMegaBytes(heapMemoryUsage.getMax()));

		FileSystemView fsv = FileSystemView.getFileSystemView();
		int i=0;
		for (File root : fsv.getRoots()) {
			output.add(SYSTEM_FILESYSTEM_NAME+"_"+i,fsv.getSystemDisplayName(root));
			metrics.put(SYSTEM_FILESYSTEM_TOTAL+"_"+i,root.getTotalSpace());
			metrics.put(SYSTEM_FILESYSTEM_FREE+"_"+i,root.getFreeSpace());
			metrics.put(SYSTEM_FILESYSTEM_USABLE+"_"+i,root.getUsableSpace());
			i++;
		}

		return metrics;
	}
	
	protected Long fromBytesToMegaBytes(long bytesValue) {
		return bytesValue / 1048576;
	}
}
