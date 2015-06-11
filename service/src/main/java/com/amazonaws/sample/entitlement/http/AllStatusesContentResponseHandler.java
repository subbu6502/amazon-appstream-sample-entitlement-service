package com.amazonaws.sample.entitlement.http;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.entity.ContentType;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * The ContentResponseHandler that ships with the apache httpcommons fluent dependency raises an exception for
 * any responses whose status is > 300. We care about the body of the response for all requests so this
 * custom version returns the content for all requests.
 */
public class AllStatusesContentResponseHandler implements ResponseHandler<ResponseContent> {

    @Override
    public ResponseContent handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        final StatusLine statusLine = response.getStatusLine();
        final HttpEntity entity = response.getEntity();

        String content = null;

        if (entity != null) {
            ContentType type = ContentType.getOrDefault(entity);
            byte[] raw = EntityUtils.toByteArray(entity);

            Charset charset = type.getCharset();
            if (charset == null) {
                charset = HTTP.DEF_CONTENT_CHARSET;
            }
            try {
                content = new String(raw, charset.name());
            } catch (final UnsupportedEncodingException ex) {
                content = new String(raw);
            }
        }

        return new ResponseContent(statusLine.getStatusCode(), content);
    }

}
