package msa.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.nio.file.Path
import java.util.regex.Pattern

/**
 * 19c 이후의 SQL 기능을 쓰지 않는다 (docs/standards/coding-conventions.md 3-3절, 3-8절).
 * Mapper XML과 Flyway SQL에서 금지 키워드를 찾는다:
 * IF NOT EXISTS, IF EXISTS, BOOLEAN(열 타입), JSON(열 타입. IS JSON 조건은 제외), SQL_MACRO, VECTOR, DOMAIN, ANNOTATIONS.
 * 흔한 단어로 쓰는 23ai 문법(FROM 없는 SELECT, GROUP BY의 별칭, VALUES로 여러 행 넣기, UPDATE의 조인)은 잡지 못한다. PR 리뷰로 본다.
 */
abstract class Sql19cKeywordCheck extends DefaultTask {

    static final Map<String, Pattern> FORBIDDEN = [
        'IF NOT EXISTS': Pattern.compile('\\bIF\\s+NOT\\s+EXISTS\\b', Pattern.CASE_INSENSITIVE),
        'IF EXISTS'    : Pattern.compile('\\bIF\\s+EXISTS\\b', Pattern.CASE_INSENSITIVE),
        'BOOLEAN'      : Pattern.compile('\\bBOOLEAN\\b', Pattern.CASE_INSENSITIVE),
        'JSON'         : Pattern.compile('\\bJSON\\b', Pattern.CASE_INSENSITIVE),
        'SQL_MACRO'    : Pattern.compile('\\bSQL_MACRO\\b', Pattern.CASE_INSENSITIVE),
        'VECTOR'       : Pattern.compile('\\bVECTOR\\b', Pattern.CASE_INSENSITIVE),
        'DOMAIN'       : Pattern.compile('\\bDOMAIN\\b', Pattern.CASE_INSENSITIVE),
        'ANNOTATIONS'  : Pattern.compile('\\bANNOTATIONS\\b', Pattern.CASE_INSENSITIVE),
    ].asImmutable()

    /** JSON 열 타입만 막는다. IS JSON, IS NOT JSON 조건은 지우고 본다. */
    static final Pattern IS_JSON = Pattern.compile('\\bIS\\s+(NOT\\s+)?JSON\\b', Pattern.CASE_INSENSITIVE)
    static final Pattern BLOCK_COMMENT = Pattern.compile('/\\*.*?\\*/', Pattern.DOTALL)
    static final Pattern LINE_COMMENT = Pattern.compile('--[^\\n]*')

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getMapperFiles()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSqlFiles()

    @Internal
    abstract DirectoryProperty getRootDirectory()

    /** 주석을 뺀 SQL 글자에서 금지 키워드의 이름을 돌려준다. */
    static List<String> findForbidden(String sql) {
        String text = IS_JSON.matcher(sql).replaceAll(' ')
        List<String> found = []
        for (Map.Entry<String, Pattern> entry : FORBIDDEN.entrySet()) {
            if (entry.value.matcher(text).find()) {
                found << entry.key
            }
        }
        return found
    }

    @TaskAction
    void check() {
        Path rootPath = rootDirectory.get().asFile.toPath()
        List<String> problems = []
        for (File file : mapperFiles.files.sort()) {
            String path = rootPath.relativize(file.toPath()).toString().replace('\\', '/')
            for (Map<String, String> statement : MapperXmlCheck.statements(file)) {
                for (String keyword : findForbidden(statement.text)) {
                    problems << "${path}: <${statement.tag} id=\"${statement.id}\">에 ${keyword}가 있다.".toString()
                }
            }
        }
        for (File file : sqlFiles.files.sort()) {
            String path = rootPath.relativize(file.toPath()).toString().replace('\\', '/')
            String sql = file.getText('UTF-8')
            sql = BLOCK_COMMENT.matcher(sql).replaceAll(' ')
            sql = LINE_COMMENT.matcher(sql).replaceAll(' ')
            for (String keyword : findForbidden(sql)) {
                problems << "${path}에 ${keyword}가 있다.".toString()
            }
        }
        if (!problems.isEmpty()) {
            throw new GradleException("19c 이후 SQL 기능 검사에 실패했다. Oracle 19c에서 도는 SQL만 쓴다:\n  " + problems.join('\n  '))
        }
    }
}
