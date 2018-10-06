/*
// Licensed to Julian Hyde under one or more contributor license
// agreements. See the NOTICE file distributed with this work for
// additional information regarding copyright ownership.
//
// Julian Hyde licenses this file to you under the Modified BSD License
// (the "License"); you may not use this file except in compliance with
// the License. You may obtain a copy of the License at:
//
// http://opensource.org/licenses/BSD-3-Clause
*/
package sqlline;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static sqlline.SqlLineHighlighterLowLevelTest.ExpectedHighlightStyle;
import static sqlline.SqlLineHighlighterLowLevelTest.getSqlLine;

/**
 * Tests for sql and command syntax highlighting in sqlline.
 */
@RunWith(JMockit.class)
public class SqlLineHighlighterTest {

  private Map<SqlLine, SqlLineHighlighter> sqlLine2HighLighter = null;

  /**
   * To add your color scheme to tests just put sqlline object
   * with corresponding highlighter into the map like below.
   * @throws Exception if error while sqlline initialization happens
   */
  @Before
  public void setUp() throws Exception {
    sqlLine2HighLighter = new HashMap<>();
    SqlLine defaultSqlline = getSqlLine(SqlLineProperty.DEFAULT);
    SqlLine darkSqlLine = getSqlLine("dark");
    SqlLine lightSqlLine = getSqlLine("light");
    sqlLine2HighLighter
        .put(defaultSqlline, new SqlLineHighlighter(defaultSqlline));
    sqlLine2HighLighter.put(darkSqlLine, new SqlLineHighlighter(darkSqlLine));
    sqlLine2HighLighter.put(lightSqlLine, new SqlLineHighlighter(lightSqlLine));
  }

  @After
  public void tearDown() {
    sqlLine2HighLighter = null;
  }

  @Test
  public void testCommands() {
    String[] linesRequiredToBeCommands = {
        "!set",
        "!commandhandler",
        "!quit",
        "!isolation",
        "!dbinfo",
        "!help",
        "!connect"
    };

    for (String line : linesRequiredToBeCommands) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.commands.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testKeywords() {
    String[] linesRequiredToBeKeywords = {
        "from",
        "outer",
        "select",
        "values",
        "where",
        "join",
        "cross"
    };

    for (String line : linesRequiredToBeKeywords) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.keywords.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testSingleQuotedStrings() {
    String[] linesRequiredToBeSingleQuoted = {
        "'from'",
        "''''",
        "''",
        "'",
        "'test '' \n''select'",
        "'/* \n'",
        "'-- \n--'",
        "'\"'"
    };

    for (String line : linesRequiredToBeSingleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.singleQuotes.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testSqlIdentifierQuotes() {
    // default sql identifier is a double quote
    // {@code SqlLineHighlighter#DEFAULT_SQL_IDENTIFIER_QUOTE}.
    String[] linesRequiredToBeDoubleQuoted = {
        "\"",
        "\"\"",
        "\"from\"",
        "\"''\"",
        "\"test '' \n''select\"",
        "\"/* \\\"kjh\"",
        "\"/*   \"",
        "\"--   \"",
        "\"\n  \n\""
    };

    for (String line : linesRequiredToBeDoubleQuoted) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.sqlIdentifierQuotes.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testCommentedStrings() {
    String[] linesRequiredToBeComments = {
        "-- 'asdasd'asd",
        "--select",
        "/* \"''\"",
        "/*",
        "--",
        "--\n/*",
        "/* kh\n'asd'ad*/",
        "/*\"-- \"values*/"
    };

    for (String line : linesRequiredToBeComments) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.comments.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testNumberStrings() {
    String[] linesRequiredToBeNumbers = {
        "123456789",
        "0123",
        "1"
    };

    for (String line : linesRequiredToBeNumbers) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.numbers.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
  }

  @Test
  public void testComplexStrings() {
    // command with argument
    String line = "!set version";
    ExpectedHighlightStyle expectedStyle =
        new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with quoted argument
    line = "!set csvdelimiter '\"'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("'\"'"));
    expectedStyle.singleQuotes.set(line.indexOf("'\"'"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with double quoted argument
    line = "!set csvdelimiter \"'\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\""));
    expectedStyle.sqlIdentifierQuotes.set(line.indexOf("\"'\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    // command with double quoted argument and \n
    line = "!set csvdelimiter \"'\n\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.commands.set(0, "!set".length());
    expectedStyle.defaults.set("!set".length(), line.indexOf("\"'\n\""));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"'\n\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    line = "select '1'";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length(), line.indexOf(' ') + 1);
    expectedStyle.singleQuotes.set(line.indexOf(' ') + 1, line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //no spaces
    line = "select'1'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf("as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf("\"21\""));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"21\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //escaped sql identifiers
    line = "select '1' as \"\\\"value\n\\\"\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set(line.indexOf(" '"));
    expectedStyle.singleQuotes.set(line.indexOf('\''), line.indexOf(" as"));
    expectedStyle.defaults.set(line.indexOf(" as"));
    expectedStyle
        .keywords.set(line.indexOf("as"), line.indexOf(" \"\\\"value"));
    expectedStyle
        .sqlIdentifierQuotes.set(line.indexOf("\"\\\"value"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with comments /**/ and not ended quoted line
    line = "select/*123'1'*/'as\"21\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*123'1'*/"), line.indexOf("'as\"21\""));
    expectedStyle.singleQuotes.set(line.indexOf("'as\"21\""), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with comments /**/ and not ended sql identifier quoted line
    line = "select/*comment*/ as \"21\\\"";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments
        .set(line.indexOf("/*"), line.indexOf(" as"));
    expectedStyle.defaults.set(line.indexOf(" as"));
    expectedStyle.keywords.set(line.indexOf("as"), line.indexOf(" \"21"));
    expectedStyle.defaults.set(line.indexOf(" \"21"));
    expectedStyle.sqlIdentifierQuotes.set(line.indexOf("\"21"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //not valid sql with not ended multiline comment
    line = "select /*\n * / \n 123 as \"q\" \nfrom dual\n where\n 1 = 1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.defaults.set("select".length());
    expectedStyle.comments
        .set(line.indexOf("/*\n"), line.length());
    checkLineAgainstAllHighlighters(line, expectedStyle);

    //multiline sql with comments
    line = "select/*multiline\ncomment\n*/0 as \"0\","
        + "'qwe'\n--comment\nas\"21\"from t\n where 1=1";
    expectedStyle = new ExpectedHighlightStyle(line.length());
    expectedStyle.keywords.set(0, "select".length());
    expectedStyle.comments.set("select".length(), line.indexOf("0 as"));
    expectedStyle.numbers.set(line.indexOf("0 as"));
    expectedStyle.defaults.set(line.indexOf(" as \"0\","));
    expectedStyle.keywords
        .set(line.indexOf("as \"0\","), line.indexOf(" \"0\","));
    expectedStyle.defaults.set(line.indexOf(" \"0\","));
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"0\","), line.indexOf(",'qwe"));
    expectedStyle.defaults.set(line.indexOf(",'qwe"));
    expectedStyle.singleQuotes
        .set(line.indexOf("'qwe'"), line.indexOf("\n--comment\nas"));
    expectedStyle.defaults.set(line.indexOf("\n--comment"));
    expectedStyle.comments
        .set(line.indexOf("--comment\n"), line.indexOf("as\"21\""));
    expectedStyle.keywords
        .set(line.indexOf("as\"21\""), line.indexOf("\"21\"from"));
    expectedStyle.sqlIdentifierQuotes
        .set(line.indexOf("\"21\""), line.indexOf("from"));
    expectedStyle.keywords.set(line.indexOf("from"), line.indexOf(" t\n"));
    expectedStyle.defaults.set(line.indexOf(" t\n"), line.indexOf("where"));
    expectedStyle.keywords.set(line.indexOf("where"), line.indexOf(" 1=1"));
    expectedStyle.defaults.set(line.indexOf(" 1=1"));
    expectedStyle.numbers.set(line.indexOf("1=1"));
    expectedStyle.defaults.set(line.indexOf("=1"));
    expectedStyle.numbers.set(line.indexOf("=1") + 1);
    checkLineAgainstAllHighlighters(line, expectedStyle);
  }

  /**
   * The test checks additional highlighting while having connection to db.
   * 1) if keywords from getSQLKeywords are highlighted
   * 2) if a connection is cleared from sqlhighlighter
   * in case the connection is closing
   */
  @Test
  public void testH2SqlKeywordsFromDatabase() {
    // The list is taken from H2 1.4.197 getSQLKeywords output
    String[] linesRequiredToBeConnectionSpecificKeyWords = {
        "LIMIT",
        "MINUS",
        "OFFSET",
        "ROWNUM",
        "SYSDATE",
        "SYSTIME",
        "SYSTIMESTAMP",
        "TODAY",
    };

    for (String line : linesRequiredToBeConnectionSpecificKeyWords) {
      ExpectedHighlightStyle expectedStyle =
          new ExpectedHighlightStyle(line.length());
      expectedStyle.defaults.set(0, line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle);
    }
    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2HighLighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      SqlLineHighlighter sqlLineHighlighter =
          sqlLine2HighLighterEntry.getValue();
      sqlLine.runCommands(
          Collections.singletonList("!connect "
              + SqlLineArgsTest.ConnectionSpec.H2.url + " "
              + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\""),
          dc);

      for (String line : linesRequiredToBeConnectionSpecificKeyWords) {
        ExpectedHighlightStyle expectedStyle =
            new ExpectedHighlightStyle(line.length());
        expectedStyle.keywords.set(0, line.length());
        checkLineAgainstHighlighter(
            line, expectedStyle, sqlLine, sqlLine2HighLighterEntry.getValue());
      }

      sqlLine.getDatabaseConnection().close();
    }
  }

  /**
   * The test mocks default sql identifier to back tick
   * and then checks that after connection done sql
   * identifier quote will be taken from driver
   */
  @Test
  public void testH2SqlIdentifierFromDatabase() {
    new MockUp<SqlLineHighlighter>() {
      @Mock
      String getDefaultSqlIdentifierQuote() {
        return "`";
      }
    };

    String[] linesWithBackTickSqlIdentifiers = {
        "select 1 as `one` from dual",
        "select 1 as `on\\`e` from dual",
        "select 1 as `on\\`\ne` from dual",
    };

    String[] linesWithDoubleQuoteSqlIdentifiers = {
        "select 1 as \"one\" from dual",
        "select 1 as \"on\\\"e\" from dual",
        "select 1 as \"on\\\"\ne\" from dual",
    };

    ExpectedHighlightStyle[] expectedStyle =
        new ExpectedHighlightStyle[linesWithBackTickSqlIdentifiers.length];
    for (int i = 0; i < expectedStyle.length; i++) {
      String line = linesWithBackTickSqlIdentifiers[i];
      expectedStyle[i] = new ExpectedHighlightStyle(line.length());
      expectedStyle[i].keywords.set(0, "select".length());
      expectedStyle[i].defaults.set(line.indexOf(" 1"));
      expectedStyle[i].numbers.set(line.indexOf("1 as"));
      expectedStyle[i].defaults.set(line.indexOf(" as"));
      expectedStyle[i].keywords.set(line.indexOf("as"), line.indexOf(" `on"));
      expectedStyle[i].defaults.set(line.indexOf(" `on"));
      expectedStyle[i].sqlIdentifierQuotes.
          set(line.indexOf("`on"), line.indexOf(" from"));
      expectedStyle[i].defaults.set(line.indexOf(" from"));
      expectedStyle[i].keywords
          .set(line.indexOf("from"), line.indexOf(" dual"));
      expectedStyle[i].defaults.set(line.indexOf(" dual"), line.length());
      checkLineAgainstAllHighlighters(line, expectedStyle[i]);
    }

    DispatchCallback dc = new DispatchCallback();

    for (Map.Entry<SqlLine, SqlLineHighlighter> sqlLine2HighLighterEntry
        : sqlLine2HighLighter.entrySet()) {
      SqlLine sqlLine = sqlLine2HighLighterEntry.getKey();
      SqlLineHighlighter sqlLineHighlighter =
          sqlLine2HighLighterEntry.getValue();
      sqlLine.runCommands(
          Collections.singletonList("!connect "
              + SqlLineArgsTest.ConnectionSpec.H2.url + " "
              + SqlLineArgsTest.ConnectionSpec.H2.username + " \"\""),
          dc);

      for (int i = 0; i < linesWithDoubleQuoteSqlIdentifiers.length; i++) {
        checkLineAgainstHighlighter(
            linesWithDoubleQuoteSqlIdentifiers[i],
            expectedStyle[i],
            sqlLine,
            sqlLine2HighLighterEntry.getValue());
      }

      sqlLine.getDatabaseConnection().close();
    }
  }

  private void checkHighlightedLine(
      SqlLine sqlLine,
      String line,
      ExpectedHighlightStyle expectedHighlightStyle,
      SqlLineHighlighter highlighter) {
    final AttributedString attributedString =
        highlighter.highlight(sqlLine.getLineReader(), line);
    final HighlightStyle highlightStyle = sqlLine.getHighlightStyle();
    int commandStyle = highlightStyle.getCommandStyle().getStyle();
    int keywordStyle = highlightStyle.getKeywordStyle().getStyle();
    int singleQuoteStyle = highlightStyle.getQuotedStyle().getStyle();
    int identifierStyle = highlightStyle.getIdentifierStyle().getStyle();
    int commentStyle = highlightStyle.getCommentStyle().getStyle();
    int numberStyle = highlightStyle.getNumberStyle().getStyle();
    int defaultStyle = highlightStyle.getDefaultStyle().getStyle();

    for (int i = 0; i < line.length(); i++) {
      checkSymbolStyle(line, i, expectedHighlightStyle.commands,
          attributedString, commandStyle, "command");

      checkSymbolStyle(line, i, expectedHighlightStyle.keywords,
          attributedString, keywordStyle, "key word");

      checkSymbolStyle(line, i, expectedHighlightStyle.singleQuotes,
          attributedString, singleQuoteStyle, "single quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.sqlIdentifierQuotes,
          attributedString, identifierStyle, "sql identifier quote");

      checkSymbolStyle(line, i, expectedHighlightStyle.numbers,
          attributedString, numberStyle, "number");

      checkSymbolStyle(line, i, expectedHighlightStyle.comments,
          attributedString, commentStyle, "comment");

      checkSymbolStyle(line, i, expectedHighlightStyle.defaults,
          attributedString, defaultStyle, "default");
    }
  }

  private void checkDefaultLine(
      SqlLine sqlLine,
      String line,
      SqlLineHighlighter defaultHighlighter) {
    final AttributedString attributedString =
        defaultHighlighter.highlight(sqlLine.getLineReader(), line);
    int defaultStyle = AttributedStyle.DEFAULT.getStyle();

    for (int i = 0; i < line.length(); i++) {
      if (Character.isWhitespace(line.charAt(i))) {
        continue;
      }
      assertEquals(getFailedStyleMessage(line, i, "default"),
          i == 0 ? defaultStyle + 32 : defaultStyle,
          attributedString.styleAt(i).getStyle());
    }
  }

  private void checkLineAgainstAllHighlighters(
      String line, ExpectedHighlightStyle expectedHighlightStyle) {
    for (Map.Entry<SqlLine, SqlLineHighlighter> mapEntry
        : sqlLine2HighLighter.entrySet()) {
      checkLineAgainstHighlighter(
          line, expectedHighlightStyle, mapEntry.getKey(), mapEntry.getValue());
    }
  }

  private void checkLineAgainstHighlighter(
      String line,
      ExpectedHighlightStyle expectedHighlightStyle,
      SqlLine sqlLine,
      SqlLineHighlighter sqlLineHighlighter) {
    if (SqlLineProperty.DEFAULT.equals(sqlLine.getOpts().getColorScheme())) {
      checkDefaultLine(sqlLine, line, sqlLineHighlighter);
    } else {
      checkHighlightedLine(sqlLine,
          line, expectedHighlightStyle, sqlLineHighlighter);
    }
  }

  private void checkSymbolStyle(
      String line,
      int i,
      BitSet styleBitSet,
      AttributedString highlightedLine,
      int style,
      String styleName) {
    if (styleBitSet.get(i)) {
      assertEquals(getFailedStyleMessage(line, i, styleName),
          i == 0 ? style + 32 : style,
          highlightedLine.styleAt(i).getStyle());
    } else {
      if (!Character.isWhitespace(line.charAt(i))) {
        assertNotEquals(getNegativeFailedStyleMessage(line, i, styleName),
            i == 0 ? style + 32 : style,
            highlightedLine.styleAt(i).getStyle());
      }
    }
  }

  private String getFailedStyleMessage(String line, int i, String style) {
    return getFailedStyleMessage(line, i, style, true);
  }

  private String getNegativeFailedStyleMessage(
      String line, int i, String style) {
    return getFailedStyleMessage(line, i, style, false);
  }

  private String getFailedStyleMessage(
      String line, int i, String style, boolean positive) {
    return "String '" + line + "', symbol '" + line.charAt(i)
        + "' at (" + i + ") " + "position should "
        + (positive ? "" : "not ") + "be " + style + " style";
  }

}

// End SqlLineHighlighterTest.java