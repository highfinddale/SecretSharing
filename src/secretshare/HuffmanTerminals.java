/**
 * 
 */
package secretshare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Map.Entry;

/**
 * @author Dell
 *
 */
public final class HuffmanTerminals {
	static class HuffmanNode {
		String ch;
		Integer frequency;
		HuffmanNode left;
		HuffmanNode right;

		HuffmanNode(String ch, Integer frequency, HuffmanNode left,
				HuffmanNode right) {
			this.ch = ch;
			this.frequency = frequency;
			this.left = left;
			this.right = right;
		}

	}

	private HuffmanTerminals() {
	};

	private static class HuffManComparator implements Comparator<HuffmanNode> {
		@Override
		public int compare(HuffmanNode node1, HuffmanNode node2) {
			return node1.frequency - node2.frequency;
		}
	}

	private static HashMap<String, Integer> sortValue(HashMap<String, Integer> t) {

		// Transfer as List and sort it
		ArrayList<Map.Entry<String, Integer>> l = new ArrayList(t.entrySet());
		Collections.sort(l, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> arg0,
					Entry<String, Integer> arg1) {
				// TODO Auto-generated method stub
				return arg0.getValue() - arg1.getValue();
			}
		});
		// repack the hashmap
		Iterator<Map.Entry<String, Integer>> itr = l.iterator();
		t.clear();
		while (itr.hasNext()) {
			Map.Entry<String, Integer> element = itr.next();
			t.put(element.getKey(), element.getValue());
		}
		return t;
	}

	public static HashMap<String, String> CreateHuffmanTree(
			HashMap<String, Integer> distribution) {
		// create a sorted queue from the given hashMap in descending order of
		// frequency
		Queue<HuffmanNode> PQueue = new PriorityQueue<HuffmanNode>(1,
				new HuffManComparator());
		HashMap<String, String> codeWords = new HashMap<String, String>();
		Iterator<Map.Entry<String, Integer>> itr = sortValue(distribution)
				.entrySet().iterator();
		while (itr.hasNext()) {
			Map.Entry<String, Integer> element = itr.next();
			// System.out.println(element);
			PQueue.add(new HuffmanNode(element.getKey(), element.getValue(),
					null, null));
		}
		// build huffman tree from PQ

		while (PQueue.size() > 1) // to consider only element list
		{
			HuffmanNode left = PQueue.remove();
			HuffmanNode right = PQueue.remove();
			HuffmanNode parent = new HuffmanNode("0", left.frequency
					+ right.frequency, left, right);
			PQueue.add(parent);
		}
		// root of the tree
		HuffmanNode root = PQueue.remove();
		// get the code from this HuffanTree
		GenerateCode(codeWords, root, "");
		return codeWords;

	}

	private static void GenerateCode(HashMap<String, String> map,
			HuffmanNode h, String s) {
		if (h.left == null && h.right == null) {
			map.put(h.ch, s);
			return;
		}
		GenerateCode(map, h.right, s + "1");
		GenerateCode(map, h.left, s + "0");
	}

}