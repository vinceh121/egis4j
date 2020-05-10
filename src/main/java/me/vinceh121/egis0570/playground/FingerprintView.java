package me.vinceh121.egis0570.playground;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.Timer;

import me.vinceh121.egis0570.Egis;

public class FingerprintView extends JFrame {
	private static final long serialVersionUID = -3376973889631479859L;
	private byte[] data;
	private boolean fingerState;

	public static void main(String[] args) {
		final FingerprintView f = new FingerprintView();
		f.pack();
		f.setVisible(true);
	}

	public FingerprintView() {
		setDefaultCloseOperation(EXIT_ON_CLOSE);

		final Egis egis = new Egis();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> egis.terminate()));
		egis.init();
		try {
			data = egis.readFingerprint();
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		final Timer timer = new Timer(100, e -> {
			try {
				System.out.println("Getting...");
				data = egis.repeatFingerprint();
				fingerState = egis.fingerStatus(data);
				// if (fingerState)
				repaint();
				System.out.println("Done!\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		timer.start();
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (timer.isRunning())
					timer.stop();
				else
					timer.start();
			}
		});
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		if (data == null)
			return;

		g.setColor(fingerState ? Color.RED : Color.BLACK);
		g.drawRect(0, 0, getWidth(), getHeight());

		final int width = 114;
		final int height = 285;

		final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

		final DataBufferByte buf = new DataBufferByte(data, 32512);
		final Raster raster = Raster.createPackedRaster(buf, width, height, 8, new Point());
		img.setData(raster);

		g.drawImage(img, 0, 0, this);
		System.out.println(img.getHeight());
	}

	// @Override
	// public void paint(Graphics g) {
	// if (data == null)
	// return;
	//
	// g.setColor(fingerState ? Color.RED : Color.BLACK);
	// g.drawRect(0, 0, getWidth(), getHeight());
	//
	// final int width = 114;
	// final int height = 285;
	//
	// int maxWidth = 0;
	// int maxHeight = 0;
	//
	// int i = 0;
	// for (int x = 0; x < height; x++) {
	// for (int y = 0; y < width; y++, i++) {
	// try {
	// final int d = data[i];
	// g.setColor(new Color(d));
	// g.drawRect(x, y, 1, 1);
	// } catch (ArrayIndexOutOfBoundsException e) {
	// System.err.println("i = " + i);
	// }
	// maxWidth = Math.max(maxWidth, width);
	// maxHeight = Math.max(maxHeight, height);
	// }
	// }
	// System.out.println("max w: " + maxWidth);
	// System.out.println("max h: " + maxHeight);
	// }

	@Override
	public void paintAll(Graphics g) {
	}

	@Override
	public void paintComponents(Graphics g) {
	}
}
