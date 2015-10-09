package secretshare;

import secretshare.SecretShare;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class TaggerDemo {

	public static void main(String[] args) throws Exception {
		if (args.length != 4) {
			System.err
					.println("usage: java TaggerDemo modelFile fileToTag fileToEncode filetoWriteShare");
			return;
		}

		// next step is to simplify the grammar and generate an encoded message
		// Encode the file
		SecretShare s = new SecretShare();
		HashMap<String, HashMap<String, String>> TerminalCodes = new HashMap<String, HashMap<String, String>>();// stores
																												// code
																												// maps
		TerminalCodes = s.CreateAugmentedCFG(args[1], args[0]);
		s.Encode(args[2], args[3], TerminalCodes, false);
		// arguments are .. file read path , file write path, number of shares,
		// number of shares to combine, blocksize
		// return shares in - concatenated for each block
		int blockSize = 15; // we chose a block size of multiple of three to
							// avoid improper splitting
		HashMap<Integer, StringBuffer> share = s.SecretShareData(args[3],
				args[3], 18, 3, blockSize);

		Iterator<Map.Entry<Integer, StringBuffer>> elements = share.entrySet()
				.iterator();
		int length = 0;
		while (elements.hasNext()) {
			Map.Entry<Integer, StringBuffer> e = elements.next();
			length += e.getValue().length();
		}
		System.out.println("Average Length" + length / 18);

		HashMap<String, String> shareConfig = new HashMap<String, String>();
		shareConfig.put("NN", "6:3");
		shareConfig.put("IN", "6:3");
		shareConfig.put("JJ", "6:3");
		shareConfig.put("VB", "6:3");
		shareConfig.put("DT", "6:3");
		s.Encode(args[2], args[3], TerminalCodes, true);
		HashMap<String, List<StringBuffer>> shares = s.SecretShareDiscrete(
				args[3], args[3], shareConfig, blockSize);

		System.out.println("Generated Shares");
		System.out.println(shares);

		length = 0;

		Iterator<Map.Entry<String, List<StringBuffer>>> ele = shares.entrySet()
				.iterator();
		while (ele.hasNext()) {
			Map.Entry<String, List<StringBuffer>> element = ele.next();
			for (StringBuffer st : element.getValue())
				if (st.indexOf("_") == -1) {
					length += st.length();
				}
		}

		System.out.println("Average Length using discrete share" + length / 18);

		// decode using standard method
		HashMap<Integer, StringBuffer> sharePart = new HashMap<Integer, StringBuffer>();
		sharePart.put(1, share.get(1));
		sharePart.put(3, share.get(3));
		sharePart.put(4, share.get(4));
		sharePart.put(5, share.get(5));
		System.out.println("Rec msg Normal sharing:  "
				+ s.ReConstruction(sharePart, blockSize));
		System.out.println(s.Decode(s.ReConstruction(sharePart, blockSize),
				TerminalCodes));

		// new method proposed
		List<String> messageSegment = new ArrayList<String>();
		HashMap<String, List<StringBuffer>> sharePartDiscrete = new HashMap<String, List<StringBuffer>>();
		sharePartDiscrete.put("JJ1", shares.get("JJ1"));
		sharePartDiscrete.put("JJ2", shares.get("JJ2"));
		sharePartDiscrete.put("JJ4", shares.get("JJ4"));
		sharePartDiscrete.put("JJ3", shares.get("JJ3"));

		// System.out.println(TerminalCodes);
		System.out.println(s.ReConstructDiscrete(sharePartDiscrete, blockSize));
		messageSegment.add(s.ReConstructDiscrete(sharePartDiscrete, blockSize));

		sharePartDiscrete = new HashMap<String, List<StringBuffer>>();

		sharePartDiscrete.put("DT1", shares.get("DT1"));
		sharePartDiscrete.put("DT2", shares.get("DT2"));
		sharePartDiscrete.put("DT4", shares.get("DT4"));
		System.out.println(s.ReConstructDiscrete(sharePartDiscrete, blockSize));
		messageSegment.add(s.ReConstructDiscrete(sharePartDiscrete, blockSize));

		sharePartDiscrete = new HashMap<String, List<StringBuffer>>();

		sharePartDiscrete.put("NN1", shares.get("NN1"));
		sharePartDiscrete.put("NN2", shares.get("NN2"));
		sharePartDiscrete.put("NN4", shares.get("NN4"));
		System.out.println(s.ReConstructDiscrete(sharePartDiscrete, blockSize));
		messageSegment.add(s.ReConstructDiscrete(sharePartDiscrete, blockSize));

		sharePartDiscrete = new HashMap<String, List<StringBuffer>>();

		sharePartDiscrete.put("VB1", shares.get("VB1"));
		sharePartDiscrete.put("VB6", shares.get("VB6"));
		sharePartDiscrete.put("VB4", shares.get("VB4"));
		System.out.println(s.ReConstructDiscrete(sharePartDiscrete, blockSize));
		messageSegment.add(s.ReConstructDiscrete(sharePartDiscrete, blockSize));

		/*
		 * sharePartDiscrete=new HashMap<String, List<StringBuffer>>();
		 * 
		 * sharePartDiscrete.put("IN1",shares.get("IN1"));
		 * sharePartDiscrete.put("IN6",shares.get("IN6"));
		 * sharePartDiscrete.put("IN4",shares.get("IN4"));
		 * 
		 * messageSegment.add(s.ReConstructDiscrete(sharePartDiscrete,
		 * blockSize));
		 */

		System.out.println("Discrete Msg for sharing:" + messageSegment);

		System.out.println(s.Decode(messageSegment, TerminalCodes));

		/*
		 * there is a problem if a is reconstructed as ^ due to rounding off
		 * error in that case Levenshtein distance does not produce correct
		 * result one option is to use grammar that does not use such small
		 * terminals like a , an
		 */

	}

}
