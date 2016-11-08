package com.mtk.offlinek.fragment;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mtk.offlinek.ETTRunner;
import com.mtk.offlinek.FileHandler;
import com.mtk.offlinek.NativeLoader;
import com.mtk.offlinek.R;
import com.mtk.offlinek.component.DeviceType;
import com.mtk.offlinek.fragment.RecordItem.ViewHolder;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("HandlerLeak")
public class RecordFragment extends Fragment {
	private View thisView;
	private ListView mListView = null;
	private BaseAdapter actionAdapter;
	Button mBatchBtn;
	Handler uiHandler;
	Handler progressHD;
	public static final int MSG_SHOW_TEXT = 1;
	public static final int MSG_DUMP_TOAST = 2;
	public static final int MSG_POP_DIALOG = 3;
	public static final int MSG_SHOW_PROGRESS = 4;
	protected static final int MSG_CHG_BTN_UI = 5;
	public static final int MSG_RESULT_AVAILABLE = 6;
	public long mCurClickIndex = 0;
	public int mProgress = 0;
	public int mCurDoItem = 0;
	Button.OnClickListener mDelBtnClickListener;
	OnTouchListener mViewHolderListener;
	Dialog mSolutionDialog;
	class TuningParameter {
		int CMD_RSP_TA_CNTR;
		int CKGEN_MSDC_DLY_SEL;
		int PAD_CMD_RESP_RXDLY;
		int R_SMPL;
		int PAD_CMD_RXDLY;
		int INT_DAT_LATCH_CK_SEL;
		int R_D_SMPL;
		int PAD_DATA_RD_RXDLY;
		int WRDAT_CRCS_TA_CNTR;
		int W_D_SMPL;
		int PAD_DATA_WR_RXDLY;
	}
	
	static String[] TuningKeys = new String[]{"CMD_RSP_TA_CNTR", "CKGEN_MSDC_DLY_SEL", "PAD_CMD_RESP_RXDLY", 
                                "R_SMPL", "PAD_CMD_RXDLY", "INT_DAT_LATCH_CK_SEL", "R_D_SMPL", "PAD_DATA_RD_RXDLY", 
                                "WRDAT_CRCS_TA_CNTR", "W_D_SMPL", "PAD_DATA_WR_RXDLY"};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		thisView = inflater.inflate(R.layout.record_fragment, container, false);
		init();
		return thisView;
	}

	private void init() {
		mListView = (ListView) thisView.findViewById(R.id.listview);
		TextView textView = (TextView) thisView
				.findViewById(R.id.outputOrder_string);
		actionAdapter = new MyListViewAdapter(thisView.getContext());
		textView.setText(RecordItem.orderSample);
		mListView.setAdapter(actionAdapter);
		//mListView.setItemsCanFocus(true);
		//mListView.setClickable(true);
		
		mBatchBtn = (Button) thisView.findViewById(R.id.batchRunButton);
		mBatchBtn.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Add list permuation
				new Thread(new Runnable() {
					@Override
					public void run() {
						mCurDoItem = 0;
						ETTRunner fRunner;
						fRunner = new ETTRunner(thisView.getContext(),
								uiHandler, progressHD, 0);
						if (fRunner.checkPermission()) {
							for (int i = 0; i < RecordItem.getCount(); i++) {
								mCurDoItem = i;
								RecordItem curItem = RecordItem.recordList.get(i);
								if(curItem.isDone())
									continue;
								fRunner = new ETTRunner(thisView.getContext(),
										uiHandler, progressHD, i);
								if(curItem.mDevice.toLowerCase(Locale.ENGLISH).contains("lte")){
									fRunner.setDevice(DeviceType.LTE);
								} else {
									fRunner.setDevice(DeviceType.WIFI);
								}
								fRunner.setPort(Integer.parseInt(curItem.mPort));
								Message msg = progressHD.obtainMessage(MSG_CHG_BTN_UI, 0, i);
								progressHD.sendMessage(msg);
								msg = null;
								fRunner.run();
								fRunner = null;
							}

							for (int i = 0; i < RecordItem.getCount(); i++) {
								Message msg = progressHD.obtainMessage(MSG_CHG_BTN_UI, 1, i);
								progressHD.sendMessage(msg);
								msg = null;
							}
						}
						fRunner = null;
					}
				}).start();

			}
		});
		NativeLoader.getInstance().init();
		createUIHandler();
		createLVHandler();
		createDelBtnListener();
		createSolutionDialog();
		createViewHoverListener();
	}
	
	
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
    		ContextMenuInfo menuInfo) {
		//AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
		menu.setHeaderTitle("Menu");
		menu.add(Menu.NONE, 0, 0, "ReDo");
		mCurClickIndex = mListView.getPositionForView(v);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
	    //AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
	    int menuItemIndex = item.getItemId();
	    if(menuItemIndex == 0){
	    	RecordItem recItem = RecordItem.recordList.get((int)mCurClickIndex);
	    	//recItem.isDone = false;
	    	recItem.mProgress = 0;
	    	recItem.mLogPath = null;
	    	recItem.isDone = false;
	    	//ViewHolder holder = (ViewHolder)mListView.getItemAtPosition((int)mCurClickIndex);
	    	//holder.iv.setImageResource(android.R.drawable.ic_menu_help);
	    	actionAdapter.notifyDataSetChanged();
	    }
		return true;
    }
    
	@Override
	public void onHiddenChanged(boolean hidden) {
		super.onHiddenChanged(hidden);
		if (!hidden) {
			actionAdapter.notifyDataSetChanged();
		}
	}

	private class MyListViewAdapter extends BaseAdapter {

		private LayoutInflater inflater = null;

		public MyListViewAdapter(Context con) {
			inflater = (LayoutInflater) con
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			int count = RecordItem.getCount();
			return count;
		}

		@Override
		public Object getItem(int position) {
			return RecordItem.recordList.get(position).mViewHolder;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			RecordItem item;
			item = RecordItem.recordList.get(position);
			if (convertView == null) {
				holder = new ViewHolder();
				convertView = inflater.inflate(R.layout.record_list_item, null);
				holder.iv = (ImageView) (convertView.findViewById(R.id.statusIcon));
				holder.mBtn = (Button) (convertView
						.findViewById(R.id.btnActive));
				holder.pb = (ProgressBar) (convertView
						.findViewById(R.id.pbTask));
				holder.tv = (TextView) (convertView
						.findViewById(R.id.tvTaskDesc));
				holder.mBtn.setOnClickListener(mDelBtnClickListener);
				holder.iv.setOnTouchListener(mViewHolderListener);
				registerForContextMenu(convertView);
				
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			holder.tv.setText(item.pack());
			holder.pb.setProgress(item.mProgress);
			if(!item.isDone()){
				holder.iv.setImageResource(android.R.drawable.ic_menu_help);
			} else {
				holder.iv.setImageResource(android.R.drawable.ic_menu_edit);
			}
			item.mViewHolder = holder;
			convertView.setTag(holder);
			
			
			return convertView;
		}
	}

	private void createSolutionDialog() {
		mSolutionDialog = new Dialog(thisView.getContext());
		mSolutionDialog.setContentView(R.layout.solution_dialog);
		mSolutionDialog.setTitle("Solution");
		mSolutionDialog.setCancelable(false);
		mSolutionDialog.dismiss();
	}



	private void getParameter(String logTxt) {
		TuningParameter data = new TuningParameter();
		//Field[] fld = data.getClass().getDeclaredFields();

		for (int i = 0; i < TuningKeys.length; i++) {
			//String patternStr = fld[i].getName() + "\\s*=\\s*(\\d+)";
            String patternStr = TuningKeys[i] + "\\s*=\\s*(\\d+)";
	        Pattern pattern = Pattern.compile(patternStr);
			Matcher matcher = pattern.matcher(logTxt);
			if (matcher.find()) {
				try {
					//fld[i].setInt(data, Integer.parseInt(matcher.group(1)));
					//dialogTxtInput.append(fld[i].getName().trim() + " "	+ matcher.group(1).trim() + "\n");
					dialogTxtInput.append(TuningKeys[i].trim() + " "	+ matcher.group(1).trim() + "\n");
				} catch (Exception e) {
				}
			}
		}
		data = null;
	}

	TextView dialogTxtInput;

	private void createViewHoverListener() {
		dialogTxtInput = (TextView) mSolutionDialog
				.findViewById(R.id.solutionInput);
		dialogTxtInput.setClickable(true);
		mSolutionDialog.setCanceledOnTouchOutside(true);
		mViewHolderListener = new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				int index = mListView.getPositionForView(view);
				int resIndex;
				String logText;
				int what = event.getAction();

				switch (what) {
				case MotionEvent.ACTION_DOWN:
					RecordItem item = RecordItem.recordList.get(index);
					if (item.isDone()) {
						// String curPath =
						// "/storage/emulated/0/Android/data/com.mtk.offlinek/files/200_2_LTE_1_1_1_1_125_.txt";
						// logText = FileHandler.getFileText(curPath);
						logText = FileHandler.getFileText(item.mLogPath);
						dialogTxtInput.setText("");
						resIndex = logText.indexOf(RecordItem.ETT_KEY);
						getParameter(logText.substring(resIndex));
						mSolutionDialog.show();
					}
					break;
				}
				return false;
			}
		};
	}

	private void createDelBtnListener() {
		mDelBtnClickListener = new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				int delBtnIdx;
				delBtnIdx = mListView.getPositionForView(v);
				RecordItem.delRecord((int) delBtnIdx);
				actionAdapter.notifyDataSetChanged();
			}
		};
	}

	private void createLVHandler() {
		progressHD = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				//Bundle bundle = msg.getData();
				ViewHolder holder = (ViewHolder) mListView.getItemAtPosition(msg.arg2);
				Drawable draw_pic;
				switch (msg.what) {
				case MSG_RESULT_AVAILABLE:
					if(!((RecordItem)msg.obj).isDone()){
						holder.iv.setImageResource(android.R.drawable.ic_menu_help);
					} else {
						holder.iv.setImageResource(android.R.drawable.ic_menu_edit);
					}
					break;
				case MSG_SHOW_PROGRESS:
					holder.pb.setProgress(msg.arg1);
					break;
				case MSG_CHG_BTN_UI:
					if (msg.arg1 == 0) {
						holder.mBtn.setEnabled(false);
						draw_pic = getResources().getDrawable(android.R.drawable.ic_lock_lock);
					} else {
						holder.mBtn.setEnabled(true);
						draw_pic = getResources().getDrawable(android.R.drawable.ic_delete);
					}
					// android.R.drawable.ic_lock_lock);
					holder.mBtn.setCompoundDrawablesWithIntrinsicBounds(null, draw_pic, null, null);
					//holder.mBtn.refreshDrawableState();
					//holder.mBtn.postInvalidate();
					break;
				}
				
				mListView.setTag(holder);
				//actionAdapter.notifyDataSetChanged();
			}
		};
	}

	private void createUIHandler() {
		uiHandler = new Handler() {
			public void handleMessage(final Message msg) {
				Bundle bundle;
				switch (msg.what) {
				case MSG_SHOW_TEXT:
					bundle = msg.getData();
					// text.setText(bundle.getString("TEXT"));
					break;
				case MSG_DUMP_TOAST:
					bundle = msg.getData();
					Toast.makeText(thisView.getContext(),
							bundle.getString("TEXT"), Toast.LENGTH_LONG).show();
					break;
				case MSG_POP_DIALOG:
					AlertDialog.Builder dialog = new AlertDialog.Builder(
							thisView.getContext());
					dialog.setTitle("Permission Check Fail");
					bundle = msg.getData();
					dialog.setMessage("How to solve:\n1.Must be in ENG load\n2.adb shell chmod 777 "
							+ bundle.getString("TEXT"));
					dialog.setIcon(android.R.drawable.ic_dialog_alert);
					dialog.setCancelable(false);
					dialog.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog = null;
								}
							});
					dialog.show();
					break;
				default:
					break;
				}
			}
		};
	}
}
