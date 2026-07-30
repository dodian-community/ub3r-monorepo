package com.osroyale.profile;

import com.osroyale.Model;
import com.osroyale.Rasterizer2D;
import com.osroyale.Rasterizer3D;
import com.osroyale.Sprite;

/**
 * Renders player models away from the live client canvas and fits their complete visible bounds
 * into the fixed account-preview image.
 */
final class ProfilePreviewRenderer
{
	static final int PREVIEW_WIDTH  = 300;
	static final int PREVIEW_HEIGHT = 450;
	static final int PREVIEW_PADDING = 20; // Room for hats, feathers, and capes

	private static final int WORKING_SIZE = 512;
	private static final int MODEL_ROTATION_X = 40;    // pitch – slight forward tilt
	private static final int MODEL_ROTATION_Y = 0;     // yaw   – 0 faces character toward viewer
	private static final int MODEL_TRANSLATION_Y = 160; // shift model down in canvas so head/helm never clips top
	private static final int MODEL_ZOOM = 380;         // camera zoom

	private ProfilePreviewRenderer()
	{
	}

	static Sprite render(Model model)
	{
		if (model == null)
		{
			return new Sprite(PREVIEW_WIDTH, PREVIEW_HEIGHT);
		}

		Sprite working = new Sprite(WORKING_SIZE, WORKING_SIZE);
		RasterState previous = RasterState.capture();
		try
		{
			Rasterizer3D.aBoolean1464 = false;
			Rasterizer3D.world = false;
			Rasterizer2D.initDrawingArea(working.raster, WORKING_SIZE, WORKING_SIZE);
			Rasterizer3D.useViewport();

			int sine   = Model.SINE[MODEL_ROTATION_Y]   * MODEL_ZOOM >> 16;
			int cosine = Model.COSINE[MODEL_ROTATION_Y] * MODEL_ZOOM >> 16;
			Rasterizer3D.renderOnGpu = true;
			// renderModel(pitch, roll, yaw, translationX, translationY, translationZ)
			model.renderModel(MODEL_ROTATION_X, 0, MODEL_ROTATION_Y, 0, MODEL_TRANSLATION_Y, cosine);
		}
		finally
		{
			previous.restore();
		}

		return fitToCanvas(
			working.raster,
			WORKING_SIZE,
			WORKING_SIZE,
			PREVIEW_WIDTH,
			PREVIEW_HEIGHT,
			PREVIEW_PADDING
		);
	}

	static Sprite fitToCanvas(int[] source, int sourceWidth, int sourceHeight,
		int targetWidth, int targetHeight, int padding)
	{
		if (source == null || sourceWidth <= 0 || sourceHeight <= 0
			|| source.length < sourceWidth * sourceHeight)
		{
			throw new IllegalArgumentException("Invalid source preview raster");
		}
		if (targetWidth <= 0 || targetHeight <= 0 || padding < 0
			|| padding * 2 >= targetWidth || padding * 2 >= targetHeight)
		{
			throw new IllegalArgumentException("Invalid target preview dimensions");
		}

		Sprite result = new Sprite(targetWidth, targetHeight);
		int minX = sourceWidth;
		int minY = sourceHeight;
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < sourceHeight; y++)
		{
			int row = y * sourceWidth;
			for (int x = 0; x < sourceWidth; x++)
			{
				if ((source[row + x] & 0x00FFFFFF) == 0)
				{
					continue;
				}
				minX = Math.min(minX, x);
				minY = Math.min(minY, y);
				maxX = Math.max(maxX, x);
				maxY = Math.max(maxY, y);
			}
		}

		if (maxX < minX || maxY < minY)
		{
			return result;
		}

		int contentWidth = maxX - minX + 1;
		int contentHeight = maxY - minY + 1;
		int availableWidth = targetWidth - padding * 2;
		int availableHeight = targetHeight - padding * 2;
		double scale = Math.min(
			availableWidth / (double) contentWidth,
			availableHeight / (double) contentHeight
		);
		int scaledWidth = Math.max(1, Math.min(availableWidth, (int) Math.round(contentWidth * scale)));
		int scaledHeight = Math.max(1, Math.min(availableHeight, (int) Math.round(contentHeight * scale)));
		int targetX = (targetWidth - scaledWidth) / 2;
		int targetY = targetHeight - scaledHeight - padding;

		for (int y = 0; y < scaledHeight; y++)
		{
			int sourceY = minY + Math.min(contentHeight - 1, y * contentHeight / scaledHeight);
			int sourceRow = sourceY * sourceWidth;
			int targetRow = (targetY + y) * targetWidth + targetX;
			for (int x = 0; x < scaledWidth; x++)
			{
				int sourceX = minX + Math.min(contentWidth - 1, x * contentWidth / scaledWidth);
				result.raster[targetRow + x] = source[sourceRow + sourceX];
			}
		}
		return result;
	}

	private static final class RasterState
	{
		private final int[] pixels;
		private final int width;
		private final int height;
		private final int topY;
		private final int bottomY;
		private final int leftX;
		private final int bottomX;
		private final int lastX;
		private final int viewportCenterX;
		private final int viewportCenterY;
		private final int originViewX;
		private final int originViewY;
		private final int[] scanOffsets;
		private final boolean restrictEdges;
		private final boolean world;
		private final boolean renderOnGpu;

		private RasterState()
		{
			pixels = Rasterizer2D.pixels;
			width = Rasterizer2D.width;
			height = Rasterizer2D.height;
			topY = Rasterizer2D.topY;
			bottomY = Rasterizer2D.bottomY;
			leftX = Rasterizer2D.leftX;
			bottomX = Rasterizer2D.bottomX;
			lastX = Rasterizer2D.lastX;
			viewportCenterX = Rasterizer2D.viewportCenterX;
			viewportCenterY = Rasterizer2D.viewportCenterY;
			originViewX = Rasterizer3D.originViewX;
			originViewY = Rasterizer3D.originViewY;
			scanOffsets = Rasterizer3D.scanOffsets;
			restrictEdges = Rasterizer3D.aBoolean1464;
			world = Rasterizer3D.world;
			renderOnGpu = Rasterizer3D.renderOnGpu;
		}

		private static RasterState capture()
		{
			return new RasterState();
		}

		private void restore()
		{
			Rasterizer2D.pixels = pixels;
			Rasterizer2D.width = width;
			Rasterizer2D.height = height;
			Rasterizer2D.topY = topY;
			Rasterizer2D.bottomY = bottomY;
			Rasterizer2D.leftX = leftX;
			Rasterizer2D.bottomX = bottomX;
			Rasterizer2D.lastX = lastX;
			Rasterizer2D.viewportCenterX = viewportCenterX;
			Rasterizer2D.viewportCenterY = viewportCenterY;
			Rasterizer3D.originViewX = originViewX;
			Rasterizer3D.originViewY = originViewY;
			Rasterizer3D.scanOffsets = scanOffsets;
			Rasterizer3D.aBoolean1464 = restrictEdges;
			Rasterizer3D.world = world;
			Rasterizer3D.renderOnGpu = renderOnGpu;
		}
	}
}
