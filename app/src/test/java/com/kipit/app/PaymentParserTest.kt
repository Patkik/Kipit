package com.kipit.app
import org.junit.Test
import org.junit.Assert.*
import com.kipit.app.service.PaymentParser
class PaymentParserTest {
 @Test fun supportedOutgoing(){val p=PaymentParser.parse("com.globe.gcash.android","You paid PHP 150.50 to a merchant");assertEquals(15050L,p?.amount)}
 @Test fun spoofedPackage(){assertNull(PaymentParser.parse("com.fake.wallet","You paid PHP 150.50"))}
 @Test fun incomingIsNotExpense(){assertNull(PaymentParser.parse("com.paymaya","You received PHP 150.50"))}
 @Test fun otpIsNotPayment(){assertNull(PaymentParser.parse("com.paymaya","OTP for payment successful PHP 150.50"))}
 @Test fun rejectedIsNotExpense(){assertNull(PaymentParser.parse("com.paymaya","Payment failed PHP 150.50"))}
 @Test fun feesStaySeparate(){val p=PaymentParser.parse("com.paymaya","You sent PHP 150.50, fee PHP 15.00");assertEquals(15050L,p?.amount);assertEquals(1500L,p?.fee)}
}