package com.cmniu.drawingview;

import com.cmniu.niudrawingkit.R;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;

public class ColorFragment extends DialogFragment implements OnColorChangedListener{
	private ColorPicker colorPicker;
	private SVBar svBar;
	private OpacityBar opacityBar;
	public static ColorFragment getInstance(int id){
		ColorFragment colorDialog = new ColorFragment();
	    Bundle args = new Bundle();
	    args.putInt("id", id);
	    colorDialog.setArguments(args);
	    return colorDialog;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	ContextThemeWrapper context = new ContextThemeWrapper(getActivity(), android.R.style.Theme_Holo_Light_Dialog_NoActionBar);
    	LayoutInflater mInflater = LayoutInflater.from(getActivity());
    	View layout = mInflater.inflate(R.layout.color_dialog_layout, null);
    	colorPicker = (ColorPicker) layout.findViewById(R.id.picker);
    	colorPicker.setOnColorChangedListener(this);
    	
    	svBar = (SVBar) layout.findViewById(R.id.svbar);
    	opacityBar = (OpacityBar) layout.findViewById(R.id.opacitybar);
    	
    	colorPicker.addSVBar(svBar);
    	colorPicker.addOpacityBar(opacityBar);
    	
        return new AlertDialog.Builder(context)
                .setTitle(getArguments().getString("Color picker"))
                .setView(layout)
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
//                        	((MainActivity)getActivity()).getDrawingView().setPaintColor(colorPicker.getColor());
                        }
                    }
                )
                .setNegativeButton("Cancel", null).create();
    }
	
	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}
	
	@Override
	public void onColorChanged(int color) {
//		((MainActivity)getActivity()).getDrawingView().setPaintColor(color);
	}
	
	
}
