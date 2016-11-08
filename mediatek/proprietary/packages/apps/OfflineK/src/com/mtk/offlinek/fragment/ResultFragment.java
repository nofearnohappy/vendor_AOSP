package com.mtk.offlinek.fragment;

import java.io.File;
import java.io.IOException;

import com.mtk.offlinek.FileHandler;
import com.mtk.offlinek.R;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ResultFragment extends Fragment {
	
	View thisView;
	Button mergeBtn;
	TextView zipLocation;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		thisView = inflater.inflate( R.layout.result_fragment, container, false); 
		mergeBtn =  (Button)thisView.findViewById(R.id.mergeLogBtn); 
		zipLocation = (TextView)thisView.findViewById(R.id.zipPath);
		mergeBtn.setOnClickListener(new Button.OnClickListener(){
			@Override
			public void onClick(View arg0) {
				File file = thisView.getContext().getExternalFilesDir(null);
				File []files = file.listFiles();
				String [] fileNames = new String[files.length];
				for(int i=0; i<files.length; i++){
					fileNames[i] = files[i].getAbsolutePath();
				}
				try {
					FileHandler.zip(fileNames, zipLocation.getText().toString());
				} catch (IOException e) {
					Toast.makeText(thisView.getContext(), "Zip Folder Error", Toast.LENGTH_LONG).show();
				}
				Toast.makeText(thisView.getContext(), "Zip File Done!!", Toast.LENGTH_LONG).show();
			}
        });
		return thisView;
	}
}
