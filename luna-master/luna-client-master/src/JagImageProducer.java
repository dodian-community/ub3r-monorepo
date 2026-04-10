// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

public class JagImageProducer implements ImageProducer, ImageObserver {

	public JagImageProducer(int _width, int _height, Component component) {
		width = _width;
		height = _height;
		pixels = new int[_width * _height];
		colorModel = new DirectColorModel(32, 0xff0000, 65280, 255);
		image = component.createImage(this);
		method232();
		component.prepareImage(image, this);
		method232();
		component.prepareImage(image, this);
		method232();
		component.prepareImage(image, this);
		method230();
	}

	public void method230() {
		Drawable.method444(width, height, pixels);
	}

	public void method231(int x, int y, Graphics g) {
		method232();
		g.drawImage(image, x, y, this);
	}

	public synchronized void addConsumer(ImageConsumer imageconsumer) {
		consumer = imageconsumer;
		imageconsumer.setDimensions(width, height);
		imageconsumer.setProperties(null);
		imageconsumer.setColorModel(colorModel);
		imageconsumer.setHints(14);
	}

	public synchronized boolean isConsumer(ImageConsumer imageconsumer) {
		return consumer == imageconsumer;
	}

	public synchronized void removeConsumer(ImageConsumer imageconsumer) {
		if (consumer == imageconsumer)
			consumer = null;
	}

	public void startProduction(ImageConsumer imageconsumer) {
		addConsumer(imageconsumer);
	}

	public void requestTopDownLeftRightResend(ImageConsumer imageconsumer) {
		System.out.println("TDLR");
	}

	public synchronized void method232() {
		if (consumer == null) {
			return;
		} else {
			consumer.setPixels(0, 0, width, height, colorModel, pixels, 0, width);
			consumer.imageComplete(2);
			return;
		}
	}

	public boolean imageUpdate(Image image, int i, int j, int k, int l, int i1) {
		return true;
	}

	public int pixels[];
	public int width;
	public int height;
	public ColorModel colorModel;
	public ImageConsumer consumer;
	public Image image;
}
