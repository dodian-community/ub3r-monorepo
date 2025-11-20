package org.seven.scene.graphic;
import com.runescape.draw.Rasterizer2D;
import com.runescape.draw.Rasterizer3D;

public class Fog {    
    
    /**
     * Sets the fog color.
     */
    public static final int FOG_COLOR = 0xD3D3D3;
    
    /**
     * 
     * @param fogStartDistance
     * @param fogEndDistance
     * @param fogIntensity
     */
    public void renderFog(boolean belowGround, int fogStartDistance, int fogEndDistance, int fogIntensity) {
        int pos = Rasterizer3D.scanOffsets[0];
        int src, dst, alpha;
        int fogBegin = (int) (fogStartDistance);
        int fogEnd = (int) (fogEndDistance);
        for (int y = 0; y < Rasterizer2D.bottomY; y++) {
            for (int x = 0; x < Rasterizer2D.lastX; x++) {
                if (Rasterizer2D.depthBuffer[pos] >= fogEnd) {
                    Rasterizer2D.pixels[pos] = FOG_COLOR;
                } else if (Rasterizer2D.depthBuffer[pos] >= fogBegin) {
                    alpha = (int)(Rasterizer2D.depthBuffer[pos] - fogBegin) / fogIntensity;
                    src = ((FOG_COLOR & 0xff00ff) * alpha >> 8 & 0xff00ff) + ((FOG_COLOR & 0xff00) * alpha >> 8 & 0xff00);
                    alpha = 256 - alpha;
                    dst = Rasterizer2D.pixels[pos];
                    dst = ((dst & 0xff00ff) * alpha >> 8 & 0xff00ff) + ((dst & 0xff00) * alpha >> 8 & 0xff00);
                    Rasterizer2D.pixels[pos] = src + dst;
                }
                pos++;
            }
            pos += Rasterizer2D.width - Rasterizer2D.lastX;
        }
    }
}
