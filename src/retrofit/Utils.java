/*
 * Copyright (C) 2012 Square, Inc.
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package retrofit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import cn.salesuite.saf.utils.IOUtil;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

final class Utils {

	/**
	 * Conditionally replace a {@link Request} with an identical copy whose body
	 * is backed by a byte[] rather than an input stream.
	 */
	static Request readBodyToBytesIfNecessary(Request request)
			throws IOException {
		TypedOutput body = request.getBody();
		if (body == null || body instanceof TypedByteArray) {
			return request;
		}

		String bodyMime = body.mimeType();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		body.writeTo(baos);
		body = new TypedByteArray(bodyMime, baos.toByteArray());

		return new Request(request.getMethod(), request.getUrl(),
				request.getHeaders(), body);
	}

	/**
	 * Conditionally replace a {@link Response} with an identical copy whose
	 * body is backed by a byte[] rather than an input stream.
	 */
	static Response readBodyToBytesIfNecessary(Response response)
			throws IOException {
		TypedInput body = response.getBody();
		if (body == null || body instanceof TypedByteArray) {
			return response;
		}

		String bodyMime = body.mimeType();
		InputStream is = body.in();
		try {
			byte[] bodyBytes = IOUtil.readInputStream(is);
			body = new TypedByteArray(bodyMime, bodyBytes);

			return replaceResponseBody(response, body);
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	static Response replaceResponseBody(Response response, TypedInput body) {
		return new Response(response.getUrl(), response.getStatus(),
				response.getReason(), response.getHeaders(), body);
	}

	static <T> void validateServiceClass(Class<T> service) {
		if (!service.isInterface()) {
			throw new IllegalArgumentException(
					"Only interface endpoint definitions are supported.");
		}
		// Prevent API interfaces from extending other interfaces. This not only
		// avoids a bug in
		// Android (http://b.android.com/58753) but it forces composition of API
		// declarations which is
		// the recommended pattern.
		if (service.getInterfaces().length > 0) {
			throw new IllegalArgumentException(
					"Interface definitions must not extend other interfaces.");
		}
	}

	static class SynchronousExecutor implements Executor {
		@Override
		public void execute(Runnable runnable) {
			runnable.run();
		}
	}

	private Utils() {
		// No instances.
	}
}
