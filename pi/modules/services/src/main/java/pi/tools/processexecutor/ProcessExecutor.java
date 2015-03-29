/*
 * Copyright (c) 2015, Werner Klieber. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package pi.tools.processexecutor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper class for the java ProcessBuilder class to simplify calling external processes.
 * - redirect output streams to current process with an user defined prefix for each line.
 * - use FileTools.getJavaBinaryFile() to get the path to your java executable -> if you
 * want to start a new java process.
 * - use MiscTools.getClasspath() to get the classpath of the current vm -> if you want to clone it
 * in the new vm
 *
 * @author Werner Klieber
 */
public class ProcessExecutor {
    public ProcessExecutor() {
    }

    /**
     * start a new external process. Read the doc in the header of this class to get additional
     * information e.g. how to get the path to the java executable or retrieve the classpath.
     *
     * @return a result containing the started process. e.g. use process.waitFor() to block until the process is done.
     */
    public ProcessExecutorResult start(ProcessExecutorSettings settings) {
        if (settings == null) {
            throw new IllegalArgumentException("settings may not be null");
        }

        List<String> args = settings.getArgs();

        Process process;
        Thread threadOut = null;
        Thread threadErr = null;
        try {
            List<String> call = new ArrayList<String>();
            call.add(settings.getCommand());
            call.addAll(args);

            /*System.out.println("---------------------------");
            for (String s:call) {
                assert s != null;  // when an argument is null, processBuilder.start() will throw an nullpointerexception
                System.out.println("\"" + s + "\"");
            }
            System.out.println("---------------------------");*/

            ProcessBuilder processBuilder = new ProcessBuilder(call);
            processBuilder.redirectErrorStream(false); // do not split error and out stream
            Map<String, String> env = settings.getEnv();
            if (env == null) {
                env = System.getenv();
            }
            processBuilder.environment().clear();
            processBuilder.environment().putAll(env);

            String workDir = settings.getWorkDir();
            if (workDir != null) {
                File dir = new File(workDir);
                if (!dir.exists() || !dir.isDirectory()) {
                    throw new IllegalArgumentException("workdir does not exist: '" + workDir + "'");
                }
                processBuilder.directory(new File(workDir));
            }

            /* System.out.println("Start Process: \"" + command + "\"");
            for (int i = 0; i < args.size(); i++) {
                String arg = args.get(i);
                System.out.println("ARG " + (i+1) + ": <" + arg + ">");
            }*/

            process = processBuilder.start();

            String prf = settings.getOutputPrefix();
            String prefix;
            String errPrefix;
            if (prf == null) {
                prefix = "";
                errPrefix = "";
            } else {
                prefix = prf;
                errPrefix = prefix + "[ERR] ";
            }

            OutputStream out = settings.getOutputStream();
            OutputStream tOut;
            OutputStream tErrorOut;
            if (out == null) {
                tOut = System.out;
                tErrorOut = System.err;
            } else {
                tOut = out;
                tErrorOut = out;
            }

            threadOut = new Thread(new StreamWriterThread(process.getInputStream(), tOut, prefix));
            threadOut.setDaemon(true);
            threadOut.start();

            threadErr = new Thread(new StreamWriterThread(process.getErrorStream(), tErrorOut, errPrefix));
            threadErr.setDaemon(true);
            threadErr.start();
        } catch (Exception e) {
            throw new UnsupportedOperationException("error starting the new process: "  + e.toString(), e);
        }

        ProcessExecutorResult returnValue = new ProcessExecutorResult(process, threadOut, threadErr);
        return returnValue;
    }

    /**
     * simple invoke method: start the process, wait until finished and return any output in the return String.
     * the return value of the process is ignored
     *
     * @return a string containing all console output (including error output) from the external process
     */
    public String startSimple(ProcessExecutorSettings settings) throws InterruptedException {
        if (settings == null) {
            throw new IllegalArgumentException("settings may not be null");
        }

        if (settings.getOutputStream() != null) {
            throw new IllegalArgumentException("outputstream must be null. This method instantiate it's own one to redirect the output");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        settings.setOutputStream(out);

        ProcessExecutorResult result = start(settings);
        Process process = result.getProcess();
        int ret = result.waitFor();

        String returnValue = out.toString();
        return returnValue.trim();
    }
}
