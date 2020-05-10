package me.vinceh121.egis0570;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;

public class Egis {
	public static final short VENDOR_ID = 0x1c7a, PRODUCT_ID = 0x0570;
	public static final byte DEV_EPOUT = 0x04, DEV_EPIN = (byte) 0x83, DEV_INTF = 0x0, DEV_CONF = 0x1;
	private DeviceHandle dev;

	public void init() {
		int status = LibUsb.init(null);
		if (status != 0) {
			throw new RuntimeException("Failed to init libusb");
		}

		// LibUsb.setOption(null, LibUsb.OPTION_LOG_LEVEL, LibUsb.LOG_LEVEL_DEBUG);

		// final DeviceList list = new DeviceList();
		// LibUsb.getDeviceList(null, list);
		// for (Device d : list) {
		// final DeviceDescriptor des = new DeviceDescriptor();
		// LibUsb.getDeviceDescriptor(d, des);
		// if (des.idVendor() == VENDOR_ID)
		// System.out.println(des.dump());
		// }

		this.dev = LibUsb.openDeviceWithVidPid(null, Egis.VENDOR_ID, Egis.PRODUCT_ID);

		if (this.dev == null) {
			throw new RuntimeException("Could not find Egis 0570 Fingerprint reader");
		}

		if (LibUsb.kernelDriverActive(this.dev, Egis.DEV_INTF) == 1) {
			System.out.println("Detaching kernal driver");
			LibUsb.detachKernelDriver(this.dev, Egis.DEV_INTF);
		}

		status = LibUsb.setConfiguration(this.dev, Egis.DEV_CONF);
		if (status != 0) {
			throw new RuntimeException("Could not set configuration");
		}

		status = LibUsb.claimInterface(this.dev, Egis.DEV_INTF);
		if (status != 0) {
			throw new RuntimeException("Could not claim interface");
		}

		LibUsb.resetDevice(this.dev);

		System.out.println("Successful init");
	}

	public byte[] readFingerprint() throws IOException {
		return processSequence(Packets.PKG_INIT);
	}

	public byte[] repeatFingerprint() throws IOException {
		return processSequence(Packets.PKG_REPEAT);
	}

	public byte[] processSequence(byte[][] seq) throws IOException {
		Objects.requireNonNull(seq);
		final ByteBuffer data = ByteBuffer.allocateDirect(32512);
		ByteBuffer pkg;
		final IntBuffer transfered = IntBuffer.allocate(1);

		System.out.println(seq.length);
		for (int i = 0; i < seq.length; i++) {
			pkg = ByteBuffer.allocateDirect(seq[i].length);
			pkg.put(seq[i]);
			pkg.rewind();
			int status = LibUsb.bulkTransfer(this.dev, Egis.DEV_EPOUT, pkg, transfered, 0);
			if (status != 0) {
				throw new IOException("Error while sending init packet " + i + ": " + LibUsb.errorName(status));
			}

			status = LibUsb.bulkTransfer(this.dev, Egis.DEV_EPIN, data, transfered, 0);
			if (status != 0) {
				throw new IOException("Error while reading packet " + i + ": " + LibUsb.errorName(status));
			}
			// System.out.println(new String(this.getArrayOfBuffer(data)));
		}

		return this.getArrayOfBuffer(data);
	}

	private byte[] getArrayOfBuffer(final ByteBuffer buf) {
		final byte[] out = new byte[buf.capacity()];
		buf.get(out);
		buf.rewind();
		return out;
	}

	public boolean fingerStatus(final byte[] data) {
		int total = 0;
		int min, max;
		min = max = data[0];
		for (int i = 0; i < data.length; ++i) {
			total += data[i];
			if (data[i] < min)
				min = data[i];
			if (data[i] > max)
				max = data[i];
		}

		int avg = total / data.length;
		return ((avg > 210) && (min < 130));
	}

	public void writeImg(final OutputStream out, final byte[] data, final int width, final int height)
			throws IOException {
		final String formatMark = "P5";

		out.write((formatMark + "\n" + width + " " + height + "\n" + 255 + "\n").getBytes());

		int i = 0;
		for (int x = 0; x < height; x++) {
			for (int y = 0; y < width; y++, i++) {
				out.write(data[i]);
				if (y < width - 1) {
					out.write(' ');
				}
			}
			out.write('\n');
		}
	}

	public void terminate() {
		LibUsb.releaseInterface(this.dev, Egis.DEV_INTF);
		LibUsb.attachKernelDriver(this.dev, Egis.DEV_INTF);
		LibUsb.close(this.dev);
		LibUsb.exit(null);
	}

}
