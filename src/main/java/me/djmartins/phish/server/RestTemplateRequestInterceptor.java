package me.djmartins.phish.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class RestTemplateRequestInterceptor implements ClientHttpRequestInterceptor {

  private static final Logger LOGGER = LoggerFactory.getLogger(RestTemplateRequestInterceptor.class);

  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
    if (request.getURI().toString().contains("verifica_acesso.php")){
      traceRequest(request, body);
    }
    //traceRequest(request, body);
    ClientHttpResponse response = execution.execute(request, body);
    //traceResponse(response);
    return response;
  }

  private void traceRequest(HttpRequest request, byte[] body) throws IOException {
    LOGGER.info("===========================request begin================================================");
    LOGGER.info("URL         : {}", request.getURI());
    LOGGER.info("Method      : {}", request.getMethod());
    LOGGER.info("Headers     : {}", request.getHeaders() );
    LOGGER.info("Request body: {}", new String(body, "UTF-8"));
    LOGGER.info("==========================request end================================================");
  }

  private void traceResponse(ClientHttpResponse response) throws IOException {
    StringBuilder inputStringBuilder = new StringBuilder();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response.getBody(), "UTF-8"));
    String line = bufferedReader.readLine();
    while (line != null) {
      inputStringBuilder.append(line);
      inputStringBuilder.append('\n');
      line = bufferedReader.readLine();
    }
    LOGGER.info("============================response begin==========================================");
    LOGGER.info("Status code  : {}", response.getStatusCode());
    LOGGER.info("Status text  : {}", response.getStatusText());
    LOGGER.info("Headers      : {}", response.getHeaders());
    LOGGER.info("Response body: {}", inputStringBuilder.toString());
    LOGGER.info("=======================response end=================================================");
  }

}
