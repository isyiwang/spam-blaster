package filtering;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

public class SpamFilter {
	/** Class to associate a spam token with its spamicity - Used to easily sort spam tokens */
	private class SpamToken {
		private String name;
		private double spamicity;
		
		public SpamToken(String name, double spamicity) {
			this.setName(name);
			this.setSpamicity(spamicity);
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
		
		public void setSpamicity(double spamicity) {
			this.spamicity = spamicity;
		}

		public double getSpamicity() {
			return spamicity;
		}
	}
	
	/** Comparator to compare SpamTokens */
	public class SpamComparator implements Comparator<SpamToken> {
		@Override
		public int compare(SpamToken token1, SpamToken token2) {
			return (token1.getSpamicity() > token2.getSpamicity()? -1 : (token1.getSpamicity() == token2.getSpamicity()? 0 : 1));
		}
	}
	
	// Instance Variables
	private HashMap<String, Integer> spamTokenCounter;
	private HashMap<String, Integer> hamTokenCounter;
	private HashMap<String, Double> spamicity;
	private int spamCount;
	private int hamCount;
	
	// Constants
	private static final int MAX_NUM_TOKENS = 15;
	
	public SpamFilter() {
		spamTokenCounter = new HashMap<String, Integer>();
		hamTokenCounter = new HashMap<String, Integer>();
		spamicity = new HashMap<String, Double>();
		spamCount = 0;
		hamCount = 0;
	}
	
	/** Reads the spam message and trains filter against tokens in the message */
	public void addSpam(String file) throws IOException {
		spamTokenCounter = countTokens(file);
		spamCount++;
	}

	/**  Reads the ham message and trains filter against tokens in the message */
	public void addHam(String file) throws IOException {
		hamTokenCounter = countTokens(file);
		hamCount++;
	}
	
	/** Updates spamicity values of all spam tokens.  Spamicity is measured by a Bayesian classifier
	 * 	TODO: Use a better algorithm to measure spamicity
	 */
	public void updateSpamicity() {
		for (String token : spamTokenCounter.keySet()) {
			double value = 0.5;
			
			// Check that the token is valid
			if (spamTokenCounter.containsKey(token) && hamTokenCounter.containsKey(token)) {
				value = (spamTokenCounter.get(token) / spamCount) / (spamTokenCounter.get(token) / spamCount + hamTokenCounter.get(token) / hamCount);
			}
			
			spamicity.put(token, Math.abs(0.5 - value));
		}
	}
	
	/** Checks if the message is spam or not by performing the following:
	 * 	1 - Tokenize message in file
	 * 	2 - Sort tokens in order to measure only highest rated tokens
	 * 	3 - Combine token spamicity scores to determine whether or not message is spam
	 */
	public boolean isSpam(String file) throws IOException {
		// Initialize SpamToken list to easily sort
		ArrayList<SpamToken> spamTokens = new ArrayList<SpamToken>();
		
		// Tokenize message into SpamToken objects
		ArrayList<String> tokens = tokenizeMessage(file);
		for (String token : tokens) {
			spamTokens.add(new SpamToken(token, spamicity.get(token)));
		}
		
		// Take top MAX_NUM_TOKENS words with highest spamicity value
		Collections.sort(spamTokens, new SpamComparator());
		spamTokens = (ArrayList<SpamToken>) spamTokens.subList(0, MAX_NUM_TOKENS);
		
		// Calculate message spamicity by using Bayes' Theorem
		double n = 0;
		for (SpamToken token : spamTokens) {
			n += (Math.log(1 - token.getSpamicity()) - Math.log(token.getSpamicity()));
		}
		double messageSpamicity = 1.0 / (1.0 + Math.exp(n));
		
		// Train filter with message
		if (messageSpamicity > 0.5) {
			// Spam detected
			addSpam(file);
			return true;
		} else {
			// Ham detected
			addHam(file);
			return false;
		}
	}
	
	/** Trains filter with spam messages */
	public void addSpam(ArrayList<String> files) throws IOException {
		for (String file : files) {
			addSpam(file);
		}
	}
	
	/** Trains filter with ham messages */
	public void addHam(ArrayList<String> files) throws IOException {
		for (String file : files) {
			addHam(file);
		}
	}
	
	/** Reads a message from a file, counts the number of tokens in the message, and
	 *  returns the frequency of tokens as a hashmap
	 */
	private HashMap<String, Integer> countTokens(String file) throws IOException {
		// Initialize
		HashMap<String, Integer> tokenCount = new HashMap<String, Integer>();
		
		// Count number of tokens in message
		for (String token : tokenizeMessage(file)) {
			if (tokenCount.containsKey(token)) {
				tokenCount.put(token, tokenCount.get(token) + 1);
			} else {
				tokenCount.put(token, 1);
			}
		}
		
		return tokenCount;
	}
	
	/** Splits a message into an arraylist of tokens.  Currently only looks for message tokens.
	 *	TODO: Reimplement this method to account for To, From, Subject, and Return-path fields
	 */	
	private ArrayList<String> tokenizeMessage(String file) throws IOException {
		// Initialize
		FileReader fileReader = new FileReader(file);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		String line;
		boolean parse = false;
		ArrayList<String> tokens = new ArrayList<String>();
		
		// Read all lines and parse only the lines after 'Date:'
		while ((line = bufferedReader.readLine()) != null) {
			if (!parse) {
				// Check for empty line to look only at message
				//parse = (line.indexOf("Date:") != -1);
				parse = (line.isEmpty());
				continue;
			}
			
			//System.out.println("Valid line: " + line);
			//String ending = "";
			// Add tokens
			for (String token : line.split("[ ,.-]+")) {
				tokens.add(token);
				//System.out.print(ending + token);
				//ending = ", ";
			}
			//System.out.println(" \n");
		}
		
		// Remove duplicates by converting to HashSet then back to ArrayList
		HashSet<String> set = new HashSet<String>();
		set.addAll(tokens);
		tokens.clear();
		tokens.addAll(set);
		
		// Debug
		/*
		System.out.println("Number of keys: " + tokens.size());
		String ending = "";
		for (String token : tokens) {
			System.out.print(ending + token);
			ending = ", ";
		}
		*/
		
		return tokens;
	}
	
	/** Returns all filenames in a directory */
	public static ArrayList<String> getFilenames(String directory) {
		ArrayList<String> filenames = new ArrayList<String>();
		File folder = new File(directory);
		
		for (File file : folder.listFiles()) {
			if (file.isFile()) {
				filenames.add(file.getAbsolutePath());
			}
		}
		
		return filenames;
	}
	
	public static void main(String[] args) throws IOException {
		// Initialize
		SpamFilter filter = new SpamFilter();
		System.out.println("Spam Blaster - Isaac Wang");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		// Train filter with spam and ham
		System.out.print("Set directory with spam emails: ");
		String spamDirectory = br.readLine();
		ArrayList<String> spamFiles = getFilenames(spamDirectory);
		System.out.println("Added " + spamFiles.size() + " spam files to filter");
		filter.addSpam(spamFiles);
		
		System.out.print("Set directory with ham emails: ");
		String hamDirectory = br.readLine();
		ArrayList<String> hamFiles = getFilenames(hamDirectory);
		System.out.println("Added " + hamFiles.size() + " ham files to filter");
		filter.addHam(hamFiles);
		
		// Process spamicity values
		filter.updateSpamicity();
		
		// Test unfiltered messages
		System.out.print("Set directory with unfiltered emails: ");
		String unfilteredDirectory = br.readLine();
		ArrayList<String> unfilteredFiles = getFilenames(unfilteredDirectory);
		int spamCount = 0;
		for (String file: unfilteredFiles) {
			if (filter.isSpam(file)) {
				System.out.println("Spam detected: " + file);
				spamCount++;
			}
		}
		
		System.out.println("Detected " + spamCount + " / " + unfilteredFiles.size() + " spam messages");
	}
}
