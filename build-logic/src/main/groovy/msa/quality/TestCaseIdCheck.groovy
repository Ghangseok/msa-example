package msa.quality

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * 인수 시나리오 테스트에 테스트 케이스 ID를 남긴다 (docs/standards/testing.md 3절, docs/standards/coding-conventions.md 3-8절).
 * 두 가지를 본다.
 * 첫째, docs/test-cases/order-placement.md의 모든 테스트 케이스 ID가 단계 표(config/quality/test-case-stages.csv)에 있는가.
 * 둘째, 단계가 지금 단계(gradle.properties의 msa.stage) 이하인 줄마다, 그 서비스의 테스트 소스에 @DisplayName이 그 ID로 시작하는 테스트가 하나 이상 있는가.
 * 단계가 T002인 줄(티켓 002로 넘긴 부분)은 단계 순서에 들지 않아 둘째 검사에서 건너뛴다. 운영 테스트는 대상이 아니다.
 */
abstract class TestCaseIdCheck extends DefaultTask {

    static final List<String> STAGE_ORDER = ['P1', 'P2', 'P3'].asImmutable()
    static final Pattern TEST_CASE_HEADING = Pattern.compile('(?m)^##\\s+(TC-\\d{3})\\s')

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getTestCaseDocument()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract RegularFileProperty getStageTable()

    /** gradle.properties의 msa.stage */
    @Input
    abstract Property<String> getCurrentStage()

    /** 단계 표의 service 칸 값. 예: inventory */
    @Input
    abstract Property<String> getServiceName()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getTestSources()

    @TaskAction
    void check() {
        String stage = currentStage.get()
        int currentIndex = STAGE_ORDER.indexOf(stage)
        if (currentIndex < 0) {
            throw new GradleException("gradle.properties의 msa.stage는 ${STAGE_ORDER} 가운데 하나여야 한다. 지금 값: ${stage}".toString())
        }

        List<String> documentIds = []
        Matcher headings = TEST_CASE_HEADING.matcher(testCaseDocument.get().asFile.getText('UTF-8'))
        while (headings.find()) {
            documentIds << headings.group(1)
        }

        List<List<String>> rows = []
        testCaseStageRows(stageTable.get().asFile, rows)
        Set<String> tableIds = new HashSet<>()
        for (List<String> row : rows) {
            tableIds << row[0]
        }

        List<String> problems = []
        for (String id : documentIds) {
            if (!tableIds.contains(id)) {
                problems << "${id}: 테스트 케이스 문서에는 있는데 단계 표(config/quality/test-case-stages.csv)에 없다.".toString()
            }
        }

        String testSourceText = ''
        for (File file : testSources.files.sort()) {
            testSourceText += file.getText('UTF-8') + '\n'
        }
        for (List<String> row : rows) {
            String id = row[0]
            int rowIndex = STAGE_ORDER.indexOf(row[2])
            if (row[1] == serviceName.get() && rowIndex >= 0 && rowIndex <= currentIndex) {
                Pattern displayName = Pattern.compile('@DisplayName\\(\\s*"' + Pattern.quote(id))
                if (!displayName.matcher(testSourceText).find()) {
                    problems << "${id}: ${row[1]} ${row[2]} 줄인데, @DisplayName이 이 ID로 시작하는 테스트가 없다.".toString()
                }
            }
        }

        if (!problems.isEmpty()) {
            throw new GradleException("테스트 케이스 ID 검사에 실패했다(지금 단계 ${stage}):\n  " + problems.join('\n  '))
        }
    }

    /** 단계 표를 읽어 [tc_id, service, stage] 줄을 rows에 담는다. 첫 줄은 열 이름이다. */
    static void testCaseStageRows(File csv, List<List<String>> rows) {
        List<String> lines = csv.readLines('UTF-8')
        for (String line : lines.drop(1)) {
            if (line.trim().isEmpty()) {
                continue
            }
            String[] columns = line.split(',', -1)
            if (columns.length != 3) {
                throw new GradleException("${csv}: 단계 표 줄은 tc_id,service,stage 세 칸이어야 한다: ${line}".toString())
            }
            rows << [columns[0].trim(), columns[1].trim(), columns[2].trim()]
        }
    }
}
