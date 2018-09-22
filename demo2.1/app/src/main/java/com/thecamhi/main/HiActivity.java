package com.thecamhi.main;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;

import com.hichip.progressload.DialogUtils;
import com.hichip1.R;

public class HiActivity extends FragmentActivity {
	protected ProgressDialog progressDialog;
	public Builder mDlgBuilder;
	public Dialog mJhLoading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.out_to_right, R.anim.in_from_left);
	}

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		super.startActivityForResult(intent, requestCode);
		overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
			break;
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public void startActivity(Intent intent) {
		super.startActivity(intent);
		overridePendingTransition(R.anim.in_from_right, R.anim.out_to_left);
	}

	public void showYesNoDialog(int msg, DialogInterface.OnClickListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(HiActivity.this);

		builder.setMessage(getResources().getString(msg)).setTitle(R.string.tips_warning).setPositiveButton(getResources().getString(R.string.btn_yes), listener)
				.setNegativeButton(getResources().getString(R.string.btn_no), listener).show();

	}

	public void showAlert(CharSequence message) {

		AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this);
		dlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
		dlgBuilder.setTitle(R.string.tips_warning);
		dlgBuilder.setMessage(message);
		dlgBuilder.setPositiveButton(getText(R.string.btn_ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		}).show();
	}

	public void showAlert(CharSequence message, DialogInterface.OnClickListener listener, boolean cancelable) {
		if (mDlgBuilder == null) {
			mDlgBuilder = new AlertDialog.Builder(this);
			mDlgBuilder.setIcon(android.R.drawable.ic_dialog_alert);
			mDlgBuilder.setTitle(R.string.tips_warning);
			mDlgBuilder.setMessage(message);
			mDlgBuilder.setCancelable(cancelable);
			mDlgBuilder.setPositiveButton(getText(R.string.btn_ok), listener).show();
		}
	}

	public interface MyDismiss {
		public void OnDismiss();
	}

	MyDismiss myDismiss;

	public void setOnLoadingProgressDismissListener(HiActivity.MyDismiss dismiss) {
		this.myDismiss = dismiss;
	}

	public void showLoadingProgress() {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(HiActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(false);
			progressDialog.setIcon(R.drawable.ic_launcher);
			progressDialog.setMessage(getText(R.string.tips_loading));

			progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (myDismiss != null) {
						myDismiss.OnDismiss();
						progressDialog = null;
					}

				}
			});
			progressDialog.show();
		}
	}

	/**
	 * 
	 * @param theme
	 * @param isOutSideOnTouch
	 *            //外部是否可以点击
	 */
	public void showLoadingProgress(int theme, boolean isOutSideOnTouch) {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(HiActivity.this, theme);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(isOutSideOnTouch);
			progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (myDismiss != null) {
						myDismiss.OnDismiss();
						progressDialog = null;
					}

				}
			});
			progressDialog.show();
		}
	}

	/**
	 * 
	 * @param isOutSideOnTouch
	 * @param title
	 *            Progressdialog 的Content Message
	 */
	public void showLoadingProgress(boolean isOutSideOnTouch, CharSequence title) {
		if (progressDialog == null) {
			progressDialog = new ProgressDialog(HiActivity.this);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.setCancelable(true);
			progressDialog.setCanceledOnTouchOutside(isOutSideOnTouch);
			progressDialog.setMessage(title);
			progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {

				@Override
				public void onDismiss(DialogInterface dialog) {
					if (myDismiss != null) {
						myDismiss.OnDismiss();
						progressDialog = null;
					}

				}
			});
			progressDialog.show();
		}
	}

	public void showAlertnew(Drawable iconId, CharSequence title, CharSequence message, CharSequence cancel, CharSequence okch, DialogInterface.OnClickListener listener) {
		if (mDlgBuilder == null) {
			mDlgBuilder = new AlertDialog.Builder(this);
			mDlgBuilder.setIcon(iconId);
			mDlgBuilder.setTitle(title);
			mDlgBuilder.setMessage(message);
			mDlgBuilder.setPositiveButton(okch, listener).setNegativeButton(cancel, listener).show();
		}
	}

	public void dismissLoadingProgress() {
		if (progressDialog != null) {
			progressDialog.cancel();
			progressDialog = null;
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
			mJhLoading = DialogUtils.createLoadingDialog(this, true);
		}
		mJhLoading.show();
	}
}
