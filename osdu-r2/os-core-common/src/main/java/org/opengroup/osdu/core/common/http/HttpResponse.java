// Copyright 2017-2019, Schlumberger
// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.core.common.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HttpResponse {
    static final Gson gson = new Gson();
    static final JsonParser parser = new JsonParser();
    Map<String, List<String>> headers;
    private String body = "";
    private String contentType = "";
    private int responseCode = 0;
    private Exception exception;
    private HttpRequest request;
    private long latency = 0;

    public Boolean isSuccessCode() {
        return responseCode >= 200 && responseCode <= 299;
    }

    public Boolean isServerErrorCode() {
        return responseCode >= 500;
    }

    public Boolean IsNotFoundCode() {
        return responseCode == 404;
    }

    public Boolean IsUnauthorizedCode() {
        return responseCode == 401;
    }

    public Boolean IsForbiddenCode() {
        return responseCode == 403;
    }

    public Boolean IsBadRequestCode() {
        return responseCode == 400;
    }

    public Boolean hasException() {
        return exception != null;
    }

    public JsonObject getAsJsonObject() {
        if (StringUtils.isBlank(body))
            return null;

        return parser.parse(body).getAsJsonObject();
    }

    public <T> T parseBody(Class<T> type) {
        if (StringUtils.isBlank(body))
            return null;

        return gson.fromJson(body, type);
    }
}
