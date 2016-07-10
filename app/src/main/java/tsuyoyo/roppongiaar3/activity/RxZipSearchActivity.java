package tsuyoyo.roppongiaar3.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import tsuyoyo.roppongiaar3.R;
import tsuyoyo.roppongiaar3.databinding.ZipSearchActivityBinding;
import tsuyoyo.roppongiaar3.model.ZipSearchActivityModel;
import tsuyoyo.roppongiaar3.task.ZipSearchTask;

public class RxZipSearchActivity extends AppCompatActivity {

    private static final String BUNDLE_KEY_MODEL = "model";

    private static final String TAG_PROGRESS_DIALOG = "progress_dialog";

    private static class MySubscriber extends Subscriber<List<String>> {

        private RxZipSearchActivity activeActivity;

        @Override
        public void onCompleted() {
            Log.d("TestTestTest", "onCompleted");
        }

        @Override
        public void onError(Throwable e) {
            // 何か問題があったらアドレスを表示する所にエラーメッセージを表示させる
            notifyResult(e.getMessage());
        }

        @Override
        public void onNext(List<String> result) {

            String address = "";
            for (String r : result) {
                address += r + "\n";
            }

            notifyResult(address);

            // onCompletedを呼ぶとunsubscribeも自動的に行われる
            onCompleted();
        }

        private void notifyResult(String resultMessage) {
            if (activeActivity != null) {
                activeActivity.dismissProgressDialog();
                activeActivity.model.address.set(resultMessage);
                activeActivity.mySubscriber = null;
            }
        }
    }

    private ZipSearchActivityModel model;

    private MySubscriber mySubscriber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            model = (ZipSearchActivityModel) savedInstanceState.getSerializable(BUNDLE_KEY_MODEL);
        } else {
            model = new ZipSearchActivityModel();
        }

        // 縦横回転で引き継がれているsubscriber
        // こいつが「今activeなActivityは誰だ」という事を知っていれば、API通信の結果は適切に返せる
        mySubscriber = (MySubscriber) getLastCustomNonConfigurationInstance();
        if (mySubscriber != null) {
            mySubscriber.activeActivity = this;
        }

        // 一度modelをセットしてしまえば、あとはmodelの操作だけで画面上のTextが変わる
        ZipSearchActivityBinding binding = DataBindingUtil.setContentView(
                this, R.layout.zip_search_activity);
        binding.setModel(model);
        binding.setSearchBtnListener(searchBtnListener);
    }

    private Button.OnClickListener searchBtnListener = btn -> {
        if (mySubscriber == null) {
            mySubscriber = new MySubscriber();
            mySubscriber.activeActivity = this;
        }
        showProgressDialog();

        // API通信をkickする。
        // Activityと一切関連を持たせないようにするのが重要。あくまで欲しいのは完了通知。
        ZipSearchTask.buildRequest(model.zipCode.get())
                .flatMap(ZipSearchTask::submitRequest)
                .flatMap(ZipSearchTask::parseResponse)
                .delay(5, TimeUnit.SECONDS)
                // ここまで↑書いたObservableによる処理を実行するスレッドを指定
                .subscribeOn(Schedulers.newThread())
                // Subscriberが動作するスレッドを指定
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mySubscriber);

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(btn.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    };

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(BUNDLE_KEY_MODEL, model);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // まだ通信の途中だったらdialogを出す
        if (mySubscriber != null && !mySubscriber.isUnsubscribed()) {
            showProgressDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // (メモ)
        // onSaveInstanceStateよりも後にdialogのdismissはできないので、onStopじゃなくてここでdismiss
        dismissProgressDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mySubscriber != null) {
            mySubscriber.activeActivity = null;
        }
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return mySubscriber;
    }

    public static class ProgressDialogFragment extends AppCompatDialogFragment {
        public ProgressDialogFragment() {
            super();
            setCancelable(false);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog dialog = new ProgressDialog(getActivity());
            dialog.setMessage("Calling API");
            return dialog;
        }
    }

    private void showProgressDialog() {
        if (getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG) == null) {
            new ProgressDialogFragment().show(getSupportFragmentManager(), TAG_PROGRESS_DIALOG);
        }
    }

    private void dismissProgressDialog() {
        ProgressDialogFragment progressDialog = (ProgressDialogFragment)
                getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG);

        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}
