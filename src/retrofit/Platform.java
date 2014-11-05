/*
 * Copyright (C) 2013 Square, Inc.
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

import static java.lang.Thread.MIN_PRIORITY;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import retrofit.android.AndroidLog;
import retrofit.android.MainThreadExecutor;
import retrofit.client.Client;
import retrofit.client.RetrofitClient;
import retrofit.client.UrlConnectionClient;
import retrofit.converter.Converter;
import retrofit.converter.FastjsonConverter;
import android.os.Build;
import cn.salesuite.saf.executor.concurrent.BackgroundPriorityThreadFactory;

/**
 * TODO frankswu : change okhttp to restClient
 */
abstract class Platform {
	private static final Platform PLATFORM = findPlatform();

	static final boolean HAS_RX_JAVA = hasRxJavaOnClasspath();

	static Platform get() {
		return PLATFORM;
	}

	private static Platform findPlatform() {
		try {
			Class.forName("android.os.Build");
			if (Build.VERSION.SDK_INT != 0) {
				return new Android();
			}
		} catch (ClassNotFoundException ignored) {
		}

		return new Base();
	}

	abstract Converter defaultConverter();

	abstract Client.Provider defaultClient();

	abstract Executor defaultHttpExecutor();

	abstract Executor defaultCallbackExecutor();

	abstract RestAdapter.Log defaultLog();

	/** Provides sane defaults for operation on the JVM. */
	private static class Base extends Platform {
		@Override
		Converter defaultConverter() {
			return new FastjsonConverter();
		}

		@Override
		Client.Provider defaultClient() {
			final Client client = new UrlConnectionClient();

			return new Client.Provider() {
				@Override
				public Client get() {
					return client;
				}
			};
		}

		@Override
		Executor defaultHttpExecutor() {
			return Executors.newCachedThreadPool(new ThreadFactory() {
				@Override
				public Thread newThread(final Runnable r) {
					return new Thread(new Runnable() {
						@Override
						public void run() {
							Thread.currentThread().setPriority(MIN_PRIORITY);
							r.run();
						}
					}, RestAdapter.IDLE_THREAD_NAME);
				}
			});
		}

		@Override
		Executor defaultCallbackExecutor() {
			return new Utils.SynchronousExecutor();
		}

		@Override
		RestAdapter.Log defaultLog() {
			return new RestAdapter.Log() {
				@Override
				public void log(String message) {
					System.out.println(message);
				}
			};
		}
	}

	/** Provides sane defaults for operation on Android. */
	private static class Android extends Platform {
		@Override
		Converter defaultConverter() {
			return new FastjsonConverter();
		}

		@Override
		Client.Provider defaultClient() {
			final Client client = new RetrofitClient();
					
			return new Client.Provider() {
				@Override
				public Client get() {
					return client;
				}
			};
		}

		@Override
		Executor defaultHttpExecutor() {
			return Executors
					.newCachedThreadPool(new BackgroundPriorityThreadFactory(
							RestAdapter.IDLE_THREAD_NAME));
		}

		@Override
		Executor defaultCallbackExecutor() {
			return new MainThreadExecutor();
		}

		@Override
		RestAdapter.Log defaultLog() {
			return new AndroidLog("Retrofit");
		}
	}

	private static boolean hasRxJavaOnClasspath() {
		try {
			Class.forName("rx.Observable");
			return true;
		} catch (ClassNotFoundException ignored) {
		}
		return false;
	}

}
