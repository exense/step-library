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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import ch.exense.step.library.commons.AbstractProcessKeyword;
import step.handlers.javahandler.Keyword;

public class TypePerfKeywords extends AbstractProcessKeyword {
	protected List<Metric> metrics;
	protected ScriptEngine engine;
	protected String hostname;

	public TypePerfKeywords() {
		metrics = new ArrayList<Metric>();
		ScriptEngineManager manager = new ScriptEngineManager();
		engine = manager.getEngineByName("groovy");
		hostname = getLocalHostname();
	}

	@Keyword(name = "Typeperf", schema = "{\"properties\":{}}")
	public void getTypePerf() throws Exception {
		String cmd = buildCommandLine();
		executeManagedCommand(cmd, PROCESS_TIMEOUT, new OutputConfiguration(false, 1000, 10000, true, true), p -> {
			try {
				executionPostProcess(p.getProcessOutputLog());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	private String createTypePerfArgs() throws Exception {
		StringBuilder sb = new StringBuilder();
		InputStream inputStream = this.getClass().getResourceAsStream("/typePerf.csv");

		try (BufferedReader bf = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = bf.readLine()) != null) {
				if (line.startsWith("Metric_name,Typeperf_counter")) {
					continue;
				}
				String[] columns = line.split(",");
				String expression = (columns.length > 2) ? columns[2] : "";
				metrics.add(new Metric(columns[0], columns[1], expression));
				sb.append(" \"").append(columns[1]).append("\"");
			}
			sb.append(" -sc 1");
			if (input.containsKey("hostname")) {
				sb.append(" -s ").append(input.getString("hostname"));
			}
		}
		return sb.toString();
	}

	protected void executionPostProcess(File file) throws Exception {
		// Extract the output line with values ex: "04/04/2019
		// 14:05:07.730","79.863592","9181.000000"
		String line = Files.readAllLines(file.toPath(), Charset.defaultCharset()).get(2).replaceAll("\"", "");
		String[] values = line.split(",");
		Bindings bindings = engine.createBindings();

		if (metrics.size() == (values.length - 1)) {
			for (int i = 1; i < values.length; i++) {
				long value = Math.round(Double.parseDouble(values[i]));
				if (!metrics.get(i - 1).getGroovyExpression().isEmpty()) {
					bindings.put("value", value);
					Object gResult = engine.eval(metrics.get(i - 1).getGroovyExpression(), bindings);
					value = (long) gResult;
				}
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("eId", "OSmonitor");
				map.put("hostname", hostname);
				String name = metrics.get(i - 1).getName();
				output.addMeasure(name, value, map);
				output.add(name, value);
			}
		}
	}

	protected String buildCommandLine() throws Exception {
		return "typeperf " + createTypePerfArgs();
	}

	protected String getLocalHostname() {
		String hostname = null;
		try {
			InetAddress ip = InetAddress.getLocalHost();
			hostname = ip.getHostName();
		} catch (UnknownHostException e) {
			logger.error("Error while getting local hostname", e);
		}
		return hostname;
	}

	public class Metric {
		private String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getCounter() {
			return counter;
		}

		public void setCounter(String counter) {
			this.counter = counter;
		}

		public String getGroovyExpression() {
			return groovyExpression;
		}

		public void setGroovyExpression(String groovyExpression) {
			this.groovyExpression = groovyExpression;
		}

		public Metric(String name, String counter, String groovyExpression) {
			super();
			this.name = name;
			this.counter = counter;
			this.groovyExpression = groovyExpression;
		}

		private String counter;
		private String groovyExpression;

	}
}
