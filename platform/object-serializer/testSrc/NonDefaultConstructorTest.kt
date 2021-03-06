// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.serialization

import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.assertions.Assertions.assertThatThrownBy
import com.intellij.testFramework.rules.InMemoryFsRule
import com.intellij.util.io.write
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.lang.reflect.InvocationTargetException

class NonDefaultConstructorTest {
  @Rule
  @JvmField
  val testName = TestName()

  @JvmField
  @Rule
  val fsRule = InMemoryFsRule()

  private fun test(bean: Any) = test(bean, testName, defaultTestWriteConfiguration)

  @Test
  fun `no default constructor`() {
    test(NoDefaultConstructorBean("foo", arrayListOf(42, 21)))
  }

  @Test
  fun `null string as arg`() {
    test(NullableArgBean("foo", null, ""))
  }

  @Test
  fun `id registration`() {
    // test that bean created by non-default constructor is registered in object table
    val bean = ContainingBean()
    bean.b = Bean2("foo")
    bean.b2 = Bean3()
    bean.b2!!.b = bean.b
    test(bean)
  }

  @Test
  fun `remove versioned file on parameter error`() {
    val file = VersionedFile(fsRule.fs.getPath("/cache.ion"), 42)
    file.file.write("""
      {
        version:42,
        formatVersion:1,
        data:{
        }
      }
    """.trimIndent())
    assertThatThrownBy {
      assertThat(file.read(NoDefaultConstructorBean::class.java)).isNull()
    }
      .isInstanceOf(AssertionError::class.java)
      .hasCauseInstanceOf(InvocationTargetException::class.java)
    assertThat(file.file).doesNotExist()
  }
}

private class ContainingBean {
  @JvmField
  var b: Bean2? = null
  var b2: Bean3? = null
}

private class Bean2 @PropertyMapping("name") constructor(@JvmField val name: String)

private class Bean3 {
  @JvmField
  var b: Bean2? = null
}

@Suppress("UNUSED_PARAMETER", "unused")
private class NoDefaultConstructorBean @PropertyMapping("someParameter", "intList") constructor(@JvmField val someParameter: String,
                                                                                                  @JvmField val intList: List<Int>)

@Suppress("UNUSED_PARAMETER", "unused")
private class NullableArgBean @PropertyMapping("p", "p2", "p3") constructor(@JvmField val p: String?, @JvmField val p2: String?, @JvmField val p3: String?)
