/*
 * Copyright (C) 2013 SpidaWeb LLC, http://www.spidasoftware.com
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spidasoftware.EclipseFormatter;

import org.apache.log4j.Logger;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Appender;
import org.apache.log4j.PatternLayout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.util.ArrayList;
import java.util.Arrays;
import org.apache.log4j.LogManager;

/**
 * 
 * Extracted various classes from Eclipse and Groovy-Eclipse to create an automatic java 
 * and groovy source code formatter.
 *
 * This class, containing a main method, will take in command-line arguments and call on the JavaFormat
 * and GroovyFormat classes to automatically format java and groovy files.
 *
 * @author Nicholas Joodi
 */
public class Formatter {
	private static Logger log = Logger.getRootLogger();
	private static Options options = new Options();

	/**
	 * This method will pass the command line arguments to the runFormatter method. 
	 * @param args the command-line arguments
	 */
	public static void main(String[] args) {
		instantiateLogger();
		runFormatter(args);
		LogManager logManager = new LogManager();
		logManager.shutdown();
	}

	/**
	 * This method will perform the main logic of the program
	 * @param args the command-line arguments
	 */
	public static void runFormatter(String[] args) {
		CommandLine cmd = getOptions(args, options);
		args = cmd.getArgs();
		if (cmd.hasOption("help")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("format [option] <directory or file with relative path>", options);
		} else if (cmd.hasOption("version")) {
			log.info("Format");
			log.info("java and groovy eclipse-formatter");
			log.info("version 1.0");
		} else {
			optionToFormat(cmd, args);
		}
	}

	/**
	 * A two argument method that takes a Command-Line object, as well as the string [], args,
	 * and initiates the formatting.
	 *
	 * @param cmd the list of command-line arguments.
	 * @param args the command-line arguments.
	 * @return  boolean indicating if the file/directory exists
	 */
	public static boolean optionToFormat(CommandLine cmd, String[] args) {
		boolean exists = false;
		if (args.length != 1) {
			log.info("Exactly one file can be formatted");
		} else {
			File pathToFile = new File(args[0]);
			if (pathToFile.isDirectory()) {
				exists = true;
				ArrayList<File> files = new ArrayList<File>(Arrays.asList(pathToFile.listFiles()));
				for (File f : files) {
					if (f.isFile()) {
						formatOne(f, cmd);
					}
				}
			} else if (pathToFile.isFile()) {
				formatOne(pathToFile, cmd);
				exists = true;
			} else {
				log.info("cannot format: " + args[0] + ".");
			}
		}
		return exists;
	}

	/**
	 * A three-argument method that will take a filename string, the contents of 
	 * that file as a string (before formatted), and the Command-Line arguments and
	 * format the respective file.
	 *
	 * @param file File that will be formatted
	 * @param cmd the list of command-line arguments.
	 */
	public static String formatOne(File file, CommandLine cmd) {
		String nameWithDate = null;
		String extension = FilenameUtils.getExtension(file.getPath());
		if (extension.length() > 0) {
			nameWithDate = formatUsingExtension(file, cmd);
		} else {
			nameWithDate = formatUsingHashBang(file, cmd);
		}
		return nameWithDate;
	}

	/**
	 * A one-argument method that will take a filename and return a string containing 
	 * the contents of that file.
	 * 
	 * @param fileName The name of that file.
	 * @return a String that contans the contents of that file.
	 */
	@SuppressWarnings("resource")
	public static String readInFile(String fileName) {
		String code = null;
		try {
			File file = new File(fileName);
			code = FileUtils.readFileToString(file, null);
		} catch (IOException e) {
			log.error("Error occured when opening " + fileName);
			return null;
		}
		return code;
	}

	/**
	 * A two-argument method that will take a fileName string and the contents of 
	 * that file (before formatted), and return a backup file.
	 *
	 * @param fileName  string representing the name of the file.
	 * @param before  string containing the contents of that file
	 * @return a String that represents the name of the backup file created, null otherwise.
	 *
	 */
	public static String createBackupFile(String fileName, String before) {
		String nameWithDate = null;
		PrintWriter safety = null;
		try {
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_yyyy_h_mm_ss");
			String formattedDate = sdf.format(date);
			nameWithDate = fileName + "_BACKUP_" + formattedDate;
			FileWriter file = new FileWriter(nameWithDate);
			safety = new PrintWriter(file);
			safety.print(before);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (safety != null) {
				safety.close();
			}
		}

		return nameWithDate;
	}

	/**
	 * A no-argument method that will return the command-line arguments as a 
	 * CommandLine object.
	 *
	 * @param args The command line arguments. 
	 * @param options The options that this class can identify.
	 * @return the CommandLine object holding the command line arguments.
	 */
	public static CommandLine getOptions(String[] args, Options options) {
		options.addOption("b", false, "create a backup file");
		options.addOption("help", false, "print this message");
		options.addOption("version", false, "version of the formatter");
		options.addOption("java", false, "only format java files");
		options.addOption("groovy", false, "only format groovy files");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			log.error(e, e);
		}
		return cmd;
	}

	/**
	 * A no-argument method used to intantiate the logger.
	 */
	public static void instantiateLogger() {
		ConsoleAppender console = new ConsoleAppender();
		String PATTERN = "%m%n";
		console.setLayout(new PatternLayout(PATTERN));
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
		log.addAppender(console);
	}

	/**
	 * Format by using the extension of the file.
	 *
	 * @param file File that will be formatted
	 * @param cmd The list of command line arguments
	 * @return a String that represents the name of the backup file created, null otherwise.
	 */
	public static String formatUsingExtension(File file, CommandLine cmd) {
		String fileName = file.getPath();
		String extension = fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length());
		String nameWithDate = null;
		String code = null;
		if (extension.equals("java") && javaFormatting(cmd)) {
			code = readInFile(fileName);
			Format javaFormatter = new JavaFormat();
			javaFormatter.format(fileName, code);
			if (javaFormatter.isFormatted() && cmd.hasOption("b"))
				nameWithDate = createBackupFile(fileName, code);

		} else if ((extension.equals("groovy")) && groovyFormatting(cmd)) {
			code = readInFile(fileName);
			Format groovyFormatter = new GroovyFormat();
			groovyFormatter.format(fileName, code);
			if (groovyFormatter.isFormatted() && cmd.hasOption("b"))
				nameWithDate = createBackupFile(fileName, code);

		} else {
			//log.info("Sorry, no formatting could be applied to " + fileName);
		}
		return nameWithDate;
	}

	/**
	 * Format by using the first line of the file.
	 *
	 * @param file File that will be formatted
	 * @param cmd the list of command line arguments.
	 * @return a String that represents the name of the backup file created, null otherwise.
	 */
	public static String formatUsingHashBang(File file, CommandLine cmd) {
		String fileName = file.getPath();
		String nameWithDate = null;
		String code = readInFile(fileName);
		if (code.indexOf("\n") > -1) {
			String firstLine = code.substring(0, code.indexOf("\n"));
			if (firstLine.indexOf("#!/usr/bin/env groovy") > -1 && groovyFormatting(cmd)) {
				GroovyFormat groovyFormatter = new GroovyFormat();
				groovyFormatter.format(fileName, code);
				if (groovyFormatter.isFormatted() && cmd.hasOption("b"))
					nameWithDate = createBackupFile(fileName, code);
			}
		}
		return nameWithDate;
	}

	/* 
	 * To decouple the groovy and java formatters, the next two methods were created:
	 */

	/** 
	 * If groovy option was set, return false
	 *
	 * @param cmd which contains the options passed in
	 * @return a boolean that is false if a groovy option was passed
	 */
	public static boolean javaFormatting(CommandLine cmd) {
		boolean java = true;
		if (cmd.hasOption("groovy")) {
			java = false;
		}
		return java;
	}

	/** 
	 * If java option was set, return false
	 *
	 * @param cmd which contains the options passed in
	 * @return a boolean that is false if a java option was passed
	 */
	public static boolean groovyFormatting(CommandLine cmd) {
		boolean groovy = true;
		if (cmd.hasOption("java")) {
			groovy = false;
		}
		return groovy;
	}

}

