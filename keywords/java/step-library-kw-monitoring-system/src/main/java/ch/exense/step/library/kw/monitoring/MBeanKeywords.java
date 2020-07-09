package ch.exense.step.library.kw.monitoring;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

import com.sun.management.OperatingSystemMXBean;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;

@SuppressWarnings("restriction")
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

		long freePhysicalMemorySize = operatingSystemMXBean.getFreePhysicalMemorySize();
		addMeasureAndOutput(FREE_PHYSICAL_MEMORY_SIZE, freePhysicalMemorySize);
		
		long totalPhysicalMemorySize = operatingSystemMXBean.getTotalPhysicalMemorySize();
		addMeasureAndOutput(TOTAL_PHYSICAL_MEMORY_SIZE, totalPhysicalMemorySize);

		long freeSwapMemorySize = operatingSystemMXBean.getFreeSwapSpaceSize();
		addMeasureAndOutput(FREE_SWAP_MEMORY_SIZE, freeSwapMemorySize);

		long totalSwapSpaceSize = operatingSystemMXBean.getTotalSwapSpaceSize();
		addMeasureAndOutput(TOTAL_SWAP_SPACE_SIZE, totalSwapSpaceSize);
		
		MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
		MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
		addMeasureAndOutput(HEAP_MEMORY_USAGE_USED, heapMemoryUsage.getUsed());
		addMeasureAndOutput(HEAP_MEMORY_USAGE_MAX, heapMemoryUsage.getMax());
	}

	protected void addMeasureAndOutput(String measureName, long systemCpuLoadPercentage) {
		output.addMeasure(measureName, systemCpuLoadPercentage);
		output.add(measureName, Long.toString(systemCpuLoadPercentage));
	}
}
