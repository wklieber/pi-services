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

package pi;

import com.codahale.metrics.annotation.Timed;
import org.apache.commons.io.IOUtils;
import pi.tools.processexecutor.ProcessExecutor;
import pi.tools.processexecutor.ProcessExecutorResult;
import pi.tools.processexecutor.ProcessExecutorSettings;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by wklieber on 08.03.2015.
 */
@Path("/pi")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class PiCameraResource {
    private final AtomicLong imageCaputureCounter;

    public PiCameraResource() {
        this.imageCaputureCounter = new AtomicLong();
    }

    @GET
    @Timed
    @Path("/image/capture")
    @Produces("image/png")
    public Response capture() throws Exception {
        // use the command line tool "raspistill".

        //try {
            String id = String.valueOf(imageCaputureCounter.incrementAndGet());
            String fileName = "/tmp/image-" + id + "-" + System.currentTimeMillis() + ".png";
            executeCaptureCommand(fileName);


            InputStream inputStream = new FileInputStream(fileName);
            byte[] imageData = IOUtils.toByteArray(inputStream);

            executeDeleteImageCommand(fileName);

            // uncomment line below to send non-streamed
            return Response.ok(imageData).build();

            // uncomment line below to send streamed
            // return Response.ok(new ByteArrayInputStream(imageData)).build();
        /*} catch (Exception e) {
            String msg = e.toString();
            throw new WebApplicationException(msg);
        }*/
    }

    private void executeCaptureCommand(String fileName) throws InterruptedException {
        //-o, --output     : Output filename <filename> (to write to stdout, use '-o -'). If not specified, no file is saved
        //-e, --encoding  : Encoding to use for output file (jpg, bmp, gif, png)
        //-t, --timeout    : Time (in ms) before takes picture and shuts down (if not specified, set to 5s) minimum 30ms, setting to 0 waits forever
        //-q, --quality    : Set jpeg quality <0 to 100>
        String[] args = new String[]{"-o", fileName, "-e", "png"};
        ProcessExecutor executor = new ProcessExecutor();
        ProcessExecutorSettings settings = new ProcessExecutorSettings("raspistill", args);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        settings.setOutputStream(out);
        ProcessExecutorResult result = executor.start(settings);
        Process process = result.getProcess();
        int ret = result.waitFor();
        if(ret != 0) {
           throw new UnsupportedOperationException("error executing image capture command line tool 'raspistill'. " +
                   "return code was " + ret + ", Console output: '" + out.toString() + "'");
        }
    }

    /**
     * delete captured file from raspberry after reading.
     */
    private void executeDeleteImageCommand(String fileName) throws InterruptedException {
        String[] args = new String[]{"rm", "-rf", fileName};
        ProcessExecutor executor = new ProcessExecutor();
        ProcessExecutorSettings settings = new ProcessExecutorSettings("raspistill", args);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        settings.setOutputStream(out);
        ProcessExecutorResult result = executor.start(settings);
        Process process = result.getProcess();
        int ret = result.waitFor();
        if(ret != 0) {
            String message = "error deleting image file '" + fileName +
                    "'. Return code was " + ret + ", Console output: '" + out.toString() + "'";
            System.err.println(message);
        }
    }
}
