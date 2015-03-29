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


import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * specify setting for starting an external process
 *
 * @author Werner Klieber
 */
public class ProcessExecutorSettings {
    /**
     * environmenVariables for the new proces.
     * if null, system defaults of this vm is used (System.getEnv())
     */
    private Map<String, String> env;

    /**
     * working directory for the new process. May be null
     *
     * @param text
     */
    private String workDir;

    /**
     * optional text-prefix for each redirected text line in the console.
     * This is useful to determin the output of each process.
     * for the error an [ERR] prefix is added.
     * if null, nothing is appended - even no error prefix
     */
    private String outputPrefix;

    /**
     * the program to execute
     */
    private String command;

    /**
     * addional argurments for the command.
     * Note: must be really arguments. E.g. linux pipes are not supported (ls -lah | grep /home will fail)
     */
    private List<String> args;

    /**
     * stream the get the program output. if null, data is logged to the console
     */
    private OutputStream outputStream;

    /**
     * param command  the command to execute, e.g. dir or ls
     *
     * @param args optional set some arguments passing to the command to execute
     */
    public ProcessExecutorSettings(String command, String... args) {
        this.command = command;
        if (args != null) {
            List<String> list = getArgs();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                list.add(arg);
            }
        }
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(String workDir) {
        this.workDir = workDir;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix) {
        this.outputPrefix = outputPrefix;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public List<String> getArgs() {
        if (args == null) args = new ArrayList<String>();
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    /**
     * return a human readable string containing the command and all arguments as beeing executed
     */
    public String printCommandLine() {
        StringBuilder str = new StringBuilder();
        str.append(command);
        for (String arg : args) {
            str.append(" ");
            str.append(arg);
        }

        return str.toString();
    }

    public String toString() {
        return "ProcessExecutorSettings{" +
                "env=" + env +
                ", workDir='" + workDir + '\'' +
                ", outputPrefix='" + outputPrefix + '\'' +
                ", command='" + command + '\'' +
                ", args=" + args +
                ", outputStream=" + outputStream +
                '}';
    }
}
