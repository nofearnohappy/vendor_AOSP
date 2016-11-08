package com.mtk.offlinek.fragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.mtk.offlinek.MainActivity;
import com.mtk.offlinek.R;
import com.mtk.offlinek.component.DeviceType;


import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class ConfigFragment extends Fragment {

	TextView text;
	Button addItemBtn;
	private View thisView;
	//private Activity mParentContext;
	Handler uiHandler;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		thisView  = inflater.inflate(R.layout.config_fragment, container, false);
		
		addItemBtn = (Button)thisView.findViewById(R.id.addItemBtn);
		addItemBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View v) {
				//ETTRunner fRunner = new ETTRunner(mParentContext, uiHandler);
				//fRunner.start();
				TextView frequencyText = (TextView)thisView.findViewById(R.id.mhzInput);
				Spinner port = (Spinner) thisView.findViewById(R.id.hostPortList);
				Spinner device = (Spinner) thisView.findViewById(R.id.deviceList);
				TextView cmdTimesText = (TextView)thisView.findViewById(R.id.cmdTimesInput);
				TextView rDataTimesText = (TextView)thisView.findViewById(R.id.rDataTimesInput);
				TextView wDataTimesText = (TextView)thisView.findViewById(R.id.wDataTimesInput);
				Spinner voltage = (Spinner) thisView.findViewById(R.id.spVoltage); 
				RecordItem item = new RecordItem(frequencyText.getText().toString(), port.getSelectedItem().toString()
						, device.getSelectedItem().toString(), cmdTimesText.getText().toString(), 
						rDataTimesText.getText().toString(), wDataTimesText.getText().toString(), 
						voltage.getSelectedItem().toString());
				RecordItem.addRecord(item);
			}  
		});
		/*uiHandler = new Handler(){
			public void handleMessage(final Message msg){
				Bundle bundle;
				switch(msg.what){
					case RecordFragment.MSG_SHOW_TEXT:
						bundle = msg.getData();
						//text.setText(bundle.getString("TEXT"));
						break;
					case RecordFragment.MSG_DUMP_TOAST:
						bundle = msg.getData();
						Toast.makeText(thisView.getContext(), bundle.getString("TEXT"), Toast.LENGTH_LONG).show();
						break;
					case RecordFragment.MSG_POP_DIALOG:
						AlertDialog.Builder dialog = new AlertDialog.Builder(thisView.getContext());
				        dialog.setTitle("Permission Check Fail");
				        bundle = msg.getData();
				        dialog.setMessage("How to solve:\n1.Must be in ENG load\n2.adb shell chmod 777 "+bundle.getString("TEXT"));
				        dialog.setIcon(android.R.drawable.ic_dialog_alert);
				        dialog.setCancelable(false);  
				        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {  
				            public void onClick(DialogInterface dialog, int which) {
				            	dialog = null;
				            }  
				        }); 
				        dialog.show();
						break;
					default:
						break;
				}
			}
		};*/
		createPortAdapter();
		createDeviceAdapter();
		createVoltageAdapter();
		/*new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				ETTRunner fRunner = new ETTRunner(thisView.getContext(), uiHandler);
				fRunner.start();
			}
			
		}).start();
		*/
		
		return thisView;
	}
	
	private void createPortAdapter(){
		/*ArrayList<Integer> portList = new ArrayList<Integer>();
        portList.add(0);
        portList.add(1);
        portList.add(2);
        portList.add(3);
		 */
		List<Integer> portList = MainActivity.hwConfig.getPortList();
		Spinner portSpinner = (Spinner) thisView.findViewById(R.id.hostPortList);
        //建立一個ArrayAdapter物件，並放置下拉選單的內容
        ArrayAdapter<Integer> portAdapter = new ArrayAdapter<Integer>(thisView.getContext(),
        	     android.R.layout.simple_spinner_item, portList);
        
        //設定下拉選單的樣式
        portAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        portSpinner.setAdapter(portAdapter);
	}
	
	private void createDeviceAdapter(){
		ArrayList<String> deviceList = new ArrayList<String>();
        //deviceList.add("LTE");
        //deviceList.add("WiFi");
		Map<DeviceType, Integer> map = MainActivity.hwConfig.getDeviceMap();
		Set<DeviceType> set = map.keySet();
		for(DeviceType data:set){
			if(data == DeviceType.LTE)
				deviceList.add(DeviceType.LTE.toString());
			else if(data == DeviceType.WIFI)
				deviceList.add(DeviceType.WIFI.toString());
		}
        Spinner spinner = (Spinner) thisView.findViewById(R.id.deviceList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(thisView.getContext(),
        	     android.R.layout.simple_spinner_item, deviceList);
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);        
	}
	
	private void createVoltageAdapter(){
		List<String> voltageList = new ArrayList<String>();
		List<String> hwVolList = MainActivity.hwConfig.getVoltageList();
		for(int i=0; i<hwVolList.size(); i++){
			Long longTxt = Long.parseLong(hwVolList.get(i));
			double dbData =  ((double)longTxt/1000000);
			voltageList.add(String.valueOf(dbData));
		}
		//voltageList.add("1.0175");
        Spinner spinner = (Spinner) thisView.findViewById(R.id.spVoltage);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(thisView.getContext(),
        	     android.R.layout.simple_spinner_item, voltageList);
        
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
	}
	
	/*@Override
	public void onAttach(Activity activity) {
		// TODO Auto-generated method stub
		super.onAttach(activity);
		mParentContext = activity;
	}*/
}
