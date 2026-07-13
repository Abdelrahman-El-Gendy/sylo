package com.sylo.core.common.payments

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentNotificationParserTest {

    @Test
    fun parses_bank_debit_with_merchant() {
        val p = PaymentNotificationParser.parse(
            packageName = "com.cib.mobile",
            title = "CIB",
            text = "You spent EGP 250.00 at CARREFOUR",
        )
        assertNotNull(p); p!!
        assertEquals(25000L, p.amountMinor)
        assertEquals("EGP", p.currency)
        assertEquals("CARREFOUR", p.merchant)
        assertEquals("Groceries", p.category)
        assertFalse(p.isIncome)
    }

    @Test
    fun parses_instapay_incoming_transfer() {
        val p = PaymentNotificationParser.parse(
            packageName = "com.egyptianbanks.instapay",
            title = "InstaPay",
            text = "You received EGP 500 from Ahmed",
        )
        assertNotNull(p); p!!
        assertEquals(50000L, p.amountMinor)
        assertTrue(p.isIncome)
        assertEquals("InstaPay", p.sourceApp)
    }

    @Test
    fun parses_number_before_currency_and_dollar_symbol() {
        val p = PaymentNotificationParser.parse("com.paypal", "PayPal", "Payment of \$12.99 to Netflix sent")
        assertNotNull(p); p!!
        assertEquals(1299L, p.amountMinor)
        assertEquals("USD", p.currency)
        assertEquals("Netflix", p.merchant)
    }

    @Test
    fun ignores_non_payment_notifications() {
        assertNull(PaymentNotificationParser.parse("com.whatsapp", "Mom", "Are you coming for dinner?"))
        assertNull(PaymentNotificationParser.parse("com.android.chrome", "News", "10 things you didn't know"))
    }

    @Test
    fun ignores_amountless_text_from_bank() {
        assertNull(PaymentNotificationParser.parse("com.cib.mobile", "CIB", "Your statement is ready"))
    }
}
