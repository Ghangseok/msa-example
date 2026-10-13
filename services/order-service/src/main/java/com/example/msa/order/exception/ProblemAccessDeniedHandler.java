package com.example.msa.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 로그인은 했지만 권한이 없는 요청(예: ADMIN 역할이 없는 사람의 관리자 조회)을 403 Problem Details로 거부한다.
 * 권한 검사는 컨트롤러 앞의 보안 필터에서 실패하므로 {@link ProblemDetailsHandler}(컨트롤러 예외 처리)가 닿지 못한다.
 * 그래서 같은 모양(type, title, status, detail, code)을 여기서 직접 쓴다. 토큰 내용은 응답과 로그에 담지 않는다.
 */
@Component
public class ProblemAccessDeniedHandler implements AccessDeniedHandler {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        ProblemDetail problem = ProblemDetailsHandler.forbidden();
        response.setStatus(HttpStatus.FORBIDDEN.value());
        converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
    }
}
