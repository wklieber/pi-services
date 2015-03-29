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

/**
 * @author Werner Klieber
 */
public class ProcessExecutorResult {
    private Process process;
    private Thread threadOut;
    private Thread threadErr;

    protected ProcessExecutorResult(Process process, Thread threadOut, Thread threadErr) {
        this.process = process;
        this.threadOut =threadOut;
        this.threadErr=threadErr;
    }

    public Process getProcess() {
        return process;
    }

    /** wait until process finished and all output is written to the streams */
    public int waitFor() throws InterruptedException {
        int ret = process.waitFor();
        threadOut.join();
        threadErr.join();
        return ret;
    }

    public String toString() {
        return "ProcessExecutorResult{" +
                "process=" + process +
                '}';
    }
}
