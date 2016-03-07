package com.csce.tamu.eyebell;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.exceptions.BackendlessException;
import com.backendless.persistence.local.UserIdStorageFactory;
import com.backendless.persistence.local.UserTokenStorageFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static private boolean validLogin;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private BackendlessUser currentUser;
    private String[] mMenuTitles;
    private CharSequence mTitle;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private UserLogOutTask mOutTask = null;
    private UserGetTask mUseTask = null;
    private View headerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LayoutInflater inflater = getLayoutInflater();

        headerView = inflater.inflate(R.layout.header_list, null, false);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        loadCurrentUser();

        mTitle = mDrawerTitle = getTitle();
        mMenuTitles = getResources().getStringArray(R.array.menu_item_names);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        int[] mIcons = new int[]{
                R.drawable.ic_action_home,
                R.drawable.ic_action_visitors,
                R.drawable.ic_action_logout,
        };
        ArrayList<HashMap<String,String>> mList = new ArrayList<HashMap<String,String>>();
        for(int i=0;i<3;i++){
            HashMap<String, String> hm = new HashMap<String,String>();
            hm.put("text1", mMenuTitles[i]);
            hm.put("icon1", Integer.toString(mIcons[i]));
            mList.add(hm);
        }
        String from[] = {"text1", "icon1"};
        int to[] = {R.id.text1, R.id.icon1};
        mDrawerList.addHeaderView(headerView);
        // Set the adapter for the list view
        mDrawerList.setAdapter(new SimpleAdapter(this, mList,
                R.layout.drawer_list_item, from, to));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

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

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    public void selectItem(int position) {
        String choice_title = getResources().getStringArray(R.array.menu_item_names)[position];
        Log.i("MenuSelection", "User selected: " + choice_title);
        switch (position) {
            case 0:
                // Header selected
                // TODO: Do Something Else
                break;
            case 1:
            case 2:
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
            case 3:
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
        mDrawerToggle.syncState();
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
            } catch (BackendlessException e) {
                Log.i("User Load Failure", "Failure: " + e.getDetail() + " " + e.getMessage());
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
    public static class ContentFragment extends Fragment {
        public static final String ARG_MENU_SELECTION = "planet_number";

        public ContentFragment() {
            // Empty constructor required for fragment subclasses
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int i = getArguments().getInt(ARG_MENU_SELECTION) - 1;
            String choice_title = getResources().getStringArray(R.array.menu_item_names)[i];
            View rootView = inflater.inflate(R.layout.fragment_home, container, false);

            int imageId = getResources().getIdentifier(choice_title.toLowerCase(Locale.getDefault()),
                    "drawable", getActivity().getPackageName());
            ((ImageView) rootView.findViewById(R.id.frag_image)).setImageResource(imageId);
            ((TextView) rootView.findViewById(R.id.frag_title)).setText(choice_title);
            getActivity().setTitle(choice_title);
            return rootView;
        }
    }
}
