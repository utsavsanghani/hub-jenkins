package com.blackducksoftware.integration.hub.jenkins.scan;

import hudson.model.BuildListener;

import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.blackducksoftware.integration.hub.jenkins.HubJenkinsLogger;

public class ScannerSplitStream extends OutputStream {

    // https://www.cs.cmu.edu/~pattis/15-1XX/common/handouts/ascii.html

    private static final int EOF = -1; // End of file

    private static final int ETX = 3; // End of text, should have not more data

    private static final int EOT = 4; // End of transmission, no more data

    private static final int LF = 10; // Line feed, new line

    private static final int CR = 13; // Carriage return

    private static final String ERROR = "ERROR:";

    private static final String WARN = "WARN:";

    private static final String INFO = "INFO:";

    private static final String DEBUG = "DEBUG:";

    private static final String TRACE = "TRACE:";

    private final StringBuilder outputBuilder = new StringBuilder();

    private final OutputStream outputFileStream;

    private final HubJenkinsLogger logger;

    private ByteBuffer buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();

    public ScannerSplitStream(BuildListener listener, OutputStream outputFileStream) {
        this.outputFileStream = outputFileStream;
        logger = new HubJenkinsLogger(listener);
    }

    public String getOutput() {
        return outputBuilder.toString();
    }

    public Boolean hasOutput() {
        return outputBuilder != null && outputBuilder.length() > 0;
    }

    @Override
    public void write(int b) throws IOException {
        outputFileStream.write(b);
        if (buffer.capacity() - buffer.position() <= 4) {
            ByteBuffer newBuffer = (ByteBuffer) ByteBuffer.allocate(buffer.capacity() + 512).mark();

            newBuffer.put(buffer);

            buffer = newBuffer;
        }
        String string = "";
        switch (b) {
        case ETX:
            string = new String(buffer.array(), "UTF-8");
            writeToConsole(string);
            buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
            return;
        case EOT:
            string = new String(buffer.array(), "UTF-8");
            writeToConsole(string);
            buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
            return;
        case LF:
            // FIXME info logs and error logs may take multiple lines
            string = new String(buffer.array(), "UTF-8");
            if (string.contains(ERROR) || string.contains(WARN) || string.contains(INFO) || string.contains(DEBUG) || string.contains(TRACE)) {
                writeToConsole(string);
                buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
            }
            return;
        case CR:
            string = new String(buffer.array(), "UTF-8");
            writeToConsole(string);
            buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
            return;
        case EOF:
            throw new EOFException();
        default:
            buffer.putInt(b);
            return;
        }
    }

    @Override
    public void write(byte[] byteArray) throws IOException {
        outputFileStream.write(byteArray);

        // for (byte b : byteArray) {
        // write(b);
        // }
        String string = new String(byteArray, "UTF-8");

        if (buffer.position() == 0) {
            // Start of log message, store in buffer till the next log message
            buffer.put(byteArray);
        } else if (!string.contains(ERROR) && !string.contains(WARN) && !string.contains(INFO) &&
                !string.contains(DEBUG) && !string.contains(TRACE)) {
            // part of the stored log message, needs to be added into the buffer
            if (buffer.capacity() - buffer.position() <= 4) {
                ByteBuffer newBuffer = (ByteBuffer) ByteBuffer.allocate(buffer.capacity() + 512).mark();

                newBuffer.put(buffer);

                buffer = newBuffer;
            }
            buffer.put(byteArray);
        } else {
            // next real log message came in, print the log in the buffer
            string = new String(buffer.array(), "UTF-8");
            writeToConsole(string);

            buffer = (ByteBuffer) ByteBuffer.allocate(512).mark();
            buffer.put(byteArray);

        }

        // TODO will need to do a flush to print the last logs
    }

    @Override
    public void write(byte[] byteArray, int offset, int length) throws IOException {
        outputFileStream.write(byteArray, offset, length);

        String string = new String(byteArray, offset, length, "UTF-8");
        if (!string.contains(ERROR) && !string.contains(WARN) && !string.contains(INFO) &&
                !string.contains(DEBUG) && !string.contains(TRACE)) {
            if (buffer.capacity() - buffer.position() <= 4) {
                ByteBuffer newBuffer = (ByteBuffer) ByteBuffer.allocate(buffer.capacity() + 512).mark();

                newBuffer.put(buffer);

                buffer = newBuffer;
            }

            buffer.put(byteArray);
        } else {
            writeToConsole(string);
        }
    }

    private void writeToConsole(String line) {

        if (line.contains("Exception ")) {
            // looking for 'Exception in thread' type messages
            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.error(line);
        } else if (line.contains("Finished in")) {
            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.info(line);
        } else if (line.contains(ERROR)) {
            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.error(line);
        } else if (line.contains(WARN)) {
            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.warn(line);
        } else if (line.contains(INFO)) {
            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.info(line);
        }
        else if (!line.contains(DEBUG) && !line.contains(TRACE)) {
            // TODO still including parts of debug messages

            outputBuilder.append(line + System.getProperty("line.separator"));
            logger.info(line);
        }

    }
}
