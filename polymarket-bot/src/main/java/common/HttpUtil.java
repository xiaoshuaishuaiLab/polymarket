package common;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * HTTP 工具类 - 基于 Apache HttpClient 连接池
 */
@Slf4j
public final class HttpUtil {

    private static final CloseableHttpClient HTTP_CLIENT;

    // 默认超时配置（毫秒）
    private static final int CONNECT_TIMEOUT = 10_000;
    private static final int SOCKET_TIMEOUT = 30_000;
    private static final int CONNECTION_REQUEST_TIMEOUT = 5_000;

    static {
        var cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(100);
        cm.setDefaultMaxPerRoute(20);

        var requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .build();

        HTTP_CLIENT = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }

    private HttpUtil() {
        // 工具类禁止实例化
    }

    // ==================== GET 请求 ====================

    /**
     * GET 请求，返回字符串
     */
    public static Optional<String> get(String url) {
        var httpGet = new HttpGet(url);
        try (var response = HTTP_CLIENT.execute(httpGet)) {
            return handleResponse(response);
        } catch (IOException e) {
            log.error("GET request failed: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * GET 请求带查询参数
     */
    public static Optional<String> get(String url, List<NameValuePair> params) {
        try {
            var httpGet = new HttpGet(url);
            var queryString = EntityUtils.toString(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
            httpGet.setURI(new URI(httpGet.getURI().toString() + "?" + queryString));
            try (var response = HTTP_CLIENT.execute(httpGet)) {
                return handleResponse(response);
            }
        } catch (Exception e) {
            log.error("GET request with params failed: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * GET 请求带请求头和查询参数，返回 JSON
     */
    public static Optional<JSONObject> getJson(String url, Map<String, String> headers, List<NameValuePair> params) {
        try {
            var httpGet = new HttpGet(url);

            if (!CollectionUtils.isEmpty(headers)) {
                headers.forEach(httpGet::setHeader);
            }
            if (!CollectionUtils.isEmpty(params)) {
                var queryString = EntityUtils.toString(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));
                httpGet.setURI(new URI(httpGet.getURI().toString() + "?" + queryString));
            }

            try (var response = HTTP_CLIENT.execute(httpGet)) {
                return handleResponse(response)
                        .map(JSONObject::parseObject);
            }
        } catch (Exception e) {
            log.error("GET JSON request failed: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * GET 请求返回 JSON（无参数）
     */
    public static Optional<JSONObject> getJson(String url) {
        return get(url).map(JSONObject::parseObject);
    }

    // ==================== POST 请求 ====================

    /**
     * POST JSON 请求
     */
    public static Optional<String> postJson(String url, Object body) {
        var httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(JsonUtil.toJson(body), StandardCharsets.UTF_8));

        try (var response = HTTP_CLIENT.execute(httpPost)) {
            return handleResponse(response);
        } catch (IOException e) {
            log.error("POST JSON request failed: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * POST 字符串请求，返回 JSON
     */
    public static Optional<JSONObject> postJson(String url, String body) {
        var httpPost = new HttpPost(url);
        httpPost.setEntity(new StringEntity(body, StandardCharsets.UTF_8));

        try (var response = HTTP_CLIENT.execute(httpPost)) {
            return handleResponse(response)
                    .map(JSONObject::parseObject);
        } catch (IOException e) {
            log.error("POST JSON request failed: {}", url, e);
            return Optional.empty();
        }
    }

    /**
     * POST 表单请求
     */
    public static Optional<JSONObject> postForm(String url, List<NameValuePair> params) {
        var httpPost = new HttpPost(url);
        httpPost.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

        try (var response = HTTP_CLIENT.execute(httpPost)) {
            return handleResponse(response)
                    .map(JSONObject::parseObject);
        } catch (IOException e) {
            log.error("POST form request failed: {}", url, e);
            return Optional.empty();
        }
    }

    // ==================== 私有方法 ====================

    private static Optional<String> handleResponse(HttpResponse response) throws IOException {
        var statusLine = response.getStatusLine();
        var statusCode = statusLine.getStatusCode();

        if (statusCode >= 400) {
            log.warn("HTTP request failed with status {}: {}", statusCode, statusLine.getReasonPhrase());
            return Optional.empty();
        }

        var entity = response.getEntity();
        if (entity == null) {
            return Optional.empty();
        }

        var body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        EntityUtils.consume(entity); // 确保资源释放
        return Optional.ofNullable(body);
    }

    /**
     * 关闭连接池（应用关闭时调用）
     */
    public static void shutdown() {
        try {
            HTTP_CLIENT.close();
            log.info("HTTP client connection pool closed");
        } catch (IOException e) {
            log.error("Failed to close HTTP client", e);
        }
    }
}
