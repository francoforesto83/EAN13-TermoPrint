package com.example.model

import kotlin.random.Random

object Ean13Generator {

  // L-codes for digits 0-9
  private val L_PATTERNS = arrayOf(
    "0001101", "0011001", "0010011", "0111101", "0100011",
    "0110001", "0101111", "0111011", "0110111", "0001011"
  )

  // G-codes for digits 0-9
  private val G_PATTERNS = arrayOf(
    "0100111", "0110011", "0011011", "0100001", "0011101",
    "0111001", "0000101", "0010001", "0001001", "0010111"
  )

  // R-codes for digits 0-9
  private val R_PATTERNS = arrayOf(
    "1110010", "1100110", "1101100", "1000010", "1011100",
    "1001110", "1010000", "1000100", "1001000", "1110100"
  )

  // Parity tables based on the first digit
  private val FIRST_DIGIT_PARITY = arrayOf(
    "LLLLLL", // 0
    "LLGLGG", // 1
    "LLGGLG", // 2
    "LLGGGL", // 3
    "LGLLGG", // 4
    "LGGLLG", // 5
    "LGGGLL", // 6
    "LGLGLG", // 7
    "LGLGGL", // 8
    "LGGLGL"  // 9
  )

  /**
   * Calculates GS1 EAN-13 check digit for 12 numeric digits
   */
  fun calculateCheckDigit(twelveDigits: String): Int? {
    val clean = twelveDigits.filter { it.isDigit() }
    if (clean.length != 12) return null

    var sum = 0
    for (i in 0 until 12) {
      val digit = clean[i].digitToInt()
      // Odd position (1-indexed, i=0,2,4...): weight 1
      // Even position (1-indexed, i=1,3,5...): weight 3
      sum += if (i % 2 == 0) digit else digit * 3
    }
    val remainder = sum % 10
    return if (remainder == 0) 0 else 10 - remainder
  }

  /**
   * Validates if a 13-digit code has a valid GS1 check digit
   */
  fun isValid(ean13: String): Boolean {
    val clean = ean13.filter { it.isDigit() }
    if (clean.length != 13) return false
    val expectedCheck = calculateCheckDigit(clean.substring(0, 12)) ?: return false
    return clean[12].digitToInt() == expectedCheck
  }

  /**
   * Generates a valid random EAN-13 with given country/system prefix (e.g., 779 Argentina)
   */
  fun generateRandomEan13(prefix: String = "779"): String {
    val sb = StringBuilder(prefix)
    while (sb.length < 12) {
      sb.append(Random.nextInt(0, 10))
    }
    val checkDigit = calculateCheckDigit(sb.toString()) ?: 0
    return sb.append(checkDigit).toString()
  }

  /**
   * Converts a 13-digit EAN-13 code into a 95-module binary string ("1" = bar, "0" = space)
   * Also returns whether each module is part of guard bars (which are typically rendered taller).
   */
  data class Ean13BarcodeData(
    val modules: BooleanArray, // true = black, false = white
    val isGuardBar: BooleanArray, // true if guard bar (drawn slightly taller)
    val formattedDisplay: String // e.g., "7 791234 567890"
  )

  fun encodeToBinary(ean13: String): Ean13BarcodeData? {
    val clean = ean13.filter { it.isDigit() }
    if (clean.length != 13) return null

    val firstDigit = clean[0].digitToInt()
    val parity = FIRST_DIGIT_PARITY[firstDigit]

    val moduleList = mutableListOf<Boolean>()
    val guardList = mutableListOf<Boolean>()

    // Helper to add bit string
    fun appendPattern(pattern: String, isGuard: Boolean) {
      for (ch in pattern) {
        moduleList.add(ch == '1')
        guardList.add(isGuard)
      }
    }

    // 1. Start Guard: 101
    appendPattern("101", true)

    // 2. Left 6 digits (digits 2 to 7, indexes 1..6)
    for (i in 1..6) {
      val digit = clean[i].digitToInt()
      val patternType = parity[i - 1]
      val pattern = if (patternType == 'L') L_PATTERNS[digit] else G_PATTERNS[digit]
      appendPattern(pattern, false)
    }

    // 3. Center Guard: 01010
    appendPattern("01010", true)

    // 4. Right 6 digits (digits 8 to 13, indexes 7..12) with R-patterns
    for (i in 7..12) {
      val digit = clean[i].digitToInt()
      appendPattern(R_PATTERNS[digit], false)
    }

    // 5. End Guard: 101
    appendPattern("101", true)

    val formatted = "${clean[0]} ${clean.substring(1, 7)} ${clean.substring(7, 13)}"

    return Ean13BarcodeData(
      modules = moduleList.toBooleanArray(),
      isGuardBar = guardList.toBooleanArray(),
      formattedDisplay = formatted
    )
  }
}
