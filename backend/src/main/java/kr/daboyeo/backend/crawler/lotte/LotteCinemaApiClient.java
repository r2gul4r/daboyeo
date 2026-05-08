package kr.daboyeo.backend.crawler.lotte;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LotteCinemaApiClient {

    private static final String API_URL = "https://www.lottecinema.co.kr/LCWS/Event/EventData.aspx";
    private static final String REFERER_URL = "https://www.lottecinema.co.kr/NLCHS/Event/DetailList?code=";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public LotteCinemaApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public String fetchEventPage(String categoryCode, int pageNo) {
        int maxRetries = 3;
        for (int retry = 1; retry <= maxRetries; retry++) {
            try {
                return executeRequest(categoryCode, pageNo);
            } catch (Exception exception) {
                if (retry == maxRetries) {
                    return null;
                }
                try {
                    Thread.sleep(1000L * retry);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private String executeRequest(String categoryCode, int pageNo) throws Exception {
        String paramList = objectMapper.writeValueAsString(buildParams(categoryCode, pageNo));
        String body = "paramList=" + URLEncoder.encode(paramList, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("User-Agent", "Mozilla/5.0")
            .header("Referer", REFERER_URL + categoryCode)
            .timeout(Duration.ofSeconds(10))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unexpected status code: " + response.statusCode());
        }
        return decode(response.body());
    }

    private Map<String, Object> buildParams(String categoryCode, int pageNo) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("MethodName", "GetEventLists");
        params.put("channelType", "HO");
        params.put("osType", "W");
        params.put("osVersion", "Mozilla/5.0");
        params.put("EventClassificationCode", categoryCode);
        params.put("SearchText", "");
        params.put("CinemaID", "");
        params.put("PageNo", pageNo);
        params.put("PageSize", 100);
        params.put("MemberNo", "0");
        return params;
    }

    private String decode(byte[] body) {
        if (body == null || body.length == 0) {
            return null;
        }
        for (Charset charset : List.of(StandardCharsets.UTF_8, Charset.forName("EUC-KR"), Charset.forName("CP949"))) {
            String decoded = new String(body, charset);
            if (!decoded.contains("\uFFFD")) {
                return decoded;
            }
        }
        return new String(body, StandardCharsets.UTF_8);
    }
}
