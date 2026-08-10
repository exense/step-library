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
package ch.exense.step.library.kw.system;

import ch.exense.commons.io.FileHelper;
import step.functions.io.AbstractSession;
import step.functions.io.Input;
import step.functions.io.Output;
import step.handlers.javahandler.KeywordExecutor;
import step.reporting.LiveReporting;
import step.streaming.client.upload.impl.local.LocalDirectoryBackedStreamingUploadProvider;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Execution context for keyword tests, equivalent to {@link step.handlers.javahandler.KeywordRunner.ExecutionContext}
 * but with support for streamed attachments.
 * <p>
 * The KeywordRunner discards everything a keyword streams via liveReporting.fileUploads, which makes streamed
 * attachments impossible to assert. This context streams the uploads to a local directory instead and exposes
 * them as {@link StreamedAttachment}s.
 */
public class StreamingAttachmentsExecutionContext implements AutoCloseable {

	/**
	 * Name of the file the standard output of a managed process is streamed to
	 */
	public static final String PROCESS_OUTPUT_LOG = "ProcessOut.log";
	/**
	 * Name of the file the error output of a managed process is streamed to
	 */
	public static final String PROCESS_ERROR_LOG = "ProcessError.log";

	// The local upload provider prefixes the uploaded files with a timestamp and a sequence number
	// in order to make them unique, i.e. 20250819_100434_001_ProcessOut.log
	private static final Pattern UPLOADED_FILE_NAME = Pattern.compile("\\d{8}_\\d{6}_\\d{3}_(.+)");

	private final List<Class<?>> keywordClasses;
	private final Map<String, String> contextProperties;
	private final File uploadDirectory;
	private final ExecutorService uploadExecutor = Executors.newCachedThreadPool();
	private final LiveReporting liveReporting;
	private final KeywordExecutor keywordExecutor;
	private final AbstractSession tokenSession = new AbstractSession();
	private final AbstractSession tokenReservationSession = new AbstractSession();

	public StreamingAttachmentsExecutionContext(Class<?>... keywordClasses) throws IOException {
		this(Map.of(), keywordClasses);
	}

	public StreamingAttachmentsExecutionContext(Map<String, String> properties, Class<?>... keywordClasses) throws IOException {
		this.keywordClasses = Arrays.asList(keywordClasses);
		this.contextProperties = properties;
		this.uploadDirectory = FileHelper.createTempFolder();
		this.liveReporting = new LiveReporting(
				new LocalDirectoryBackedStreamingUploadProvider(uploadExecutor, uploadDirectory), null);
		this.keywordExecutor = new KeywordExecutor(true, liveReporting);
	}

	public void setThrowExceptionOnError(boolean throwExceptionOnError) {
		keywordExecutor.setThrowExceptionOnError(throwExceptionOnError);
	}

	public Output<JsonObject> run(String keyword, String argument) throws Exception {
		Map<String, String> properties = new HashMap<>();
		properties.put(KeywordExecutor.KEYWORD_CLASSES, keywordClasses.stream().map(Class::getName)
				.collect(Collectors.joining(KeywordExecutor.KEYWORD_CLASSES_DELIMITER)));

		Input<JsonObject> input = new Input<>();
		input.setFunction(keyword);
		input.setPayload(Json.createReader(new StringReader(argument)).readObject());
		input.setProperties(properties);

		Map<String, String> allProperties = new HashMap<>(contextProperties);
		allProperties.putAll(properties);

		return keywordExecutor.handle(input, tokenSession, tokenReservationSession, allProperties, null);
	}

	/**
	 * @return all the attachments streamed by the keywords executed in this context, ordered by the name of the
	 * local file they have been streamed to i.e. by upload timestamp and sequence number
	 */
	public List<StreamedAttachment> getStreamedAttachments() {
		File[] uploadedFiles = uploadDirectory.listFiles();
		if (uploadedFiles == null) {
			return List.of();
		}
		List<StreamedAttachment> attachments = new ArrayList<>();
		Arrays.stream(uploadedFiles).sorted(Comparator.comparing(File::getName)).forEach(file -> {
			Matcher matcher = UPLOADED_FILE_NAME.matcher(file.getName());
			if (!matcher.matches()) {
				throw new IllegalStateException("Unexpected name of uploaded file: " + file.getName());
			}
			attachments.add(new StreamedAttachment(matcher.group(1), file));
		});
		return attachments;
	}

	/**
	 * @return the attachments streamed by the keywords executed in this context, excluding the process outputs
	 * which are streamed for every process execution
	 */
	public List<StreamedAttachment> getStreamedAttachmentsWithoutProcessOutputs() {
		return getStreamedAttachments().stream()
				.filter(attachment -> !attachment.getName().equals(PROCESS_OUTPUT_LOG)
						&& !attachment.getName().equals(PROCESS_ERROR_LOG))
				.collect(Collectors.toList());
	}

	/**
	 * @param name the name of the attachment as streamed by the keyword
	 * @return the first attachment matching the provided name
	 */
	public StreamedAttachment getStreamedAttachment(String name) {
		return getStreamedAttachments().stream().filter(attachment -> attachment.getName().equals(name)).findFirst()
				.orElseThrow(() -> new IllegalStateException("No attachment found with name " + name + ". Streamed attachments: "
						+ getStreamedAttachments().stream().map(StreamedAttachment::getName).collect(Collectors.toList())));
	}

	@Override
	public void close() {
		try {
			liveReporting.close();
			uploadExecutor.shutdown();
			tokenSession.close();
			tokenReservationSession.close();
		} finally {
			FileHelper.deleteFolder(uploadDirectory);
		}
	}

	public static class StreamedAttachment {

		private final String name;
		private final File file;

		public StreamedAttachment(String name, File file) {
			this.name = name;
			this.file = file;
		}

		/**
		 * @return the name of the file as streamed by the keyword
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the local file the attachment has been streamed to
		 */
		public File getFile() {
			return file;
		}

		public String getContentAsString() throws IOException {
			return new String(getContent(), StandardCharsets.UTF_8);
		}

		public byte[] getContent() throws IOException {
			return Files.readAllBytes(file.toPath());
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
