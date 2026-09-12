/*
 * BTL Music (2026)
 * © ||BTL||™ (balajitechlabs)
 * GNU GPL-3.0 License
 */

package moe.rukamori.archivetune.lyrics

import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransliterationManager @Inject constructor() {

    enum class ScriptType {
        JAPANESE_KANA,
        DEVANAGARI,
        CYRILLIC,
        LATIN,
        OTHER,
    }

    fun detectScript(text: String): ScriptType {
        for (char in text) {
            val block = Character.UnicodeBlock.of(char) ?: continue
            when (block) {
                Character.UnicodeBlock.HIRAGANA,
                Character.UnicodeBlock.KATAKANA -> return ScriptType.JAPANESE_KANA
                Character.UnicodeBlock.DEVANAGARI -> return ScriptType.DEVANAGARI
                Character.UnicodeBlock.CYRILLIC -> return ScriptType.CYRILLIC
                else -> {}
            }
        }
        return ScriptType.LATIN
    }

    /**
     * Transliterates non-Latin scripts into standard Latin characters
     * for synchronized karaoke phonetics.
     */
    fun transliterate(text: String): String {
        if (text.isBlank()) return text
        val script = detectScript(text)
        return when (script) {
            ScriptType.JAPANESE_KANA -> transliterateKana(text)
            ScriptType.CYRILLIC -> transliterateCyrillic(text)
            ScriptType.DEVANAGARI -> transliterateDevanagari(text)
            ScriptType.LATIN, ScriptType.OTHER -> text
        }
    }

    private fun transliterateKana(text: String): String {
        val result = StringBuilder()
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val mapped = KANA_TABLE[ch.toString()]
            if (mapped != null) {
                result.append(mapped)
            } else {
                result.append(ch)
            }
            i++
        }
        return result.toString()
    }

    private fun transliterateCyrillic(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(CYRILLIC_TABLE[ch] ?: ch)
        }
        return sb.toString()
    }

    private fun transliterateDevanagari(text: String): String {
        val sb = StringBuilder()
        for (ch in text) {
            sb.append(DEVANAGARI_TABLE[ch] ?: ch)
        }
        return sb.toString()
    }

    companion object {
        private val KANA_TABLE = mapOf(
            "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
            "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
            "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
            "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
            "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
            "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
            "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
            "や" to "ya", "ゆ" to "yu", "よ" to "yo",
            "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
            "わ" to "wa", "を" to "wo", "ん" to "n",
            "ア" to "a", "イ" to "i", "ウ" to "u", "エ" to "e", "オ" to "o",
            "カ" to "ka", "キ" to "ki", "ク" to "ku", "ケ" to "ke", "コ" to "ko",
        )

        private val CYRILLIC_TABLE = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
            'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
            'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
            'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
            'у' to "u", 'ф' to "f", 'х' to "kh", 'ц' to "ts", 'ч' to "ch",
            'ш' to "sh", 'щ' to "shch", 'ы' to "y", 'э' to "e", 'ю' to "yu",
            'я' to "ya",
        )

        private val DEVANAGARI_TABLE = mapOf(
            'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u",
            'ऊ' to "oo", 'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au",
            'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh",
            'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh",
            'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
            'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
            'प' to "p", 'फ' to "ph", 'ब' to "b", 'भ' to "bh", 'म' to "m",
            'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v", 'श' to "sh",
            'ष' to "sh", 'स' to "s", 'ह' to "h",
        )
    }
}
