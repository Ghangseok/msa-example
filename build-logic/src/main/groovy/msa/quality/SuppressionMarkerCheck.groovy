package msa.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

/**
 * 검사를 끄는 표시를 사용자 승인 없이 넣지 않는다 (저장소 루트 CLAUDE.md 7절, docs/standards/coding-conventions.md 3-8절).
 * src/ 아래 Java 소스와 build.gradle에서 표시를 찾고, 허용 목록(config/quality/suppression-allowlist.csv)에 없으면 실패한다.
 * 허용 목록의 열은 path,marker,reason,approved_pr이다. path는 저장소 루트 기준 경로이고, marker는 아래 표시의 이름이다.
 */
abstract class SuppressionMarkerCheck extends DefaultTask {

    /** 표시의 이름과 그것을 찾는 패턴. 이름이 허용 목록의 marker 칸 값이다. */
    static final Map<String, Pattern> JAVA_MARKERS = [
        '@Disabled'         : Pattern.compile('@Disabled\\b'),
        '@DisabledIf'       : Pattern.compile('@DisabledIf\\b'),
        '@EnabledIf'        : Pattern.compile('@EnabledIf\\b'),
        'Assumptions.assume': Pattern.compile('Assumptions\\.assume'),
        '@SuppressWarnings' : Pattern.compile('@SuppressWarnings\\b'),
        '@SuppressFBWarnings': Pattern.compile('@SuppressFBWarnings\\b'),
        'NOPMD'             : Pattern.compile('NOPMD'),
        'spotless:off'      : Pattern.compile('spotless:off'),
        '@formatter:off'    : Pattern.compile('@formatter:off'),
        'FreezingArchRule'  : Pattern.compile('FreezingArchRule'),
    ].asImmutable()

    /** build.gradle에서만 찾는 표시. 테스트 작업의 exclude다. */
    static final Map<String, Pattern> GRADLE_ONLY_MARKERS = [
        'exclude': Pattern.compile('\\bexclude\\b'),
    ].asImmutable()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getJavaFiles()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getBuildFiles()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getAllowlist()

    @Internal
    abstract DirectoryProperty getRootDirectory()

    /** 허용 목록에서 "경로|표시" 모음을 읽는다. 첫 줄은 열 이름이다. */
    static Set<String> readAllowlist(File csv) {
        Set<String> allowed = new HashSet<>()
        List<String> lines = csv.readLines('UTF-8')
        lines.drop(1).eachWithIndex { String line, int index ->
            if (line.trim().isEmpty()) {
                return
            }
            String[] columns = line.split(',', -1)
            if (columns.length < 4) {
                throw new GradleException("${csv}:${index + 2}: 허용 목록 줄은 path,marker,reason,approved_pr 네 칸이어야 한다.")
            }
            allowed << (columns[0].trim() + '|' + columns[1].trim())
        }
        return allowed
    }

    String relativePath(File file) {
        return rootDirectory.get().asFile.toPath().relativize(file.toPath()).toString().replace('\\', '/')
    }

    @TaskAction
    void check() {
        Set<String> allowed = readAllowlist(allowlist.get().asFile)
        List<String> problems = []
        Map<String, Pattern> javaMarkers = JAVA_MARKERS
        javaFiles.files.sort().each { File file ->
            scan(file, javaMarkers, allowed, problems)
        }
        Map<String, Pattern> gradleMarkers = new LinkedHashMap<>(JAVA_MARKERS)
        gradleMarkers.putAll(GRADLE_ONLY_MARKERS)
        buildFiles.files.findAll { it.exists() }.sort().each { File file ->
            scan(file, gradleMarkers, allowed, problems)
        }
        if (!problems.isEmpty()) {
            throw new GradleException(
                '검사를 끄는 표시가 허용 목록에 없다. 표시를 지우거나, 사용자 승인을 받아 config/quality/suppression-allowlist.csv에 줄을 더한다.\n  '
                    + problems.join('\n  '))
        }
    }

    void scan(File file, Map<String, Pattern> markers, Set<String> allowed, List<String> problems) {
        String path = relativePath(file)
        file.readLines('UTF-8').eachWithIndex { String line, int index ->
            markers.each { String marker, Pattern pattern ->
                if (pattern.matcher(line).find() && !allowed.contains(path + '|' + marker)) {
                    problems << "${path}:${index + 1}: ${marker}".toString()
                }
            }
        }
    }
}
