/**
 * 
 */
package securesearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

;

/**
 * @author Dell
 *
 */
public class searchMain {

	/**
	 * @param args
	 * 
	 */
	static HashMap<String, List<String>> keyWordIndex = new HashMap<String, List<String>>();
	static List<String> keyWords = new ArrayList<String>();

	static void PKSE(int noKeyWords) {

		String path = "D:\\Users\\WorkSpace\\Trial\\Resource\\Input";
		indexing in = new indexing();

		keyWordIndex = in.KeywordExtract(path, noKeyWords);
		String para = "D:\\Users\\WorkSpace\\Trial\\Resource\\sampleParagraph.txt";
		String languageModel = "D:\\Users\\WorkSpace\\Trial\\Resource\\english-bidirectional-distsim.tagger";
		String indexStore = "D:\\Users\\WorkSpace\\Trial\\Resource\\IndexStore";

		try {
			keyWords = in.ObfuscateIndex(para, languageModel, indexStore,
					keyWordIndex);
			// System.out.println(keyWordIndex);
			// System.out.println(keyWords);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static String search(String text, String passKey) {
		String filePath = "";
		String indexStore = "D:\\Users\\WorkSpace\\Trial\\Resource\\IndexStore";
		String para = "D:\\Users\\WorkSpace\\Trial\\Resource\\sampleParagraph.txt";
		String languageModel = "D:\\Users\\WorkSpace\\Trial\\Resource\\english-bidirectional-distsim.tagger";
		String hashKey = "";
		if (keyWords.contains(text)) {
			try {
				if (passKey == "SECRETPASS") {
					indexing in = new indexing();
					hashKey = in.obfuscateText(text, para, languageModel);
					Iterator<String> lines = new BufferedReader(new FileReader(
							indexStore)).lines().iterator();
					while (lines.hasNext()) {
						String data = lines.next();
						if (data.split(">")[0].equals(hashKey))
							filePath = data.split(">")[1];
					}
					if (filePath == "")
						filePath = "Search Not Found";
				} else {
					filePath = "Invalid access code";
				}

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return filePath;

	}

	static String getFileSize(String filePath) {
		String size = "";
		Path p = Paths.get(new File(filePath).getAbsolutePath());
		try {
			BasicFileAttributes view = Files.getFileAttributeView(p,
					BasicFileAttributeView.class).readAttributes();
			// get file storage space
			FileStore f = Files.getFileStore(p);
			size = String
					.valueOf((f.getTotalSpace() - f.getUnallocatedSpace()) / 1024);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return size;
	}

	public static void main(String[] args) {
		// get total number of files
		int noFiles = new File("D:\\Users\\WorkSpace\\Trial\\Resource\\Input")
				.listFiles().length;
		// total keywords noFiles* keywords per file
		long startTime = System.currentTimeMillis();
		PKSE(50);
		long endTime = System.currentTimeMillis() - startTime;
		// get size of index file
		double fileSize = Double.valueOf(new File(
				"D:\\Users\\WorkSpace\\Trial\\Resource\\IndexStore").length()) / 1024;
		// System.out.println("Index File size :"+fileSize);
		// summary report

		// System.out.println(search("pages", "SECRETPASS"));
		System.out.println("Total file in Store " + noFiles);
		System.out.println("No of Keywords " + keyWords.size());
		System.out.println("Size of Index File " + fileSize);
		System.out.println("TIme to create the indexFile in sec "
				+ (double) endTime / 1000);
	}
}
