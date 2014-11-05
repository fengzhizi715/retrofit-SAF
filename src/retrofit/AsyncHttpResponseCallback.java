/**
 * 
 */
package retrofit;

import retrofit.client.Response;

/**
 * @author Tony Shen
 * @param <T>
 *
 */
public abstract class AsyncHttpResponseCallback<T> implements Callback<T>{

	public void onStart() {
	}

	public void onFinish() {
	}

	@Override
	public void success(T t, Response response) {
		onStart();
		onSuccess(t, response);
		onFinish();
	}
	
	public abstract void onSuccess(T t, Response response);
}
