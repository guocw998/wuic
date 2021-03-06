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


package com.github.wuic.test.dao;

import com.github.wuic.NutType;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.AbstractNutDao;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutDaoListener;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * Test for features provided by {@link com.github.wuic.nut.AbstractNutDao}.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.2
 * @since 0.4.0
 */
@RunWith(JUnit4.class)
public class AbstractNutDaoTest {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * <p>
     * A mocked DAO for tests.
     * </p>
     *
     * @author Guillaume DROUET
     * @version 1.0
     * @since 0.4.0
     */
    private class MockNutDaoTest extends AbstractNutDao {

        /**
         * Age of last nut.
         */
        private Long age;

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param pollingSeconds polling interleave
         */
        private MockNutDaoTest(final int pollingSeconds) {
            super("/", false, new String[] { "1", "2", "3", "4", }, pollingSeconds, false);
            age = System.currentTimeMillis();
        }

        /**
         * <p>
         * Creates a new instance.
         * </p>
         *
         * @param contentBasedVersionNumber use version number computed from content
         */
        private MockNutDaoTest(final Boolean contentBasedVersionNumber) {
            super("/", false, new String[] { "1", "2", "3", "4", }, -1, contentBasedVersionNumber);
            age = System.currentTimeMillis();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected List<String> listNutsPaths(final String pattern) throws StreamException {
            return Arrays.asList("foo.js");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Nut accessFor(final String realPath, final NutType type) throws StreamException {
            final Nut mock = mock(Nut.class);
            when(mock.getName()).thenReturn(realPath);
            when(mock.getVersionNumber()).thenReturn(new BigInteger("1"));

            return mock;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Long getLastUpdateTimestampFor(final String path) throws StreamException {
            if (age + 1500L < System.currentTimeMillis()) {
                age = System.currentTimeMillis();
            }

            logger.info("getLastUpdateTimestampFor(java.lang.String) returns {}", age);

            return age;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream newInputStream(final String path) throws StreamException {
            final AtomicInteger bits = new AtomicInteger(4000);

            return new InputStream() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public int read() throws IOException {
                    return bits.decrementAndGet();
                }
            };
        }
    }

    /**
     * Test version number computation.
     *
     * @throws Exception e
     */
    @Test
    public void versionNumberTest() throws Exception  {
        final NutDao first = new MockNutDaoTest(true);
        final NutDao second = new MockNutDaoTest(true);
        final NutDao third = new MockNutDaoTest(false);
        final NutDao fourth = new MockNutDaoTest(false);

        Assert.assertEquals(first.create("").get(0).getVersionNumber(), second.create("").get(0).getVersionNumber());
        Assert.assertNotSame(second.create("").get(0).getVersionNumber(), third.create("").get(0).getVersionNumber());
        Assert.assertEquals(third.create("").get(0).getVersionNumber(), fourth.create("").get(0).getVersionNumber());
    }

    /**
     * Test for poll scheduling.
     *
     * @throws Exception if test fails
     */
    @Test
    public void pollSchedulingTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final long start = System.currentTimeMillis();
        final NutDao dao = new MockNutDaoTest(1);
        dao.create("");
        dao.observe("", new NutDaoListener() {
            @Override
            public boolean polling(final Set<String> paths) {
                return true;
            }

            @Override
            public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
                logger.info("Nut updated");
                synchronized (AbstractNutDaoTest.class) {
                    if (count.incrementAndGet() > 1) {
                        AbstractNutDaoTest.class.notify();
                    }
                }

                return true;
            }
        });

        synchronized (AbstractNutDaoTest.class) {
            AbstractNutDaoTest.class.wait(5000L);
        }

        final long duration = System.currentTimeMillis() - start;
        logger.info("Polling took {} ms", duration);

        Assert.assertTrue(duration < 4500L);
    }

    /**
     * <p>
     * Test when stop polling.
     * </p>
     *
     * @throws Exception if test fails
     */
    @Test
    public void stopPollingTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final MockNutDaoTest dao = new MockNutDaoTest(1);
        dao.create("");
        dao.observe("", new NutDaoListener() {
            @Override
            public boolean polling(Set<String> paths) {
                return true;
            }

            @Override
            public boolean nutPolled(NutDao dao, String path, Long timestamp) {
                logger.info("Nut updated");
                synchronized (AbstractNutDaoTest.class) {
                    count.incrementAndGet();
                }

                return true;
            }
        });

        Thread.sleep(1500L);
        dao.setPollingInterleave(-1);
        Thread.sleep(2500L);
        Assert.assertEquals(count.intValue(), 1);
    }

    /**
     * <p>
     * Tests that a listener is not called when it asks for exclusion.
     * </p>
     *
     * @throws Exception
     */
    @Test
    public void excludePollingListenerTest() throws Exception {
        final AtomicInteger count = new AtomicInteger(0);
        final MockNutDaoTest dao = new MockNutDaoTest(1);
        dao.create("1");
        dao.create("2");

        final NutDaoListener listener = new NutDaoListener() {
            @Override
            public boolean polling(final Set<String> paths) {
                logger.info("Polling nut");
                synchronized (AbstractNutDaoTest.class) {
                    count.incrementAndGet();
                }

                // Ask for exclusion by returning false
                return false;
            }

            @Override
            public boolean nutPolled(final NutDao dao, final String path, final Long timestamp) {
                return true;
            }
        };

        dao.observe("1", listener);
        dao.observe("2", listener);

        Thread.sleep(2500L);
        Assert.assertEquals(count.intValue(), 2);
    }

    /**
     * Concurrent test.
     *
     * @throws Exception if test fails
     */
    @Test
    public void concurrentTest() throws Exception {
        final AbstractNutDao dao = new MockNutDaoTest(1);
        final Nut nut = mock(Nut.class);
        when(nut.getName()).thenReturn("mock");
        when(nut.getVersionNumber()).thenReturn(new BigInteger("1"));

        for (int i = 0; i < 750; i++) {
            new Thread(new Runnable() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void run() {
                    try {
                        dao.create("");
                        dao.observe("", new NutDaoListener() {
                            @Override
                            public boolean polling(Set<String> paths) {
                                return false;
                            }

                            @Override
                            public boolean nutPolled(NutDao dao, String path, Long timestamp) {
                                return false;
                            }
                        });

                        dao.setPollingInterleave(2);
                        dao.getPollingInterleave();
                        dao.computeRealPaths("", NutDao.PathFormat.ANY);
                        dao.proxyUriFor(nut);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Assert.fail(e.getMessage());
                    }
                }
            }).start();
        }

        Thread.sleep(1500L);
    }
}
