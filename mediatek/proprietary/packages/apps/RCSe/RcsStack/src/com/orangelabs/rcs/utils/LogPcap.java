package com.orangelabs.rcs.utils;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.os.Environment;

import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ResolverConfig;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;




public class LogPcap {
	private int size = 0;
	   private static LogPcap instance = null;

	
	private static final String REGEX_IPV4 = "\\b((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.|$)){4}\\b";

	private Record[] getDnsRequest(String domain, ExtendedResolver resolver,
			int type) {
		try {
			Lookup lookup = new Lookup(domain, type);
			lookup.setResolver(resolver);
			Record[] result = lookup.run();
			int code = lookup.getResult();
			if (code != Lookup.SUCCESSFUL) {
			}
			return result;
		} catch (TextParseException e) {
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private String getDnsA(String domain) {
		try {
			return InetAddress.getByName(domain).getHostAddress();
		} catch (UnknownHostException e) {
			return null;
		}
	}

	public class DnsResolvedFields {
		public String ipAddress = null;
		public int port = -1;

		public DnsResolvedFields(String ipAddress, int port) {
			this.ipAddress = ipAddress;
			this.port = port;
		}
	}
	
	private SRVRecord getBestDnsSRV(Record[] records) {
		SRVRecord result = null;
        for (int i = 0; i < records.length; i++) {
        	SRVRecord srv = (SRVRecord)records[i];
			if (result == null) {
				// First record
				result = srv;
			} else {
				// Next record
				if (srv.getPriority() < result.getPriority()) {
					// Lowest priority
					result = srv;
				} else
				if (srv.getPriority() == result.getPriority()) {
					// Highest weight
					if (srv.getWeight() > result.getWeight()) {
						result = srv;
					}
				}
			}
        }
        return result;
	}
	
	
	public synchronized void logPcapUDP(String srcIPA, String dstIPA,
			int srcPort, int dstPort, byte[] data) {
		FileOutputStream out;
		String srcIP = srcIPA;
		String dstIP = dstIPA;
		if (srcIPA.contains("rcs"))
			srcIP = getIPfromHostName(srcIPA);
		if (dstIPA.contains("rcs"))
			dstIP = getIPfromHostName(dstIPA);


		// Parse IP parts into an int array
		int[] ip = new int[4];
		String[] parts = srcIP.split("\\.");

		for (int i = 0; i < 4; i++) {
			ip[i] = Integer.parseInt(parts[i]);
		}

		// Parse IP parts into an int array
		int[] ip2 = new int[4];
		String[] parts2 = dstIP.split("\\.");

		for (int i = 0; i < 4; i++) {
			ip2[i] = Integer.parseInt(parts2[i]);
		}

		try {

			int size = data.length;
			File f = new File(getTraceFile());

			boolean firstUse = f.exists();
			// we need to write wireshark file header only once
			out = new FileOutputStream(getTraceFile(), true);

			if (firstUse == false) {
				// global header
				out.write(0xd4);// Magic Number HARDCODING
				out.write(0xc3);
				out.write(0xb2);
				out.write(0xa1);

				out.write(0x02);// Version HARDCODING
				out.write(0x00);
				out.write(0x04);
				out.write(0x00);

				for (int i = 0; i < 8; i++) { // Epoch time TimeZone , IT IS
												// MOSTLY
												// 0
					out.write(0x00);
				}

				for (int i = 0; i < 2; i++) {// maximum length of the captured
												// packets
					out.write(0xff);
				}
				for (int i = 0; i < 2; i++) {// maximum length of the captured
												// packets
												// lower bytes
					out.write(0x00);
				}

				out.write(0x01); // ETHERNET II
				for (int i = 0; i < 3; i++) {// End
					out.write(0x00);
				}
			}

			// Frame
			long timestamp = System.currentTimeMillis();
			int seconds = (int) (timestamp / 1000);
			int microseconds = (int) (timestamp % 1000);
			ByteBuffer b = ByteBuffer.allocate(4);
			b.putInt(seconds); // time stamp
			byte[] result = b.array();
			for (int i = 0; i < 4; i++) {
				out.write(result[i]);
			}

			b.clear();
			b.putInt(microseconds); // time stamp microsecond part of when
									// packet
									// was captured
			result = b.array();
			for (int i = 0; i < 4; i++) {
				out.write(result[i]);
			}

			int lenOfPacket = 14 + 20 + 8 + size; // Total Length
													// ethernet+ip+udp+data
			b.clear();
			b.putInt(lenOfPacket);
			result = b.array();
			for (int i = 3; i > -1; i--) {// size of the saved packet data in
											// our
											// file hex
				out.write(result[i]);
			}
			for (int i = 3; i > -1; i--) {// size of the saved packet data in
											// our
				// file hex
				out.write(result[i]);
			}

			// TODO calculate Mac address
			/*
			 * WifiManager wifiManager = (WifiManager)
			 * getSystemService(Context.WIFI_SERVICE); WifiInfo wInfo =
			 * wifiManager.getConnectionInfo(); String macAddress =
			 * wInfo.getMacAddress();
			 */
			// ethernet header
			for (int i = 0; i < 6; i++) { // mac sddress src dummy
				out.write(i);
			}

			for (int i = 0; i < 6; i++) { // mac address dst dummy
				out.write(i);
			}

			out.write(0x08);// ethernet type IP for UDP traffic
			out.write(0x00);

			// IP Header
			out.write(0x45);// version and header length
			out.write(0x00); // differentiated service default TOS

			int len = 20 + 8 + size; // Total Length
			b.clear();
			b.putInt(len);
			result = b.array();
			out.write(result[2]);
			out.write(result[3]);

			// Identification 2 bytes dummy
			out.write(0xff);
			out.write(0x00);

			out.write(0x00);
			out.write(0x00);// Flags ,Fragment offset 2 bytes and future use
			// flags
			// 3 bits

			out.write(0x80); // Time to live
			out.write(0x11); // Protocol UDP

			// TODO calculate checksum
			out.write(0xff); // header checksum dummy
			out.write(0xff);

			for (int i = 0; i < 4; i++) {
				out.write((byte) ip[i]);
			}

			for (int i = 0; i < 4; i++) {
				out.write((byte) ip2[i]);
			}

			// User Datagram Protocol
			ByteBuffer port = ByteBuffer.allocate(2); // Source port
			port.putShort((short) (srcPort));
			byte[] aa = port.array();
			for (int i = 0; i < 2; i++) {
				out.write(aa[i]);
			}
			port.clear(); // Destination port
			port.putShort((short) (dstPort));
			aa = port.array();
			for (int i = 0; i < 2; i++) {
				out.write(aa[i]);
			}
			port.clear(); // Length
			port.putShort((short) (size + 8));
			aa = port.array();

			out.write(aa[0]);
			out.write(aa[1]);

			out.write(0xff);// Checksum dummy for now
			out.write(0xff);

			out.write(data);// Data

			out.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public synchronized void logPcapHTTP(String srcIPA, String dstIPA,
			int srcPort, int dstPort, byte[] data) {
		FileOutputStream out;
		String srcIP = srcIPA;
		String dstIP = dstIPA;
		if (srcIPA.contains("rcs"))
			srcIP = getIPfromHostName(srcIPA);
		if (dstIPA.contains("rcs"))
			dstIP = getIPfromHostName(dstIPA);


		// Parse IP parts into an int array
		int[] ip = new int[4];
		String[] parts = srcIP.split("\\.");

		for (int i = 0; i < 4; i++) {
			ip[i] = Integer.parseInt(parts[i]);
		}

		// Parse IP parts into an int array
		int[] ip2 = new int[4];
		String[] parts2 = dstIP.split("\\.");

		for (int i = 0; i < 4; i++) {
			ip2[i] = Integer.parseInt(parts2[i]);
		}

		try {

			int size = data.length;
			File f = new File(getTraceFile());

			boolean firstUse = f.exists();
			// we need to write wireshark file header only once
			out = new FileOutputStream(getTraceFile(), true);

			if (firstUse == false) {
				// global header
				out.write(0xd4);// Magic Number HARDCODING
				out.write(0xc3);
				out.write(0xb2);
				out.write(0xa1);

				out.write(0x02);// Version HARDCODING
				out.write(0x00);
				out.write(0x04);
				out.write(0x00);

				for (int i = 0; i < 8; i++) { // Epoch time TimeZone , IT IS
												// MOSTLY
												// 0
					out.write(0x00);
				}

				for (int i = 0; i < 2; i++) {// maximum length of the captured
												// packets
					out.write(0xff);
				}
				for (int i = 0; i < 2; i++) {// maximum length of the captured
												// packets
												// lower bytes
					out.write(0x00);
				}

				out.write(0x01); // ETHERNET II
				for (int i = 0; i < 3; i++) {// End
					out.write(0x00);
				}
			}

			// Frame
			long timestamp = System.currentTimeMillis();
			int seconds = (int) (timestamp / 1000);
			int microseconds = (int) (timestamp % 1000);
			ByteBuffer b = ByteBuffer.allocate(4);
			b.putInt(seconds); // time stamp
			byte[] result = b.array();
			for (int i = 0; i < 4; i++) {
				out.write(result[i]);
			}

			b.clear();
			b.putInt(microseconds); // time stamp microsecond part of when
									// packet
									// was captured
			result = b.array();
			for (int i = 0; i < 4; i++) {
				out.write(result[i]);
			}

			int lenOfPacket = 14 + 20 + 8 + size; // Total Length
													// ethernet+ip+udp+data
			b.clear();
			b.putInt(lenOfPacket);
			result = b.array();
			for (int i = 3; i > -1; i--) {// size of the saved packet data in
											// our
											// file hex
				out.write(result[i]);
			}
			for (int i = 3; i > -1; i--) {// size of the saved packet data in
											// our
				// file hex
				out.write(result[i]);
			}

			// TODO calculate Mac address
			/*
			 * WifiManager wifiManager = (WifiManager)
			 * getSystemService(Context.WIFI_SERVICE); WifiInfo wInfo =
			 * wifiManager.getConnectionInfo(); String macAddress =
			 * wInfo.getMacAddress();
			 */
			// ethernet header
			for (int i = 0; i < 6; i++) { // mac sddress src dummy
				out.write(i);
			}

			for (int i = 0; i < 6; i++) { // mac address dst dummy
				out.write(i);
			}

			out.write(0x08);// ethernet type IP for UDP traffic
			out.write(0x00);

			// IP Header
			out.write(0x45);// version and header length
			out.write(0x00); // differentiated service default TOS

			int len = 20 + 8 + size; // Total Length
			b.clear();
			b.putInt(len);
			result = b.array();
			out.write(result[2]);
			out.write(result[3]);

			// Identification 2 bytes dummy
			out.write(0xff);
			out.write(0x00);

			out.write(0x00);
			out.write(0x00);// Flags ,Fragment offset 2 bytes and future use
			// flags
			// 3 bits

			out.write(0x80); // Time to live
			out.write(0x11); // Protocol UDP

			// TODO calculate checksum
			out.write(0xff); // header checksum dummy
			out.write(0xff);

			for (int i = 0; i < 4; i++) {
				out.write((byte) ip[i]);
			}

			for (int i = 0; i < 4; i++) {
				out.write((byte) ip2[i]);
			}

			// User Datagram Protocol
			ByteBuffer port = ByteBuffer.allocate(2); // Source port
			port.putShort((short) (srcPort));
			byte[] aa = port.array();
			for (int i = 0; i < 2; i++) {
				out.write(aa[i]);
			}
			port.clear(); // Destination port
			port.putShort((short) (dstPort));
			aa = port.array();
			for (int i = 0; i < 2; i++) {
				out.write(aa[i]);
			}
			port.clear(); // Length
			port.putShort((short) (size + 8));
			aa = port.array();

			out.write(aa[0]);
			out.write(aa[1]);

			out.write(0xff);// Checksum dummy for now
			out.write(0xff);
			
			//http data

			out.write(data);// Data

			
			
			
			out.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	public long calculateChecksum(byte[] buf) {
		int length = buf.length;
		int i = 0;

		long sum = 0;
		long data;

		// Handle all pairs
		while (length > 1) {
			data = (((buf[i] << 8) & 0xFF00) | ((buf[i + 1]) & 0xFF));
			sum += data;
			// 1's complement carry bit correction in 16-bits (detecting sign
			// extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}

			i += 2;
			length -= 2;
		}

		// Handle remaining byte in odd length buffers
		if (length > 0) {
			sum += (buf[i] << 8 & 0xFF00);
			// 1's complement carry bit correction in 16-bits (detecting sign
			// extension)
			if ((sum & 0xFFFF0000) > 0) {
				sum = sum & 0xFFFF;
				sum += 1;
			}
		}

		// Final 1's complement value correction to 16-bits
		sum = ~sum;
		sum = sum & 0xFFFF;
		return sum;

	}

	
	
	/**
	 * Get SIP trace file
	 * 
	 * @return SIP trace file
	 */
	public String getTraceFile() {
		return Environment.getExternalStorageDirectory().getPath() + "/RcsDebug" +"/rcs.pcap";

	}



	String getIPfromHostName(String host) {

		DnsResolvedFields dnsResolvedFields;
		boolean useDns = true;
		if (host.matches(REGEX_IPV4)) {
			useDns = false;
			dnsResolvedFields = new DnsResolvedFields(host, 5060);
		} else {
			dnsResolvedFields = new DnsResolvedFields(null, 5060);
		}

		if (useDns) {
			// Set DNS resolver
			ResolverConfig.refresh();
			ExtendedResolver resolver = null;
			try {
				resolver = new ExtendedResolver();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String service = "SIP+D2U";

			boolean resolved = false;
			Record[] naptrRecords = getDnsRequest(host, resolver, Type.NAPTR);
			if ((naptrRecords != null) && (naptrRecords.length > 0)) {
				// First try with NAPTR
				for (int i = 0; i < naptrRecords.length; i++) {
					NAPTRRecord naptr = (NAPTRRecord) naptrRecords[i];
					if ((naptr != null)
							&& naptr.getService().equalsIgnoreCase(service)) {
						// DNS SRV lookup
						Record[] srvRecords = getDnsRequest(naptr
								.getReplacement().toString(), resolver,
								Type.SRV);
						if ((srvRecords != null) && (srvRecords.length > 0)) {
							SRVRecord srvRecord = getBestDnsSRV(srvRecords);
							dnsResolvedFields.ipAddress = getDnsA(srvRecord
									.getTarget().toString());
							dnsResolvedFields.port = srvRecord.getPort();
						} else {
							// Direct DNS A lookup
							dnsResolvedFields.ipAddress = getDnsA(host);
						}
						resolved = true;
					}
				}
			}

			if (!resolved) {
				// If no NAPTR: direct DNS SRV lookup
				String query;
				if (host.startsWith("_sip.")) {
					query = host;
				} else {
					query = "_sip._" + "UDP".toLowerCase() + "." + host;
				}
				Record[] srvRecords = getDnsRequest(query, resolver, Type.SRV);
				if ((srvRecords != null) && (srvRecords.length > 0)) {
					SRVRecord srvRecord = getBestDnsSRV(srvRecords);
					dnsResolvedFields.ipAddress = getDnsA(srvRecord.getTarget().toString());
					dnsResolvedFields.port = srvRecord.getPort();
					resolved = true;
				}

				if (!resolved) {
					// If not resolved: direct DNS A lookup
					dnsResolvedFields.ipAddress = getDnsA(host);
				}
			}
		}

		if (dnsResolvedFields.ipAddress == null) {

			String imsProxyAddrResolved = getDnsA(host);
			if (imsProxyAddrResolved != null) {
				dnsResolvedFields = new DnsResolvedFields(imsProxyAddrResolved,5060);
			} else {
				return null;
			}
		}

		return dnsResolvedFields.ipAddress;

	}

	public static LogPcap getInstance() {
		 if(instance == null) {
	         instance = new LogPcap();
	      }
	      return instance;
	   }


}


