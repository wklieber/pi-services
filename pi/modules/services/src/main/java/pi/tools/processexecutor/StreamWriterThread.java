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


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * thread that reads the input of one stream and write it to an other stream.
 * Can be used e.g. when starting a external program to redirect its output to System.out
 *
 * @author Werner Klieber
 */
public class StreamWriterThread implements Runnable {
    private static Logger log = Logger.getLogger(StreamWriterThread.class.getName());
    private InputStream inputStream;
    private OutputStream outputStream;
    private String prefix; // is written before each outputline


    public StreamWriterThread(InputStream inputStream, OutputStream outputStream, String prefix) {
        init(inputStream, outputStream, prefix);
    }

    private void init(InputStream inputStream, OutputStream outputStream, String prefix) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.prefix = prefix;
    }

    // run thread until the inputStream is closed
    public void run() {
        try {
            byte buffer[] = new byte[1024];
            int availableBytes;

            if (prefix == null) {
                prefix = "[Process] ";
            }

            log.fine("thread logger '" + prefix + "' started");
            while ((availableBytes = inputStream.read(buffer)) > -1) {
                //System.out.println("got bytes: " + availableBytes + ", content: " + new String(buffer));
                writeToOutput(buffer, availableBytes);
            }
            byte[] b = "\n".getBytes();
            writeToOutputStream(b, 0, b.length);

        } catch (IOException e) {
            log.severe("Exception:" + e.toString());
            e.printStackTrace();
        }
        log.fine("thread " + prefix + " logger finished");
    }

    private void writeToOutput(byte[] buffer, int availableBytes) throws IOException {

        if (availableBytes > 0) {
            // parse into lines and add the prefix on each new line
            // windows=13,10, Linux=10

            byte[] prefixBytes = prefix.getBytes();

            int offset = 0;
            int len;
            for (int i = 0; i < availableBytes; i++) {
                byte b = buffer[i];
                if (b == 10) {
                    len = i - offset + 1;
                    writeToOutputStream(buffer, offset, len);
                    writeToOutputStream(prefixBytes, 0, prefixBytes.length);
                    offset = i + 1;
                }
            }

            // now write the rest that has no line breaks
            len = availableBytes - offset;
            writeToOutputStream(buffer, offset, len);
        }
    }

    private void writeToOutputStream(byte[] buffer, int offset, int len) throws IOException {
        if (len > 0 && buffer.length > offset) {
            outputStream.write(buffer, offset, len);
            outputStream.flush();
        }
    }
}
