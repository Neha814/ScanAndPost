/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.scanandpost.client.android;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.scanandpost.client.android.Fragments.NavSyncFragment;
import com.scanandpost.client.android.R;
import com.scanandpost.BarcodeFormat;
import com.scanandpost.Result;
import com.scanandpost.ResultMetadataType;
import com.scanandpost.ResultPoint;
import com.scanandpost.client.android.camera.CameraManager;
import com.scanandpost.client.android.history.HistoryManager;
import com.scanandpost.client.android.result.ResultButtonListener;
import com.scanandpost.client.android.result.ResultHandler;
import com.scanandpost.client.android.result.ResultHandlerFactory;
import com.scanandpost.client.android.result.supplement.SupplementalInfoRetriever;
import com.scanandpost.client.android.share.ShareActivity;
import com.scanandpost.client.database.DatabaseHandler;
import com.scanandpost.client.functions.Constants;
import com.scanandpost.client.functions.NetConnection;
import com.scanandpost.client.functions.StringUtils;
import com.scanandpost.client.model.BarcodeData;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.ClipboardManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * The barcode reader activity itself. This is loosely based on the CameraPreview
 * example included in the Android SDK.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

	public static JSONArray Scan_Results_Store = new JSONArray();
	public int Results_scans = 0; 
  private static final String TAG = CaptureActivity.class.getSimpleName();

  private static final int SHARE_ID = Menu.FIRST;
  private static final int HISTORY_ID = Menu.FIRST + 1;
  private static final int SETTINGS_ID = Menu.FIRST + 2;
  private static final int HELP_ID = Menu.FIRST + 3;
  private static final int ABOUT_ID = Menu.FIRST + 4;

  private static final long INTENT_RESULT_DURATION = 1500L;
  private static final long BULK_MODE_SCAN_DELAY_MS = 1000L;

  private static final String PACKAGE_NAME = "com.scanandpost.client.android";
  private static final String PRODUCT_SEARCH_URL_PREFIX = "http://www.google";
  private static final String PRODUCT_SEARCH_URL_SUFFIX = "/m/products/scan";
  private static final String ZXING_URL = "http://zxing.appspot.com/scan";
  private static final String RETURN_CODE_PLACEHOLDER = "{CODE}";
  private static final String RETURN_URL_PARAM = "ret";

  private static final Set<ResultMetadataType> DISPLAYABLE_METADATA_TYPES;
  static {
    DISPLAYABLE_METADATA_TYPES = new HashSet<ResultMetadataType>(5);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ISSUE_NUMBER);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.SUGGESTED_PRICE);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.ERROR_CORRECTION_LEVEL);
    DISPLAYABLE_METADATA_TYPES.add(ResultMetadataType.POSSIBLE_COUNTRY);
  }

  private enum Source {
    NATIVE_APP_INTENT,
    PRODUCT_SEARCH_LINK,
    ZXING_LINK,
    NONE
  }
  	private boolean isLighOn = false;
	private CameraManager camera;
	private Button button;
	private Button historyBtn;
    private Button sync_bt;
	private Button settingBtn;
	private Button flashBtn;
  private CaptureActivityHandler handler;
  private ViewfinderView viewfinderView;
  private TextView statusView;
  private View resultView;
  private Result lastResult;
  private boolean hasSurface;
  private boolean copyToClipboard;
  private Source source;
  private String sourceUrl;
  private String returnUrlTemplate;
  private Vector<BarcodeFormat> decodeFormats;
  private String characterSet;
  private String versionName;
  private HistoryManager historyManager;
  private InactivityTimer inactivityTimer;
  private BeepManager beepManager;



  SharedPreferences sp;

  JSONArray array = new JSONArray();

  Boolean isConnected;
  private AsyncHttpClient client;
  private ProgressDialog dialog;

  DatabaseHandler db ;

  String WAREOHOUSE_TABLE = "warehouse_table";
  String LOADTRUCK_TABLE = "loadtruck_table";

  String TABLENAME="" ;

  MediaPlayer mp;


  private final DialogInterface.OnClickListener aboutListener =
      new DialogInterface.OnClickListener() {
    public void onClick(DialogInterface dialogInterface, int i) {
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.zxing_url)));
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      startActivity(intent);
    }
  };

  ViewfinderView getViewfinderView() {
    return viewfinderView;
  }

  public Handler getHandler() {
    return handler;
  }
  
  CameraManager getCameraManager() {
	    return CameraManager.get();
	  }

  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);

    Window window = getWindow();
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.capture);

    isConnected = NetConnection.checkInternetConnectionn(getApplicationContext());
    sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    db = new DatabaseHandler(this);

    if(Constants.COMING_FROM.equalsIgnoreCase("WAREHOUSE")){
      TABLENAME = WAREOHOUSE_TABLE;
    }else if(Constants.COMING_FROM.equalsIgnoreCase("LOADTRUCK")){
      TABLENAME = LOADTRUCK_TABLE;
    }

    mp = MediaPlayer.create(CaptureActivity.this,R.raw.beep_sound);

    client = new AsyncHttpClient();
    client.setTimeout(40 * 1000);
    dialog = new ProgressDialog(CaptureActivity.this);
    dialog.setMessage("Loading..");
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);

    CameraManager.init(getApplication());
    //cameraManager = new CameraManager(getApplication());
    viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
    resultView = findViewById(R.id.result_view);
    statusView = (TextView) findViewById(R.id.status_view);
    button = (Button) findViewById(R.id.scan_done_btn);
    sync_bt = (Button) findViewById(R.id.sync_bt);
    settingBtn = (Button) findViewById(R.id.settingBtnId);
    flashBtn = (Button) findViewById(R.id.flashLightBtnId);
    handler = null;
    lastResult = null;
    hasSurface = false;
    historyManager = new HistoryManager(this);
    historyManager.trimHistory();
    inactivityTimer = new InactivityTimer(this);
    beepManager = new BeepManager(this);

    // showHelpOnFirstLaunch();
    

  }

  @Override
  protected void onResume() {
    super.onResume();
    resetStatusView();

    SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
    SurfaceHolder surfaceHolder = surfaceView.getHolder();
    if (hasSurface) {
      // The activity was paused but not stopped, so the surface still exists. Therefore
      // surfaceCreated() won't be called, so init the camera here.
      initCamera(surfaceHolder);
    } else {
      // Install the callback and wait for surfaceCreated() to init the camera.
      surfaceHolder.addCallback(this);
      surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    Intent intent = getIntent();
    String action = intent == null ? null : intent.getAction();
    String dataString = null;//intent == null ? null : intent.getDataString();
    if (intent != null && action != null) {
      if (action.equals(Intents.Scan.ACTION)) {
        // Scan the formats the intent requested, and return the result to the calling activity.
        source = Source.NONE;//Source.NATIVE_APP_INTENT;
        decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
        if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
          int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
          int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
          if (width > 0 && height > 0) {
            CameraManager.get().setManualFramingRect(width, height);
        	  //cameraManager.setManualFramingRect(width, height);
          }
        }
      } else if (dataString != null && dataString.contains(PRODUCT_SEARCH_URL_PREFIX) &&
          dataString.contains(PRODUCT_SEARCH_URL_SUFFIX)) {
        // Scan only products and send the result to mobile Product Search.
        source = Source.NONE;//Source.PRODUCT_SEARCH_LINK;
        sourceUrl = dataString;
        decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;
      } else if (dataString != null && dataString.startsWith(ZXING_URL)) {
        // Scan formats requested in query string (all formats if none specified).
        // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
        source = Source.NONE;//Source.ZXING_LINK;
        sourceUrl = dataString;
        Uri inputUri = Uri.parse(sourceUrl);
        returnUrlTemplate = inputUri.getQueryParameter(RETURN_URL_PARAM);
        decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
      } else {
        // Scan all formats and handle the results ourselves (launched from Home).
    	  Log.d("Error", getIntent().toString());
    	  sourceUrl = getIntent().getDataString();
    	  if(sourceUrl!=null)
    	  {
              Uri inputUri = Uri.parse(sourceUrl);
              returnUrlTemplate = inputUri.getQueryParameter(RETURN_URL_PARAM);
              Log.d("URL :", returnUrlTemplate);
    	  }else{
    		  returnUrlTemplate = "http://ship.als-otg.com/androidscans2.cfm?code={CODE}";
    	  }
        source = Source.NONE;
        decodeFormats = null;
      }
      characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
    } else {
      source = Source.NONE;
      decodeFormats = null;
      characterSet = null;
    }

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    copyToClipboard = prefs.getBoolean(PreferencesActivity.KEY_COPY_TO_CLIPBOARD, true)
        && (intent == null || intent.getBooleanExtra(Intents.Scan.SAVE_HISTORY, true));
    
    button.setOnClickListener(new Button.OnClickListener() {

		@Override
		public void onClick(View arg0) {
          finish();
	/*		if(Scan_Results_Store.length()<1)
			{
				throwAlert();
			}else{
			String ScannedText = "";
			for(int i =0; Scan_Results_Store.length() > i; i++)
		  	{
		  		JSONArray TheArray = Scan_Results_Store;
		  		try {
					String Re = TheArray.getString(i);
					if((Scan_Results_Store.length() - 1)!= i)
					{
					ScannedText = (ScannedText == "" ? "" : ScannedText)+Re+",";
					}else{
						ScannedText = (ScannedText == "" ? "" : ScannedText)+Re;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		  	}

			Scan_Results_Store = new JSONArray(); // Reset already scanned barcodes
			Message message = Message.obtain(handler, R.id.launch_product_query);
		      message.obj = returnUrlTemplate.replace(RETURN_CODE_PLACEHOLDER,
		    		  ScannedText);
		      handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
			}*/
		}});
    
    /*historyBtn.setOnClickListener(new Button.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			AlertDialog historyAlert = historyManager.buildAlert();
	        historyAlert.show();
		}
    });*/

    sync_bt.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        DatabaseHandler db = new DatabaseHandler(getApplicationContext());
        List<BarcodeData> contacts = db.getAllContacts(TABLENAME);
        if(contacts.size()<1){
          Toast.makeText(getApplicationContext(), "No data available to sync.", Toast.LENGTH_SHORT).show();
        } else {
          putDataInAnArray();

        }
      }
    });

    
    settingBtn.setOnClickListener(new Button.OnClickListener() {

		@Override
		public void onClick(View arg0) {
			switchToSettings();
		}
    });
    
    
    flashBtn.setOnClickListener(new Button.OnClickListener() {
    	@Override
		public void onClick(View arg0) {
			if(!isLighOn)
			{
				isLighOn = true;
				flashBtn.setText("Torch Off");
				CameraManager.get().TorchOn();
			}else{
				isLighOn = false;
				flashBtn.setText("Torch On");
				CameraManager.get().TorchOff();
			}
		}
    });
    
    beepManager.updatePrefs();

    inactivityTimer.onResume();
  }

  private void putDataInAnArray() {
    DatabaseHandler db = new DatabaseHandler(getApplicationContext());
    List<BarcodeData> contacts = db.getAllContacts(TABLENAME);
    for(int i=0;i<contacts.size();i++){
      JSONObject object = new JSONObject();
      try{
        //{Barcode:'1234',DriverID:'99',Modified:'02-06-2016',ActivityTypeID:'37'}

        Calendar c = Calendar.getInstance();
        System.out.println("Current time => " + c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = df.format(c.getTime());

        object.put("Barcode", contacts.get(i).getBarcode());
        object.put("DriverID", contacts.get(i).getDriverId());
        object.put("Modified", contacts.get(i).getModified());
        object.put("ActivityTypeID",contacts.get(i).getActivityTypeId());
        if(TABLENAME.equalsIgnoreCase(LOADTRUCK_TABLE)){
                    /*[
                    { Barcode:'222',DriverID:'1234',Modified:'02-06-2016',ActivityTypeID:'37',TrackingID:'1',
                            CompanyID:'99',Notes:'ghgh',ContainerID:'5',Route:'hg',StopNum:'hh'}
                    ]*/

          object.put("TrackingID","1");
          object.put("CompanyID","99");
          object.put("Notes","Load truck");
          object.put("ContainerID","5");
          object.put("Route","abc");
          object.put("StopNum","xyz");
        }
      } catch(Exception e){
        e.printStackTrace();
      }
      array.put(object);
    }

    if(isConnected) {
      if(TABLENAME.equalsIgnoreCase(WAREOHOUSE_TABLE)){
        submitWarehouseScannedData();
      } else if(TABLENAME.equalsIgnoreCase(LOADTRUCK_TABLE)) {
        submitLoadTruckScannedData();
      }

    }else {
      StringUtils.showDialog("No internet connection.Please try again", getApplicationContext());
    }
  }


  public void switchToSettings()
  {
	  
	 /* Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.setClassName(this, PreferencesActivity.class.getName());
      Log.d("Class name -:", PreferencesActivity.class.getName());
      startActivity(intent);*/

  }
  
  public void throwAlert()
  {
	  AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				this);

			// set title
			alertDialogBuilder.setTitle("Warning");

			// set dialog message
			alertDialogBuilder
				.setMessage("There is no barcode scanned yet! Please Scan some barcode to proceed")
				.setCancelable(false)
				.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    // if this button is clicked, just close
                    // the dialog box and do nothing
                    dialog.cancel();
                  }
                });

				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();

				// show it
				alertDialog.show();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (handler != null) {
      handler.quitSynchronously();
      handler = null;
    }
    inactivityTimer.onPause();
    CameraManager.get().closeDriver();
    //cameraManager.closeDriver();
  }

  @Override
  protected void onDestroy() {
    inactivityTimer.shutdown();
    super.onDestroy();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (source == Source.NATIVE_APP_INTENT) {
        setResult(RESULT_CANCELED);
        finish();
        return true;
      } else if ((source == Source.NONE || source == Source.ZXING_LINK) && lastResult != null) {
        resetStatusView();
        if (handler != null) {
          handler.sendEmptyMessage(R.id.restart_preview);
        }
        return true;
      }
    } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
      // Handle these events so they don't launch the Camera app
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }
  
  
  

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
	  /*super.onCreateOptionsMenu(menu);
    menu.add(0, SHARE_ID, 0, R.string.menu_share)
        .setIcon(android.R.drawable.ic_menu_share);
    menu.add(0, HISTORY_ID, 0, R.string.menu_history)
        .setIcon(android.R.drawable.ic_menu_recent_history);
    menu.add(0, SETTINGS_ID, 0, R.string.menu_settings)
        .setIcon(android.R.drawable.ic_menu_preferences);
    menu.add(0, HELP_ID, 0, R.string.menu_help)
        .setIcon(android.R.drawable.ic_menu_help);
    menu.add(0, ABOUT_ID, 0, R.string.menu_about)
        .setIcon(android.R.drawable.ic_menu_info_details);*/
    return true;
  }

  // Don't display the share menu item if the result overlay is showing.
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    super.onPrepareOptionsMenu(menu);
    menu.findItem(SHARE_ID).setVisible(lastResult == null);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case SHARE_ID: {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setClassName(this, ShareActivity.class.getName());
        startActivity(intent);
        break;
      }
      case HISTORY_ID: {
        AlertDialog historyAlert = historyManager.buildAlert();
        historyAlert.show();
        break;
      }
      case SETTINGS_ID: {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setClassName(this, PreferencesActivity.class.getName());
        Log.d("Class name -:", PreferencesActivity.class.getName());
        startActivity(intent);
        break;
      }
      case HELP_ID: {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.setClassName(this, HelpActivity.class.getName());
        startActivity(intent);
        break;
      }
      case ABOUT_ID:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.title_about) + versionName);
        builder.setMessage(getString(R.string.msg_about) + "\n\n" + getString(R.string.zxing_url));
        builder.setIcon(R.drawable.launcher_icon);
        builder.setPositiveButton(R.string.button_open_browser, aboutListener);
        builder.setNegativeButton(R.string.button_cancel, null);
        builder.show();
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  public void surfaceCreated(SurfaceHolder holder) {
    if (!hasSurface) {
      hasSurface = true;
      initCamera(holder);
    }
  }

  public void surfaceDestroyed(SurfaceHolder holder) {
    hasSurface = false;
  }

  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  /**
   * A valid barcode has been found, so give an indication of success and show the results.
   *
   * @param rawResult The contents of the barcode.
   * @param barcode   A greyscale bitmap of the camera data which was decoded.
   */
  public void handleDecode(Result rawResult, Bitmap barcode) {
    inactivityTimer.onActivity();
    lastResult = rawResult;
    ResultHandler resultHandler = ResultHandlerFactory.makeResultHandler(this, rawResult);
    historyManager.addHistoryItem(rawResult, resultHandler);

    if (barcode == null) {
      // This is from history -- no saved barcode
      handleDecodeInternally(rawResult, resultHandler, null);
    } else {
     // beepManager.playBeepSoundAndVibrate();
      mp.start();
      drawResultPoints(barcode, rawResult);
      switch (source) {
        case NATIVE_APP_INTENT:
        case PRODUCT_SEARCH_LINK:
          handleDecodeExternally(rawResult, resultHandler, barcode);
          break;
        case ZXING_LINK:
          if (returnUrlTemplate == null){
            handleDecodeInternally(rawResult, resultHandler, barcode);
          } else {
            handleDecodeExternally(rawResult, resultHandler, barcode);
          }
          break;
        case NONE:
          SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
          boolean IWorks = true;
          if (IWorks/*prefs.getBoolean(PreferencesActivity.KEY_BULK_MODE, false)*/) {
        	  Results_scans++;
        		  Scan_Results_Store.put(rawResult.toString());
        	  Log.d("I work", Scan_Results_Store.toString());
            Toast.makeText(this, rawResult.toString(), Toast.LENGTH_SHORT).show();
            putDataInDb(rawResult.toString());
            // Wait a moment or else it will scan the same barcode continuously about 3 times
            if (handler != null) {
              handler.sendEmptyMessageDelayed(R.id.restart_preview, BULK_MODE_SCAN_DELAY_MS);
            }
            resetStatusView();
          } else {
        	  Log.d("I work", "FALSE");
            handleDecodeInternally(rawResult, resultHandler, barcode);
          }
          break;
      }
    }
  }

  private void putDataInDb(String symData) {
    Calendar c = Calendar.getInstance();
    System.out.println("Current time => " + c.getTime());
    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    String formattedDate = df.format(c.getTime());

    db.addContact(new BarcodeData(symData, sp.getString("client_id", ""), formattedDate, "37"),TABLENAME);
  }

  /**
   * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
   *
   * @param barcode   A bitmap of the captured image.
   * @param rawResult The decoded results which contains the points to draw.
   */
  private void drawResultPoints(Bitmap barcode, Result rawResult) {
    ResultPoint[] points = rawResult.getResultPoints();
    if (points != null && points.length > 0) {
      Canvas canvas = new Canvas(barcode);
      Paint paint = new Paint();
      paint.setColor(getResources().getColor(R.color.result_image_border));
      paint.setStrokeWidth(3.0f);
      paint.setStyle(Paint.Style.STROKE);
      Rect border = new Rect(2, 2, barcode.getWidth() - 2, barcode.getHeight() - 2);
      canvas.drawRect(border, paint);

      paint.setColor(getResources().getColor(R.color.result_points));
      if (points.length == 2) {
        paint.setStrokeWidth(4.0f);
        drawLine(canvas, paint, points[0], points[1]);
      } else if (points.length == 4 &&
                 (rawResult.getBarcodeFormat().equals(BarcodeFormat.UPC_A) ||
                  rawResult.getBarcodeFormat().equals(BarcodeFormat.EAN_13))) {
        // Hacky special case -- draw two lines, for the barcode and metadata
        drawLine(canvas, paint, points[0], points[1]);
        drawLine(canvas, paint, points[2], points[3]);
      } else {
        paint.setStrokeWidth(10.0f);
        for (ResultPoint point : points) {
          canvas.drawPoint(point.getX(), point.getY(), paint);
        }
      }
    }
  }

  private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b) {
    canvas.drawLine(a.getX(), a.getY(), b.getX(), b.getY(), paint);
  }

  // Put up our own UI for how to handle the decoded contents.
  private void handleDecodeInternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {
    statusView.setVisibility(View.GONE);
    viewfinderView.setVisibility(View.GONE);
    resultView.setVisibility(View.VISIBLE);

    ImageView barcodeImageView = (ImageView) findViewById(R.id.barcode_image_view);
    if (barcode == null) {
      barcodeImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),
          R.drawable.launcher_icon));
    } else {
      barcodeImageView.setImageBitmap(barcode);
    }

    TextView formatTextView = (TextView) findViewById(R.id.format_text_view);
    formatTextView.setText(rawResult.getBarcodeFormat().toString());

    TextView typeTextView = (TextView) findViewById(R.id.type_text_view);
    typeTextView.setText(resultHandler.getType().toString());

    DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
    String formattedTime = formatter.format(new Date(rawResult.getTimestamp()));
    TextView timeTextView = (TextView) findViewById(R.id.time_text_view);
    timeTextView.setText(formattedTime);


    TextView metaTextView = (TextView) findViewById(R.id.meta_text_view);
    View metaTextViewLabel = findViewById(R.id.meta_text_view_label);
    metaTextView.setVisibility(View.GONE);
    metaTextViewLabel.setVisibility(View.GONE);
    Map<ResultMetadataType,Object> metadata =
        (Map<ResultMetadataType,Object>) rawResult.getResultMetadata();
    if (metadata != null) {
      StringBuilder metadataText = new StringBuilder(20);
      for (Map.Entry<ResultMetadataType,Object> entry : metadata.entrySet()) {
        if (DISPLAYABLE_METADATA_TYPES.contains(entry.getKey())) {
          metadataText.append(entry.getValue()).append('\n');
        }
      }
      if (metadataText.length() > 0) {
        metadataText.setLength(metadataText.length() - 1);
        metaTextView.setText(metadataText);
        metaTextView.setVisibility(View.VISIBLE);
        metaTextViewLabel.setVisibility(View.VISIBLE);
      }
    }

    TextView contentsTextView = (TextView) findViewById(R.id.contents_text_view);
    CharSequence displayContents = resultHandler.getDisplayContents();
    contentsTextView.setText(displayContents);
    // Crudely scale betweeen 22 and 32 -- bigger font for shorter text
    int scaledSize = Math.max(22, 32 - displayContents.length() / 4);
    contentsTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, scaledSize);

    TextView supplementTextView = (TextView) findViewById(R.id.contents_supplement_text_view);
    supplementTextView.setText("");
    supplementTextView.setOnClickListener(null);
    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
        PreferencesActivity.KEY_SUPPLEMENTAL, true)) {
      SupplementalInfoRetriever.maybeInvokeRetrieval(supplementTextView, resultHandler.getResult(),
          handler, this);
    }

    int buttonCount = resultHandler.getButtonCount();
    ViewGroup buttonView = (ViewGroup) findViewById(R.id.result_button_view);
    buttonView.requestFocus();
    for (int x = 0; x < ResultHandler.MAX_BUTTON_COUNT; x++) {
      TextView button = (TextView) buttonView.getChildAt(x);
      if (x < buttonCount) {
        button.setVisibility(View.VISIBLE);
        button.setText(resultHandler.getButtonText(x));
        button.setOnClickListener(new ResultButtonListener(resultHandler, x));
      } else {
        button.setVisibility(View.GONE);
      }
    }

    if (copyToClipboard && !resultHandler.areContentsSecure()) {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      clipboard.setText(displayContents);
    }
  }

  // Briefly show the contents of the barcode, then handle the result outside Barcode Scanner.
  private void handleDecodeExternally(Result rawResult, ResultHandler resultHandler, Bitmap barcode) {
    viewfinderView.drawResultBitmap(barcode);

    // Since this message will only be shown for a second, just tell the user what kind of
    // barcode was found (e.g. contact info) rather than the full contents, which they won't
    // have time to read.
    statusView.setText(getString(resultHandler.getDisplayTitle()));

    if (copyToClipboard && !resultHandler.areContentsSecure()) {
      ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      clipboard.setText(resultHandler.getDisplayContents());
    }

    if (source == Source.NATIVE_APP_INTENT) {
      // Hand back whatever action they requested - this can be changed to Intents.Scan.ACTION when
      // the deprecated intent is retired.
      Intent intent = new Intent(getIntent().getAction());
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
      intent.putExtra(Intents.Scan.RESULT, rawResult.toString());
      intent.putExtra(Intents.Scan.RESULT_FORMAT, rawResult.getBarcodeFormat().toString());
      byte[] rawBytes = rawResult.getRawBytes();
      if (rawBytes != null && rawBytes.length > 0) {
        intent.putExtra(Intents.Scan.RESULT_BYTES, rawBytes);
      }
      Message message = Message.obtain(handler, R.id.return_scan_result);
      message.obj = intent;
      handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
    } else if (source == Source.PRODUCT_SEARCH_LINK) {
      // Reformulate the URL which triggered us into a query, so that the request goes to the same
      // TLD as the scan URL.
      Message message = Message.obtain(handler, R.id.launch_product_query);
      int end = sourceUrl.lastIndexOf("/scan");
      message.obj = sourceUrl.substring(0, end) + "?q=" +
          resultHandler.getDisplayContents().toString() + "&source=zxing";
      handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
    } else if (source == Source.ZXING_LINK) {
      // Replace each occurrence of RETURN_CODE_PLACEHOLDER in the returnUrlTemplate
      // with the scanned code. This allows both queries and REST-style URLs to work.
      Message message = Message.obtain(handler, R.id.launch_product_query);
      message.obj = returnUrlTemplate.replace(RETURN_CODE_PLACEHOLDER,
          resultHandler.getDisplayContents().toString());
      handler.sendMessageDelayed(message, INTENT_RESULT_DURATION);
    }
  }

  /**
   * We want the help screen to be shown automatically the first time a new version of the app is
   * run. The easiest way to do this is to check android:versionCode from the manifest, and compare
   * it to a value stored as a preference.
   */
  private boolean showHelpOnFirstLaunch() {
    try {
      PackageInfo info = getPackageManager().getPackageInfo(PACKAGE_NAME, 0);
      int currentVersion = info.versionCode;
      // Since we're paying to talk to the PackageManager anyway, it makes sense to cache the app
      // version name here for display in the about box later.
      this.versionName = info.versionName;
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
      int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
      if (currentVersion > lastVersion) {
        prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
        Intent intent = new Intent(this, HelpActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        // Show the default page on a clean install, and the what's new page on an upgrade.
        String page = lastVersion == 0 ? HelpActivity.DEFAULT_PAGE : HelpActivity.WHATS_NEW_PAGE;
        intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, page);
        startActivity(intent);
        return true;
      }
    } catch (PackageManager.NameNotFoundException e) {
      Log.w(TAG, e);
    }
    return false;
  }

  private void initCamera(SurfaceHolder surfaceHolder) {
	  if (surfaceHolder == null) {
	      throw new IllegalStateException("No SurfaceHolder provided");
	    }
    try {
      CameraManager.get().openDriver(surfaceHolder);
      // Creating the handler starts the preview, which can also throw a RuntimeException.
      if (handler == null) {
        handler = new CaptureActivityHandler(this, decodeFormats, characterSet);
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
      displayFrameworkBugMessageAndExit();
    } catch (RuntimeException e) {
      // Barcode Scanner has seen crashes in the wild of this variety:
      // java.?lang.?RuntimeException: Fail to connect to camera service
      Log.w(TAG, "Unexpected error initializating camera", e);
      displayFrameworkBugMessageAndExit();
    }
  }

  private void displayFrameworkBugMessageAndExit() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.app_name));
    builder.setMessage(getString(R.string.msg_camera_framework_bug));
    builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
    builder.setOnCancelListener(new FinishListener(this));
    builder.show();
  }

  private void resetStatusView() {
    resultView.setVisibility(View.GONE);
    statusView.setText(R.string.msg_default_status);
    statusView.setVisibility(View.VISIBLE);
    viewfinderView.setVisibility(View.VISIBLE);
    lastResult = null;
  }

  public void drawViewfinder() {
    viewfinderView.drawViewfinder();
  }

  //*************************** API ******************************************

  private void submitWarehouseScannedData() {

    RequestParams params = new RequestParams();
    params.put("Post", array.toString());
    //   client.addHeader("Content-Type","application/json");

    Log.e("parameters", params.toString());

    client.post(getApplicationContext(), "http://ship2.als-otg.com/api/AddSortingFacilityBarcodes3/", params, new JsonHttpResponseHandler() {

      @Override
      public void onStart() {
        super.onStart();
        dialog.show();
      }

      @Override
      public void onFinish() {
        super.onFinish();
        dialog.dismiss();
      }

      @Override
      public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        super.onSuccess(statusCode, headers, response);
        try {
          Log.e(TAG, "" + response);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        Log.e(TAG, responseString + "/" + statusCode);
        // idhr aaa rha h  wait
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }

        db.deleteWholeData(TABLENAME);
        showDialog("Data synced successfully.", CaptureActivity.this);

      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        Log.e(TAG, "/" + statusCode);
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
        Log.e(TAG, "/" + statusCode);
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }
      }
    });
  }

  //*************************** API ******************************************

  private void submitLoadTruckScannedData() {


    RequestParams params = new RequestParams();
    params.put("Post", array.toString());
    //   client.addHeader("Content-Type","application/json");

    Log.e("parameters", params.toString());

    client.post(getApplicationContext(), "http://ship2.als-otg.com/api/LoadTruckBarcodes3/", params, new JsonHttpResponseHandler() {

      @Override
      public void onStart() {
        super.onStart();
        dialog.show();
      }

      @Override
      public void onFinish() {
        super.onFinish();
        dialog.dismiss();
      }

      @Override
      public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        super.onSuccess(statusCode, headers, response);
        try {
          Log.e(TAG, ""+response);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        Log.e(TAG, responseString + "/" + statusCode);
        // idhr aaa rha h  wait
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }

        db.deleteWholeData(TABLENAME);
        showDialog("Data synced successfully.", CaptureActivity.this);

      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        Log.e(TAG, "/" + statusCode);
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }
      }

      @Override
      public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
        Log.e(TAG, "/" + statusCode);
        if (headers != null && headers.length > 0) {
          for (int i = 0; i < headers.length; i++)
            Log.e("here", headers[i].getName() + "//" + headers[i].getValue());
        }
      }
    });
  }

  public void showDialog(String msg, Context context) {
    try {
      AlertDialog alertDialog = new AlertDialog.Builder(
              context).create();


      // Setting Dialog Message
      alertDialog.setMessage(msg);

      // Setting Icon to Dialog
      //	alertDialog.setIcon(R.drawable.browse);

      // Setting OK Button
      alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
          // Write your code here to execute after dialog closed
          dialog.cancel();
          finish();
        }
      });

      // Showing Alert Message
      alertDialog.show();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
