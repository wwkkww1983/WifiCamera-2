package com.thecamhi.main;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.Fragment;
import com.hichip1.R;
import com.hichip.progressload.DialogUtils;
import com.thecamhi.main.HiActivity.MyDismiss;

public class HiFragment extends Fragment{
	protected ProgressDialog progressDialog;
	MyDismiss myDismiss;
	public Dialog mJhLoading;
	
	public void showLoadingProgress(){
		progressDialog=new ProgressDialog(getActivity());
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progressDialog.setCancelable(true);
		progressDialog.setCanceledOnTouchOutside(false);
		progressDialog.setIcon(R.drawable.ic_launcher);
		progressDialog.setMessage(getText(R.string.tips_loading));
		
		progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {  
			  
            @Override  
            public void onDismiss(DialogInterface dialog) {  
              if(myDismiss!=null){
            	  myDismiss.OnDismiss();
              }
  
            }  
        }); 
		
		
		progressDialog.show();

	}
	
	public void dismissLoadingProgress() {
		if(progressDialog!=null) {
			progressDialog.cancel();
		}
	}
	
	/**
	 * 菊花转的dialog
	 */
	public void dismissjuHuaDialog() {
		if (mJhLoading != null) {
			mJhLoading.dismiss();
		}
	}

	/**
	 * 菊花转的dialog
	 */
	public void showjuHuaDialog() {
		if (mJhLoading == null) {
			mJhLoading = DialogUtils.createLoadingDialog(getContext(), true);
		}
		mJhLoading.show();
	}
	
	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		getActivity().overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
	}
	
	
	
	
}




