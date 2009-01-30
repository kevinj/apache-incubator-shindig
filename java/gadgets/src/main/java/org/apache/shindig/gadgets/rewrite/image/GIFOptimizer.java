/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.shindig.gadgets.rewrite.image;

import org.apache.shindig.gadgets.http.HttpResponse;

import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Optimize GIF images by converting them to PNGs or even JPEGs depending on content
 */
public class GIFOptimizer extends PNGOptimizer {

  private boolean usePng;

  public GIFOptimizer(OptimizerConfig config, HttpResponse original)
      throws IOException {
    super(config, original);
  }

  protected void rewriteImpl(BufferedImage image) throws IOException {
    if (image.getColorModel().getTransparency() != Transparency.OPAQUE) {
      // We can rewrite tranparent GIFs to PNG but for IE6 it requires the use of
      // the AlphaImageReader and some pain. Deferring this until that is proven to work
      // Opacity check is valid as GIF always produces IndexColorModel

      // Re-palettize and write to stip metadata
      write(ImageUtils.palettize(image, config.getMaxPaletteSize()));
    } else {
      usePng = true;
      writer = ImageIO.getImageWritersByFormatName("png").next();
      super.rewriteImpl(image);
    }
  }

  @Override
  protected String getOriginalContentType() {
    return "image/gif";
  }
}
