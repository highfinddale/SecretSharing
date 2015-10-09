/**
 * 
 */
package secretshare;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * @author Dell
 *
 */
public class SecretShare {

	/*
	 * Description function name : CreateAugmentedCFG Input : 1. String: File
	 * containing paragraph that is used to create the augmented CFG 2. String:
	 * Model file used by POS tagger - in example
	 * english-bidirectional-distsim.tagger is used Output: HashMap<String,
	 * HashMap<String, String>> contains < nonterminal , <word, huffmancode>
	 * Example : Description: this function first tag the paragraph using
	 * stanford POS tagger and classify the words in different category. Then
	 * frequency of each words belonging to a category is computed and the word
	 * - frequency distribution is used to create code for each word hence
	 * completing the augmented CFG
	 */

	public HashMap<String, HashMap<String, String>> CreateAugmentedCFG(
			String filetoTag, String modelFile) {
		// Initialise with NonTerminal types available in POS tagger
		HashMap<String, String> NonTerminalMap = new HashMap<String, String>();
		NonTerminalMap.put("NNP", "NN");
		NonTerminalMap.put("NNPS", "NN");
		NonTerminalMap.put("NNS", "NN");
		NonTerminalMap.put("NN", "NN");
		NonTerminalMap.put("JJ", "JJ");
		NonTerminalMap.put("JJR", "JJ");
		NonTerminalMap.put("JJ", "JJ");
		NonTerminalMap.put("VBD", "VB");
		NonTerminalMap.put("VBG", "VB");
		NonTerminalMap.put("VBZ", "VB");
		NonTerminalMap.put("VBP", "VB");
		NonTerminalMap.put("VBN", "VB");
		NonTerminalMap.put("VB", "VB");
		NonTerminalMap.put("IN", "IN");
		NonTerminalMap.put("CC", "CC");
		NonTerminalMap.put("DT", "DT");

		SortedMap<String, HashMap<String, Integer>> Terminals = new TreeMap<String, HashMap<String, Integer>>(); // store
																													// word
																													// frequency
		SortedMap<String, Integer> WordString = new TreeMap<String, Integer>(); // contains
																				// tagged
																				// word
																				// and
																				// their
																				// frequency
		HashMap<String, HashMap<String, String>> TerminalCodes = new HashMap<String, HashMap<String, String>>();// stores
																												// code
																												// maps

		// end of initialization

		// POS tagging postagger
		try {
			MaxentTagger tagger = new MaxentTagger(modelFile);
			List<List<HasWord>> sentences = MaxentTagger
					.tokenizeText(new BufferedReader(new FileReader(filetoTag)));
			for (List<HasWord> sentence : sentences) {
				List<TaggedWord> tSentence = tagger.tagSentence(sentence);
				// System.out.println(Sentence.listToString(tSentence, false));
				Iterator<String> itr = Arrays.asList(
						Sentence.listToString(tSentence, false).split(" "))
						.iterator();
				while (itr.hasNext()) {
					String s = itr.next();

					if (WordString.containsKey(s.toUpperCase()))
						WordString.put(s.toUpperCase(),
								WordString.get(s.toUpperCase()) + 1);
					else
						WordString.put(s.toUpperCase(), 1);
				}

			}

			// distribute he words tagged according to the POS
			// separate noun , verb , adjective and compute the frequency

			Iterator<String> keys = WordString.keySet().iterator();
			while (keys.hasNext()) {
				String val = keys.next();
				if (NonTerminalMap.get(val.split("/")[1]) != null) {
					// here we map the word to type defined in the grammar
					// based on non terminal map we change the type of the word
					String type = NonTerminalMap.get(val.split("/")[1]);

					if (Terminals
							.containsKey(NonTerminalMap.get(val.split("/")[1]))) {
						HashMap<String, Integer> value = Terminals
								.get(NonTerminalMap.get(val.split("/")[1]));
						// System.out.print(value);
						value.put(val.split("/")[0] + "/" + type,
								WordString.get(val));
						// value.put(val, WordString.get(val));
						Terminals.put(type, value);
					} else {
						HashMap<String, Integer> value = new HashMap<String, Integer>();
						value.put(val.split("/")[0] + "/" + type,
								WordString.get(val));
						// value.put(val, WordString.get(val));
						Terminals.put(type, value);
						// System.out.println("Pushed");
					}
				}

				// print the distribution

				// create huffman tree for each type
				Iterator<String> iteration = Terminals.keySet().iterator();
				while (iteration.hasNext()) {
					String terminalType = iteration.next();
					HashMap<String, Integer> element = Terminals
							.get(terminalType);

					// System.out.println(element+"Before encoding");

					HashMap<String, String> codewords = HuffmanTerminals
							.CreateHuffmanTree(element);

					// default codes for 0 and 1
					if (!codewords.containsValue("0"))
						codewords.put("Z" + terminalType + "/" + terminalType,
								"0");
					if (!codewords.containsValue("1"))
						codewords.put("NZ" + terminalType + "/" + terminalType,
								"1");
					TerminalCodes.put(terminalType, codewords);
					// System.out.println(codewords);
				}

			}
			// print the code generated

			// System.out.println(Terminals);
			// System.out.println(TerminalCodes);

		} catch (FileNotFoundException f) {
			System.err.println("File stream not available " + f.getMessage());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		return TerminalCodes;

	}

	/*
	 * Function name: PartialEncode Input: 1. HashMap<String, String> all words
	 * and their corresponding mapping for a given terminal 2. String: input in
	 * Binary stream Output: String Description : This function replaces the
	 * maximal initial matching binary stream with corresponding words
	 */

	public String PartialEncode(HashMap<String, String> codeWords,
			String input, boolean isTagRequired) {
		// System.out.println(input +"as input to partial encode");
		List<Map.Entry<String, String>> values = new ArrayList<Entry<String, String>>(
				codeWords.entrySet());
		Collections.sort(values, new Comparator<Map.Entry<String, String>>() {

			@Override
			public int compare(Map.Entry<String, String> arg0,
					Map.Entry<String, String> arg1) {
				// TODO Auto-generated method stub
				return arg1.getValue().length() - arg0.getValue().length();
			}

		});
		// System.out.println("Codewords"+values);
		Iterator<Entry<String, String>> itr = values.iterator();
		while (itr.hasNext()) {
			Entry<String, String> codeValuePair = itr.next();
			if (input.startsWith(codeValuePair.getValue())) {
				if (isTagRequired)
					input = input
							.replaceFirst(input.substring(0, codeValuePair
									.getValue().length()),
									codeValuePair.getKey() + " ");
				else
					input = input.replaceFirst(input.substring(0, codeValuePair
							.getValue().length()), codeValuePair.getKey()
							.split("/")[0] + " ");
				// System.out.println(codeValuePair.getKey());

				break;
			} else
				continue;
		}
		return input;
	}

	/*
	 * Function name: Encode Input: 1. String : data file path containing binary
	 * stream 2. String: file path to write the encoded stream 3.
	 * HashMap<String, HashMap<String, String>> : augmented CFG terminal codes
	 * 4: isTagRequired: identifies whether to retain the tag in obfuscated data
	 * output : void Description: this function translates the given binary
	 * stream message to english word arrangement based on augmented cfg grammar
	 * codes
	 */

	public void Encode(String fileName, String fileWrite,
			HashMap<String, HashMap<String, String>> TerminalCodes,
			boolean isTagRequired) {
		// Grammar to parse and translate the data
		HashMap<String, String[]> Grammar = new HashMap<String, String[]>();
		Grammar.put("S", new String[] { "NP_VP" });
		Grammar.put("VP", new String[] { "VB_NP", "VB" });
		Grammar.put("NP", new String[] { "DT_NP", "JJ_NN" });

		List<String> POS = new ArrayList<String>();
		POS.add("NN");
		POS.add("VB");
		POS.add("JJ");
		POS.add("IN");
		POS.add("DT");

		BufferedWriter destination = null;
		int length = 0;
		// these are list of non POS elements allowed in Grammar
		try {
			BufferedReader source = new BufferedReader(new FileReader(fileName));
			destination = new BufferedWriter(new FileWriter(fileWrite));
			String line = "";
			int startIndex;
			while ((line = source.readLine()) != null && line != "") {
				length += line.length();
				while (line.indexOf("0") >= 0 || line.indexOf("1") >= 0) {
					// System.out.println(line);

					Stack<String> NonTerminals = new Stack<String>();
					List<String> elements = Arrays.asList(Grammar.get("S")[0]
							.split("_"));
					Collections.reverse(elements);
					NonTerminals.addAll(elements);
					System.out.println(NonTerminals);
					while (!NonTerminals.isEmpty()) {
						if (line.indexOf("1") == -1 && line.indexOf("0") == -1) {
							// end of string hence remove non non terminals and
							// break
							NonTerminals.empty();
							break;
						} else if (line.indexOf("0") == -1)
							startIndex = line.indexOf("1");
						else if (line.indexOf("0") != -1
								&& line.indexOf("1") != -1)
							startIndex = line.indexOf("0") < line.indexOf("1") ? line
									.indexOf("0") : line.indexOf("1");
						else {
							startIndex = line.indexOf("0");
						}
						String element = NonTerminals.pop();
						if (!POS.contains(element)) {
							// System.out.println(element
							// +Grammar.get(element).length+"   "+(line.charAt(startIndex)));
							/*
							 * if more than one grammar exists then chose the
							 * form based on first character of the remaining
							 * binary string to be encoded 48 is subtracted to
							 * get 0 or 1 in int
							 */
							elements = Arrays
									.asList(Grammar.get(element).length > 1 ? Grammar
											.get(element)[(int) line
											.charAt(startIndex) - 48]
											.split("_")
											: Grammar.get(element)[0]
													.split("_"));
							Collections.reverse(elements);
							NonTerminals.addAll(elements);
							// System.out.println(NonTerminals);
						} else {
							// System.out.println("index position"+startIndex);
							line = line.substring(0, startIndex)
									+ PartialEncode(
											TerminalCodes.get(element),
											line.substring(startIndex,
													line.length()),
											isTagRequired);
						}
					}
				}
				destination.write(line);
				System.out.println(line);

			}
			System.out.println("total input file length" + length);
			source.close();
			destination.close();

		} catch (FileNotFoundException f) {
			f.printStackTrace();
			System.err.println("No File selected");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.getMessage();
		} catch (Exception ex) {
			ex.getMessage();
		} finally {

		}
	}

	// the share should be of the form f(x) mod p --- needs to do that
	// shares are separated by '-'for each block

	/*
	 * Function name: Encode Input: 1. String : text input to obfuscator 2.
	 * HashMap<String, HashMap<String, String>> : augmented CFG terminal codes
	 * output : string: obfuscated string Description: this function translates
	 * the given binary stream message to english word arrangement based on
	 * augmented cfg grammar codes
	 */

	public String Encode(String text,
			HashMap<String, HashMap<String, String>> TerminalCodes) {
		// Grammar to parse and translate the data
		HashMap<String, String[]> Grammar = new HashMap<String, String[]>();
		Grammar.put("S", new String[] { "NP_VP" });
		Grammar.put("VP", new String[] { "VB_NP", "VB" });
		Grammar.put("NP", new String[] { "DT_NP", "JJ_NN" });

		List<String> POS = new ArrayList<String>();
		POS.add("NN");
		POS.add("VB");
		POS.add("JJ");
		POS.add("IN");
		POS.add("DT");

		String line = "";
		// convert to binary string

		for (int i = 0; i < text.length(); i++)
			line += Integer.toBinaryString(text.codePointAt(i));

		// these are list of non POS elements allowed in Grammar
		try {
			int startIndex;

			while (line.indexOf("0") >= 0 || line.indexOf("1") >= 0) {
				// System.out.println(line);

				Stack<String> NonTerminals = new Stack<String>();
				List<String> elements = Arrays.asList(Grammar.get("S")[0]
						.split("_"));
				Collections.reverse(elements);
				NonTerminals.addAll(elements);
				System.out.println(NonTerminals);
				while (!NonTerminals.isEmpty()) {
					if (line.indexOf("1") == -1 && line.indexOf("0") == -1) {
						// end of string hence remove non non terminals and
						// break
						NonTerminals.empty();
						break;
					} else if (line.indexOf("0") == -1)
						startIndex = line.indexOf("1");
					else if (line.indexOf("0") != -1 && line.indexOf("1") != -1)
						startIndex = line.indexOf("0") < line.indexOf("1") ? line
								.indexOf("0") : line.indexOf("1");
					else {
						startIndex = line.indexOf("0");
					}
					String element = NonTerminals.pop();
					if (!POS.contains(element)) {
						// System.out.println(element
						// +Grammar.get(element).length+"   "+(line.charAt(startIndex)));
						/*
						 * if more than one grammar exists then chose the form
						 * based on first character of the remaining binary
						 * string to be encoded 48 is subtracted to get 0 or 1
						 * in int
						 */
						elements = Arrays
								.asList(Grammar.get(element).length > 1 ? Grammar
										.get(element)[(int) line
										.charAt(startIndex) - 48].split("_")
										: Grammar.get(element)[0].split("_"));
						Collections.reverse(elements);
						NonTerminals.addAll(elements);
						// System.out.println(NonTerminals);
					} else {
						// System.out.println("index position"+startIndex);
						line = line.substring(0, startIndex)
								+ PartialEncode(
										TerminalCodes.get(element),
										line.substring(startIndex,
												line.length()), false);
					}
				}
			}
		} catch (Exception ex) {
			ex.getMessage();
		} finally {

		}

		return line;
	}

	/*
	 * FunctionName: SecretShare Input: 1. String: path to the file containing
	 * translated message that is shared 2. String: path to write the shares 3.
	 * int : number of shares to be generated 4. int : number of minimum shares
	 * required to regenerate the message in (m,n) sharing scheme 5. int: block
	 * size maximum 15 multiple of 3 to avoid improper splitting output:
	 * HashMap<Integerm StringBuffer> : shares for each user . Each share is -
	 * separated collection of string of at most 15 digit Description : This
	 * method implement shamir's secret sharing algorithm to generate share for
	 * the given text data
	 */

	public HashMap<Integer, StringBuffer> SecretShareData(String filePath,
			String fileWrite, int numberOfhare, int noToCombine, int blockSize) {
		String line = "";
		HashMap<Integer, StringBuffer> shares = new HashMap<Integer, StringBuffer>();
		List<String> coef = new ArrayList<String>();

		StringBuffer intLine = new StringBuffer();
		StringBuffer dummy = new StringBuffer();

		try {
			BufferedReader bf = new BufferedReader(new FileReader(filePath));
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileWrite,
					true));
			int index = 0;
			while ((line = bf.readLine()) != null) {
				// encode into representational number
				for (int i = 0; i < line.length(); i++) {
					String code = Integer.toString(line.codePointAt(i));
					intLine.append(code.length() > 2 ? code : "0" + code); // append
																			// leading
																			// zero
																			// for
																			// 2
																			// digit
																			// code
				}
			}
			System.out.println(intLine);

			while (index < intLine.length()) {
				// assume the data to be distributed among 6 members and 3 of
				// them can combine to get the key
				// hence the polynomial is of order 3
				// create the polynomial
				StringBuffer intLineSeg;
				if (index + blockSize < intLine.length())
					intLineSeg = new StringBuffer(intLine.substring(index,
							index + blockSize));
				else
					intLineSeg = new StringBuffer(intLine.substring(index));
				index = index + blockSize;
				dummy = new StringBuffer(intLineSeg);
				coef = new ArrayList<String>();
				for (int i = 0; i < noToCombine - 1; i++) {
					coef.add(dummy.reverse().toString());
					dummy.replace(0, dummy.length(), dummy
							.substring(new Random().nextInt(dummy.length())));
				}
				// System.out.println(coef);
				// create share based on polynomial created
				for (int i = 1; i <= numberOfhare; i++) {
					StringBuffer shareVal = new StringBuffer(intLineSeg);
					for (int j = 1; j < noToCombine; j++) {
						shareVal = new StringBuffer(
								UtilityAdd(
										shareVal.toString(),
										UtilityMult(coef.toArray()[j - 1]
												.toString(),
												String.valueOf((int) (Math.pow(
														i, j))))));
					}

					// System.out.println("share of "+i+"is "+shareVal.toString());
					if (shares.get(i) != null) {
						shares.put(i, new StringBuffer(shares.get(i) + "-"
								+ shareVal));
					} else
						shares.put(i, shareVal);
				}

				// System.out.println(shares);
			}
			Iterator<Map.Entry<Integer, StringBuffer>> itr = shares.entrySet()
					.iterator();
			while (itr.hasNext()) {
				Entry<Integer, StringBuffer> element = itr.next();
				bw.write("\n\n" + element.getKey() + "=>" + element.getValue());
				System.out
						.println(element.getKey() + "=>" + element.getValue());
			}
			bf.close();
			bw.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch blocks
			e.printStackTrace();
		} finally {

		}
		return shares;
	}

	/*
	 * FunctionName: SecretShareDiscrete Input: 1. String: path to the file
	 * containing translated message that is shared 2. String: path to write the
	 * shares 3. ShareConfig: a collection of string in following format
	 * terminalType => nunberToShare:numberToCombine 4. int: block size maximum
	 * 15 multiple of 3 to avoid improper splitting output: HashMap<Integerm
	 * List<StringBuffer>> : shares for each user . Share is reresentedAs
	 * ShateType+ShareIndex => { share as - separated block,
	 * ShareType_positional of the words in message as _ separated } Description
	 * : This method implement shamir's secret sharing algorithm to generate
	 * share for the given text data
	 */

	public HashMap<String, List<StringBuffer>> SecretShareDiscrete(
			String filePath, String fileWrite,
			HashMap<String, String> shareConfig, int blockSize) {
		String line = "";
		HashMap<String, List<StringBuffer>> shares = new HashMap<String, List<StringBuffer>>();
		List<String> coef = new ArrayList<String>();
		List<StringBuffer> shareSegment = new ArrayList<StringBuffer>();
		HashMap<String, String> textMessages = new HashMap<String, String>();
		StringBuffer intLine = new StringBuffer();
		StringBuffer dummy = new StringBuffer();

		try {
			BufferedReader bf = new BufferedReader(new FileReader(filePath));
			BufferedWriter bw = new BufferedWriter(new FileWriter(fileWrite,
					true));
			int index = 0;
			while ((line = bf.readLine()) != null && !line.isEmpty())
			// encode into representational number
			// message is represented as
			// terminal type => [words of this type separated by space]:[index
			// of the words _ separated]
			// we find the words belonging to a given category and their
			// positional occurrences and store them as : separated
			{
				String[] words = line.split(" ");
				for (int i = 0; i < words.length; i++) {
					String s = words[i];
					if (!textMessages.keySet().isEmpty()
							&& textMessages.containsKey(s.split("/")[1])) {
						String message = textMessages.get(s.split("/")[1]);
						textMessages
								.put(s.split("/")[1],
										message.split(":")[0] + " "
												+ s.split("/")[0] + ":"
												+ message.split(":")[1] + "_"
												+ i);
					} else
						textMessages.put(s.split("/")[1], s.split("/")[0] + ":"
								+ i);
				}
			}

			for (Map.Entry<String, String> element : textMessages.entrySet()) {

				// now share the data among m1 members
				index = 0;
				intLine = new StringBuffer();
				line = element.getValue().split(":")[0];
				for (int i = 0; i < line.length(); i++) {
					String code = Integer.toString(line.codePointAt(i));
					intLine.append(code.length() > 2 ? code : "0" + code); // append
																			// leading
																			// zero
																			// for
																			// 2
																			// digit
																			// code
				}
				// System.out.println(intLine);
				while (index < intLine.length()) {
					// assume the data to be distributed among 6 members and 3
					// of them can combine to get the key
					// hence the polynomial is of order 3
					// create the polynomial
					StringBuffer intLineSeg;
					if (index + blockSize < intLine.length())
						intLineSeg = new StringBuffer(intLine.substring(index,
								index + blockSize));
					else
						intLineSeg = new StringBuffer(intLine.substring(index));
					index = index + blockSize;
					dummy = new StringBuffer(intLineSeg);
					coef = new ArrayList<String>();
					for (int i = 0; i < Integer.parseInt(shareConfig.get(
							element.getKey()).split(":")[1]) - 1; i++) {
						coef.add(dummy.reverse().toString());
						dummy.replace(0, dummy.length(),
								dummy.substring(new Random().nextInt(dummy
										.length())));
					}
					// System.out.println(coef);
					// create share based on polynomial created
					for (int i = 1; i <= Integer.parseInt(shareConfig.get(
							element.getKey()).split(":")[0]); i++) {
						StringBuffer shareVal = new StringBuffer(intLineSeg);
						for (int j = 1; j < Integer.parseInt(shareConfig.get(
								element.getKey()).split(":")[1]); j++) {
							shareVal = new StringBuffer(UtilityAdd(
									shareVal.toString(),
									UtilityMult(coef.toArray()[j - 1]
											.toString(), String
											.valueOf((int) (Math.pow(i, j))))));
						}

						// System.out.println("share of "+i+"is "+shareVal.toString());
						if (shares.get(element.getKey() + i) != null) {
							shareSegment = shares.get(element.getKey() + i);
							List<StringBuffer> copyShareSegment = new ArrayList<StringBuffer>();
							for (StringBuffer s : shareSegment) {
								if (s.toString().indexOf("_") > 0) // check if
																	// the
																	// string
																	// contains
																	// _
								{
									copyShareSegment.add(s);
								} else
									copyShareSegment.add(new StringBuffer(s
											+ "-" + shareVal));
							}
							shares.put(element.getKey() + i, copyShareSegment);
						} else {
							shareSegment = new ArrayList<StringBuffer>();
							shareSegment.add(shareVal);// share value as -
														// seperated
							shareSegment.add(new StringBuffer(element.getKey()
									+ "_" + element.getValue().split(":")[1])); // ElementType_Index[_seperated]
							shares.put(element.getKey() + i, shareSegment);
						}

					}

				}
				// write down the shares created
				// final share of the category is of the from of a triplet <
				// index, share, category_positionalValue>

				bw.write("Shares for Category" + element.getKey());
				Iterator<Map.Entry<String, List<StringBuffer>>> itr = shares
						.entrySet().iterator();
				while (itr.hasNext()) {
					Entry<String, List<StringBuffer>> val = itr.next();
					bw.write("<" + val.getKey() + "=>");
					for (StringBuffer s : val.getValue())
						bw.write("\n\n\n" + s);
					bw.write("\n");
				}

			}
			bf.close();
			bw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch blocks
			e.printStackTrace();
		} finally {

		}
		return shares;
	}

	/*
	 * Function Name: Interpolate Input: 1. HashMap<Integer, StringBUffer> :
	 * Shares of length at most 15 digit 2. int : x for f(x) ideally x=0 Output:
	 * BigInteger : f(0) for the given shares segment Description : this method
	 * implements lagrange's interpolation to compute f(0) for given shares
	 */

	public BigInteger Interpolate(HashMap<Integer, StringBuffer> shares, int val) {

		BigDecimal code = new BigDecimal("0");

		// System.out.println(totalShare);
		// from interpolation f(0) will give the result
		Iterator<Map.Entry<Integer, StringBuffer>> itr = shares.entrySet()
				.iterator();
		while (itr.hasNext()) {
			int numer = 1, denom = 1;
			Set<Integer> keys = shares.keySet();
			Entry<Integer, StringBuffer> element = itr.next();
			Iterator<Integer> itrKey = keys.iterator();
			while (itrKey.hasNext()) {
				int keyval = itrKey.next();
				numer = element.getKey() == keyval ? numer : numer
						* (val - keyval);
				;
				denom = element.getKey() == keyval ? denom : denom
						* (element.getKey() - keyval);
			}
			// System.out.println("numer :"+numer+"denom"+denom);

			String pass = numer < 0 ? "-" + String.valueOf(-numer) : String
					.valueOf(numer);
			// System.out.println(pass);
			pass = UtilityMult(pass, element.getValue().toString());
			// System.out.println("pass:"+pass);
			code = code.add(new BigDecimal(new BigInteger(pass).doubleValue()
					/ (int) denom));

			/* System.out.println("code is "+code); */
		}

		return code.toBigInteger();
	}

	// code for reconstructing the hidden message
	/*
	 * Function Name: ReConstruction Input: 1. HashMap<Integer, StringBUffer> :
	 * Shares of length atmost 15 digit 2. int : block size : maximum 15 , any
	 * multiple of 3 to avoid improper splitting Output: String : Compute the
	 * text data from n possible shares that is shared among m people
	 * Description : this method re construct the message from its share
	 */

	public String ReConstruction(HashMap<Integer, StringBuffer> sharePart,
			int blockSize) {
		// get original message
		int blockCount = sharePart.entrySet().iterator().next().getValue()
				.toString().split("-").length;
		int codePoints = 0;
		String originalMessageInt = "", originalMessageTxt = "";
		String blockMessage = "";
		while (blockCount-- > 0) {
			Iterator<Map.Entry<Integer, StringBuffer>> iter = sharePart
					.entrySet().iterator();
			HashMap<Integer, StringBuffer> test = new HashMap<Integer, StringBuffer>();
			while (iter.hasNext()) {
				Map.Entry<Integer, StringBuffer> element = iter.next();
				test.put(element.getKey(), new StringBuffer(element.getValue()
						.toString().split("-")[blockCount]));
			}
			blockMessage = Interpolate(test, 0).toString();
			// System.out.println("block segment"+blockCount+
			// "message:"+blockMessage+"length"+blockMessage.length());

			// block size should be multiple of 3 to avoid improper splitting
			// if blockSize is less than multiple 3 identifies that leading
			// zeroes are truncated in computation

			int blockLength = blockMessage.length() % 3;
			if (blockLength != 0)
				for (int i = 0; i < 3 - blockLength; i++)
					blockMessage = "0" + blockMessage;

			originalMessageInt = blockMessage + "" + originalMessageInt;
			// System.out.println(originalMessage);
		}
		// System.out.println(originalMessageInt);

		// codePoints identify how many letters are there in given section of
		// interpolated message which is always multiple of 9 as 3 digit code
		// for each letter
		codePoints = originalMessageInt.length() / 3;
		try {
			while (codePoints > 0) {
				if (originalMessageInt.length() > 3) {
					originalMessageTxt += (char) Integer
							.parseInt(originalMessageInt.substring(0, 3));
					originalMessageInt = originalMessageInt.substring(3);
				} else
					originalMessageTxt += (char) Integer
							.parseInt(originalMessageInt);
				codePoints--;

			}
		} catch (Exception ex) {
			System.out.println(originalMessageTxt);
		}
		// System.out.println(originalMessageTxt);
		return originalMessageTxt;

	}

	public String ReConstructDiscrete(
			HashMap<String, List<StringBuffer>> sharePart, int blockSize) {

		int blockCount = sharePart.entrySet().iterator().next().getValue()
				.toString().split("-").length;
		int codePoints = 0;
		String originalMessageInt = "", originalMessageTxt = "";
		String blockMessage = "";
		String positionalIndex = "";

		// remove additional header details
		Iterator<Map.Entry<String, List<StringBuffer>>> iter = sharePart
				.entrySet().iterator();
		HashMap<Integer, StringBuffer> coreData = new HashMap<Integer, StringBuffer>();
		HashMap<Integer, StringBuffer> test = new HashMap<Integer, StringBuffer>();

		// coredata contain index=> string - seperated format
		// other data is stripped off

		iter = sharePart.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, List<StringBuffer>> element = iter.next();
			for (StringBuffer s : element.getValue())
				if (s.indexOf("_") == -1) {
					StringBuffer dummy = new StringBuffer(element.getKey());

					// dummy=new
					// StringBuffer(dummy.reverse().substring(0,dummy.indexOf(terminalType)));
					// regex based solution for finding the integer sequence in
					// the key

					Matcher intMatch = Pattern.compile("\\d+").matcher(
							dummy.toString());
					if (intMatch.find())
						coreData.put(Integer.parseInt(intMatch.group(0)), s);
				} else
					positionalIndex = s.toString();
		}

		// reconstruct the message based of core data and block size
		// System.out.println(coreData);
		while (blockCount-- > 0) {
			Iterator<Map.Entry<Integer, StringBuffer>> itrCore = coreData
					.entrySet().iterator();

			while (itrCore.hasNext()) {
				Map.Entry<Integer, StringBuffer> elementCore = itrCore.next();
				test.put(elementCore.getKey(), new StringBuffer(elementCore
						.getValue().toString().split("-")[blockCount]));
			}
			// System.out.println(test);
			blockMessage = Interpolate(test, 0).toString();
			// System.out.println("block segment"+blockCount+
			// "message:"+blockMessage+"length"+blockMessage.length());

			// block size should be multiple of 3 to avoid improper splitting
			// if blockSize is less than multiple 3 identifies that leading
			// zeroes are truncated in computation

			int blockLength = blockMessage.length() % 3;
			if (blockLength != 0)
				for (int i = 0; i < 3 - blockLength; i++)
					blockMessage = "0" + blockMessage;

			originalMessageInt = blockMessage + "" + originalMessageInt;
			// System.out.println(originalMessage);
		}
		// System.out.println(originalMessageInt);
		codePoints = originalMessageInt.length() / 3;
		try {
			while (codePoints > 0) {
				if (originalMessageInt.length() > 3) {
					// correct error in space value that can not be corrected
					// using LD
					if (Integer.parseInt(originalMessageInt.substring(0, 3)) == 31
							|| Integer.parseInt(originalMessageInt.substring(0,
									3)) == 33)
						originalMessageTxt += " ";
					else
						originalMessageTxt += (char) Integer
								.parseInt(originalMessageInt.substring(0, 3));
					originalMessageInt = originalMessageInt.substring(3);
				} else
					originalMessageTxt += (char) Integer
							.parseInt(originalMessageInt);
				codePoints--;

			}
		} catch (Exception ex) {
			System.out.println(originalMessageTxt);
		}
		// System.out.println(originalMessageTxt);
		return originalMessageTxt + "|" + positionalIndex;

	}

	/*
	 * // decode the encoded message into main binary stream Function Name:
	 * Decode Input: 1.Text: Text Message reconstructed from shares
	 * 2.HashMap<String, HashMap<String, String>> : huffman code for each word
	 * in a given terminal category OutPut: String: Original Message in binary
	 * form Description: Augmented terminal codes are scanned for each word to
	 * identify corresponding binary code Levenshtein distance is used to
	 * correct sightly incorrect reconstruction due to rounding off error in
	 * interpolation process.
	 */
	public String Decode(String text,
			HashMap<String, HashMap<String, String>> TerminalCodes) {
		String result = "";
		String[] words = text.split(" ");
		System.out.println(words.length);
		for (int i = 0; i < words.length; i++) {
			String word = words[i];
			String matchCode = "";
			String val = "";
			double distanceRatio = 1.0;
			Iterator<HashMap<String, String>> iter = TerminalCodes.values()
					.iterator();
			while (iter.hasNext()) {
				HashMap<String, String> maps = iter.next();
				Set<String> key = maps.keySet();
				// System.out.println(key);
				Iterator<String> keyIter = key.iterator();
				while (keyIter.hasNext()) {
					// string longest sequence
					// (StringUtils.longestCommonSubstring(word,
					// keyElement.split("/")[0] )>= word.length())

					String keyElement = keyIter.next();
					val = keyElement.split("/")[0];
					double newDistanceRatio = (double) distance(word, val)
							/ val.length();
					if (val.length() == word.length()
							&& newDistanceRatio <= distanceRatio) // 1 error in
																	// 3
																	// character
																	// max
					{
						// System.out.println(keyElement +"::"+ word +
						// newDistanceRatio+ "   "+maps.get(keyElement));
						// result+=keyElement.split("/")[0]+maps.get(keyElement);
						matchCode = maps.get(keyElement);
						distanceRatio = newDistanceRatio;
					}

				}
			}
			result += matchCode;
		}
		return result;
	}

	public String Decode(List<String> text,
			HashMap<String, HashMap<String, String>> TerminalCodes) {

		int wordCount = 0;
		String result = "";
		// read all count of words
		for (String s : text)
			wordCount += s.split("\\|")[0].split(" ").length;

		// create a string array to store all words
		String[] intermediateStrings = new String[wordCount];
		// now populate
		for (String s : text) {
			String[] wordList = s.split("\\|")[0].split(" ");
			String[] indexList = s.split("\\|")[1].split("_");
			HashMap<String, String> wordCodes = TerminalCodes.get(indexList[0]);
			for (int i = 0; i < wordList.length; i++) {
				Iterator<Map.Entry<String, String>> iter = wordCodes.entrySet()
						.iterator();
				double minRatio = 1.0;
				String matchCode = "";
				String val = "";
				while (iter.hasNext()) {
					Entry<String, String> keyElement = iter.next();
					val = keyElement.getKey().split("/")[0];
					double newDistanceRatio = (double) distance(wordList[i],
							val) / val.length();
					if (val.length() == wordList[i].length()
							&& newDistanceRatio <= minRatio) // 1 error in 3
																// character max
					{
						// System.out.println(keyElement +"::"+ word +
						// newDistanceRatio+ "   "+maps.get(keyElement));
						// result+=keyElement.split("/")[0]+maps.get(keyElement);
						matchCode = keyElement.getValue();
						minRatio = newDistanceRatio;
					}
				}
				// intermediateStrings[Integer.parseInt(indexList[i+1])]=matchCode;
				intermediateStrings[Integer.parseInt(indexList[i + 1])] = matchCode;
			}
		}
		for (String s : intermediateStrings)
			result += s;
		return result;

	}

	// Utility functions
	private String UtilityAdd(String inputA, String inputB) {
		/*
		 * System.out.println("Sum inputA"+inputA);
		 * System.out.println("Sum inputB"+inputB);
		 */

		/* sign of the result */
		if (inputA.charAt(0) == '-' && inputB.charAt(0) == '-')
			return "-"
					+ UtilityAdd(inputB.substring(1, inputB.length()),
							inputA.substring(1, inputA.length()));
		else if (inputA.charAt(0) == '-' || inputB.charAt(0) == '-')
			if (inputA.charAt(0) == '-')
				return UtilitySub(inputB, inputA.substring(1, inputA.length()));
			else
				return UtilitySub(inputA, inputB.substring(1, inputB.length()));

		StringBuffer sum = new StringBuffer();
		int carry = 0, i = 0, j = 0;
		for (i = inputA.length() - 1, j = inputB.length() - 1; i >= 0 && j >= 0; i--, j--) {
			sum.append((((int) inputA.charAt(i) - 48)
					+ ((int) inputB.charAt(j) - 48) + carry) % 10);
			carry = (((int) inputA.charAt(i) - 48)
					+ ((int) inputB.charAt(j) - 48) + carry) / 10;
		}
		while (i >= 0) {
			sum.append((((int) inputA.charAt(i) - 48) + carry) % 10);
			carry = (((int) inputA.charAt(i) - 48) + carry) / 10;
			i--;
		}
		while (j >= 0) {
			sum.append((((int) inputB.charAt(j) - 48) + carry) % 10);
			carry = (((int) inputB.charAt(j) - 48) + carry) / 10;
			j--;
		}
		if (carry != 0)
			sum = sum.append(carry);
		// System.out.println( "reverse result"+sum);
		return sum.reverse().toString();
	}

	private String UtilityMult(String inputA, String inputB) {

		StringBuffer result = new StringBuffer("0");
		int carry = 0;
		// inputs are

		// sign of the result if -A*-B=(AB) -A*B=-(A*B) vice versa
		if (inputA.charAt(0) == '-' && inputB.charAt(0) == '-')
			return UtilityMult(
					UtilityTrimZero(inputA.substring(1, inputA.length())),
					UtilityTrimZero(inputA.substring(1, inputA.length())));
		// if either of them is negative check - as the start symbol
		else if (inputA.charAt(0) == '-' || inputB.charAt(0) == '-') {

			inputA = inputA.indexOf('-') == 0 ? UtilityTrimZero(inputA
					.substring(1, inputA.length())) : UtilityTrimZero(inputA);
			inputB = inputB.indexOf('-') == 0 ? UtilityTrimZero(inputB
					.substring(1, inputB.length())) : UtilityTrimZero(inputB);

			return "-" + UtilityMult(inputA, inputB);
		}
		if (inputA.length() < inputB.length()
				|| ((inputA.length() == inputB.length()) && inputA
						.compareTo(inputB) < 0)) {
			return UtilityMult(inputB, inputA);
		}
		// perform the multiplication

		for (int i = inputB.length() - 1; i >= 0; i--) {
			// append zero
			StringBuffer temp = new StringBuffer();
			for (int j = i; j < inputB.length() - 1; j++)
				temp.append("0");
			for (int j = inputA.length() - 1; j >= 0; j--) {
				temp.append((((int) inputA.charAt(j) - 48)
						* ((int) inputB.charAt(i) - 48) + carry) % 10);
				// System.out.println(inputA.charAt(j)+ "*"+ inputB.charAt(i)
				// +"carry"+carry);
				carry = (((int) inputA.charAt(j) - 48)
						* ((int) inputB.charAt(i) - 48) + carry) / 10;
			}
			if (carry != 0) {
				temp = temp.append(carry);
				carry = 0;
			}
			result = new StringBuffer(UtilityAdd(result.toString(), temp
					.reverse().toString()));

		}

		return result.toString();
	}

	private String UtilityTrimZero(String inputA) {
		while (inputA.charAt(0) == '0')
			inputA = inputA.substring(1, inputA.length());
		return inputA;
	}

	private String UtilitySub(String inputA, String inputB) {
		StringBuffer sub = new StringBuffer();
		int borrow = 0, i = 0, j = 0;

		// negative subtraction logic -A-B=-(A+B) , A-(-B)=A+B -A-(-B)= B-A

		if (inputA.charAt(0) == '-' && inputB.charAt(0) == '-')
			return UtilitySub(inputB.substring(1, inputB.length()),
					inputA.substring(1, inputA.length()));
		else if (inputA.charAt(0) == '-' || inputB.charAt(0) == '-') {
			if (inputA.charAt(0) == '-')
				return "-"
						+ UtilityAdd(inputA.substring(1, inputA.length()),
								inputB);
			else
				return UtilityAdd(inputA, inputB.substring(1, inputB.length()));
		}
		// trim input with leading zeros
		inputA = UtilityTrimZero(inputA);
		inputB = UtilityTrimZero(inputB);
		// System.out.println(inputA+inputB);
		// return negative subtraction i.e. A<B => -(B-A)
		if (inputA.length() < inputB.length()
				|| ((inputA.length() == inputB.length()) && inputA
						.compareTo(inputB) < 0)) {
			return "-" + UtilitySub(inputB, inputA);
		}

		for (i = inputA.length() - 1, j = inputB.length() - 1; i >= 0 && j >= 0; i--, j--) {
			if (inputA.charAt(i) > inputB.charAt(j)) {
				sub.append((int) (inputA.charAt(i) - 48)
						- (int) (inputB.charAt(j) - 48) - borrow);
				borrow = 0;
			} else if (inputA.charAt(i) < inputB.charAt(j)) {
				sub.append(((int) (10 + inputA.charAt(i) - 48)
						- (int) (inputB.charAt(j) - 48) - borrow) % 10);
				borrow = 1;
			} else {
				if (borrow == 0) {
					sub.append("0");
					borrow = 0;
				} else {
					sub.append(((int) (10 + inputA.charAt(i) - 48)
							- (int) (inputB.charAt(j) - 48) - borrow));
					borrow = 1;
				}
			}
		}
		while (i >= 0) {
			if ((inputA.charAt(i) - 48) < borrow) {
				sub.append(10 + (int) inputA.charAt(i) - 48 - (int) borrow);
				borrow = 1;
			} else {
				sub.append((int) inputA.charAt(i) - 48 - (int) borrow);
				borrow = 0;
			}
			i--;
		}

		return sub.reverse().toString();
	}

	private String UtilityDivison(String inputA, String inputB) {
		String result = null;
		// sign of the result if -A*-B=(AB) -A*B=-(A*B) vice versa
		if (inputA.charAt(0) == '-' && inputB.charAt(0) == '-')
			return UtilityDivison(
					UtilityTrimZero(inputA.substring(1, inputA.length())),
					UtilityTrimZero(inputA.substring(1, inputA.length())));
		// if either of them is negative check - as the start symbol
		else if (inputA.charAt(0) == '-' || inputB.charAt(0) == '-') {

			inputA = inputA.indexOf('-') == 0 ? UtilityTrimZero(inputA
					.substring(1, inputA.length())) : UtilityTrimZero(inputA);
			inputB = inputB.indexOf('-') == 0 ? UtilityTrimZero(inputB
					.substring(1, inputB.length())) : UtilityTrimZero(inputB);

			return "-" + UtilityDivison(inputA, inputB);
		}

		inputA = UtilityTrimZero(inputA);
		inputB = UtilityTrimZero(inputB);

		while (inputA.length() > inputB.length()
				|| ((inputA.length() == inputB.length()) && inputA
						.compareTo(inputB) > 0)) {
			inputA = UtilityTrimZero(UtilitySub(inputA, inputB));
		}
		return result;

	}

	// taken from http://rosettacode.org/wiki/Levenshtein_distance#Java
	private int distance(String a, String b) {
		a = a.toLowerCase();
		b = b.toLowerCase();
		// i == 0
		int[] costs = new int[b.length() + 1];
		for (int j = 0; j < costs.length; j++)
			costs[j] = j;
		for (int i = 1; i <= a.length(); i++) {
			// j == 0; nw = lev(i - 1, j)
			costs[0] = i;
			int nw = i - 1;
			for (int j = 1; j <= b.length(); j++) {
				int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
						a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
				nw = costs[j];
				costs[j] = cj;
			}
		}
		return costs[b.length()];
	}

}
