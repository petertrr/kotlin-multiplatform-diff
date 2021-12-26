/*
 * Copyright 2021 Peter Trifanov.
 * Copyright 2009-2021 java-diff-utils.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file has been modified by Peter Trifanov when porting from Java to Kotlin.
 */
package io.github.petertrr.diffutils.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffRowGeneratorTest {
    @Test
    fun testGenerator_Default() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            columnWidth = Int.MAX_VALUE // do not wrap
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
    }

    /**
     * Test of normalize method, of class StringUtils.
     */
    @Test
    fun testNormalize_List() {
        val generator = DiffRowGenerator()
        assertEquals(listOf("    test"), generator.normalizeLines(listOf("\ttest")))
    }

    @Test
    fun testGenerator_Default2() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            columnWidth = 0 // do not wrap
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
    }

    @Test
    fun testGenerator_InlineDiff() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            columnWidth = Int.MAX_VALUE // do not wrap
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
        assertTrue(rows[0].oldLine.indexOf("<span") > 0)
    }

    @Test
    fun testGenerator_IgnoreWhitespaces() {
        val first = "anything \n \nother\nmore lines"
        val second = "anything\n\nother\nsome more lines"
        val generator = DiffRowGenerator(
            ignoreWhiteSpaces = true,
            columnWidth = Int.MAX_VALUE // do not wrap
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(4, rows.size)
        assertEquals(rows[0].tag, DiffRow.Tag.EQUAL)
        assertEquals(rows[1].tag, DiffRow.Tag.EQUAL)
        assertEquals(rows[2].tag, DiffRow.Tag.EQUAL)
        assertEquals(rows[3].tag, DiffRow.Tag.CHANGE)
    }

    @Test
    fun testGeneratorWithWordWrap() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            columnWidth = 5
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "anyth<br/>ing ", "anyth<br/>ing"), rows[0])
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, " ", ""), rows[1])
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "other", "other"), rows[2])
    }

    @Test
    fun testGeneratorWithMerge() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "anything<span class=\"editOldInline\"> </span>", "anything"), rows[0])
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "<span class=\"editOldInline\"> </span>", ""), rows[1])
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "other", "other"), rows[2])
    }

    @Test
    fun testGeneratorWithMerge2() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
        )
        val rows: List<DiffRow> = generator.generateDiffRows(listOf("Test"), listOf("ester"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "<span class=\"editOldInline\">T</span>est<span class=\"editNewInline\">er</span>", "ester"), rows[0])
    }

    @Test
    fun testGeneratorWithMerge3() {
        val first = "test\nanything \n \nother"
        val second = "anything\n\nother\ntest\ntest2"
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
        )
        val rows = generator.generateDiffRows(first.lines(), second.lines())
        println(rows)
        assertEquals(6, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "<span class=\"editOldInline\">test</span>", "anything"), rows[0])
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "anything<span class=\"editOldInline\"> </span>", ""), rows[1])
        assertEquals(DiffRow(DiffRow.Tag.DELETE, "<span class=\"editOldInline\"> </span>", ""), rows[2])
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "other", "other"), rows[3])
        assertEquals(DiffRow(DiffRow.Tag.INSERT, "<span class=\"editNewInline\">test</span>", "test"), rows[4])
        assertEquals(DiffRow(DiffRow.Tag.INSERT, "<span class=\"editNewInline\">test2</span>", "test2"), rows[5])
    }

    @Test
    fun testGeneratorWithMergeByWord4() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
        )
        val rows: List<DiffRow> = generator.generateDiffRows(listOf("Test"), listOf("ester"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "<span class=\"editOldInline\">Test</span><span class=\"editNewInline\">ester</span>", "ester"), rows[0])
    }

    @Test
    fun testGeneratorWithMergeByWord5() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
            columnWidth = 80
        )
        val rows: List<DiffRow> = generator.generateDiffRows(listOf("Test feature"), listOf("ester feature best"))
        print(rows)
        assertEquals(1, rows.size)
        assertEquals(
            DiffRow(DiffRow.Tag.CHANGE, "<span class=\"editOldInline\">Test</span><span class=\"editNewInline\">ester</span> <br/>feature<span class=\"editNewInline\"> best</span>", "ester feature best"),
            rows[0]
        )
    }

    @Test
    fun testSplitString() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test,test2", DiffRowGenerator.SPLIT_BY_WORD_PATTERN)
        assertEquals(3, list.size)
        assertEquals("[test, ,, test2]", list.toString())
    }

    @Test
    fun testSplitString2() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test , test2", DiffRowGenerator.SPLIT_BY_WORD_PATTERN)
        println(list)
        assertEquals(5, list.size)
        assertEquals("[test,  , ,,  , test2]", list.toString())
    }

    @Test
    fun testSplitString3() {
        val list = DiffRowGenerator.splitStringPreserveDelimiter("test,test2,", DiffRowGenerator.SPLIT_BY_WORD_PATTERN)
        println(list)
        assertEquals(4, list.size)
        assertEquals("[test, ,, test2, ,]", list.toString())
    }

    @Test
    fun testGeneratorExample1() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("This is a test senctence."),
            listOf("This is a test for diffutils.")
        )
        println(rows[0].oldLine)
        assertEquals(1, rows.size)
        assertEquals("This is a test ~senctence~**for diffutils**.", rows[0].oldLine)
    }

    @Test
    fun testGeneratorExample2() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("This is a test senctence.", "This is the second line.", "And here is the finish."),
            listOf("This is a test for diffutils.", "This is the second line.")
        )
        println("|original|new|")
        println("|--------|---|")
        for (row in rows) {
            println("|" + row.oldLine + "|" + row.newLine + "|")
        }
        assertEquals(3, rows.size)
        assertEquals("This is a test ~senctence~.", rows[0].oldLine)
        assertEquals("This is a test **for diffutils**.", rows[0].newLine)
    }

    @Test
    fun testGeneratorUnchanged() {
        val first = "anything \n \nother"
        val second = "anything\n\nother"
        val generator = DiffRowGenerator(
            columnWidth = 5,
            reportLinesUnchanged = true,
        )
        val rows: List<DiffRow> = generator.generateDiffRows(first.lines(), second.lines())
        print(rows)
        assertEquals(3, rows.size)
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "anything ", "anything"), rows[0])
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, " ", ""), rows[1])
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "other", "other"), rows[2])
    }

    @Test
    fun testGeneratorIssue14() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffSplitter = { line -> DiffRowGenerator.splitStringPreserveDelimiter(line, Regex(",")) },
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("J. G. Feldstein, Chair"),
            listOf("T. P. Pastor, Chair")
        )
        println(rows[0].oldLine)
        assertEquals(1, rows.size)
        assertEquals("~J. G. Feldstein~**T. P. Pastor**, Chair", rows[0].oldLine)
    }

    @Test
    fun testGeneratorIssue15() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true, //show the ~ ~ and ** ** symbols on each difference
            inlineDiffByWord = true, //show the ~ ~ and ** ** around each different word instead of each letter
            //reportLinesUnchanged = true) //experiment
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val listOne: List<String> = """
            TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, NULLABLE,
            ACTIONS_C17005, ID, NUMBER, 22, 19, N,
            ACTIONS_C17005, ISSUEID, NUMBER, 22, 19, Y,
            ACTIONS_C17005, MODIFIED, NUMBER, 22, 10, Y,
            ACTIONS_C17005, TABLE, VARCHAR2, 1020, null, Y,
            ACTIONS_C17005, S_NAME, CLOB, 4000, null, Y,
            ACTIONS_C17008, ID, NUMBER, 22, 19, N,
            ACTIONS_C17008, ISSUEID, NUMBER, 22, 19, Y,
            ACTIONS_C17008, MODIFIED, NUMBER, 22, 10, Y,
        """.trimIndent().lines()
        val listTwo: List<String> = """
            TABLE_NAME, COLUMN_NAME, DATA_TYPE, DATA_LENGTH, DATA_PRECISION, NULLABLE,
            ACTIONS_C16913, ID, NUMBER, 22, 19, N,
            ACTIONS_C16913, ISSUEID, NUMBER, 22, 19, Y,
            ACTIONS_C16913, MODIFIED, NUMBER, 22, 10, Y,
            ACTIONS_C16913, VRS, NUMBER, 22, 1, Y,
            ACTIONS_C16913, ZTABS, VARCHAR2, 255, null, Y,
            ACTIONS_C16913, ZTABS_S, VARCHAR2, 255, null, Y,
            ACTIONS_C16913, TASK, VARCHAR2, 255, null, Y,
            ACTIONS_C16913, HOURS_SPENT, VARCHAR2, 255, null, Y,
        """.trimIndent().lines()
        val rows = generator.generateDiffRows(listOne, listTwo)
        assertEquals(9, rows.size)
        for (row in rows) {
            println("|" + row.oldLine + "| " + row.newLine + " |")
            if (!row.oldLine.startsWith("TABLE_NAME")) {
                assertTrue(row.newLine.startsWith("**ACTIONS_C16913**"))
                assertTrue(row.oldLine.startsWith("~ACTIONS_C1700"))
            }
        }
    }

    @Test
    fun testGeneratorIssue22() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val aa = "This is a test senctence."
        val bb = "This is a test for diffutils.\nThis is the second line."
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf(*aa.split("\n").toTypedArray()),
            listOf(*bb.split("\n").toTypedArray())
        )
        rows.zip(listOf(
            DiffRow(DiffRow.Tag.CHANGE, "This is a test ~senctence~.", "This is a test **for diffutils**."),
            DiffRow(DiffRow.Tag.INSERT, "", "**This is the second line.**")
        ))
            .forEach {
                assertEquals(it.first, it.second)
            }
        println("|original|new|")
        println("|--------|---|")
        for (row in rows) {
            println("|" + row.oldLine + "|" + row.newLine + "|")
        }
    }

    @Test
    fun testGeneratorIssue22_2() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val aa = "This is a test for diffutils.\nThis is the second line."
        val bb = "This is a test senctence."
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf(*aa.split("\n").toTypedArray()),
            listOf(*bb.split("\n").toTypedArray())
        )
        rows.zip(listOf(
            DiffRow(DiffRow.Tag.CHANGE, "This is a test ~for diffutils~.", "This is a test **senctence**."),
            DiffRow(DiffRow.Tag.DELETE, "~This is the second line.~", "")
        ))
            .forEach {
                assertEquals(it.first, it.second)
            }
    }

    @Test
    fun testGeneratorIssue22_3() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val aa = "This is a test senctence."
        val bb = "This is a test for diffutils.\nThis is the second line.\nAnd one more."
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf(*aa.split("\n").toTypedArray()),
            listOf(*bb.split("\n").toTypedArray())
        )
        rows.zip(listOf(
            DiffRow(DiffRow.Tag.CHANGE, "This is a test ~senctence~.", "This is a test **for diffutils**."),
            DiffRow(DiffRow.Tag.INSERT, "", "**This is the second line.**"),
            DiffRow(DiffRow.Tag.INSERT, "", "**And one more.**"),
        ))
            .forEach {
                assertEquals(it.first, it.second)
            }
    }

    @Test
    fun testGeneratorIssue41DefaultNormalizer() {
        val generator = DiffRowGenerator(
        )
        val rows: List<DiffRow> = generator.generateDiffRows(listOf("<"), listOf("<"))
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "&lt;", "&lt;"), rows.single())
    }

    @Test
    fun testGeneratorIssue41UserNormalizer() {
        val generator = DiffRowGenerator(
            lineNormalizer = { str -> str.replace("\t", "    ") }
        )
        var rows: List<DiffRow?> = generator.generateDiffRows(listOf("<"), listOf("<"))
        assertEquals(DiffRow(DiffRow.Tag.EQUAL, "<", "<"), rows.single())
        rows = generator.generateDiffRows(listOf("\t<"), listOf("<"))
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "    <", "<"), rows.single())
    }

    @Test
    fun testGenerationIssue44reportLinesUnchangedProblem() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            reportLinesUnchanged = true,
            oldTag = { _, _ -> "~~" },
            newTag = { _, _ -> "**" },
        )
        val rows: List<DiffRow> = generator.generateDiffRows(listOf("<dt>To do</dt>"), listOf("<dt>Done</dt>"))
        assertEquals(DiffRow(DiffRow.Tag.CHANGE, "<dt>~~T~~o~~ do~~</dt>", "<dt>**D**o**ne**</dt>"), rows.single())
    }

    @Test
    fun testIgnoreWhitespaceIssue66() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            ignoreWhiteSpaces = true,
            mergeOriginalRevised = true,
            oldTag = { _, _ -> "~" }, //introduce markdown style for strikethrough,
            newTag = { _, _ -> "**" } //introduce markdown style for bold,
        )

        //compute the differences for two test texts.
        //CHECKSTYLE:OFF
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("This\tis\ta\ttest."),
            listOf("This is a test")
        )
        //CHECKSTYLE:ON
        assertEquals("This    is    a    test~.~", rows[0].oldLine)
    }

    @Test
    fun testIgnoreWhitespaceIssue66_2() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            ignoreWhiteSpaces = true,
            mergeOriginalRevised = true,
            oldTag = { _, _ -> "~" }, //introduce markdown style for strikethrough,
            newTag = { _, _ -> "**" } //introduce markdown style for bold,
        )

        //compute the differences for two test texts.
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("This  is  a  test."),
            listOf("This is a test")
        )
        assertEquals("This  is  a  test~.~", rows[0].oldLine)
    }

    @Test
    fun testIgnoreWhitespaceIssue64() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            ignoreWhiteSpaces = true,
            mergeOriginalRevised = true,
            oldTag = { _, _ -> "~" }, //introduce markdown style for strikethrough,
            newTag = { _, _ -> "**" } //introduce markdown style for bold,
        )

        //compute the differences for two test texts.
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf(*"test\n\ntestline".split("\n").toTypedArray()),
            listOf(*"A new text line\n\nanother one".split("\n").toTypedArray())
        )
        rows.map { it.oldLine }
            .zip(
                listOf(
                    "~test~**A new text line**",
                    "",
                    "~testline~**another one**"
                )
            )
            .all {
                it.first == it.second
            }
    }

    @Test
    fun testReplaceDiffsIssue63() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            mergeOriginalRevised = true,
            oldTag = { _, _ -> "~" }, //introduce markdown style for strikethrough,
            newTag = { _, _ -> "**" }, //introduce markdown style for bold,
            processDiffs = { str -> str.replace(" ", "/") },
        )

        //compute the differences for two test texts.
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf("This  is  a  test."),
            listOf("This is a test")
        )
        assertEquals("This~//~**/**is~//~**/**a~//~**/**test~.~", rows[0].oldLine)
    }

    @Test
    fun testProblemTooManyDiffRowsIssue65() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            reportLinesUnchanged = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
            mergeOriginalRevised = true,
            inlineDiffByWord = false,
            replaceOriginalLinefeedInChangesWithSpaces = true
        )
        val diffRows: List<DiffRow> = generator.generateDiffRows(
            listOf("Ich möchte nicht mit einem Bot sprechen.", "Ich soll das schon wieder wiederholen?"),
            listOf("Ich möchte nicht mehr mit dir sprechen. Leite mich weiter.", "Kannst du mich zum Kundendienst weiterleiten?")
        )
        print(diffRows)
        assertEquals(2, diffRows.size)
    }

    @Test
    fun testProblemTooManyDiffRowsIssue65_NoMerge() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            reportLinesUnchanged = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
            mergeOriginalRevised = false,
            inlineDiffByWord = false,
        )
        val diffRows: List<DiffRow> = generator.generateDiffRows(
            listOf("Ich möchte nicht mit einem Bot sprechen.", "Ich soll das schon wieder wiederholen?"),
            listOf("Ich möchte nicht mehr mit dir sprechen. Leite mich weiter.", "Kannst du mich zum Kundendienst weiterleiten?")
        )
        println(diffRows)
        assertEquals(2, diffRows.size)
    }

    @Test
    fun testProblemTooManyDiffRowsIssue65_DiffByWord() {
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            reportLinesUnchanged = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
        )
        val diffRows: List<DiffRow> = generator.generateDiffRows(
            listOf("Ich möchte nicht mit einem Bot sprechen.", "Ich soll das schon wieder wiederholen?"),
            listOf("Ich möchte nicht mehr mit dir sprechen. Leite mich weiter.", "Kannst du mich zum Kundendienst weiterleiten?")
        )
        println(diffRows)
        assertEquals(2, diffRows.size)
    }

    @Test
    fun testProblemTooManyDiffRowsIssue65_NoInlineDiff() {
        val generator = DiffRowGenerator(
            showInlineDiffs = false,
            reportLinesUnchanged = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
            mergeOriginalRevised = true,
            inlineDiffByWord = false,
        )
        val diffRows: List<DiffRow> = generator.generateDiffRows(
            listOf("Ich möchte nicht mit einem Bot sprechen.", "Ich soll das schon wieder wiederholen?"),
            listOf("Ich möchte nicht mehr mit dir sprechen. Leite mich weiter.", "Kannst du mich zum Kundendienst weiterleiten?")
        )
        println(diffRows)
        assertEquals(2, diffRows.size)
    }

    @Test
    fun testLinefeedInStandardTagsWithLineWidthIssue81() {
        val original: List<String> = listOf(
            *"""American bobtail jaguar. American bobtail bombay but turkish angora and tomcat.
Russian blue leopard. Lion. Tabby scottish fold for russian blue, so savannah yet lynx. Tomcat singapura, cheetah.
Bengal tiger panther but singapura but bombay munchkin for cougar.""".split("\n").toTypedArray()
        )
        val revised: List<String> = listOf(
            *"""bobtail jaguar. American bobtail turkish angora and tomcat.
Russian blue leopard. Lion. Tabby scottish folded for russian blue, so savannah yettie? lynx. Tomcat singapura, cheetah.
Bengal tiger panther but singapura but bombay munchkin for cougar. And more.""".split("\n").toTypedArray()
        )
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            ignoreWhiteSpaces = true,
            columnWidth = 100,
        )
        val deltas = generator.generateDiffRows(original, revised)
        println(deltas)
    }

    @Test
    fun testIssue86WrongInlineDiff() {
        val original: String = """
            MessageTime,MessageType,Instrument,InstrumentState,TradePrice,TradeVolume,TradeCond,TradeId,AskPrice1,AskVol1,BidPrice1,BidVol1,AskPrice2,AskVol2,BidPrice2,BidVol2,AskPrice3,AskVol3,BidPrice3,BidVol3,AskPrice4,AskVol4,BidPrice4,BidVol4,AskPrice5,AskVol5,BidPrice5,BidVol5
            2020-04-04T08:00:00.000Z,S,HHD_MAY20,Open,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T08:00:00.000Z,S,FHK_C23.5_MAY20,Open,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T13:49:11.522Z,Q,HHD_MAY20,,,,,,2.6,10,2.6,10,,,,,,,,,,,,,,,,
            2020-04-04T13:49:18.210Z,T,HHD_MAY20,,2.6,1,Screen,0,,,,,,,,,,,,,,,,,,,,
            2020-04-04T17:00:00.000Z,S,HHD_MAY20,Close,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T17:00:00.000Z,S,FHK_C23.5_MAY20,Close,,,,,,,,,,,,,,,,,,,,,,,,
        """.trimIndent()
        val revised: String = """
            MessageTime,MessageType,Instrument,InstrumentState,TradePrice,TradeVolume,TradeCond,TradeId,AskPrice1,AskVol1,BidPrice1,BidVol1,AskPrice2,AskVol2,BidPrice2,BidVol2,AskPrice3,AskVol3,BidPrice3,BidVol3,AskPrice4,AskVol4,BidPrice4,BidVol4,AskPrice5,AskVol5,BidPrice5,BidVol5
            2020-04-02T08:00:00.000Z,S,HHD_MAY20,Open,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-02T08:00:00.000Z,S,FHK_C23.5_MAY20,Open,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T13:49:11.522Z,Q,HHD_MAY20,,,,,,2.6,10,2.6,10,,,,,,,,,,,,,,,,
            2020-04xs-04T17dw:00:00.000Z,Sdwdw,HHD_MAY20dwdw,Closdwde,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T13:49:18.210Z,T,HHD_MAY20,,2.6,2,Screen,0,,,,,,,,,,,,,,,,,,,,
            2020-04-04T17:00:00.000Z,S,HHD_MAY20,Close,,,,,,,,,,,,,,,,,,,,,,,,
            2020-04-04T17:00:00.000Z,S,FHK_C23.5_MAY20,Close,,,,,,,,,,,,,,,,,,,,,,,,
        """.trimIndent()
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows: List<DiffRow> = generator.generateDiffRows(
            listOf(*original.split("\n").toTypedArray()),
            listOf(*revised.split("\n").toTypedArray())
        )
        rows
            .filter { it.tag !== DiffRow.Tag.EQUAL }
            .forEach(::println)
    }

    @Test
    fun testCorrectChangeIssue114() {
        val original: List<String> = listOf("A", "B", "C", "D", "E")
        val revised: List<String> = listOf("a", "C", "", "E")
        val generator = DiffRowGenerator(
            showInlineDiffs = false,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows = generator.generateDiffRows(original, revised)
        for (diff in rows) {
            println(diff)
        }
        assertTrue(rows.map { it.tag.name }
            .zip(listOf("CHANGE", "DELETE", "EQUAL", "CHANGE", "EQUAL"))
            .all { it.first == it.second }
        )
    }

    @Test
    fun testCorrectChangeIssue114_2() {
        val original: List<String> = listOf("A", "B", "C", "D", "E")
        val revised: List<String> = listOf("a", "C", "", "E")
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
        )
        val rows = generator.generateDiffRows(original, revised)
        for (diff in rows) {
            println(diff)
        }
        assertTrue(rows.map { it.tag.name }
            .zip(listOf("CHANGE", "DELETE", "EQUAL", "CHANGE", "EQUAL"))
            .all { it.first == it.second }
        )
        assertEquals(DiffRow(DiffRow.Tag.DELETE, "~B~", ""), rows[1])
    }

    @Test
    fun testIssue119WrongContextLength() {
        val original: String =
            """
                const world: string = 'world',
                      p: number | undefined = 42;

                console.log(`Hello, ${'$'}world}!`);
            """.trimIndent()
        val revised: String =
            """
                const world: string = 'world';
                const p: number | undefined = 42;

                console.log(`Hello, ${'$'}world}!`);
            """.trimIndent()
        val generator = DiffRowGenerator(
            showInlineDiffs = true,
            mergeOriginalRevised = true,
            inlineDiffByWord = true,
            oldTag = { _, _ -> "~" },
            newTag = { _, _ -> "**" },
            )
        val rows: List<DiffRow> = generator.generateDiffRows(
            original.split("\n"),
            revised.split("\n")
        )
        rows.filter { it.tag != DiffRow.Tag.EQUAL }
            .forEach { println(it) }
    }
}
