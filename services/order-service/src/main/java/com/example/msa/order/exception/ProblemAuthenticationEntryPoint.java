package com.example.msa.order.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 고객을 확인할 수 없는 요청(토큰이 없거나, 서명이 틀리거나, 만료됐다)을 401 Problem Details로 거부한다.
 * 인증은 컨트롤러 앞의 보안 필터에서 실패하므로 {@link ProblemDetailsHandler}(컨트롤러 예외 처리)가 닿지 못한다.
 * 그래서 같은 모양(type, title, status, detail, code)을 여기서 직접 쓴다. 토큰 내용은 응답과 로그에 담지 않는다.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter();

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        ProblemDetail problem = ProblemDetailsHandler.problem(
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요하다", "고객을 확인할 수 없다. 유효한 토큰으로 요청한다");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        converter.write(problem, MediaType.APPLICATION_PROBLEM_JSON, new ServletServerHttpResponse(response));
    }
}
