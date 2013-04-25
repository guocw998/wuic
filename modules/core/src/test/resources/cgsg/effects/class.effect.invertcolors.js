/*
 * Copyright (c) 2012  Capgemini Technology Services (hereinafter “Capgemini”)
 *
 * License/Terms of Use
 *
 * Permission is hereby granted, free of charge and for the term of intellectual property rights on the Software, to any
 * person obtaining a copy of this software and associated documentation files (the "Software"), to use, copy, modify
 * and propagate free of charge, anywhere in the world, all or part of the Software subject to the following mandatory conditions:
 *
 *   •    The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  Any failure to comply with the above shall automatically terminate the license and be construed as a breach of these
 *  Terms of Use causing significant harm to Capgemini.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS
 *  OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 *  TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  Except as contained in this notice, the name of Capgemini shall not be used in advertising or otherwise to promote
 *  the use or other dealings in this Software without prior written authorization from Capgemini.
 *
 *  These Terms of Use are subject to French law.
 */

/*
 * Effect to be applied to a CGSGImage or a CGSGAnimatedSprite.
 * @class CGSGEffectInvertColors
 * @module Effect
 * @extends CGSGEffect
 * @constructor
 * @beta
 * @type {CGSGEffectInvertColors}
 */
var CGSGEffectInvertColors = CGSGEffect.extend(
	{
		initialize: function () {
			this._super();
		},

		/**
		 *  This function must be filled by the inherited classes.
		 *  @method render
		 *  @param {CanvasRenderingContext2D} context context containing the image
		 *  @param {Number} width width for the image to be modified
		 *  @param {Number} height height for the image to be modified
		 */
		render: function (context, width, height) {
			try {
				var imageData = context.getImageData(0, 0, width, height);
				var data = imageData.data;

				for (var i = 0; i < data.length; i += 4) {
					data[i] = 255 - data[i]; // red
					data[i + 1] = 255 - data[i + 1]; // green
					data[i + 2] = 255 - data[i + 2]; // blue
					// i+3 = alpha
				}

				// overwrite original image
				context.putImageData(imageData, 0, 0);
			}
			catch (err) {
				//the image is not on the same domain or not on a server
			}
		}
	}
);