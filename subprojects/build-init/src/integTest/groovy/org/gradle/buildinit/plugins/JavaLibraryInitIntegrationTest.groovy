/*
 * Copyright 2013 the original author or authors.
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
 */

package org.gradle.buildinit.plugins

import org.gradle.buildinit.plugins.fixtures.ScriptDslFixture
import org.gradle.buildinit.plugins.internal.modifiers.BuildInitTestFramework
import spock.lang.Unroll

import static org.hamcrest.Matchers.allOf

class JavaLibraryInitIntegrationTest extends AbstractInitIntegrationSpec {

    public static final String SAMPLE_LIBRARY_CLASS = "src/main/java/Library.java"
    public static final String SAMPLE_LIBRARY_TEST_CLASS = "src/test/java/LibraryTest.java"
    public static final String SAMPLE_SPOCK_LIBRARY_TEST_CLASS = "src/test/groovy/LibraryTest.groovy"

    @Unroll
    def "creates sample source if no source present with #scriptDsl build scripts"() {
        when:
        run('init', '--type', 'java-library', '--dsl', scriptDsl.id)

        then:
        file(SAMPLE_LIBRARY_CLASS).exists()
        file(SAMPLE_LIBRARY_TEST_CLASS).exists()

        and:
        commonFilesGenerated(scriptDsl)
        def dslFixture = dslFixtureFor(scriptDsl)
        buildFileSeparatesImplementationAndApi(dslFixture)

        when:
        run("build")

        then:
        assertTestPassed("LibraryTest", "testSomeLibraryMethod")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "creates sample source using spock instead of junit with #scriptDsl build scripts"() {
        when:
        run('init', '--type', 'java-library', '--test-framework', 'spock', '--dsl', scriptDsl.id)

        then:
        file(SAMPLE_LIBRARY_CLASS).exists()
        file(SAMPLE_SPOCK_LIBRARY_TEST_CLASS).exists()

        and:
        commonFilesGenerated(scriptDsl)
        def dslFixture = dslFixtureFor(scriptDsl)
        buildFileSeparatesImplementationAndApi(dslFixture, 'org.spockframework')

        when:
        run("build")

        then:
        assertTestPassed("LibraryTest", "someLibraryMethod returns true")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "creates sample source using testng instead of junit with #scriptDsl build scripts"() {
        when:
        run('init', '--type', 'java-library', '--test-framework', 'testng', '--dsl', scriptDsl.id)

        then:
        file(SAMPLE_LIBRARY_CLASS).exists()
        file(SAMPLE_LIBRARY_TEST_CLASS).exists()

        and:
        commonFilesGenerated(scriptDsl)
        def dslFixture = dslFixtureFor(scriptDsl)
        buildFileSeparatesImplementationAndApi(dslFixture, 'org.testng')

        when:
        run("build")

        then:
        assertTestPassed("LibraryTest", "someLibraryMethodReturnsTrue")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "creates sample source with package and #testFramework and #scriptDsl build scripts"() {
        when:
        run('init', '--type', 'java-library', '--test-framework', 'testng', '--package', 'my.lib', '--dsl', scriptDsl.id)

        then:
        file("src/main/java/my/lib/Library.java").exists()
        file("src/test/java/my/lib/LibraryTest.java").exists()

        and:
        commonFilesGenerated(scriptDsl)

        when:
        run("build")

        then:
        assertTestPassed("my.lib.LibraryTest", "someLibraryMethodReturnsTrue")

        where:
        [scriptDsl, testFramework] << [ScriptDslFixture.SCRIPT_DSLS, [BuildInitTestFramework.JUNIT, BuildInitTestFramework.TESTNG]].combinations()
    }

    @Unroll
    def "creates sample source with package and spock and #scriptDsl build scripts"() {
        when:
        run('init', '--type', 'java-library', '--test-framework', 'spock', '--package', 'my.lib', '--dsl', scriptDsl.id)

        then:
        file("src/main/java/my/lib/Library.java").exists()
        file("src/test/groovy/my/lib/LibraryTest.groovy").exists()

        and:
        commonFilesGenerated(scriptDsl)

        when:
        run("build")

        then:
        assertTestPassed("my.lib.LibraryTest", "someLibraryMethod returns true")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "source generation is skipped when java sources detected with #scriptDsl build scripts"() {
        setup:
        file("src/main/java/org/acme/SampleMain.java") << """
        package org.acme;

        public class SampleMain{
        }
"""
        file("src/test/java/org/acme/SampleMainTest.java") << """
                package org.acme;

                public class SampleMainTest {
                }
        """
        when:
        run('init', '--type', 'java-library', '--dsl', scriptDsl.id)

        then:
        !file(SAMPLE_LIBRARY_CLASS).exists()
        !file(SAMPLE_LIBRARY_TEST_CLASS).exists()

        and:
        def dslFixture = dslFixtureFor(scriptDsl)
        dslFixture.assertGradleFilesGenerated()
        buildFileSeparatesImplementationAndApi(dslFixture)

        when:
        run("build")

        then:
        executed(":test")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    private void buildFileSeparatesImplementationAndApi(ScriptDslFixture dslFixture, String testFramework = 'junit:junit:') {
        dslFixture.buildFile.assertContents(
            allOf(
                dslFixture.containsConfigurationDependencyNotation('api', 'org.apache.commons:commons-math3'),
                dslFixture.containsConfigurationDependencyNotation('implementation', 'com.google.guava:guava:'),
                dslFixture.containsConfigurationDependencyNotation('testImplementation', testFramework)))
    }
}
