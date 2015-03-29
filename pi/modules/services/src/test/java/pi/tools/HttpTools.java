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

package pi.tools;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import sun.misc.IOUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Created by wklieber on 29.03.2015.
 */
public class HttpTools {
    private static Logger logger = Logger.getLogger("pi.tools");

    // set a timeout of 20 seconds when opening a new connection
    private static final int connectionTimeout = (int) (TimeConstants.oneMinute  * 1);

    // timeout when waiting for responses from the server - to avoid deadlocks when a server sends no results.
    private static final int connectionReadTimeout = (int) (TimeConstants.oneMinute  * 5);

    public enum FORMAT {XML, JSON}

    public static final String APPLICATION_JSON = "application/json";
    public static final String APPLICATION_XML = "application/xml";
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    public static InputStream makeHttpRequest(String httpUri, byte[] putData, int retries) {
        for (int i = 0; i < retries; i++) {
            try {
                return makeHttpRequest(httpUri, putData);
            } catch (RuntimeException e) {
                if (isRetryError(e)) {
                    logger.fine(e.getMessage() + " -> Retrying ...");
                }
                else throw e;
            }
        }

        return makeHttpRequest(httpUri, putData);
    }

    /**
     * @param httpUri http url + params to send.
     * @param putData if postData is null, a GET request is send. Otherwise the bytes are send in a PUT request
     * @return the resulting inputStream
     */
    public static InputStream makeHttpRequest(String httpUri, byte[] putData) {
        InputStream returnValue = null;

        String errorMessage = "";
        try {
            //log.debug("retrieve stream for url: " + httpUri);
            URL url;
            url = new URL(httpUri);

            URLConnection con = url.openConnection();
            con.setConnectTimeout(connectionTimeout);
            con.setReadTimeout(connectionReadTimeout);
            logger.finest("connected to url: " + httpUri);

            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) con;
                con.setDoOutput(true);

                if (putData != null) {
                    httpConnection.setRequestMethod("PUT");

                    // send post parameter
                    DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
                    //String post = "";//"param1=" + URLEncoder.encode( paramVariable1 ) + "&param2=" + URLEncoder.encode( paramVariable2 );
                    dos.write(putData);
                    dos.flush();
                    dos.close();
                }
                logger.finest("sent request data");

                int code = httpConnection.getResponseCode();
                 logger.finest("got response code: " + code);

                // read error stream and throw the message as exception
                if (code != HttpURLConnection.HTTP_OK) {
                    InputStream errorStream = httpConnection.getErrorStream();
                    if (errorStream != null) {
                        errorStream = bufferStream(errorStream);
                        //todo: remove the html fragments from the text
                        errorMessage = readFromStream(errorStream);
                        errorStream.close();
                    } else {
                        String msg = httpConnection.getResponseMessage();
                        if (msg != null) errorMessage = msg;
                    }
                     logger.finest("read error stream: " + errorMessage);

                    throw new UnsupportedOperationException("Server Error(" + code + "): " + errorMessage);
                }

                // if ok, read the iput
                InputStream stream = con.getInputStream();
                if (stream == null) throw new UnsupportedOperationException("no web search result");
                stream = bufferStream(stream);
                 logger.finest("read input stream");
                returnValue = stream;

            } else {
                throw new UnsupportedOperationException("no http-connection: '" + httpUri + "'. Only http is supported. class=" + con.getClass().getName());
                //httpConnection.setDoInput(false);
                /*con.setDoOutput(true);
                InputStream stream = con.getInputStream();
                if (stream == null) throw new UnsupportedOperationException("no web search result");
                returnValue = stream;*/
            }

        } catch (Exception e) {
            //e.printStackTrace();
            throw new UnsupportedOperationException("Server error for request '" + httpUri + "': " + errorMessage, e);
        }

        return returnValue;
    }

    /**
     * @param httpUri  http url + params to send.
     * @param postData if postData is null, a GET request is send. Otherwise the bytes are send in a POST request
     * @return the resulting inputStream
     */
    public static String makeHttpRequest(String httpUri, byte[] postData, FORMAT inputDataFormat, FORMAT outputDataFormat) throws IOException {
        InputStream returnValue = null;

        // post uses always xml for sending
        boolean isPost = (postData != null);
        //if(isPost && jsonResult) throw new UnsupportedOperationException("for post requests (putDat != null) no json is supported");

        String errorMessage = "";
        try {
            //log.debug("retrieve stream for url: " + httpUri);
            URL url;
            url = new URL(httpUri);
            URLConnection con = url.openConnection();

            con.setConnectTimeout(connectionTimeout);
             logger.finest("connected to url: " + httpUri);

            if (con instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) con;
                con.setDoOutput(true);


                String inType = (inputDataFormat == null || inputDataFormat.equals(FORMAT.JSON)) ? APPLICATION_JSON : APPLICATION_XML;
                String outType = (outputDataFormat.equals(FORMAT.JSON)) ? APPLICATION_JSON : APPLICATION_XML;

                // content type of sent input for POST requests
                httpConnection.setRequestProperty("Content-Type", inType);

                // tell server (jersey) what we want as result
                httpConnection.setRequestProperty("Accept", outType);

                if (isPost) {
                    httpConnection.setRequestMethod("POST");

                    // send post parameter
                    DataOutputStream dos = new DataOutputStream(httpConnection.getOutputStream());
                    //String post = "";//"param1=" + URLEncoder.encode( paramVariable1 ) + "&param2=" + URLEncoder.encode( paramVariable2 );
                    dos.write(postData);
                    dos.flush();
                    dos.close();
                }
                 logger.finest("sent request data");

                int code = httpConnection.getResponseCode();
                 logger.finest("got response code: " + code);

                // read error stream and throw the message as exception. get requests for "void" functions will return error code 204 - no content
                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_NO_CONTENT) {
                    InputStream errorStream = httpConnection.getErrorStream();
                    if (errorStream != null) {
                        errorStream = bufferStream(errorStream);
                        //todo: remove the html fragments from the text
                        errorMessage = readFromStream(errorStream);
                        errorStream.close();
                    } else {
                        InputStream stream = con.getInputStream();
                        if (stream != null) {
                            errorStream = bufferStream(stream);
                            errorMessage = readFromStream(errorStream);
                            errorStream.close();
                        }
                    }
                     logger.finest("read error stream: " + errorMessage);

                    throw new UnsupportedOperationException("Server Error(" + code + "): " + errorMessage);
                }

                // if ok, read the input
                InputStream stream = con.getInputStream();
                if (stream == null) throw new UnsupportedOperationException("no web search result");
                stream = bufferStream(stream);
                 logger.finest("read input stream");
                returnValue = stream;

            } else {
                throw new UnsupportedOperationException("no http-connection: '" + httpUri + "'. Only http is supported. class=" + con.getClass().getName());
                //httpConnection.setDoInput(false);
                /*con.setDoOutput(true);
                InputStream stream = con.getInputStream();
                if (stream == null) throw new UnsupportedOperationException("no web search result");
                returnValue = stream;*/
            }

        } catch (Exception e) {
            //e.printStackTrace();
            throw new UnsupportedOperationException("error for request '" + httpUri + "': " + errorMessage, e);
        }

        return readFromStream(returnValue);
    }

    /**
     * Calls {@link #readClassFromHttp(String, int, Class, byte[], String...)} with 0 retries
     *
     * @param httpUri
     * @param aClass
     * @param putData
     * @param params
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static <T> T readClassFromHttp(String httpUri, Class<T> aClass, byte[] putData, String... params) throws JAXBException, IOException {
        return readClassFromHttp(httpUri, 0, aClass, putData, params);
    }

    /**
     * if putData is null, a get request is used, otherwise the bytes are send using put.
     * if result type "aClass" is a byte array, it is deserialized using java deserializer, otherwise jaxb (xml).
     *
     * @param httpUri
     * @param retries The number of retries used when calling {@link #makeHttpRequest(String, byte[], int)}
     * @param aClass
     * @param putData
     * @param params
     * @return
     * @throws JAXBException
     * @throws IOException
     */
    public static <T> T readClassFromHttp(String httpUri, int retries, Class<T> aClass, byte[] putData, String... params) throws JAXBException, IOException {

        if (params.length % 2 != 0) {
            throw new UnsupportedOperationException("invalid parameter count. Must have an even amount");
        }

        if (params.length > 0) {
            StringBuilder str = new StringBuilder();
            str.append(httpUri);
            int counter = 0;
            boolean first = true;
            while (counter < params.length) {

                String key = params[counter++];
                if (key == null) {
                    throw new UnsupportedOperationException("Entry nr. " + counter + "may not be null!. " + Arrays.asList(params));
                }
                String value = params[counter++];

                if (value != null) {
                    if (first) {
                        str.append("?");
                        first = false;
                    } else str.append("&");
                    String encodedValue = URLEncoder.encode(value, "utf-8");
                    str.append(key).append("=").append(encodedValue);
                }
            }
            httpUri = str.toString();
        }
        InputStream inputStream = makeHttpRequest(httpUri, putData);

        T instance;
        if (aClass.equals(byte[].class)) {
            byte[] b = IOUtils.readFully(inputStream, -1, true);
            instance = (T) b;
        } else {
            instance = unmarshalXml(inputStream, aClass);
        }
        return instance;
    }

    /**
     * generic method to seralize a jaxb annotated java class to xml using formated output and utf-8.
     */
    public static String marshalXml(Serializable data) throws JAXBException, IOException {
        JAXBContext jaxbSearchContext = JAXBContext.newInstance(data.getClass());
        Marshaller suggestionResultMarshaller = jaxbSearchContext.createMarshaller();
        // generaty pretty xml with new lines and intentions
        suggestionResultMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        suggestionResultMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");


        OutputFormat outputFormat = new OutputFormat();
        // set any other options you'd like
        outputFormat.setPreserveSpace(true);
        outputFormat.setIndenting(true);
        outputFormat.setEncoding("UTF-8");

        XMLSerializer serializer = new XMLSerializer(outputFormat);

        Writer writer = new StringWriter();
        serializer.setOutputCharStream(writer);
        suggestionResultMarshaller.marshal(data, serializer);
        String xml = writer.toString();
        writer.close();
        return xml;
    }

    /**
     * generic method to seralize a jaxb annotated java class to xml using formated output and utf-8.
     */
    public static <T> T unmarshalXml(InputStream data, Class<T> aClass) throws JAXBException, IOException {
        JAXBContext jaxbSearchContext = JAXBContext.newInstance(aClass);
        Unmarshaller ms = jaxbSearchContext.createUnmarshaller();

        T t = (T) ms.unmarshal(data);
        return t;
    }

    /**
     * read the byte array and put it into a String in utf-8 encoding.
     *
     * @param inputStream
     */
    public static String readFromStream(InputStream inputStream) throws IOException {
        String returnValue;

        char buf[] = new char[4096];
        InputStreamReader reader = new InputStreamReader(inputStream, "utf-8");
        StringBuilder strBuff = new StringBuilder();

        // read as long as something is there to read
        boolean finished = false;
        while (!finished) {
            String input;
            int availableBytes = reader.read(buf);
            finished = (availableBytes < 0);
            if (finished) {
                continue;
            }

            input = new String(buf, 0, availableBytes);
            strBuff.append(input);
        }

        returnValue = strBuff.toString();

        return returnValue;
    }

    /**
     * read the stream into an bytarray and return it as ByteArrayInputStream. Use this to ensure all data from
     * an remote stream can be read. Closes the input stream *
     */
    public static ByteArrayInputStream bufferStream(InputStream inputStream) throws IOException {
        byte buf[] = new byte[4096];

        // read as long as something is there to read
        boolean finished = false;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        while (!finished) {
            int availableBytes = inputStream.read(buf);
            finished = (availableBytes < 0);
            if (finished) {
                continue;
            }

            bout.write(buf, 0, availableBytes);
        }
        inputStream.close();

        ByteArrayInputStream returnValue = new ByteArrayInputStream(bout.toByteArray());
        return returnValue;
    }

    /**
     * serialize the data of the object into a byte array so it can be written
     * into an data output stream
     * Note: copy and paste from the FileTools (to avoid extra dependency)
     *
     * @param compress if true, the byte array data is zipped
     */
    public static byte[] convertObjectToBytes(Serializable data, boolean compress) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytes);
        out.writeObject(data);
        out.close();
        byte[] b = bytes.toByteArray();

        if (compress) {
            b = zipData(b);
        }
        return b;
    }

    /**
     * convert the byte data back to an java object.
     */
    public static Serializable convertBytesToObject(byte[] byteData, boolean decompress) throws IOException, ClassNotFoundException {
        if (decompress) {
            byteData = unzipData(byteData);
        }

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(byteData));
        Serializable data = (Serializable) in.readObject();
        return data;
    }

    // Note: copy and paste from the FileTools (to avoid extra dependency)
    public static byte[] zipData(byte[] input) {

        // Create the compressor with highest level of compression
        Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);

        // Give the compressor the data to compress
        compressor.setInput(input);
        compressor.finish();

        // Create an expandable byte array to hold the compressed data.
        // You cannot use an array that's the same size as the orginal because
        // there is no guarantee that the compressed data will be smaller than
        // the uncompressed data.
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);

        // Compress the data
        byte[] buf = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buf);
            bos.write(buf, 0, count);
        }
        try {
            bos.close();
        } catch (IOException e) {
            throw new UnsupportedOperationException("error while zipping", e);
        }

        // Get the compressed data
        byte[] compressedData = bos.toByteArray();

        return compressedData;
    }

    public static byte[] unzipData(byte[] compressedData) {
        // Create the decompressor and give it the data to compress
        Inflater decompressor = new Inflater();
        decompressor.setInput(compressedData);

        // Create an expandable byte array to hold the decompressed data
        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);

        // Decompress the data
        byte[] buf = new byte[1024];
        while (!decompressor.finished()) {
            try {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            } catch (DataFormatException e) {
                throw new UnsupportedOperationException("error while unzipping", e);
            }
        }
        try {
            bos.close();
        } catch (IOException e) {
            throw new UnsupportedOperationException("error while unzipping", e);
        }

        // Get the decompressed data
        byte[] decompressedData = bos.toByteArray();

        return decompressedData;
    }

    /**
     * Decides if an operation should be retried by checking the error it threw
     *
     * @param error
     * @return
     */
    private static boolean isRetryError(Throwable error) {
        while (error != null) {
            String msg = error.getMessage();

            if (error instanceof SocketTimeoutException) {
                // Only retry on read timeouts, not on connect timeouts
                if (msg != null && msg.startsWith("Read")) return true;
            }

            Throwable cause = error.getCause();
            error = cause == error ? null : cause;
        }

        return false;
    }
}
