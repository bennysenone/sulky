/*
 * sulky-modules - several general-purpose modules.
 * Copyright (C) 2007-2018 Joern Huxhorn
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Copyright 2007-2018 Joern Huxhorn
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.huxhorn.sulky.swing;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Inspired by Filthy Rich Clients...
 */
public final class GraphicsUtilities
{
	private GraphicsUtilities()
	{}

	private static GraphicsConfiguration getConfiguration()
	{
		return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
	}

	public static BufferedImage createCompatibleImage(BufferedImage image)
	{
		return createCompatibleImage(image, image.getWidth(), image.getHeight());
	}

	public static BufferedImage createCompatibleImage(BufferedImage image, int width, int height)
	{
		return getConfiguration().createCompatibleImage(width, height, image.getTransparency());
	}

	public static BufferedImage createCompatibleImage(int width, int height, int transparency)
	{
		return getConfiguration().createCompatibleImage(width, height, transparency);
	}

	public static BufferedImage createOpaqueCompatibleImage(int width, int height)
	{
		return createCompatibleImage(width, height, Transparency.OPAQUE);
	}

	public static BufferedImage createTranslucentCompatibleImage(int width, int height)
	{
		return createCompatibleImage(width, height, Transparency.TRANSLUCENT);
	}

	public static BufferedImage toCompatibleImage(BufferedImage image)
	{
		GraphicsConfiguration gc = getConfiguration();
		if(image.getColorModel().equals(gc.getColorModel()))
		{
			return image;
		}
		return createCompatibleCopy(image);
	}

	public static BufferedImage createCompatibleCopy(BufferedImage image)
	{
		BufferedImage result = createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());

		Graphics g = result.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return result;
	}

	/**
	 * Draws the highlight for the given shape. The highlight is painted using the current color of g2.
	 *
	 * @param g2 the graphics context.
	 * @param s the shape.
	 * @param size the size of the highlight.
	 * @param opacity the opacity of the highlight.
	 */
	public static void drawHighlight(Graphics2D g2, Shape s, int size, float opacity)
	{
		if(Float.compare(opacity, 0.0f) == 0)
		{
			// return if transparent
			return;
		}

		final Logger logger = LoggerFactory.getLogger(GraphicsUtilities.class);
		Composite c = g2.getComposite();
		for(int i = -size; i <= size; i++)
		{
			for(int j = -size; j <= size; j++)
			{
				float distance = i * i + j * j;
				float alpha = opacity;
				if(Float.compare(distance, 0.0f) > 0)
				{
					alpha = (1.0f / distance) * opacity * size;
					if(logger.isDebugEnabled()) logger.debug("Calculated alpha: {}", alpha);
				}
				if(alpha > 1.0)
				{
					if(logger.isDebugEnabled()) logger.debug("Corrected alpha: {}", alpha);
					alpha = 1.0f;
				}
				//g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
				g2.translate(i, j);
				g2.fill(s);
				g2.translate(-i, -j);
			}
		}
		g2.setComposite(c);
	}

	/**
	 * <p>Returns an array of pixels, stored as integers, from a
	 * <code>BufferedImage</code>. The pixels are grabbed from a rectangular
	 * area defined by a location and two dimensions. Calling this method on
	 * an image of type different from <code>BufferedImage.TYPE_INT_ARGB</code>
	 * and <code>BufferedImage.TYPE_INT_RGB</code> will unmanage the image.</p>
	 *
	 * @param img    the source image
	 * @param x      the x location at which to start grabbing pixels
	 * @param y      the y location at which to start grabbing pixels
	 * @param w      the width of the rectangle of pixels to grab
	 * @param h      the height of the rectangle of pixels to grab
	 * @param pixels a pre-allocated array of pixels of size w*h; can be null
	 * @return <code>pixels</code> if non-null, a new array of integers
	 *         otherwise
	 * @throws IllegalArgumentException is <code>pixels</code> is non-null and
	 *                                  of length &lt; w*h
	 */
	public static int[] getPixels(BufferedImage img,
	                              int x, int y, int w, int h, int[] pixels)
	{
		if(w == 0 || h == 0)
		{
			return new int[0];
		}

		if(pixels == null)
		{
			pixels = new int[w * h];
		}
		else if(pixels.length < w * h)
		{
			throw new IllegalArgumentException("pixels array must have a length" +
				" >= w*h");
		}

		int imageType = img.getType();
		if(imageType == BufferedImage.TYPE_INT_ARGB ||
			imageType == BufferedImage.TYPE_INT_RGB)
		{
			Raster raster = img.getRaster();
			return (int[]) raster.getDataElements(x, y, w, h, pixels);
		}

		// Unmanages the image
		return img.getRGB(x, y, w, h, pixels, 0, w);
	}

	/**
	 * <p>Writes a rectangular area of pixels in the destination
	 * <code>BufferedImage</code>. Calling this method on
	 * an image of type different from <code>BufferedImage.TYPE_INT_ARGB</code>
	 * and <code>BufferedImage.TYPE_INT_RGB</code> will unmanage the image.</p>
	 *
	 * @param img    the destination image
	 * @param x      the x location at which to start storing pixels
	 * @param y      the y location at which to start storing pixels
	 * @param w      the width of the rectangle of pixels to store
	 * @param h      the height of the rectangle of pixels to store
	 * @param pixels an array of pixels, stored as integers
	 * @throws IllegalArgumentException is <code>pixels</code> is non-null and
	 *                                  of length &lt; w*h
	 */
	public static void setPixels(BufferedImage img,
	                             int x, int y, int w, int h, int[] pixels)
	{
		if(pixels == null || w == 0 || h == 0)
		{
			return;
		}
		else if(pixels.length < w * h)
		{
			throw new IllegalArgumentException("pixels array must have a length" +
				" >= w*h");
		}

		int imageType = img.getType();
		if(imageType == BufferedImage.TYPE_INT_ARGB ||
			imageType == BufferedImage.TYPE_INT_RGB)
		{
			WritableRaster raster = img.getRaster();
			raster.setDataElements(x, y, w, h, pixels);
		}
		else
		{
			// Unmanages the image
			img.setRGB(x, y, w, h, pixels, 0, w);
		}
	}

	public static BufferedImage loadCompatibleImage(URL resource)
		throws IOException
	{
		BufferedImage image = ImageIO.read(resource);
		return toCompatibleImage(image);
	}
}

