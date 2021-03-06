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

import com.github.wuic.NutType;
import com.github.wuic.engine.*;
import com.github.wuic.exception.WuicException;
import com.github.wuic.exception.wrapper.BadArgumentException;
import com.github.wuic.exception.wrapper.StreamException;
import com.github.wuic.nut.ImageNut;
import com.github.wuic.nut.Nut;
import com.github.wuic.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * <p>
 * This engine computes sprites for a set of images. It let the next engine do their job and get the result to analyze
 * a potential aggregated image.
 * </p>
 *
 * @author Guillaume DROUET
 * @version 1.0
 * @since 0.4.4
 */
public class SpriteInspectorEngine extends NodeEngine {

    /**
     * The sprite provider.
     */
    private SpriteProvider[] spriteProviders;

    /**
     * Perform inspection or not.
     */
    private Boolean doInspection;

    /**
     * <p>
     * Builds a new aggregator engine.
     * </p>
     *
     * @param inspect if inspect or not
     * @param sp the provider which generates sprites
     */
    public SpriteInspectorEngine(final Boolean inspect, final SpriteProvider[] sp) {
        spriteProviders = Arrays.copyOf(sp, sp.length);
        doInspection = inspect;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Nut> internalParse(final EngineRequest request) throws WuicException {
        /*
         * If the configuration says that no inspection should be done or if no sprite provider is defined,
         * then we return the given request nuts
         */
        if (!works() || spriteProviders.length == 0) {
            return request.getNuts();
        } else {
            int spriteCpt = 0;
            final List<Nut> res = getNext() == null ? request.getNuts() : getNext().parse(request);
            final List<Nut> retval = new ArrayList<Nut>();
            final String url = IOUtils.mergePath(request.getContextPath(), request.getWorkflowId());

            // Calculate type and dimensions of the final image
            for (final Nut n : res) {
                // Clear previous work
                initSpriteProviders(n.getName());

                if (n.getOriginalNuts() != null) {
                    for (final Nut origin : n.getOriginalNuts()) {
                        if (origin instanceof ImageNut) {
                            addRegionToSpriteProviders(ImageNut.class.cast(origin).getRegion(), origin.getName());
                        } else {
                            throw new BadArgumentException(new IllegalArgumentException("Processed nuts must refer ImageNut instances as original nuts"));
                        }
                    }
                } else {
                    InputStream is = null;

                    try {
                        is = n.openStream();
                        final ImageInputStream iis = ImageIO.createImageInputStream(is);

                        ImageReader reader = ImageIO.getImageReaders(iis).next();
                        reader.setInput(iis);
                        addRegionToSpriteProviders(new Region(0, 0, reader.getWidth(0) - 1, reader.getHeight(0) - 1), n.getName());
                    } catch (IOException ioe) {
                        throw new StreamException(ioe);
                    } finally {
                        IOUtils.close(is);
                    }
                }

                // Process referenced nut
                final String suffix;

                if (request.getPrefixCreatedNut().isEmpty()) {
                    suffix  = String.valueOf(spriteCpt++);
                } else {
                    suffix  = IOUtils.mergePath(request.getPrefixCreatedNut(), String.valueOf(spriteCpt++));
                }

                retval.add(applySpriteProviders(url, request.getHeap().getId(), suffix, n, request));
            }

            return retval;
        }
    }

    /**
     * <p>
     * Initializes all sprite providers.
     * </p>
     *
     * @param name the name
     */
    private void initSpriteProviders(final String name) {
        for (final SpriteProvider sp : spriteProviders) {
            sp.init(name);
        }
    }

    /**
     * <p>
     * Adds the region to all sprite providers.
     * </p>
     *
     * @param region the region
     * @param name the region name
     */
    private void addRegionToSpriteProviders(final Region region, final String name) {
        for (final SpriteProvider sp : spriteProviders) {
            sp.addRegion(region, name);
        }
    }

    /**
     * <p>
     * Generates sprites from all sprite providers and add it to the given nut.
     * </p>
     *
     * @param url the base URL
     * @param heapId the HEAP id
     * @param suffix the name suffix
     * @param n the nut
     * @param request the initial engine request
     * @throws WuicException if generation fails
     */
    private Nut applySpriteProviders(final String url, final String heapId, final String suffix, final Nut n, final EngineRequest request)
            throws WuicException {
        if (spriteProviders.length == 0) {
            return n;
        }

        Nut retval = null;

        for (final SpriteProvider sp : spriteProviders) {
            Nut nut = sp.getSprite(url, request.getWorkflowId(), suffix, Arrays.asList(n));
            final NodeEngine chain = request.getChainFor(nut.getNutType());

            if (chain != null) {
                /*
                 * We perform request by skipping cache to not override cache entry with the given heap ID as key.
                 * We also skip inspection because this is not necessary to detect references to this image
                 */
                final EngineType[] skip = request.alsoSkip(EngineType.CACHE, EngineType.INSPECTOR);
                final List<Nut> parsed = chain.parse(new EngineRequest(heapId, Arrays.asList(nut), request, skip));

                if (retval != null) {
                    n.addReferencedNut(parsed.get(0));
                } else {
                    retval = parsed.get(0);
                    retval.addReferencedNut(n);
                }
            } else if (retval != null) {
                n.addReferencedNut(nut);
            }  else {
                retval = nut;
                retval.addReferencedNut(n);
            }
        }

        return retval == null ? n : retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<NutType> getNutTypes() {
        return Arrays.asList(NutType.PNG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean works() {
        return doInspection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EngineType getEngineType() {
        return EngineType.INSPECTOR;
    }
}
