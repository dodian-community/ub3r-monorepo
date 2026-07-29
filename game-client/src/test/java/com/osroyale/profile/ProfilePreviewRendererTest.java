package com.osroyale.profile;

import com.osroyale.Sprite;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfilePreviewRendererTest
{
	@Test
	public void tallModelIsCenteredAndFullyContainedByThePreviewPadding()
	{
		int[] source = filledRectangle(80, 120, 30, 5, 49, 114, 0x004477AA);

		Sprite preview = ProfilePreviewRenderer.fitToCanvas(source, 80, 120, 100, 150, 6);
		Bounds bounds = visibleBounds(preview.raster, 100, 150);

		assertContained(bounds, 100, 150, 6);
		assertTrue("tall model should use the available preview height", bounds.height() >= 136);
		assertEquals(20.0 / 110.0, bounds.width() / (double) bounds.height(), 0.03);
	}

	@Test
	public void wideEquipmentIsScaledWithoutCropping()
	{
		int[] source = filledRectangle(180, 100, 2, 20, 177, 79, 0x00AA5533);

		Sprite preview = ProfilePreviewRenderer.fitToCanvas(source, 180, 100, 100, 150, 6);
		Bounds bounds = visibleBounds(preview.raster, 100, 150);

		assertContained(bounds, 100, 150, 6);
		assertTrue("wide equipment should use the available preview width", bounds.width() >= 86);
		assertEquals(176.0 / 60.0, bounds.width() / (double) bounds.height(), 0.12);
	}

	@Test
	public void emptyRenderProducesATransparentPreview()
	{
		Sprite preview = ProfilePreviewRenderer.fitToCanvas(new int[16], 4, 4, 100, 150, 6);

		for (int pixel : preview.raster)
		{
			assertEquals(0, pixel);
		}
	}

	private static int[] filledRectangle(int width, int height,
		int minX, int minY, int maxX, int maxY, int colour)
	{
		int[] pixels = new int[width * height];
		for (int y = minY; y <= maxY; y++)
		{
			for (int x = minX; x <= maxX; x++)
			{
				pixels[y * width + x] = colour;
			}
		}
		return pixels;
	}

	private static Bounds visibleBounds(int[] pixels, int width, int height)
	{
		int minX = width;
		int minY = height;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if ((pixels[y * width + x] & 0x00FFFFFF) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		return new Bounds(minX, minY, maxX, maxY);
	}

	private static void assertContained(Bounds bounds, int width, int height, int padding)
	{
		assertTrue(bounds.minX >= padding);
		assertTrue(bounds.minY >= padding);
		assertTrue(bounds.maxX < width - padding);
		assertTrue(bounds.maxY < height - padding);
	}

	private static final class Bounds
	{
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;

		private Bounds(int minX, int minY, int maxX, int maxY)
		{
			this.minX = minX;
			this.minY = minY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		private int width()
		{
			return maxX - minX + 1;
		}

		private int height()
		{
			return maxY - minY + 1;
		}
	}
}
