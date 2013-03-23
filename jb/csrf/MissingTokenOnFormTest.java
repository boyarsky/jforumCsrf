package jb.csrf;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.io.*;
import org.apache.commons.lang.*;
import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;
import org.junit.runners.Parameterized.Parameters;

import com.javaranch.asserts.*;

/**
 * Tests each form has an OWASP token.
 * 
 * @version $Id: $
 */
@RunWith(Parameterized.class)
public class MissingTokenOnFormTest {
    private File file;
    private String fileName;
    private String content;

    public MissingTokenOnFormTest(File _file) {
        file = _file;
    }

    @Before
    public void setUp() throws Exception {
        String jforumDirectory = JavaRanchTestUtil.getJforumDirectory();
        fileName = file.getAbsolutePath().replaceFirst(jforumDirectory, "");
        content = FileUtils.readFileToString(file);
    }

    @After
    public void tearDown() {
        file = null;
        fileName = null;
        content = null;
    }

    // ----------------------------------------------------------------
    @Parameters
    public static Collection<Object[]> fileNamesToCheck() throws Exception {
        String jforumDirectory = JavaRanchTestUtil.getJforumDirectory();
        String htmlDirectory = jforumDirectory + "/src/main/webapp/templates";
        return findHtmlFiles(htmlDirectory, new File(htmlDirectory));
    }

    private static Collection<Object[]> findHtmlFiles(String commonDirectory, File source) throws Exception {
        Collection<Object[]> result = new ArrayList<Object[]>();
        File[] dir = source.listFiles();
        for (File file : dir) {
            // if a directory other than version control, recurse
            if (file.isDirectory() && !file.getName().startsWith(".") && !file.getName().equals("macros")) {
                result.addAll(findHtmlFiles(commonDirectory, file));
            }
            if (file.getName().endsWith(".htm") || file.getName().endsWith(".ftl")) {
                result.add(new Object[] { file });
            }
        }
        return result;
    }

    // ----------------------------------------------------------------
    @Test
    public void pagesMissingToken() throws Exception {
        int numForms = countNumberForms(content, "<form[^>]+method=['\"]post['\"]");
        int numTokens = StringUtils.countMatches(content, "name=\"OWASP_CSRFTOKEN\"");
        assertEquals("forms must have a OWASP token set as a hidden field.  Missing " + (numForms - numTokens)
                + " in: " + fileName, numForms, numTokens);
    }

    @Test
    public void multipartRequestNeedsTokenInUrl() throws Exception {
        int numForms = countNumberForms(content, "<form[^>]+multipart/form-data");
        int numTokens = countNumberForms(content, "<form[^>]+multipart/form-data[^>]+OWASP_CSRFTOKEN=")
                + countNumberForms(content, "<form[^>]+OWASP_CSRFTOKEN=[^>]+multipart/form-data");
        assertEquals(
                "multipart forms must have a OWASP token set as a parameter in the URL due to how JForum is implemented. "
                        + (numForms - numTokens) + " in: " + fileName, numForms, numTokens);
    }

    @Test
    public void noMethod() throws Exception {
        int numForms = countNumberForms(content, "<form(?![^>]*method)[^>]*>");
        int numBlankActions = countNumberForms(content, "<form[^>]*action=['\"]['\"][^>]*>");
        int numMissingActions = countNumberForms(content, "<form(?![^>]*action)[^>]*>");
        assertEquals(
                "Forms should be get or post unless have no action (and is therefore just used for javascript on page): "
                        + fileName, numBlankActions + numMissingActions, numForms);
    }

    private int countNumberForms(String content, String patternString) {
        Pattern pattern = Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
