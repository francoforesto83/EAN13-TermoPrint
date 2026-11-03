package com.example

import com.example.model.Ean13Generator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testEan13CheckDigitCalculation() {
    // Standard test vectors
    // 400638133393 -> 1 (Stabilo Boss: 4006381333931)
    val check1 = Ean13Generator.calculateCheckDigit("400638133393")
    assertEquals(1, check1)

    // 779123456789 -> calculate check digit
    val check2 = Ean13Generator.calculateCheckDigit("779123456789")
    assertNotNull(check2)
    val fullCode = "779123456789$check2"
    assertTrue(Ean13Generator.isValid(fullCode))
  }

  @Test
  fun testInvalidEan13Fails() {
    // Wrong check digit should fail
    assertFalse(Ean13Generator.isValid("4006381333939"))
    // Too short or too long
    assertFalse(Ean13Generator.isValid("12345"))
    assertFalse(Ean13Generator.isValid("12345678901234"))
  }

  @Test
  fun testRandomEan13Generator() {
    for (i in 0 until 10) {
      val randomCode = Ean13Generator.generateRandomEan13("779")
      assertEquals(13, randomCode.length)
      assertTrue(randomCode.startsWith("779"))
      assertTrue(Ean13Generator.isValid(randomCode))
    }
  }

  @Test
  fun testBinaryEncodingStructure() {
    val barcodeData = Ean13Generator.encodeToBinary("4006381333931")
    assertNotNull(barcodeData)
    // EAN-13 always consists of exactly 95 modules:
    // 3 start + 42 left + 5 center + 42 right + 3 end = 95
    assertEquals(95, barcodeData!!.modules.size)

    val modules = barcodeData.modules
    // Start guard 101
    assertTrue(modules[0])
    assertFalse(modules[1])
    assertTrue(modules[2])

    // End guard 101
    assertTrue(modules[92])
    assertFalse(modules[93])
    assertTrue(modules[94])
  }
}
