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
package ch.exense.step.library.commons;

import ch.exense.commons.processes.ManagedProcess;
import ch.exense.commons.processes.ManagedProcess.ManagedProcessException;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.handlers.javahandler.AbstractKeyword;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public abstract class AbstractProcessKeyword extends AbstractKeyword {

    protected static final int PROCESS_TIMEOUT = 60000;

    public AbstractProcessKeyword() {
        super();
    }

    public static class OutputConfiguration {

        private final boolean alwaysAttachOutput;
        private final int maxOutputPayloadSize;
        private final int maxOutputAttachmentSize;
        private final boolean printExitCode;
        private final boolean checkExitCode;

        public OutputConfiguration() {
            this(true, 1000, 1000000, true, true);
        }

        public OutputConfiguration(boolean alwaysAttachOutput, int maxOutputPayloadSize, int maxOutputAttachmentSize,
                                   boolean printExitCode, boolean checkExitCode) {
            super();
            this.alwaysAttachOutput = alwaysAttachOutput;
            this.maxOutputPayloadSize = maxOutputPayloadSize;
            this.maxOutputAttachmentSize = maxOutputAttachmentSize;
            this.printExitCode = printExitCode;
            this.checkExitCode = checkExitCode;
        }

        protected boolean isAlwaysAttachOutput() {
            return alwaysAttachOutput;
        }

        protected int getMaxOutputPayloadSize() {
            return maxOutputPayloadSize;
        }

        protected int getMaxOutputAttachmentSize() {
            return maxOutputAttachmentSize;
        }

        protected boolean isCheckExitCode() {
            return checkExitCode;
        }
    }

    protected void executeManagedCommand(String cmd, int timeoutMs) throws Exception {
        executeManagedCommand(cmd, timeoutMs, new OutputConfiguration(), null);
    }

    protected void executeManagedCommand(String cmd, int timeoutMs, OutputConfiguration outputConfiguration) throws Exception {
        executeManagedCommand(cmd, timeoutMs, outputConfiguration, null);
    }

    protected void executeManagedCommand(List<String> cmd, int timeoutMs, OutputConfiguration outputConfiguration, Consumer<ManagedProcess> postProcess) throws Exception {
        ManagedProcess process = new ManagedProcess(cmd);
        executeManagedCommand(timeoutMs, outputConfiguration, postProcess, process);
    }

    protected void executeManagedCommand(String cmd, int timeoutMs, OutputConfiguration outputConfiguration, Consumer<ManagedProcess> postProcess) throws Exception {
        ManagedProcess process = new ManagedProcess(cmd);
        executeManagedCommand(timeoutMs, outputConfiguration, postProcess, process);
    }

    protected void executeManagedCommand(int timeoutMs, OutputConfiguration outputConfiguration,
                                         Consumer<ManagedProcess> postProcess, ManagedProcess process)
            throws ManagedProcessException, InterruptedException, IOException {
        try {
            boolean hasError = false;
            process.start();
            try {
                int exitCode = process.waitFor(timeoutMs);
                if (outputConfiguration.isCheckExitCode() && exitCode != 0) {
                    output.setBusinessError("Process exited with code " + exitCode);
                    hasError = true;
                }
                if(outputConfiguration.printExitCode) {
                    output.add("Exit_code", Integer.toString(exitCode));
                }
                if(postProcess != null) {
                    postProcess.accept(process);
                }
            } catch (TimeoutException e) {
                output.setBusinessError("Process didn't exit within the defined timeout of "+timeoutMs+"ms");
                hasError = true;
            }

            if(hasError || outputConfiguration.isAlwaysAttachOutput()) {
                attachOutputs(process, outputConfiguration);
            }
        } finally {
            process.close();
        }
    }

    protected void attachOutputs(ManagedProcess process, OutputConfiguration outputConfiguration) throws IOException {
        attachOutput("stdout", process.getProcessOutputLog(), outputConfiguration);
        attachOutput("stderr", process.getProcessErrorLog(), outputConfiguration);
    }

    protected void attachOutput(String outputName, File file, OutputConfiguration outputConfiguration) throws IOException {
        StringBuilder processOutputBuilder = new StringBuilder();
        Files.readAllLines(file.toPath(), Charset.defaultCharset()).forEach(l -> processOutputBuilder.append(l).append("\n"));

        String processOutput = processOutputBuilder.toString();

        output.add(outputName, processOutput.substring(0, Math.min(processOutput.length(), outputConfiguration.maxOutputPayloadSize)));

        if(processOutput.length() > outputConfiguration.maxOutputPayloadSize) {
            Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(
                    processOutput.substring(0, Math.min(processOutput.length(), outputConfiguration.maxOutputAttachmentSize)).getBytes(), outputName + ".log");
            output.addAttachment(attachment);

            if (file.length() > outputConfiguration.maxOutputAttachmentSize) {
                output.add("technicalWarning",
                        outputName + " size exceeded. " + outputName + " has been attached and truncated.");
            }
        }
    }
}
