package com.csce.tamu.eyebell;

import android.app.Activity;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessCollection;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessException;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.files.BackendlessFile;
import com.backendless.files.FileInfo;
import com.backendless.persistence.BackendlessDataQuery;
import com.backendless.persistence.QueryOptions;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;
import com.csce.tamu.eyebell.models.UserRelatives;
import com.csce.tamu.eyebell.models.Visitors;
import com.sinch.android.rtc.SinchError;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity implements SinchService.StartFailedListener {
    static private boolean validLogin;
    static private int REQUEST_CAMERA = 5678;
    static private int SELECT_FILE = 9876;
    private String lastRelPath;
    private List<Bitmap> relativePicsToAdd;
    private ListView mVisitorList;
    private List<List<String>> RelativePictureNames;
    private ListView mRelativeList;
    private ListView mRelativePictureList;
    private ActionBarDrawerToggle mDrawerToggle;
    private BackendlessCollection<Visitors> VisitorLog;
    private View headerView;
    private BackendlessCollection<UserRelatives> RelativeLog;
    private List<Bitmap> VisitorImages;
    private List<List<Bitmap>> RelativeImages;
    private CharSequence mDrawerTitle;
    private BackendlessUser currentUser;
    private String[] mMenuTitles;
    private CharSequence mTitle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private UserLogOutTask mOutTask = null;
    private UserGetTask mUseTask = null;
    private VisitorUpdateTask mLogTask = null;
    private RelativesUpdateTask mRelTask = null;
    private boolean intercomEnable = false;

    private void initializeDrawerMenuAdapter() {
        int[] mIcons = new int[]{
                R.drawable.ic_action_home,
                R.mipmap.ic_action_visitor_logs,
                R.drawable.ic_action_visitors,
                R.drawable.ic_action_name,
                R.drawable.ic_action_logout,
        };
        ArrayList<HashMap<String,String>> mList = new ArrayList<>();
        for(int i=0;i<mMenuTitles.length;i++){
            HashMap<String, String> hm = new HashMap<>();
            hm.put("text1", mMenuTitles[i]);
            hm.put("icon1", Integer.toString(mIcons[i]));
            mList.add(hm);
        }
        String from[] = {"text1", "icon1"};
        int to[] = {R.id.text1, R.id.icon1};
        // Set the adapter for the list view
        mDrawerList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.drawer_list_item, from, to));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
    }

    private void initializeVisitorLogAdapter() {
        String namePrompt = getString(R.string.visitor_name_prompt) + " ";
        String visitPrompt = getString(R.string.visitor_date_prompt) + " ";
        ArrayList<HashMap<String,String>> mList = new ArrayList<>();
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        for(Visitors visitor : VisitorLog.getData()){
            String formatDate = df.format(visitor.getVisitDate());
            HashMap<String, String> hm = new HashMap<>();
            Log.i("VisitorBuilder", "Adding visitor: " + visitor.getVisitorName()
                    + " " + visitor.getVisitDate());
            hm.put("visit_text1", namePrompt + visitor.getVisitorName());
            hm.put("visit_text2", visitPrompt + formatDate);
            mList.add(hm);
        }
        String from[] = {"visit_text1", "visit_text2"};
        int to[] = {R.id.visit_text1, R.id.visit_text2};
        mVisitorList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.visitor_list_item, from, to));
        mVisitorList.setOnItemClickListener(new VisitorItemClickListener());
    }

    private void initializeRelativeAdapter() {
        String namePrompt = getString(R.string.visitor_name_prompt) + " ";
        String photoPrompt = getString(R.string.saved_photos) + " ";
        ArrayList<HashMap<String,String>> mList = new ArrayList<>();
        int size = 0;
        if (RelativeLog != null) {
            if (RelativeLog.getData() != null) {
                size = RelativeLog.getData().size();
            }
        }
        for(int i = 0; i < size; ++i) {
            UserRelatives relative = RelativeLog.getData().get(i);
            HashMap<String, String> hm = new HashMap<>();
            Log.i("RelativeBuilder", "Adding relative: " + relative.getRelativeName()
                    + " with " + RelativeImages.get(i).size() + " trained photos.");
            hm.put("visit_text1", namePrompt + relative.getRelativeName());
            hm.put("visit_text2", photoPrompt + RelativeImages.get(i).size());
            mList.add(hm);
        }
        String from[] = {"visit_text1", "visit_text2"};
        int to[] = {R.id.visit_text1, R.id.visit_text2};
        mRelativeList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.visitor_list_item, from, to));
        mRelativeList.setOnItemClickListener(new RelativeItemClickListener());
    }

    private void initializeRelativeAdapterAndDelete(int abc) {
        RelativeLog.getData().remove(abc);
        RelativeImages.remove(abc);
        RelativePictureNames.remove(abc);
        String namePrompt = getString(R.string.visitor_name_prompt) + " ";
        String photoPrompt = getString(R.string.saved_photos) + " ";
        ArrayList<HashMap<String,String>> mList = new ArrayList<>();
        int size = 0;
        if (RelativeLog != null) {
            if (RelativeLog.getData() != null) {
                size = RelativeLog.getData().size();
            }
        }
        for(int i = 0; i < size; ++i) {
            UserRelatives relative = RelativeLog.getData().get(i);
            HashMap<String, String> hm = new HashMap<>();
            Log.i("RelativeBuilder", "Adding relative: " + relative.getRelativeName()
                    + " with " + RelativeImages.get(i).size() + " trained photos.");
            hm.put("visit_text1", namePrompt + relative.getRelativeName());
            hm.put("visit_text2", photoPrompt + RelativeImages.get(i).size());
            mList.add(hm);
        }
        String from[] = {"visit_text1", "visit_text2"};
        int to[] = {R.id.visit_text1, R.id.visit_text2};
        mRelativeList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.visitor_list_item, from, to));
        mRelativeList.setOnItemClickListener(new RelativeItemClickListener());
    }

    private void initializeRelativePictureAdapter(int i) {
        String namePrompt = "Image: ";
        ArrayList<HashMap<String,String>> mList = new ArrayList<>();
        for(String image : RelativePictureNames.get(i)){
            HashMap<String, String> hm = new HashMap<>();
            hm.put("visit_text1", namePrompt + image);
            hm.put("visit_text2", "");
            mList.add(hm);
        }
        String from[] = {"visit_text1", "visit_text2"};
        int to[] = {R.id.visit_text1, R.id.visit_text2};
        mRelativePictureList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.visitor_list_item, from, to));
        mRelativePictureList.setOnItemClickListener(new RelativePictureItemListener());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutInflater inflater = getLayoutInflater();
        relativePicsToAdd = new ArrayList<>();
        headerView = inflater.inflate(R.layout.header_list, null, false);


        mTitle = mDrawerTitle = getTitle();
        mMenuTitles = getResources().getStringArray(R.array.menu_item_names);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.addHeaderView(headerView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        initializeDrawerMenuAdapter();

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {
            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    public String getPath(Uri uri, Activity activity) {
        String[] projection = { MediaStore.MediaColumns.DATA };
        Cursor cursor = activity
                .managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    private void selectImage() {
        final CharSequence[] items = { "Take Photo", "Choose from Library",
                "Cancel" };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Take Photo")) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    File f = new File(android.os.Environment
                            .getExternalStorageDirectory(), "temp.jpg");
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals("Choose from Library")) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivityForResult(
                            Intent.createChooser(intent, "Select File"),
                            SELECT_FILE);
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                File f = new File(Environment.getExternalStorageDirectory()
                        .toString());
                for (File temp : f.listFiles()) {
                    if (temp.getName().equals("temp.jpg")) {
                        f = temp;
                        break;
                    }
                }
                try {
                    Bitmap bm;
                    BitmapFactory.Options btmapOptions = new BitmapFactory.Options();

                    bm = BitmapFactory.decodeFile(f.getAbsolutePath(),
                            btmapOptions);

                    // bm = Bitmap.createScaledBitmap(bm, 70, 70, true);
                    relativePicsToAdd.add(bm);

                    String path = android.os.Environment
                            .getExternalStorageDirectory()
                            + File.separator
                            + "Phoenix" + File.separator + "default";
                    f.delete();
                    OutputStream fOut = null;
                    File file = new File(path, String.valueOf(System
                            .currentTimeMillis()) + ".jpg");
                    try {
                        fOut = new FileOutputStream(file);
                        bm.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
                        fOut.flush();
                        fOut.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SELECT_FILE) {
                Uri selectedImageUri = data.getData();

                String tempPath = getPath(selectedImageUri, MainActivity.this);
                Bitmap bm;
                BitmapFactory.Options btmapOptions = new BitmapFactory.Options();
                bm = BitmapFactory.decodeFile(tempPath, btmapOptions);
                relativePicsToAdd.add(bm);
            }
            if (relativePicsToAdd.size() > 0) {
                Random rand = new Random();
                int salt = rand.nextInt() % 5001;
                Backendless.Files.Android.upload(relativePicsToAdd.get(0), Bitmap.CompressFormat.PNG, 100, Integer.toString(salt) + "myphoto.png", lastRelPath,
                        new AsyncCallback<BackendlessFile>() {
                            @Override
                            public void handleResponse(final BackendlessFile backendlessFile) {
                                Log.i("RelImage", "Upload success");
                            }

                            @Override
                            public void handleFault(BackendlessFault backendlessFault) {
                                Log.i("FailedRelImage", "Image not uploaded. " + backendlessFault.getDetail() + " AND " + backendlessFault.getMessage());
                            }
                        });
                relativePicsToAdd.remove(0);
                loadRelatives();
            }
        }
    }

    @Override
    public void onStarted() {
        intercomEnable = true;
    }

    @Override
    protected void onServiceConnected() {
        getSinchServiceInterface().setStartListener(this);

        loadCurrentUser();
        loadRelatives();
        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {

            synchronized public void run() {
                loadVisitorLogs();
            }

        }, TimeUnit.SECONDS.toMillis(30), TimeUnit.SECONDS.toMillis(30));

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onStartFailed(SinchError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private class VisitorItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectVisitor(position);
        }
    }

    private class RelativeItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectRelative(position);
        }
    }

    private class RelativePictureItemListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectRelativeImage(position);
        }
    }

    /** Swaps fragments in the main content view */
    public void selectVisitor(final int position) {
        String choice_name = VisitorLog.getData().get(position).getVisitorName();
        Log.i("VisitorSelection", "User selected visitor: " + choice_name);
        final Dialog dialog= new Dialog(this);
        LayoutInflater inflater  = getLayoutInflater();
        View v = inflater.inflate(R.layout.visitor_view, null);
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        String formatDate = df.format(VisitorLog.getData().get(position).getVisitDate());
        ((TextView) v.findViewById(R.id.visitor_name)).append(" " + VisitorLog.getData().get(position).getVisitorName());
        ((TextView) v.findViewById(R.id.visitor_date)).append(" " + formatDate);
        ((ImageView) v.findViewById(R.id.visitor_image)).setImageBitmap(VisitorImages.get(position));
        dialog.setContentView(v);
        dialog.show();
        (v.findViewById(R.id.dialog_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
                mVisitorList.setItemChecked(position, false);
            }
        });
    }


    /** Swaps fragments in the main content view */
    public void selectRelative(final int position) {
        String choice_title = "Users";
        Log.i("RelativeSelection", "User selected: " + RelativeLog.getData().get(position).getRelativeName());
        switch (position) {
            default:
                // Create a new fragment and specify the planet to show based on position
                Fragment fragment = new RelativeFragment();
                Bundle args = new Bundle();
                args.putInt(RelativeFragment.ARG_MENU_SELECTION, position);
                fragment.setArguments(args);

                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_main_frame, fragment)
                        .commit();
                setTitle(choice_title);
                break;
        }
    }

    public void addRelative() {

    }

    /** Swaps fragments in the main content view */
    public void selectRelativeImage(final int position) {
        /*String choice_name = VisitorLog.getData().get(position).getVisitorName();
        Log.i("VisitorSelection", "User selected visitor: " + choice_name);
        final Dialog dialog= new Dialog(this);
        LayoutInflater inflater  = getLayoutInflater();
        View v = inflater.inflate(R.layout.visitor_view, null);
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        String formatDate = df.format(VisitorLog.getData().get(position).getVisitDate());
        ((TextView) v.findViewById(R.id.visitor_name)).append(" " + VisitorLog.getData().get(position).getVisitorName());
        ((TextView) v.findViewById(R.id.visitor_date)).append(" " + formatDate);
        ((ImageView) v.findViewById(R.id.visitor_image)).setImageBitmap(VisitorImages.get(position));
        dialog.setContentView(v);
        dialog.show();
        (v.findViewById(R.id.dialog_btn)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
                mVisitorList.setItemChecked(position, false);
            }
        });*/
    }

    /** Swaps fragments in the main content view */
    public void selectItem(int position) {
        String choice_title = "Header";
        if (position > 0) {
            choice_title = getResources().getStringArray(R.array.menu_item_names)[position - 1];
        }
        Log.i("MenuSelection", "User selected: " + choice_title);
        switch (position) {
            case 0:
                // Header selected
                // TODO: Do Something Else
                break;
            case 1:
                if (!getSinchServiceInterface().isStarted()) {
                    getSinchServiceInterface().startClient(currentUser.getEmail());
                }
            case 2:
            case 3:
                // Create a new fragment and specify the planet to show based on position
                Fragment fragment = new ContentFragment();
                Bundle args = new Bundle();
                args.putInt(ContentFragment.ARG_MENU_SELECTION, position);
                fragment.setArguments(args);

                // Insert the fragment by replacing any existing fragment
                FragmentManager fragmentManager = getFragmentManager();
                fragmentManager.beginTransaction()
                        .replace(R.id.content_main_frame, fragment)
                        .commit();
                setTitle(mMenuTitles[position - 1]);
                break;
            case 4:
                if (intercomEnable) {
                    startActivity(new Intent(this, PlaceCallActivity.class));
                }
                break;
            case 5:
                attemptLogout();
                break;
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private void attemptLogout() {
        if (mOutTask != null) {
            return;
        }

        // Store values at the time of the login attempt.
        String email = currentUser.getEmail();

        boolean cancel = false;
        View focusView = null;

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mOutTask = new UserLogOutTask(email, this);
            mOutTask.execute((Void) null);
        }
    }

    private void loadCurrentUser() {
        if (mUseTask != null) {
            return;
        }

        boolean cancel = false;
        View focusView = null;

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mUseTask = new UserGetTask(this);
            mUseTask.execute((Void) null);
        }
    }

    private void loadRelatives() {
        if (mRelTask != null) {
            return;
        }
        mRelTask = new RelativesUpdateTask(this);
        mRelTask.execute((Void) null);
    }

    private void loadVisitorLogs() {
        if (mLogTask != null) {
            return;
        }
        mLogTask = new VisitorUpdateTask(this);
        mLogTask.execute((Void) null);
    }

    public class RelativesUpdateTask extends AsyncTask<Void, Void, Boolean> {
        private Activity mActivity;
        private List<List<String>> fileUrls;
        RelativesUpdateTask(Activity act) {
            fileUrls = new ArrayList<>();
            RelativeImages = new ArrayList<>();
            RelativePictureNames = new ArrayList<>();
            mActivity = act;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (currentUser != null) {
                String whereClause = "SerialNum = '" + currentUser.getProperty("serial") + "'";
                BackendlessDataQuery dataQuery = new BackendlessDataQuery();
                dataQuery.setWhereClause(whereClause);
                RelativeLog = Backendless.Persistence.of(UserRelatives.class).find(dataQuery);
                if (RelativeLog == null || RelativeLog.getData() == null) {
                    return false;
                }
                if (RelativeLog.getData().size() != RelativeImages.size()) {
                    int temp = RelativeImages.size();
                    for (int i = temp; i < RelativeLog.getData().size(); ++i) {
                        final int index = i - temp;
                        RelativeImages.add(index, new ArrayList<Bitmap>());
                        RelativePictureNames.add(index, new ArrayList<String>());
                        fileUrls.add(index, new ArrayList<String>());
                        // New entries will always be new people so take from front of Visitor log
                        UserRelatives relative = RelativeLog.getData().get(i - temp);
                        BackendlessCollection<FileInfo> response = Backendless.Files.listing(relative.getRelativePath(), "*.*", false);
                        for (FileInfo file : response.getCurrentPage()) {
                            String src = file.getPublicUrl();
                            RelativePictureNames.get(index).add(file.getName());
                            fileUrls.get(index).add(src);
                        }
                    }
                    for (int i = 0; i < fileUrls.size(); ++i) {
                        for (String url : fileUrls.get(i)) {
                            try {
                                URL s = new URL(url);
                                HttpURLConnection connection = (HttpURLConnection) s.openConnection();
                                connection.setDoInput(true);
                                connection.connect();
                                InputStream input = connection.getInputStream();
                                Bitmap bitmap = BitmapFactory.decodeStream(input);
                                RelativeImages.get(i).add(bitmap);
                                Log.i("Get Relative Image", url);
                            } catch (Exception e) {
                                Log.i("RelativeImageFailure", "Failure: " + e.getMessage() + " " + e.getLocalizedMessage() + " " + e.toString());
                            }
                        }
                    }
                    if (mRelativeList != null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initializeRelativeAdapter();
                                mRelativeList.deferNotifyDataSetChanged();
                            }
                        });
                    }
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRelTask = null;

            if (!success || currentUser == null) {
                Log.i("RelativeRefreshFail", "Could not refresh visitor logs.");
            } else {
                Log.i("UserRelatives loaded", "Visitor log refreshed!");
            }
        }

        @Override
        protected void onCancelled() {
            mRelTask = null;
        }
    }

    public class VisitorUpdateTask extends AsyncTask<Void, Void, Boolean> {
        private Activity mActivity;
        VisitorUpdateTask(Activity act) {
            mActivity = act;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                if (currentUser != null) {
                    String whereClause = "SerialNum = '" + currentUser.getProperty("serial") + "'";
                    BackendlessDataQuery dataQuery = new BackendlessDataQuery();
                    dataQuery.setWhereClause(whereClause);
                    QueryOptions query = new QueryOptions("VisitDate desc");
                    dataQuery.setQueryOptions(query);
                    VisitorLog = Backendless.Persistence.of(Visitors.class).find(dataQuery);
                    if (VisitorLog.getData().size() != VisitorImages.size()) {
                        int temp = VisitorImages.size();
                        for (int i = temp; i < VisitorLog.getData().size(); ++i) {
                            // New entries will always be new people so take from front of Visitor log
                            Visitors visitor = VisitorLog.getData().get(i - temp);
                            String src =
                                    "https://api.backendless.com/"+getString(R.string.app_id)+"/v1/files"+visitor.getImage();
                            Log.i("Get Image", src);
                            URL url = new URL(src);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.connect();
                            InputStream input = connection.getInputStream();
                            Bitmap bitmap = BitmapFactory.decodeStream(input);
                            VisitorImages.add(i - temp, bitmap);
                        }
                        if (mVisitorList != null) {
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    initializeVisitorLogAdapter();
                                    mVisitorList.deferNotifyDataSetChanged();
                                }
                            });
                        }
                        //MessageStatus status = Backendless.Messaging.publish(currentUser.getProperty("serial").toString(), "New visitor detected!" );
                        //Log.i("MessageSent", status.getStatus().toString() + " AND " + status.getErrorMessage());
                    }
                }

            } catch (Exception e) {
                Log.i("Image Load Failure", "Failure: " + e.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mLogTask = null;

            if (!success || currentUser == null) {
                Log.i("LogRefreshFail", "Could not refresh visitor logs.");
            } else {
                Log.i("Log refresh loaded", "Visitor log refreshed!");
            }
        }

        @Override
        protected void onCancelled() {
            mLogTask = null;
        }
    }

    public class UserLogOutTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private Activity mActivity;

        UserLogOutTask(String email, Activity activity) {
            mEmail = email;
            mActivity = activity;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Backendless.UserService.logout();
                Log.i("Logout", mEmail + " successfully logged out");
            } catch (BackendlessException e) {
                Log.i("Logout", "Failure: " + e.getDetail());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mOutTask = null;

            if (success) {
                mActivity.startActivity(new Intent(mActivity, LoginActivity.class));
                mActivity.finish();
            }
        }

        @Override
        protected void onCancelled() {
            mOutTask = null;
        }
    }

    public class UserGetTask extends AsyncTask<Void, Void, Boolean> {
        private Activity mActivity;

        UserGetTask(Activity activity) {
            mActivity = activity;
            VisitorImages = new ArrayList<>();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                String userToken = UserTokenStorageFactory.instance().getStorage().get();
                if (userToken != null && !userToken.equals("")) {
                    validLogin = Backendless.UserService.isValidLogin();
                    if (validLogin) {
                        String userId = UserIdStorageFactory.instance().getStorage().get();
                        currentUser = Backendless.Data.of(BackendlessUser.class).findById(userId);
                    }
                } else {
                    currentUser = Backendless.UserService.CurrentUser();
                }
                if (currentUser != null) {
                   /* if (!getSinchServiceInterface().isStarted()) {
                        getSinchServiceInterface().startClient(currentUser.getEmail());
                    }*/
                    String whereClause = "SerialNum = '" + currentUser.getProperty("serial") + "'";
                    BackendlessDataQuery dataQuery = new BackendlessDataQuery();
                    dataQuery.setWhereClause(whereClause);
                    QueryOptions query = new QueryOptions("VisitDate desc");
                    dataQuery.setQueryOptions(query);
                    VisitorLog = Backendless.Persistence.of(Visitors.class).find(dataQuery);
                    for (Visitors visitor : VisitorLog.getData()) {
                        String src =
                                "https://api.backendless.com/" + getString(R.string.app_id) + "/v1/files" + visitor.getImage();
                        Log.i("Get Image", src);
                        URL url = new URL(src);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream input = connection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream(input);
                        VisitorImages.add(bitmap);
                    }

                    Backendless.Messaging.registerDevice("474153288431", currentUser.getProperty("serial").toString());
                }

            } catch (BackendlessException e) {
                Log.i("User Load Failure", "Failure: " + e.getDetail() + " " + e.getMessage());
                return false;
            } catch (Exception e) {
                Log.i("Image Load Failure", "Failure: " + e.getMessage());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mOutTask = null;

            if (!success || (success && currentUser == null)) {
                Log.i("User Load", "Could not get a user.");
                mActivity.startActivity(new Intent(mActivity, LoginActivity.class));
                mActivity.finish();
            } else {
                Log.i("User Load", currentUser.getEmail() + " loaded successfully!");
                ((TextView)headerView.findViewById(R.id.profile_name)).append(" " + currentUser.getProperty("name") + "!");
                ((TextView)headerView.findViewById(R.id.profile_email)).append(" " + currentUser.getProperty("email"));
                ((MainActivity)mActivity).selectItem(1);
            }
        }

        @Override
        protected void onCancelled() {
            mOutTask = null;
        }
    }

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    public class ContentFragment extends Fragment {
        public static final String ARG_MENU_SELECTION = "planet_number";
        private FloatingActionButton relAdd;
        public ContentFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int i = getArguments().getInt(ARG_MENU_SELECTION) - 1;
            String choice_title = getResources().getStringArray(R.array.menu_item_names)[i];

            View rootView;
            if (i == 0) {
                rootView = inflater.inflate(R.layout.fragment_home, container, false);
                int imageId = getResources().getIdentifier(choice_title.toLowerCase(Locale.getDefault()),
                        "drawable", getActivity().getPackageName());
                ((ImageView) rootView.findViewById(R.id.frag_image)).setImageResource(imageId);
                ((TextView) rootView.findViewById(R.id.frag_title)).setText(choice_title);
            } else if (i == 1) {
                rootView = inflater.inflate(R.layout.fragment_visitors, container, false);
                mVisitorList = (ListView) rootView.findViewById(R.id.visitors_list);
                initializeVisitorLogAdapter();
            } else if (i == 2) {
                rootView = inflater.inflate(R.layout.fragment_relatives, container, false);
                mRelativeList = (ListView) rootView.findViewById(R.id.relatives_list);
                relAdd = (FloatingActionButton) rootView.findViewById(R.id.relative_fab_add);
                relAdd.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Log.i("AddRelative", "User selected to add relative.");
                        final Dialog dialog= new Dialog(MainActivity.this);
                        LayoutInflater inflater  = getLayoutInflater();
                        View relativeView = inflater.inflate(R.layout.add_relative_view, null);
                        final EditText greeting_name = ((EditText) relativeView.findViewById(R.id.relative_add_name));
                        final EditText greeting_text = ((EditText) relativeView.findViewById(R.id.relative_add_greeting));
                        dialog.setContentView(relativeView);
                        dialog.show();
                        (relativeView.findViewById(R.id.relative_add_add)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {
                                UserRelatives newRelative = new UserRelatives();
                                newRelative.setGreeting(greeting_text.getText().toString());
                                newRelative.setRelativeName(greeting_name.getText().toString());
                                newRelative.setSerialNum(currentUser.getProperty("serial").toString());
                                newRelative.setRelativePath("/"+newRelative.getSerialNum()+"/"+newRelative.getRelativeName().toLowerCase());
                                Backendless.Persistence.save( newRelative, new AsyncCallback<UserRelatives>() {
                                    public void handleResponse( UserRelatives response ) {
                                        // new Contact instance has been saved
                                        Log.i("RelativeSaved", "Success!");
                                        Bitmap photo = BitmapFactory.decodeResource(getResources(), R.drawable.home);
                                        final String relPath = response.getRelativePath();
                                        Backendless.Files.Android.upload(photo, Bitmap.CompressFormat.PNG, 100, "myphoto.png", relPath,
                                                new AsyncCallback<BackendlessFile>() {
                                                    @Override
                                                    public void handleResponse(final BackendlessFile backendlessFile) {
                                                        Backendless.Files.remove(relPath + "/myphoto.png", new AsyncCallback<Void>() {
                                                            @Override
                                                            public void handleResponse(Void response) {
                                                                loadRelatives();
                                                            }

                                                            @Override
                                                            public void handleFault(BackendlessFault fault) {
                                                                Log.i("FailedDeleting", fault.getMessage() + " AND " + fault.getDetail());
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void handleFault(BackendlessFault backendlessFault) {

                                                    }
                                                });
                                    }
                                    public void handleFault( BackendlessFault fault )
                                    {
                                        // an error has occurred, the error code can be retrieved with fault.getCode()
                                        Log.i("RelativeFailure", "Could not save " + fault.getDetail());
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
                        (relativeView.findViewById(R.id.relative_add_cancel)).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View arg0) {

                                dialog.dismiss();
                            }
                        });

                    }
                });
                initializeRelativeAdapter();
            } else {
                rootView = inflater.inflate(R.layout.fragment_home, container, false);
                int imageId = getResources().getIdentifier(choice_title.toLowerCase(Locale.getDefault()),
                        "drawable", getActivity().getPackageName());
                ((ImageView) rootView.findViewById(R.id.frag_image)).setImageResource(imageId);
                ((TextView) rootView.findViewById(R.id.frag_title)).setText(choice_title);
            }

            getActivity().setTitle(choice_title);
            return rootView;
        }
    }

    /**
     * Fragment that appears in the "content_frame", shows a planet
     */
    public class RelativeFragment extends Fragment {
        public static final String ARG_MENU_SELECTION = "planet_number";
        private FloatingActionButton relPicAdd;
        public RelativeFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final int i = getArguments().getInt(ARG_MENU_SELECTION);
            String choice_title = "Users";

            View rootView;
            rootView = inflater.inflate(R.layout.fragment_select_relative, container, false);
            mRelativePictureList  = (ListView) rootView.findViewById(R.id.relative_picture_list);
            ((TextView) rootView.findViewById(R.id.relative_name)).setText(RelativeLog.getData().get(i).getRelativeName());
            (rootView.findViewById(R.id.back_relative)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    selectItem(3);
                }
            });
            relPicAdd = (FloatingActionButton) rootView.findViewById(R.id.relative_fab_add_picture);
            ((Button)rootView.findViewById(R.id.delete_relative)).setText("Delete " + RelativeLog.getData().get(i).getRelativeName());
            relPicAdd.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    lastRelPath = RelativeLog.getData().get(i).getRelativePath();
                    selectImage();
                }
            });
            (rootView.findViewById(R.id.delete_relative)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    Backendless.Persistence.of(UserRelatives.class).remove(RelativeLog.getData().get(i), new AsyncCallback<Long>() {
                        public void handleResponse(Long response) {
                            Log.i("RelativeDeleted", "Successfully deleted relative");
                            initializeRelativeAdapterAndDelete(i);
                            selectItem(3);
                        }

                        public void handleFault(BackendlessFault fault) {
                            Log.i("RelativeDelete", "Fail: " + fault.getCode() + " Reason: " + fault.getDetail());
                            selectItem(1);
                        }
                    });
                }
            });
            initializeRelativePictureAdapter(i);
            getActivity().setTitle(choice_title);
            return rootView;
        }
    }
}
