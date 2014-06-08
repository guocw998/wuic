/*
 * "Copyright (c) 2014   Capgemini Technology Services (hereinafter "Capgemini")
 *
 * License/Terms of Use
 * Permission is hereby granted, free of charge and for the term of intellectual
 * property rights on the Software, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to use, copy, modify and
 * propagate free of charge, anywhere in the world, all or part of the Software
 * subject to the following mandatory conditions:
 *
 * -   The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Any failure to comply with the above shall automatically terminate the license
 * and be construed as a breach of these Terms of Use causing significant harm to
 * Capgemini.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, PEACEFUL ENJOYMENT,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 * OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Capgemini shall not be used in
 * advertising or otherwise to promote the use or other dealings in this Software
 * without prior written authorization from Capgemini.
 *
 * These Terms of Use are subject to French law.
 *
 * IMPORTANT NOTICE: The WUIC software implements software components governed by
 * open source software licenses (BSD and Apache) of which CAPGEMINI is not the
 * author or the editor. The rights granted on the said software components are
 * governed by the specific terms and conditions specified by Apache 2.0 and BSD
 * licenses."
 */


package com.github.wuic.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * <p>
 * Wraps an {@link HttpServletResponse} to capture the written stream.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class ByteArrayHttpServletResponseWrapper extends HttpServletResponseWrapper {

    /**
     * <p>
     * Extends {@link ServletOutputStream} and stores the stream in a wrapped byte array.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.4
     */
    private final class ByteArrayServletStream extends ServletOutputStream {

        /**
         * The byte array.
         */
        private ByteArrayOutputStream baos;

        /**
         * <p>
         * Builds a new instance.
         * </p>
         *
         * @param baos the wrapped byte array
         */
        private ByteArrayServletStream(final ByteArrayOutputStream baos) {
            this.baos = baos;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write(final int param) throws IOException {
            baos.write(param);
        }
    }

    /**
     * Wrapped byte array.
     */
    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    /**
     * Print writer built on top of byte array.
     */
    private PrintWriter pw = new PrintWriter(baos);

    /**
     * Servlet output stream built on top of byte array.
     */
    private ServletOutputStream sos = new ByteArrayServletStream(baos);

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param response the response to wrap
     */
    public ByteArrayHttpServletResponseWrapper(final HttpServletResponse response) {
        super(response);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return sos;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PrintWriter getWriter() throws IOException {
        return pw;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDateHeader(final String name, final long date) {
        if (!skipHeader(name)) {
            super.setDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIntHeader(final String name, final int value) {
        if (!skipHeader(name)) {
            super.setIntHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addHeader(final String name, String value) {
        if (!skipHeader(name)) {
            super.addHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHeader(final String name, String value) {
        if (!skipHeader(name)) {
            super.setHeader(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addDateHeader(final String name, final long date) {
        if (!skipHeader(name)) {
            super.addDateHeader(name, date);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addIntHeader(final String name, final int value) {
        if (!skipHeader(name)) {
            super.addIntHeader(name, value);
        }
    }

    /**
     * <p>
     * Currently skips 'Last-Modified' and 'ETag' header to evict 304 (not modified) status. This WUIC, servlet filter
     * will always serve an up to date content.
     * </p>
     *
     * @param name the header name
     * @return {@code false} if header should be set, {@code true otherwise}
     */
    private boolean skipHeader(final String name) {
        return "last-modified".equalsIgnoreCase(name) || "etag".equalsIgnoreCase(name);
    }

    /**
     * <p>
     * Gets the byte array.
     * </p>
     *
     * @return the byte array
     */
    public byte[] toByteArray() {
        return baos.toByteArray();
    }
}
