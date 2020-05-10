package me.vinceh121.egis0570.playground;

import java.util.Iterator;

import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapPacket;
import org.pcap4j.core.Pcaps;

public class CaptureJavaExtractor {

	public static void main(final String[] args) throws PcapNativeException, NotOpenException, InterruptedException {
		final PcapHandle hndl = Pcaps.openOffline("/home/vincent/VMSharedFolder/usb-fingerprint");
		// hndl.setFilter("(frame.number >= 2471 && frame.number <= 2578) &&
		// (usb.capdata contains 45:47:49:53)",
		// BpfCompileMode.OPTIMIZE);

		final byte[] egis = new byte[] { 'E', 'G', 'I', 'S' };

		// USB bulk content is from 27 to 33
		System.out.println("{");
		final Iterator<PcapPacket> it = hndl.stream().iterator();
		for (int i = 0; it.hasNext(); i++) {
			final PcapPacket pcapPck = it.next();
			if (pcapPck == null) {
				break;
			}
			if (i < 2471 || i > 2578) {
				continue;
			}
			final byte[] data = new byte[7];
			try {
				System.arraycopy(pcapPck.getRawData(), 27, data, 0, 7);
			} catch (final ArrayIndexOutOfBoundsException e) {
				continue;
			}
			if (CaptureJavaExtractor.arrayStartsWith(data, egis)) {
				System.out.print("\t{ ");
				for (int c = 0; c < data.length; c++) {
					System.out.print("0x" + Integer.toHexString(data[c]));
					if (c < data.length - 1)
						System.out.print(", ");
				}
				System.out.println(" },");
			}
		}
		System.out.println("}");
	}

	private static boolean arrayStartsWith(final byte[] content, final byte[] prefix) {
		for (int i = 0; i < prefix.length; i++) {
			if (content[i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

}
