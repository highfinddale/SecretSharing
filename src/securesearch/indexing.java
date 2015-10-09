/**
 * 
 */
package securesearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import secretshare.SecretShare;
import de.l3s.icrawl.online.TextRankWrapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.lucene.util.fst.NoOutputs;

/**
 * @author Dell
 *
 */
public class indexing {
	private static TextRankWrapper wrapper = new TextRankWrapper();

	/** keyword identifier and Index builder */

	public HashMap<String, List<String>> KeywordExtract(String fileDirectory,
			int noKeywords) {
		final HashMap<String, List<String>> keyWordIndex = new HashMap<String, List<String>>();
		final int noKeywordsFinal = noKeywords;
		// get text rank for each file and then create the index
		long startTime = System.currentTimeMillis();

		try {
			Files.walkFileTree(Paths.get(fileDirectory),
					new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file,
								BasicFileAttributes attr) {
							if (attr.isSymbolicLink()) {
								System.out.format("Symbolic link: %s ", file); // do
																				// not
																				// process
																				// symbolic
																				// link
							} else if (attr.isRegularFile()) {

								System.out.format("Regular file: %s ", file);
								// generate keyword and merge the list of
								// keywords
								List<String> keywords = TextRank(file
										.toAbsolutePath().toString(),
										noKeywordsFinal);
								Iterator<String> itr = keywords.iterator();
								while (itr.hasNext()) {
									String word = itr.next();
									if (keyWordIndex.containsKey(word)) {
										ArrayList<String> element = (ArrayList<String>) keyWordIndex
												.get(word);
										// element.add(file.toAbsolutePath().toString()+":"+file.getFileName().toString());
										element.add(file.toAbsolutePath()
												.toString());
										keyWordIndex.put(word, element);
									} else {
										ArrayList<String> element = new ArrayList<String>();
										// element.add(file.toAbsolutePath().toString()+":"+file.getFileName().toString());
										element.add(file.toAbsolutePath()
												.toString());
										keyWordIndex.put(word, element);
									}

								}
								// merge with previous hashmap

							} else {
								System.out.format("Other: %s ", file);
							}
							System.out.println("(" + attr.size() + "bytes)");
							return FileVisitResult.CONTINUE;
						}

						// Print each directory visited.
						@Override
						public FileVisitResult postVisitDirectory(Path dir,
								IOException exc) {
							System.out.format("Directory: %s%n", dir);
							return FileVisitResult.CONTINUE;
						}

						// If there is some error accessing
						// the file, let the user know.
						// If you don't override this method
						// and an error occurs, an IOException
						// is thrown.
						@Override
						public FileVisitResult visitFileFailed(Path file,
								IOException exc) {
							System.err.println(exc);
							return FileVisitResult.CONTINUE;
						}

					});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("Time to create the keyword Index"
				+ (double) endTime / 1000);

		return keyWordIndex;
	}

	private Map<String, Integer> termFrequency_File(String filePath) {
		HashMap<String, Integer> terms = new HashMap<String, Integer>();
		try {
			@SuppressWarnings("resource")
			List<String> sentences = Arrays.asList(new BufferedReader(
					new FileReader(filePath)).toString().split("/z[.|?|;]"));
			Iterator<String> itr = sentences.iterator();
			while (itr.hasNext()) {
				List<String> words = Arrays.asList(itr.next().split(" "));
				Iterator<String> wordItr = words.iterator();
				while (wordItr.hasNext()) {
					String element = wordItr.next();
					if (terms.containsKey(element))
						terms.put(element.toUpperCase(),
								terms.get(element.toUpperCase()) + 1);
					else
						terms.put(element.toUpperCase(), 1);
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return terms;
	}

	private List<String> TextRank(String filePath, int noKeywords) {
		List<String> textRankKeywords = new ArrayList<String>();
		String text = "";
		try {
			Iterator<String> lines = new BufferedReader(
					new FileReader(filePath)).lines().iterator();
			while (lines.hasNext())
				text += lines.next();
			textRankKeywords = wrapper.rank(text, Locale.ENGLISH, noKeywords);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return textRankKeywords;
	}

	public String getHash(String text) {

		byte[] output = null;

		try {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			// System.out.println(String.valueOf(Hex.encodeHex(text.getBytes("UTF-8"))));
			output = digest.digest(text.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return String.valueOf(Hex.encodeHex(output));

	}

	public List<String> ObfuscateIndex(String paragraphFile,
			String languageModel, String indexStore,
			HashMap<String, List<String>> index)
			throws NoSuchAlgorithmException {

		// read each keyword obfuscate and rearrange
		List<String> keywords = new ArrayList<String>();
		HashMap<String, List<String>> result = new HashMap<String, List<String>>();
		SecretShare s = new SecretShare();
		HashMap<String, HashMap<String, String>> TerminalCodes = new HashMap<String, HashMap<String, String>>();// stores
																												// code
																												// maps

		long startTime = System.currentTimeMillis();
		TerminalCodes = s.CreateAugmentedCFG(paragraphFile, languageModel);
		long endTime = System.currentTimeMillis() - startTime;
		System.out.println("Time to create the CFG" + (double) endTime / 1000);

		startTime = System.currentTimeMillis();
		Iterator<String> keyWordSet = index.keySet().iterator();
		while (keyWordSet.hasNext()) {
			String element = keyWordSet.next();
			// System.out.println("\n"+ s.Encode(element, TerminalCodes)+
			// ":"+element);
			String encoded = getHash(s.Encode(element, TerminalCodes));
			keywords.add(element);
			result.put(encoded, index.get(element)); // apply simple hashing to
														// the element
		}

		Iterator<Map.Entry<String, List<String>>> resultItr = result.entrySet()
				.iterator();

		// write down the index
		try {

			BufferedWriter destination = new BufferedWriter(new FileWriter(
					indexStore));
			while (resultItr.hasNext()) {

				Entry<String, List<String>> element = resultItr.next();
				String toWrite = element.getKey();
				Iterator<String> val = element.getValue().iterator();
				while (val.hasNext())
					toWrite += ">" + val.next();
				toWrite += "\n";
				destination.write(toWrite);

			}
			endTime = System.currentTimeMillis() - startTime;
			System.out.println("Time to obfuscate and Print" + (double) endTime
					/ 1000);

			destination.close();
		} catch (IOException io) {
			System.err.append(io.getMessage());
		}
		// System.out.println(index.keySet().toArray()[1].toString());
		// System.out.println(getHash(s.Encode(index.keySet().toArray()[1].toString(),
		// TerminalCodes)));
		return keywords;

	}

	/*
	 * file tree copy final Path source = ... final Path target = ...
	 * 
	 * Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS),
	 * Integer.MAX_VALUE, new SimpleFileVisitor<Path>() {
	 * 
	 * @Override public FileVisitResult preVisitDirectory(Path dir,
	 * BasicFileAttributes attrs) throws IOException { Path targetdir =
	 * target.resolve(source.relativize(dir)); try { Files.copy(dir, targetdir);
	 * } catch (FileAlreadyExistsException e) { if
	 * (!Files.isDirectory(targetdir)) throw e; } return CONTINUE; }
	 * 
	 * @Override public FileVisitResult visitFile(Path file, BasicFileAttributes
	 * attrs) throws IOException { Files.copy(file,
	 * target.resolve(source.relativize(file))); return CONTINUE; } });
	 */
	public String obfuscateText(String text, String paragraphFile,
			String languageModel) {
		String hashValue = "";
		SecretShare s = new SecretShare();
		HashMap<String, HashMap<String, String>> TerminalCodes = new HashMap<String, HashMap<String, String>>();// stores
																												// code
																												// maps
		TerminalCodes = s.CreateAugmentedCFG(paragraphFile, languageModel);
		hashValue = getHash(s.Encode(text, TerminalCodes));
		return hashValue;
	}

}
