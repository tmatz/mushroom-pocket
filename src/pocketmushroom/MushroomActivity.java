package org.tmatz.pocketmashroom;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MushroomActivity extends Activity implements OnClickListener {
    private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
    private static final String REPLACE_KEY = "replace_key";
    private String mReplaceString;
    private Button mReplaceBtn;
    private Button mCancelBtn;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent it = getIntent();
        String action = it.getAction();
        if (action != null && ACTION_INTERCEPT.equals(action)) {
            /* Simejiから呼出された時 */
            mReplaceString = it.getStringExtra(REPLACE_KEY);// 置換元の文字を取得
            setContentView(R.layout.mushroom);
            mReplaceBtn = (Button) findViewById(R.id.replace_btn);
            mReplaceBtn.setOnClickListener(this);
            mCancelBtn = (Button) findViewById(R.id.cancel_btn);
            mCancelBtn.setOnClickListener(this);
        } else {
            // Simeji以外から呼出された時
            setContentView(R.layout.main);
        }
    }

    public void onClick(View v) {
        String result = null;
        if (v == mReplaceBtn) {
            result = getCallingPackage();
        } else if (v == mCancelBtn) {
            result = mReplaceString;
        }
        replace(result);
    }
    
    /**
     * 元の文字を置き換える
     * @param result Replacing string
     */
    private void replace(String result) {
        Intent data = new Intent();
        data.putExtra(REPLACE_KEY, result);
        setResult(RESULT_OK, data);
        finish();
    }
}
