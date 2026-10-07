package msa.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

/**
 * Lombok을 쓰지 않는다 (docs/standards/coding-conventions.md 3-8절).
 * 어떤 configuration에든 org.projectlombok 그룹이 들어오면 실패한다.
 * 보조로 소스에서 "import lombok."을 찾는다.
 */
abstract class NoLombokCheck extends DefaultTask {

    static final Pattern LOMBOK_IMPORT = Pattern.compile('^\\s*import\\s+(static\\s+)?lombok\\.')

    /** 모든 configuration에 선언된 의존성과 해석된 모듈의 그룹 */
    @Input
    abstract SetProperty<String> getDependencyGroups()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSourceFiles()

    @TaskAction
    void check() {
        List<String> problems = []
        if (dependencyGroups.get().contains('org.projectlombok')) {
            problems << 'org.projectlombok 그룹의 의존성이 들어와 있다. Lombok을 쓰지 않는다.'
        }
        sourceFiles.files.sort().each { File file ->
            file.readLines('UTF-8').eachWithIndex { String line, int index ->
                if (LOMBOK_IMPORT.matcher(line).find()) {
                    problems << "${file}:${index + 1}: import lombok.을 쓰지 않는다.".toString()
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new GradleException("Lombok 검사에 실패했다:\n  " + problems.join('\n  '))
        }
    }
}
