package com.sylo.feature.voice

import com.sylo.feature.voice.speech.TranscriptParser
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies the spoken-text -> structured-expense logic that gets saved locally. */
class TranscriptParserTest {

    @Test
    fun parses_amount_currency_and_category_from_dirhams_lunch() {
        val p = TranscriptParser.parse("Add 45 dirhams for lunch")
        assertEquals(4500L, p.amountMinor)
        assertEquals("AED", p.currency)
        assertEquals("Food", p.category)
        assertEquals("Add 45 dirhams for lunch", p.note)
        assertEquals("45.00", p.amountDisplay)
    }

    @Test
    fun parses_decimal_dollars_and_transport() {
        val p = TranscriptParser.parse("spent 18.50 dollars on an uber ride")
        assertEquals(1850L, p.amountMinor)
        assertEquals("USD", p.currency)
        assertEquals("Transport", p.category)
    }

    @Test
    fun falls_back_to_defaults_when_no_number_or_keyword() {
        val p = TranscriptParser.parse("just a random memo")
        assertEquals(0L, p.amountMinor)
        assertEquals("AED", p.currency)
        assertEquals("General", p.category)
    }

    @Test
    fun handles_comma_decimal_and_netflix_entertainment() {
        val p = TranscriptParser.parse("15,99 for netflix subscription")
        assertEquals(1599L, p.amountMinor)
        assertEquals("Entertainment", p.category)
    }

    @Test
    fun parses_arabic_dirhams_and_lunch() {
        val p = TranscriptParser.parse("اضف 45 درهم غداء")
        assertEquals(4500L, p.amountMinor)
        assertEquals("AED", p.currency)
        assertEquals("Food", p.category)
    }

    @Test
    fun parses_eastern_arabic_indic_digits_and_taxi() {
        // "١٨٫٥٠" is 18.50 written with Eastern Arabic-Indic digits and the Arabic decimal separator.
        val p = TranscriptParser.parse("١٨٫٥٠ ريال تاكسي")
        assertEquals(1850L, p.amountMinor)
        assertEquals("SAR", p.currency)
        assertEquals("Transport", p.category)
    }

    @Test
    fun parses_persian_digits_and_bills_category() {
        val p = TranscriptParser.parse("۱۲۰ دولار فاتورة الكهرباء")
        assertEquals(12000L, p.amountMinor)
        assertEquals("USD", p.currency)
        assertEquals("Bills", p.category)
    }
}
