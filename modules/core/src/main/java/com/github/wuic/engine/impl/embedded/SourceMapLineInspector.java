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


package com.github.wuic.engine.impl.embedded;

import com.github.wuic.engine.EngineRequest;
import com.github.wuic.engine.EngineType;
import com.github.wuic.engine.LineInspector;
import com.github.wuic.engine.NodeEngine;
import com.github.wuic.exception.WuicException;
import com.github.wuic.nut.Nut;
import com.github.wuic.nut.NutDao;
import com.github.wuic.nut.NutsHeap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This inspector removes from scripts the "sourceMappingURL" statement when the chain is going the minify/aggregate it.
 * Otherwise, extracts the source map.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.5
 */
public class SourceMapLineInspector extends LineInspector {

    /**
     * Logger.
     */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /**
     * The engine that uses this inspector.
     */
    private NodeEngine engine;

    /**
     * <p>
     * Builds a new instance.
     * </p>
     *
     * @param enclosingEngine the engine that uses this injector
     */
    public SourceMapLineInspector(final NodeEngine enclosingEngine) {
        super(Pattern.compile("sourceMappingURL=([^\\s]*)"));
        engine = enclosingEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> appendTransformation(final Matcher matcher,
                                          final StringBuilder replacement,
                                          final EngineRequest request,
                                          final NutsHeap heap,
                                          final Nut originalNut) throws WuicException {
        NodeEngine next = engine.getNext();

        while (next != null) {
            // Do not rewrite the statement
            if ((next.getEngineType().equals(EngineType.AGGREGATOR)
                    || next.getEngineType().equals(EngineType.MINIFICATION))
                    && next.works()) {
                return Collections.emptyList();
            }

            next = next.getNext();
        }

        // Extract the nut
        final String referencedPath = matcher.group(1);
        final List<Nut> nuts = heap.create(originalNut, referencedPath, NutDao.PathFormat.RELATIVE_FILE);
        final List<Nut> res;

        if (!nuts.isEmpty()) {
            res = manageAppend(nuts.iterator().next(), replacement, request, heap);
        } else {
            log.warn("{} is referenced as a relative file but not found with in the DAO. Keeping same value...", referencedPath);
            replacement.append(matcher.group());
            res = Collections.emptyList();
        }

        return res;
    }
}
