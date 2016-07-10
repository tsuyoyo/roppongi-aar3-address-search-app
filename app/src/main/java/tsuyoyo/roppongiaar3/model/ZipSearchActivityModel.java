package tsuyoyo.roppongiaar3.model;

import android.databinding.ObservableField;

import java.io.Serializable;

public class ZipSearchActivityModel implements Serializable {

    private static final long serialVersionUID = -4889838022727659942L;

    /**
     * ユーザの入力した郵便番号
     *
     */
    public ObservableField<String> zipCode = new ObservableField<>();

    /**
     * 検索した結果の住所
     * (場合によってはエラー文言を表示するのに使う)
     *
     */
    public ObservableField<String> address = new ObservableField<>();

}
