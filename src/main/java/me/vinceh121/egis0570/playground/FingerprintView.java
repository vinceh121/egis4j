package me.vinceh121.egis0570.playground;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import me.vinceh121.egis0570.Egis;
import me.vinceh121.egis0570.Egis.EgisMode;

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
		setLayout(new FlowLayout());

		final Egis egis = new Egis();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> egis.terminate()));
		egis.init();
		egis.setDefaultsForReading();

		final FingerprintComponent fpc = new FingerprintComponent();
		add(fpc);

		final JButton btnRegis = new JButton("Edit register value");
		btnRegis.addActionListener(e -> {
			int reg = Integer.parseInt(JOptionPane.showInputDialog("Register (hex, no prefix):"), 16);
			int val = Integer.parseInt(
					JOptionPane.showInputDialog("Value for " + Integer.toHexString(reg) + " (hex, no prefix):"),
					16);
			egis.writeRegister(reg, val);
		});
		add(btnRegis);

		final JSlider slGain = new JSlider(0, Byte.MAX_VALUE);
		slGain.setToolTipText("Small gain");
		slGain.setValue(egis.getSmallGain());
		slGain.addChangeListener(e -> {
			egis.setSmallGain(slGain.getValue());
		});
		add(slGain);

		final JTextField flWidth = new JTextField(String.valueOf(Egis.IMG_WIDTH));
		flWidth.addActionListener(e -> {
			fpc.setWidth(Integer.parseInt(flWidth.getText()));
		});
		add(flWidth);

		final JTextField flHeight = new JTextField(String.valueOf(Egis.IMG_HEIGHT));
		flHeight.addActionListener(e -> {
			fpc.setHeight(Integer.parseInt(flHeight.getText()));
		});
		add(flHeight);

		final JToggleButton tglStatus = new JToggleButton("Finger detected");
		tglStatus.setEnabled(false);
		add(tglStatus);

		final JButton btnMode = new JButton(String.valueOf(egis.getMode()));
		btnMode.addActionListener(e -> {
			final int choice = JOptionPane.showOptionDialog(null,
					"Choose a mode",
					"Egis mode",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null,
					EgisMode.values(),
					null);
			if (choice == -1)
				return;
			egis.setMode(EgisMode.values()[choice]);
		});
		add(btnMode);

		final Timer timer = new Timer(100, e -> {
			try {
				data = egis.requestFlyEstimation();
				tglStatus.setText(String.valueOf(egis.fingerStatus()));
				repaint();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		});

		timer.start();
	}

	class FingerprintComponent extends JComponent {
		private static final long serialVersionUID = 5842761368020689137L;

		private int width = 114;
		private int height = 285;

		public FingerprintComponent() {
			setPreferredSize(new Dimension(Egis.IMG_WIDTH, Egis.IMG_HEIGHT));
		}

		@Override
		public void paint(Graphics g) {
			super.paint(g);
			if (data == null)
				return;

			g.setColor(fingerState ? Color.RED : Color.BLACK);
			g.drawRect(0, 0, getWidth(), getHeight());

			try {
				final BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

				final DataBufferByte buf = new DataBufferByte(data, Egis.IMG_SIZE);
				final Raster raster = Raster.createPackedRaster(buf, width, height, 8, new Point());
				img.setData(raster);

				g.drawImage(img, 0, 0, this);
			} catch (Exception e) {}

		}

		public int getWidth() {
			return width;
		}

		public void setWidth(int width) {
			this.width = width;
		}

		public int getHeight() {
			return height;
		}

		public void setHeight(int height) {
			this.height = height;
		}
	}

}
