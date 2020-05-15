package me.vinceh121.egis0570;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;

import org.usb4java.DeviceHandle;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class Egis {
	public static final short VENDOR_ID = 0x1c7a, PRODUCT_ID = 0x0570;
	public static final byte DEV_EPOUT = 0x04, DEV_EPIN = (byte) 0x83, DEV_INTF = 0x0, DEV_CONF = 0x1;
	public static final int IMG_SIZE = 32512, REGISTRY_SIZE = Byte.MAX_VALUE, // XXX placeholder value
			IMG_WIDTH = 114, IMG_HEIGHT = 285;
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
			throw new LibUsbException(status);
		}

		status = LibUsb.claimInterface(this.dev, Egis.DEV_INTF);
		if (status != 0) {
			throw new LibUsbException(status);
		}

		LibUsb.resetDevice(this.dev);

		System.out.println("Successful init");
	}

	// public byte[] readFingerprint() throws IOException {
	// // return processSequence(Packets.PKG_INIT);
	// this.writeRegister(reg, val)
	// return this.sendMessage(0x6, 0x0, 0xfffffffe, Egis.IMG_SIZE);
	// }

	public byte[] requestFlyEstimation() throws IOException {
		this.writeRegister(0x02, 0x0f);
		this.writeRegister(0x02, 0x2F);
		return this.sendMessage(0x6, 0x0, 0xfe, Egis.IMG_SIZE);
	}

	/**
	 * Don't know if this is gonna stay
	 */
	public void setDefaultsForReading() {
		writeRegister(0x11, 0x38);
		writeRegister(0x12, 0x0); // width
		writeRegister(0x13, 0x71); // width
		writeRegister(0x20, 0x44);
		writeRegister(0x58, 0x44);
		writeRegister(0x21, 0x9);
		writeRegister(0x57, 0x9);
		writeRegister(0x22, 0x2);
		writeRegister(0x56, 0x2);
		writeRegister(0x23, 0x1);
		writeRegister(0x55, 0x1);
		writeRegister(0x24, 0x1);
		writeRegister(0x54, 0x1);
		writeRegister(0x25, 0x0);
		writeRegister(0x53, 0x0);
		writeRegister(0x15, 0x0);
		writeRegister(0x16, 0x51);
		writeRegister(0x9, 0xa);
		writeRegister(0x14, 0);

		// writeRegister(0x2, 0xf); ////// maybe finger detection
		writeRegister(0x3, 0x80);
		// readRegister(0x2);
		// writeRegister(0x2, 0x2f);
		setSmallGain(0x51);
		setNormalGain(0xa);
		setLargeGain(0);
	}

	public byte fingerStatus() { // FIXME
		writeRegister(0x59, 0x18);
		writeRegister(0x5A, 0x08);
		writeRegister(0x5B, 0x10);
		final byte a = readRegister(0x50);
		final byte b = (byte) ((a & 0x7f) | 0x80);
		// System.out.println("a = " + Integer.toHexString(a) + "\nb = " + b);
		return readRegister(0x03);
	}

	public void setMode(final EgisMode mode) {
		writeRegister(0x02, mode.getValue()); // XXX wrong register
	}

	public EgisMode getMode() {
		return EgisMode.forValue(readRegister(0x02)); // XXX wrong register
	}

	public byte setSmallGain(int gain) {
		return this.writeRegister(0x16, gain);
	}

	public byte getSmallGain() {
		return this.readRegister(0x16);
	}

	public byte setNormalGain(int gain) {
		return this.writeRegister(0x9, gain);
	}

	public byte getNormalGain() {
		return this.readRegister(0x9);
	}

	public byte setLargeGain(int gain) {
		return this.writeRegister(0x14, gain);
	}

	public byte getLargeGain() {
		return this.readRegister(0x14);
	}

	public byte readRegister(final int reg) {
		final byte[] res = this.sendMessage(0x00, reg, 0, 7);
		return res[5];
	}

	public byte writeRegister(final int reg, final int val) {
		final byte[] res = this.sendMessage(0x01, reg, val, 7);
		return res[5];
	}

	public byte[] sendMessage(final int msg, final int arg0, final int arg1, final int respLen) {
		return this.sendPacket(new byte[] { 'E', 'G', 'I', 'S', (byte) msg, (byte) arg0, (byte) arg1 }, respLen);
	}

	public byte[] sendPacket(final byte[] msg, final int respLen) {
		Objects.requireNonNull(msg);
		final IntBuffer transfered = IntBuffer.allocate(1);
		final ByteBuffer data = ByteBuffer.allocateDirect(respLen);
		final ByteBuffer pkg = ByteBuffer.allocateDirect(msg.length);
		pkg.put(msg);
		pkg.rewind();
		int status = LibUsb.bulkTransfer(this.dev, Egis.DEV_EPOUT, pkg, transfered, 0);
		if (status != 0) {
			throw new LibUsbException(status);
		}
		status = LibUsb.bulkTransfer(this.dev, Egis.DEV_EPIN, data, transfered, 0);
		final byte[] bytes = this.getArrayOfBuffer(data);
		return bytes;
	}

	private byte[] getArrayOfBuffer(final ByteBuffer buf) {
		final byte[] out = new byte[buf.capacity()];
		buf.get(out);
		buf.rewind();
		return out;
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

	/**
	 * Mode values from etes603 driver, actual values might be different
	 *
	 */
	public enum EgisMode {
		SLEEP(0x30), CONTACT(0x31), SENSOR(0x33), FLY_ESTIMATION(0x34);

		private final byte value;

		private EgisMode(final int value) {
			this.value = (byte) value;
		}

		public byte getValue() {
			return value;
		}

		public static EgisMode forValue(byte b) {
			for (EgisMode m : EgisMode.values()) {
				if (m.getValue() == b)
					return m;
			}
			System.err.println("0x" + Integer.toHexString(b) + " isn't a correct mode");
			return null;
		}
	}

}
