package com.mtk.offlinek;



import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.mtk.offlinek.chip.GenericHW;
import com.mtk.offlinek.chip.MT6582HW;
import com.mtk.offlinek.chip.MT6592HW;
import com.mtk.offlinek.chip.MT6595HW;
import com.mtk.offlinek.chip.MT6752HW;
import com.mtk.offlinek.chip.MT6795HW;
import com.mtk.offlinek.chip.MT6735HW;
import com.mtk.offlinek.fragment.ConfigFragment;
import com.mtk.offlinek.fragment.RecordFragment;
import com.mtk.offlinek.fragment.RecordItem;
import com.mtk.offlinek.fragment.ResultFragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TabHost;

public class MainActivity extends FragmentActivity {
	public static GenericHW hwConfig;
	
	static class TabHolder{
		String mLabel = null;
		int mIconId = 0;
		Class<?> mFrag;
		TabHolder(String label, int iconId, Class<?> frag){
			mLabel = label;
			mIconId = iconId;
			mFrag = frag;
		}
	}	
	private TabHost tHost;
	List<TabHolder> tabList;
	
	
	Button button01;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//CmdAgent.doCommand("aee -m 4");
		try{
            CmdAgent.doCommand("aee -m 4");
        }catch(Exception ex){}
		setContentView(R.layout.activity_main);
		String chipID = "mt6595";
		chipID = CmdAgent.doCommand("getprop ro.hardware").trim();
		//text = (TextView)thisView.findViewById(R.id.notifyText);
		if(chipID.contains("6582")){
			hwConfig = new MT6582HW();
		} else if(chipID.contains("6592")){
			hwConfig = new MT6592HW();
		} else if(chipID.contains("6595")){
			hwConfig = new MT6595HW();
		} else if(chipID.contains("6795")){
			hwConfig = new MT6795HW();
	    	} else if(chipID.contains("6752")){
			hwConfig = new MT6752HW();
	    	} else if(chipID.contains("6735")){ //D-1 & D-3 are all with ro.hardware = mt6735
			hwConfig = new MT6735HW();
		} else {	// Unsupport version
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
	        dialog.setTitle("Unsupport Chip");
	        dialog.setMessage("Current support chip:mt6582/mt6592/mt6595/mt6795/mt6752/mt6735");
	        dialog.show();
	        return;
		}


		tHost = (TabHost)findViewById(android.R.id.tabhost);
		tHost.setup();

        tabList = new ArrayList<TabHolder>();
        tabList.add(new TabHolder("config", android.R.drawable.ic_menu_manage, ConfigFragment.class));
        tabList.add(new TabHolder("record", android.R.drawable.ic_menu_slideshow, RecordFragment.class));
        tabList.add(new TabHolder("pref", android.R.drawable.ic_menu_my_calendar, ResultFragment.class));
        TabHost.OnTabChangeListener tabChangeListener = new TabHost.OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				android.support.v4.app.FragmentManager fm =  getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				for(int i=0; i<tabList.size(); i++){
					Fragment thisFragment = fm.findFragmentByTag(tabList.get(i).mLabel);
					if(thisFragment != null)
						//ft.detach(thisFragment);
						ft.hide(thisFragment);
					if(tabId.equalsIgnoreCase(tabList.get(i).mLabel)){						
						if(thisFragment==null){		
							try {
								thisFragment = (Fragment) tabList.get(i).mFrag.newInstance();
							} catch (InstantiationException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							}
							ft.add(R.id.realtabcontent, thisFragment, tabList.get(i).mLabel);						
						}else{
							//ft.attach(thisFragment);
							ft.show(thisFragment);
						}
					}
				}				
				ft.commit();
			}
		};
		tHost.setOnTabChangedListener(tabChangeListener);
        
		/** Defining tab builder for Andriod tab */
		for(int i=0; i<tabList.size(); i++){
			addTabFragment(tabList.get(i).mLabel, tabList.get(i).mIconId);	
		}        
		NativeLoader.getInstance();
        /*button01 = (Button)findViewById(R.id.buttonaaa1);
        button01.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				new Thread(new Runnable(){
					
					@Override
					public void run() {
						getHString();
					}
				}).start();;
			}  
		});*/
		preloadRecord();
	}
	
	private void preloadRecord(){
		File fld = this.getExternalFilesDir(null);
		File []files = fld.listFiles();
		RecordItem item;
		for(int i=0; i<files.length; i++){
			item = RecordItem.getObjFromPath(files[i].getName());
			if(item != null && FileHandler.isKeywordInFile(files[i].getAbsolutePath(), RecordItem.ETT_KEY)){
				item.isDone = true;
				item.mProgress = 100;
				item.mLogPath = files[i].getAbsolutePath();
				RecordItem.addRecord(item);
			}
		}
	}
	
	private void addTabFragment(String label, int drawableId){
    	TabHost.TabSpec tSpec = tHost.newTabSpec(label);
        tSpec.setIndicator(label.substring(0, 1).toUpperCase(Locale.ENGLISH)+label.substring(1),
        		getResources().getDrawable(drawableId));        
        tSpec.setContent(new DummyTabContent(getBaseContext()));
        tHost.addTab(tSpec);
    }
	
	
}
