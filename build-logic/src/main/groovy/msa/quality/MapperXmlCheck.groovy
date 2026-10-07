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
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import java.nio.file.Path
import java.util.regex.Pattern

/**
 * Mapper XML에서 ${}와 SELECT *를 쓰지 않는다 (docs/standards/coding-conventions.md 3-2절, 3-8절).
 * Mapper XML을 XML 파서로 읽고, 문장 요소(select, insert, update, delete, sql)의 글자를 검사한다.
 * XML 주석은 빼고 본다. ${는 허용 목록에 있는 것만 통과시킨다. COUNT(*)는 잡지 않는다.
 * ${ 예외는 config/quality/suppression-allowlist.csv에 marker 칸을 ${ 로 적는다.
 */
abstract class MapperXmlCheck extends DefaultTask {

    static final List<String> STATEMENT_TAGS = ['select', 'insert', 'update', 'delete', 'sql'].asImmutable()
    static final Pattern SELECT_STAR = Pattern.compile('\\bselect\\s+\\*', Pattern.CASE_INSENSITIVE)
    static final String DOLLAR_MARKER = '${'

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getMapperFiles()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getAllowlist()

    @Internal
    abstract DirectoryProperty getRootDirectory()

    /** 문장 요소마다 [tag, id, text]를 돌려준다. text에는 XML 주석이 들어 있지 않다. */
    static List<Map<String, String>> statements(File xml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setValidating(false)
        factory.setFeature('http://apache.org/xml/features/nonvalidating/load-external-dtd', false)
        DocumentBuilder builder = factory.newDocumentBuilder()
        // Mapper XML의 DOCTYPE이 가리키는 외부 DTD를 내려받지 않는다.
        builder.setEntityResolver({ String publicId, String systemId -> new InputSource(new StringReader('')) } as EntityResolver)
        def document = builder.parse(xml)
        List<Map<String, String>> result = []
        for (String tag : STATEMENT_TAGS) {
            def nodes = document.getElementsByTagName(tag)
            for (int i = 0; i < nodes.length; i++) {
                def element = nodes.item(i)
                result << [tag: tag, id: element.getAttribute('id'), text: element.getTextContent()]
            }
        }
        return result
    }

    @TaskAction
    void check() {
        Set<String> allowed = SuppressionMarkerCheck.readAllowlist(allowlist.get().asFile)
        Path rootPath = rootDirectory.get().asFile.toPath()
        List<String> problems = []
        for (File file : mapperFiles.files.sort()) {
            String path = rootPath.relativize(file.toPath()).toString().replace('\\', '/')
            for (Map<String, String> statement : statements(file)) {
                String where = "${path}: <${statement.tag} id=\"${statement.id}\">".toString()
                if (statement.text.contains(DOLLAR_MARKER) && !allowed.contains(path + '|' + DOLLAR_MARKER)) {
                    problems << "${where}에 \${}가 있다. #{}를 쓴다. 꼭 필요하면 허용 목록에 줄을 더한다.".toString()
                }
                if (SELECT_STAR.matcher(statement.text).find()) {
                    problems << "${where}에 SELECT *가 있다. 필요한 열을 적는다.".toString()
                }
            }
        }
        if (!problems.isEmpty()) {
            throw new GradleException("Mapper XML 검사에 실패했다:\n  " + problems.join('\n  '))
        }
    }
}
