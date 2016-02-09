package org.jetbrains.ktor.tests.http

import org.jetbrains.ktor.http.*
import org.jetbrains.ktor.testing.*
import org.jetbrains.ktor.util.*
import org.junit.*
import kotlin.test.*

class UrlEncodedTest {
    @Test
    fun `should parse simple with no headers`() {
        with(TestApplicationRequest()) {
            body = "field1=%D0%A2%D0%B5%D1%81%D1%82"

            val parsed = parseUrlEncodedParameters()
            assertEquals("\u0422\u0435\u0441\u0442", parsed["field1"])
        }
    }

    @Test
    fun `should parse simple with no encoding`() {
        with(TestApplicationRequest()) {
            body = "field1=%D0%A2%D0%B5%D1%81%D1%82"
            addHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded")

            val parsed = parseUrlEncodedParameters()
            assertEquals("\u0422\u0435\u0441\u0442", parsed["field1"])
        }
    }

    @Test
    fun `should parse simple with specified encoding utf8`() {
        with(TestApplicationRequest()) {
            body = "field1=%D0%A2%D0%B5%D1%81%D1%82"
            addHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=utf-8")

            val parsed = parseUrlEncodedParameters()
            assertEquals("\u0422\u0435\u0441\u0442", parsed["field1"])
        }
    }

    @Test
    fun `should parse simple with specified encoding non utf`() {
        with(TestApplicationRequest()) {
            body = "field1=%D2%E5%F1%F2"
            addHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=windows-1251")

            val parsed = parseUrlEncodedParameters()
            assertEquals("\u0422\u0435\u0441\u0442", parsed["field1"])
        }
    }

    @Test
    fun `should parse simple with specified encoding non utf in parameter`() {
        with(TestApplicationRequest()) {
            body = "field1=%D2%E5%F1%F2&_charset_=windows-1251"
            addHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded")

            val parsed = parseUrlEncodedParameters()
            assertEquals("\u0422\u0435\u0441\u0442", parsed["field1"])
        }
    }

    @Test
    fun testRenderUrlEncoded() {
        assertEquals("p1=a+b", listOf("p1" to "a b").formUrlEncode())
        assertEquals("p%3D1=a%3Db", listOf("p=1" to "a=b").formUrlEncode())
        assertEquals("p1=a&p1=b&p2=c", listOf("p1" to "a", "p1" to "b", "p2" to "c").formUrlEncode())
    }

    @Test
    fun testRenderUrlEncodedValuesMap() {
        assertEquals("p1=a+b", valuesOf("p1" to listOf("a b")).formUrlEncode())
        assertEquals("p%3D1=a%3Db", valuesOf("p=1" to listOf("a=b")).formUrlEncode())
        assertEquals("p1=a&p1=b&p2=c", valuesOf("p1" to listOf("a", "b"), "p2" to listOf("c")).formUrlEncode())
    }
}