package tsuyoyo.roppongiaar3.task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;

public class ZipSearchTask {

    private static final String ZIP_SEARCH_API = "http://zipcloud.ibsnet.co.jp/api/search";

    public static Observable<Request> buildRequest(String zipCode) {

        return Observable.create(subscriber -> {
            if (zipCode == null || zipCode.length() != 7) {
                subscriber.onError(new IllegalArgumentException("郵便番号は7桁で入力してちょ"));
            } else {
                String apiUrl = ZIP_SEARCH_API + "?zipcode=" + zipCode;
                subscriber.onNext(new Request.Builder().url(apiUrl).build());
            }
        });
    }

    public static Observable<Response> submitRequest(Request request) {
        return Observable.create(subscriber -> {
            OkHttpClient client = new OkHttpClient();
            try {
                subscriber.onNext(client.newCall(request).execute());
            } catch (IOException e) {
                subscriber.onError(e);
            }
        });
    }

    public static Observable<List<String>> parseResponse(Response response) {
        return Observable.create(subscriber -> {

            if (response.code() != 200) {
                subscriber.onError(new Throwable("通信エラー : response code - " + response.code()));
            }

            try {
                JSONObject responseObj = new JSONObject(response.body().string());

                if (!responseObj.has("results") || responseObj.isNull("results")) {
                    subscriber.onError(new Throwable("見つかりませんでした"));
                    return;
                }

                JSONArray jsonArray = responseObj.getJSONArray("results");
                if (jsonArray.length() == 0) {
                    subscriber.onError(new Throwable("見つかりませんでした"));
                    return;
                }

                List<String> result = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject addrInJson = jsonArray.getJSONObject(i);
                    String addr = addrInJson.getString("address1") + " "
                            + addrInJson.getString("address2") + " "
                            + addrInJson.getString("address3") + " "
                            + "("
                            + addrInJson.getString("kana1") + " "
                            + addrInJson.getString("kana2") + " "
                            + addrInJson.getString("kana3") + " "
                            + ")";
                    result.add(addr);
                }

                subscriber.onNext(result);

            } catch (JSONException e) {
                subscriber.onError(e);
            } catch (IOException e) {
                subscriber.onError(e);
            }
        });
    }

}
