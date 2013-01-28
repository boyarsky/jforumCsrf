package jb.csrf;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.io.*;
import org.junit.*;

/**
 * Lists any files missing HTML head section excluding those which are included
 * in another page. Note: directory structure is different than on coderanch -
 * this goes with stock jforum 2.1.8.
 * 
 * @author Jeanne Boyarsky
 * @version $Id: $
 */
public class MissingHeadTagTest {
	private static final String JFORUM_DIRECTORY = "/Users/nyjeanne/Documents/workspace/JForum_original";
	private static Pattern INCLUDED_FILES_PATTERN = Pattern.compile(
			"<#include \"([^\"]+)\"", Pattern.DOTALL);
	private List<String> filesMissingHead;
	private Set<String> includedFiles;
	private Set<String> knownFilesWithoutHead;

	@Before
	public void setUp() {
		filesMissingHead = new ArrayList<String>();
		includedFiles = new HashSet<String>();
		knownFilesWithoutHead = new HashSet<String>();
		knownFilesWithoutHead.add("empty.htm");
		knownFilesWithoutHead.add("plain_text.htm");
		// user form includes another file that includes the header
		knownFilesWithoutHead.add("user_form.htm");
		knownFilesWithoutHead.add("rss.htm");
		knownFilesWithoutHead.add("rss_template.htm");
		// used as snippet on javaranch.com
		knownFilesWithoutHead.add("highlights_table.htm");
		knownFilesWithoutHead.add("ajax_json.htm");
		knownFilesWithoutHead.add("ajax_load_post.htm");
		knownFilesWithoutHead.add("banner_show.htm");
	}

	// ----------------------------------------------------------------
	@Test
	public void pagesWithMissingTitle() throws Exception {
		String htmlDirectory = JFORUM_DIRECTORY + "/templates";
		visitHtmlFiles(htmlDirectory, new File(htmlDirectory));
		removeIncludedFiles();
		removeKnown();
		assertEquals(
				"files must have a </head> tag or import the header.htm file. (needed for CSRF filter)  Missing: "
						+ filesMissingHead, 0, filesMissingHead.size());
	}

	private void removeIncludedFiles() {
		for (String string : new ArrayList<String>(filesMissingHead)) {
			String fileName = string.replaceFirst("^.*[/\\\\]", "");
			if (includedFiles.contains(fileName)) {
				filesMissingHead.remove(string);
			}
		}
	}

	private void removeKnown() {
		for (String string : new ArrayList<String>(filesMissingHead)) {
			String fileName = string.replaceFirst("^.*[/\\\\]", "");
			if (knownFilesWithoutHead.contains(fileName)) {
				filesMissingHead.remove(string);
			}
		}
	}

	private void visitHtmlFiles(String commonDirectory, File source)
			throws Exception {
		File[] dir = source.listFiles();
		for (File file : dir) {
			// if a directory other than version control, recurse
			if (file.isDirectory() && !file.getName().startsWith(".")
					&& !file.getName().equals("macros")) {
				visitHtmlFiles(commonDirectory, file);
			}
			if (file.getName().endsWith(".htm")
					|| file.getName().endsWith(".ftl")) {
				// files missing </head>
				String content = FileUtils.readFileToString(file);
				if (!content.toLowerCase().contains("</head>")
						&& !content.contains("header.htm")
						&& !content.contains("header.ftl")) {
					filesMissingHead.add(file.getAbsolutePath().replaceFirst(
							commonDirectory, ""));
				}
				// files included in another (which don't need head
				Matcher matcher = INCLUDED_FILES_PATTERN.matcher(content);
				while (matcher.find()) {
					includedFiles.add(matcher.group(1));
				}
			}
		}
	}
}
