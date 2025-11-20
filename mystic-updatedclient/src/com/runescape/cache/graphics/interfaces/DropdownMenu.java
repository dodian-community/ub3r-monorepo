package com.runescape.cache.graphics.interfaces;

import org.seven.cache.graphics.RSFont;

import com.runescape.Client;
import com.runescape.cache.graphics.GameFont;
import com.runescape.cache.graphics.Sprite;
import com.runescape.cache.graphics.Widget;
import com.runescape.draw.Rasterizer2D;

public class DropdownMenu {
	 private final int width;
	    private final String[] options;
	    private final Dropdown dropdown;
	    private boolean open;
	    private String selected;
	    private final int defaultOption;
	    protected int scroll;

	    private static final int SELECT_HEIGHT = 13;

	    public DropdownMenu(int width, int defaultOption, Dropdown dropwdown, String... options) {
	        this.width = width;
	        this.options = options;
	        this.selected = defaultOption == -1 ? "Select an option" : options[defaultOption];
	        this.defaultOption = defaultOption;
	        this.open = false;
	        this.dropdown = dropwdown;
	    }

	    public int getWidth() {
	        return this.width;
	    }

	    public String[] getOptions() {
	        return this.options;
	    }

	    public boolean isOpen() {
	        return this.open;
	    }

	    public void setOpen(boolean b) {
	        this.open = b;
	    }

	    public void setSelected(String selected) {
	        this.selected = selected;
	    }

	    public Dropdown getDrop() {
	        return this.dropdown;
	    }

	    private void drawDropBox(Widget child, int x, int y, boolean hover) {
	        int bgColor = hover ? 0x4C4237 : 0x483E33;
	        int textColor = hover ? 0xFFB83F : 0xFD961E;

	        Rasterizer2D.drawBox(x + 1, (y - 1), width - 2, 22, 0);
	        Rasterizer2D.drawBox(x + 2, y, width - 4, 20, bgColor);
	        Rasterizer2D.drawBoxOutline(x + 2, y , width - 4, 19, 0x474745);

	        RSFont font = Client.instance.newSmallFont;
	        String message = !child.disabledMessage.isEmpty() ? child.disabledMessage : selected;

	        x += child.centerText ? width / 2 - 2 : 6;
	        font.drawCenteredString(message, x, y + 15, textColor, 0);
	    }

	    private void drawOpenBox(Widget child, int x, int y) {
	        Sprite scrollBar = Client.cacheSprite[545];
	        if (child.hovered) {
	            scrollBar.drawTransparentSprite(x + width - 19, y + 2, 125);
	        } else {
	            scrollBar.drawSprite(x + width - 19, y + 2, 0);
	        }
	        //Rasterizer2D.drawBox(x + width - 21, y + 3, 17, 17, 0);
	        int len = options.length;
	        if (len > 5) len = 5;
	        Rasterizer2D.drawBox(x + 1, y + 19, width - 2, SELECT_HEIGHT * len + 3, 0);

	        int yy = 2;
	        RSFont font = Client.instance.newSmallFont;
	        for (int i = 0; i < len; i++) {
	            int idx = i + scroll;
	            int color = child.dropDownHover == idx ? 0x777067 : 0x534A3E;
	            Rasterizer2D.drawBox(x + 2, y + yy + 19, width - 4 - (options.length > 5 ? 8 : 0), SELECT_HEIGHT, color);
	           // Client.instance.drawScrollbar(child.height, child.scrollPosition,
				//		y, x + child.width, child.scrollMax,
				//		false);
	            String option = options[idx];
	            int textColor = child.dropDownHover == idx ? 0xFFB83F : 0xFD961E;

	            if (child.centerText) {
	                font.drawCenteredString(option, x + width / 2, y + 31 + yy, textColor, 0);
	            } else {
	                font.drawBasicString(option, x + 6, y + 31 + yy, textColor, 0);
	            }

	            yy += 13;
	        }

	        if (options.length > 5) {
	            int hidden = options.length - 5;
	            int scrollLength = 65 * hidden / options.length;
	            int scrollPos = (65 - scrollLength) * scroll / hidden;
	            Rasterizer2D.drawBox(x + width - 9, y + 24, 7, 13 * len, 0x534A3E);
	            Rasterizer2D.drawBox(x + width - 9, y + 25 + scrollPos, 7, scrollLength - 1, 0x777067);
	        }
	    }

	    public void drawDropdown(Widget child, int x, int y) {
	        DropdownMenu d = child.dropDown;
	        drawDropBox(child, x, y, child.hovered || open);

	        if (open) {
	            drawOpenBox(child, x, y);
	        } else {
	            Sprite scrollBar = Client.cacheSprite[545];
	            if (child.hovered) {
	                scrollBar.drawTransparentSprite(x + width - 19, y + 2, 125);
	            } else {
	                scrollBar.drawSprite(x + width - 19, y + 2, 0);
	            }
	           // Rasterizer2D.drawBox(x + width - 21, y + 4, 17, 17, 0);
	        }
	    }

	    public void hover(Widget parent, Widget child, int hoverX, int hoverY, int xBounds, int yBounds) {
	        if (hoverX < xBounds || hoverY < yBounds + 20) {
	            child.dropDownHover = -1;
	            return;
	        }

	        int height = 13;
	        if (options.length > 5) {
	            height *= 5;
	        } else height *= options.length;

	        if (hoverX > xBounds + width || hoverY >= yBounds + 21 + height) {
	            child.dropDownHover = -1;
	            return;
	        }

	        int yy = (yBounds + 21 + height + SELECT_HEIGHT) - hoverY;

	        int len = options.length;
	        if (len > 5) len = 5;

	        int shit = yy / SELECT_HEIGHT;
	        if (shit > len) shit = len;

	        child.dropDownHover = (len - shit) + scroll;

	        Client client = Client.instance;
	        client.menuActionText[client.menuActionRow] = "Select " + options[child.dropDownHover];
	        client.menuActionTypes[client.menuActionRow] = 770;
	        client.selectedMenuActions[client.menuActionRow] = child.id;
	        client.firstMenuAction[client.menuActionRow] = child.dropDownHover;
	        client.secondMenuAction[client.menuActionRow] = parent.id;
	        client.menuActionRow++;
	    }

}
