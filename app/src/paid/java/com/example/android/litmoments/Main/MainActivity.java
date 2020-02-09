package com.example.android.litmoments.Main;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ajts.androidmads.fontutils.FontUtils;
import com.example.android.litmoments.AddJournal.AddJournalEntry;

import com.example.android.litmoments.DashboardActivity;

import com.example.android.litmoments.Settings.SettingsActivity;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.lit.litmoments.AddJournal.JournalEntryModel;
import com.lit.litmoments.PrefMethods;
import com.lit.litmoments.R;
import com.lit.litmoments.RemoveAdsActivity;
import com.morsebyte.shailesh.twostagerating.TwoStageRate;
import com.sdsmdg.tastytoast.TastyToast;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements JournalMainAdapter.OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private FirebaseAuth mAuth;
    @BindView(R.id.toolbar) Toolbar toolbar;
    //@BindView(R.id.iconRefresh) ImageView ivRefresh;
    @BindView(R.id.addjournal_fab)  FloatingActionButton addJournalFab;
    @BindView(R.id.rvJournalEntries)  RecyclerView rvJournalEntries;
    @BindView(R.id.progressBar) ProgressBar progressBar;
    @BindView(R.id.coordinatorlayout)
    CoordinatorLayout coordinatorLayout;

    private JournalMainAdapter journalAdapter;
    private ArrayList<JournalEntryModel> journalList = new ArrayList<>();
    private DatabaseReference mDatabase,  mdefaultdatabase;

    public static final String DATABASE_UPLOADS = "User's Journal Entries";
    private SearchView searchView;
    PrefMethods prefMethods = new PrefMethods();

    Boolean isWhite = false;
    Boolean isDarkTheme = false;
    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadUiTheme(sharedPreferences);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            setUpThemeContent();
            loadWidgetColors(sharedPreferences);
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                /** Slide fadein = new Slide();
                 fadein.setDuration(3500);
                 Slide fadeout = new Slide();
                 fadeout.setDuration(3500);
                 // set an enter transition
                 getWindow().setSharedElementEnterTransition(fadein);
                 // set an exit transition
                 getWindow().setSharedElementExitTransition(fadeout);
                 // supportPostponeEnterTransition();
                 supportPostponeEnterTransition();
                 postponeEnterTransition();

                 **/
            Fade fade= new Fade();
            View view = getWindow().getDecorView();
            fade.excludeTarget(view.findViewById(R.id.action_bar_container), true);
            fade.excludeTarget(android.R.id.statusBarBackground, true);
            fade.excludeTarget(android.R.id.navigationBarBackground, true);

            getWindow().setEnterTransition(fade);
            getWindow().setExitTransition(fade);
           // getWindow().setSharedElementExitTransition(new Explode());


        }


        journalList.clear();
       // FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseApp.initializeApp(getApplicationContext());



        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
          //  Toast.makeText(MainActivity.this, "Welcome back "+" "+ mAuth.getCurrentUser().getDisplayName()+" ", Toast.LENGTH_SHORT).show();
        }

        String currentUid = mAuth.getCurrentUser().getUid();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        //firebaseDatabase.setPersistenceEnabled(true);
        mDatabase = firebaseDatabase.getReference(DATABASE_UPLOADS).child(currentUid);
       // mdefaultdatabase = firebaseDatabase.getReference("Default dbase").child(currentUid);
        if(savedInstanceState != null){
            journalList = savedInstanceState.getParcelableArrayList("JournalEntires");
        }
         else {
            journalList = journalList;
        }
        journalAdapter = new JournalMainAdapter(journalList, getApplicationContext(), this);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        rvJournalEntries.setLayoutManager(mLayoutManager);
        rvJournalEntries.setItemAnimator(new DefaultItemAnimator());

        rvJournalEntries.setAdapter(journalAdapter);
        rvJournalEntries.setNestedScrollingEnabled(false);


        addJournalFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddJournalEntry.class));
            }
        });

       /** ivRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               getJornals();
            }
        }); **/

        getJornals();

        /**final RatingDialog ratingDialog = new RatingDialog.Builder(this)
                //.icon(getResources().getDrawable(R.drawable.ic_journal))
                .session(2)
                .threshold(3)
                .title("How is your experience with us?")
                .titleTextColor(getResources().getColor(R.color.colorAccent))
                .positiveButtonText("Maybe Later")
                .negativeButtonText("Never")
                .positiveButtonTextColor(getResources().getColor(R.color.colorAccent))
                .negativeButtonTextColor(getResources().getColor(R.color.colorAccent))
                .formTitle("Submit Feedback")
                .formHint("Tell us where we can improve")
                .formSubmitText("Submit")
                .formCancelText("Cancel")
                .ratingBarColor(R.color.yellowcolorPrimary)
                .playstoreUrl("YOUR_URL")
                .onThresholdCleared(new RatingDialog.Builder.RatingThresholdClearedListener() {
                    @Override
                    public void onThresholdCleared(RatingDialog ratingDialog, float rating, boolean thresholdCleared) {
                        //do something
                        ratingDialog.dismiss();
                    }
                })
                .onThresholdFailed(new RatingDialog.Builder.RatingThresholdFailedListener() {
                    @Override
                    public void onThresholdFailed(RatingDialog ratingDialog, float rating, boolean thresholdCleared) {
                        //do something
                        ratingDialog.dismiss();
                    }
                })
                .onRatingChanged(new RatingDialog.Builder.RatingDialogListener() {
                    @Override
                    public void onRatingSelected(float rating, boolean thresholdCleared) {

                    }
                })
                .onRatingBarFormSumbit(new RatingDialog.Builder.RatingDialogFormListener() {
                    @Override
                    public void onFormSubmitted(String feedback) {

                    }
                }).build();


        FiveStarsDialog fiveStarsDialog = new FiveStarsDialog(this,"Allangisoni@gmail.com");
        fiveStarsDialog.setRateText("Rate This App")
                .setTitle("How do you like our app?")
                .setForceMode(false)
                .setNegativeReviewListener(new NegativeReviewListener() {
                    @Override
                    public void onNegativeReview(int i) {

                    }
                })
                .setUpperBound(3) // Market opened if a rating >= 2 is selected

               // .setNegativeReviewListener(this) // OVERRIDE mail intent for negative review
                //.setReviewListener(this) // Used to listen for reviews (if you want to track them )
                .showAfter(3);

         **/
        // ratingDialog.show();


        TwoStageRate twoStageRate = TwoStageRate.with(this);
//initialises condintions to 5 days of use, 10 times of launch use or 5 triggers of custom event.
        twoStageRate.setInstallDays(3).setLaunchTimes(10).setEventsTimes(5);

//If user dismisses it, it simply resets again. (when user dismissed by clicking anywhere else on screen)
        twoStageRate.resetOnDismiss(true);  //it is true by default

//If user gives rating the first time but declines to give playstore rating/ feedback we can reset the
//TwoStageRate. These are false by default.
        twoStageRate.resetOnFeedBackDeclined(true).resetOnRatingDeclined(true);

//You may choose to show/hide your app icon in rating prompt (default true)
        twoStageRate.setShowAppIcon(true);
//Finally call to show feedback dialog if any of condition is met.
        twoStageRate.showIfMeetsConditions();


    }



    public void setUpThemeContent (){
        if(isWhite == true) {
            toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
            ((TextView)toolbar.getChildAt(1)).setTextColor(getResources().getColor(android.R.color.white));
            ((ImageView) toolbar.getChildAt(0)).setColorFilter(getResources().getColor(android.R.color.white));
        } else {
            toolbar.setTitleTextColor(getResources().getColor(R.color.colorAccent));
            ((TextView)toolbar.getChildAt(1)).setTextColor(getResources().getColor(R.color.colorAccent));
            ((ImageView) toolbar.getChildAt(0)).setColorFilter(getResources().getColor(R.color.colorAccent));
        }

        if(isWhite == true && isDarkTheme== true) {
            toolbar.setTitleTextColor(getResources().getColor(R.color.blackthemeAccent));
            ((TextView)toolbar.getChildAt(1)).setTextColor(getResources().getColor(R.color.blackthemeAccent));
            ((ImageView) toolbar.getChildAt(0)).setColorFilter(getResources().getColor(R.color.blackthemeAccent));
            coordinatorLayout.setBackground(getResources().getDrawable(R.drawable.maincoordinatorlayoutscrim));

        }
    }

    private void getJornals(){


        //final ProgressDialog progressDialog = new ProgressDialog(this);
        //progressDialog.setMessage("Please wait...........");
       // progressDialog.setTitle("Retrieving data");
       // progressDialog.show();
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                journalList.clear();
                if (dataSnapshot.exists()){
                    for (DataSnapshot npsnapshot : dataSnapshot.getChildren()){


                        JournalEntryModel journalEntryModel=npsnapshot.getValue(JournalEntryModel.class);
                      /**  JournalEntryModel journalEntryModel= new JournalEntryModel(npsnapshot.getValue(JournalEntryModel.class).getJournalDate(),
                                npsnapshot.getValue(JournalEntryModel.class).getJournalLocation(),
                                npsnapshot.getValue(JournalEntryModel.class).getJournalWeather(),
                                npsnapshot.getValue(JournalEntryModel.class).getJournalMood(),
                                npsnapshot.getValue(JournalEntryModel.class).getJournalTitle(),
                                npsnapshot.getValue(JournalEntryModel.class).getJournalMessage(),
                                npsnapshot.getValue(JournalEntryModel.class).getMonth(),
                                npsnapshot.getValue(JournalEntryModel.class).getDay(),
                                npsnapshot.getValue(JournalEntryModel.class).getKey(),
                                npsnapshot.getValue(JournalEntryModel.class).getTime()
                        );  **/

                       journalList.add(journalEntryModel);
                      //  Collections.reverse(journalList);
                        //JournalEntrySorter journalEntrySorter = new JournalEntrySorter(journalList);
                        //journalList = journalEntrySorter.getSortedBytimestamp();

                        Collections.sort(journalList, new Comparator<JournalEntryModel>() {
                            public int compare(JournalEntryModel o1, JournalEntryModel o2) {
                                SimpleDateFormat formatter2=new SimpleDateFormat("yyyy-MM-dd");
                                int result =0;
                                if (o1.getJournalDate() == null || o2.getJournalDate() == null)
                                    result= 0;

                                try {
                                   result=formatter2.parse(o2.getJournalDate()).compareTo(formatter2.parse(o1.getJournalDate()));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                    result = 0;
                                }

                                return result;
                            }
                        });

                       // Collections.reverse(journalList);
                        if(Build.VERSION.SDK_INT > 24) {
                          //  journalList.sort(Comparator.comparing(o -> o.getTimestampCreatedLong()));
                        }
                    }

                    journalAdapter.notifyDataSetChanged();

                } else{
                    JournalEntryModel journalEntryMode = new JournalEntryModel();
                    journalEntryMode.setJournalDate("2019-08-02");
                    journalEntryMode.setJournalLocation("Lit Place");
                    journalEntryMode.setJournalMood("Happy");
                    journalEntryMode.setJournalWeather("Sunny");
                    //journalEntryMode.setJournalTitle("Welcome" + " " + mAuth.getCurrentUser().getDisplayName());
                    journalEntryMode.setJournalTitle("Get started with Lit Moments");
                    journalEntryMode.setJournalMessage("Capture, store and share your Lit Moments with loved ones. Don't forget to check out your Lit board!!!! ");
                    journalEntryMode.setDay("02");
                    journalEntryMode.setMonth("08");
                    //journalEntryMode.setTime(ServerValue.TIMESTAMP);
                    ArrayList<String> imageurl = new ArrayList<>();
                    imageurl.add("https://firebasestorage.googleapis.com/v0/b/litmoments-de630.appspot.com/o/uploads%2F1564700689347.null?alt=media&token=a24fe027-4b6d-44eb-8951-185691450548");
                    journalEntryMode.setJournalImagePath(imageurl);
                    journalList.add(journalEntryMode);
                    journalAdapter.notifyDataSetChanged();
                }




                    //journalList.add(journalEntryMode);
                    //journalAdapter.notifyDataSetChanged();

               // progressDialog.dismiss();
                progressBar.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            // progressDialog.dismiss();
             progressBar.setVisibility(View.INVISIBLE);

             Toast.makeText(MainActivity.this, "Please check your Internet Connection", Toast.LENGTH_SHORT).show();
            }

        });

        if (journalList.size() == 0) {



           /** DatabaseReference databaseReference = mdefaultdatabase.push();
            String refKey = databaseReference.getKey();
            //HashMap<String, String> myFilePath = new HashMap<String, String>();
            JournalEntryModel journalEntryModel = new JournalEntryModel(journalEntryMode.getJournalDate(), journalEntryMode.getJournalLocation(), journalEntryMode.getJournalWeather(),
                    journalEntryMode.getJournalMood(), journalEntryMode.getJournalTitle(), journalEntryMode.getJournalMessage(), journalEntryMode.getMonth(), journalEntryMode.getDay(),
                    imageurl, refKey);

            databaseReference.setValue(journalEntryModel);  **/
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.menu_main, menu);

        MenuItem menuItem = menu.findItem(R.id.item_save);
        MenuItem menuPhotoItem = menu.findItem(R.id.item_image);



        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        ImageView imageView = (ImageView) findViewById(R.id.toolbarImage);



        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // hide action item

                imageView.setVisibility(View.INVISIBLE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {

               imageView.setVisibility(View.VISIBLE);
                return false;

            }
        });

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted

                journalAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                journalAdapter.getFilter().filter(query);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if( id == R.id.action_refresh){
            getJornals();
            return  true;
        } else if(id == R.id.action_search){

            return true;


        } else  if(id == R.id.action_dashboard){
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            intent.putExtra("JournalList", journalList);
            startActivity(intent);
            return true;
        } else if(id == R.id.action_removeads) {

            Intent intent = new Intent(MainActivity.this, RemoveAdsActivity.class);
            startActivity(intent);
            return  true;

        }

        else {

        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(JournalEntryModel journalEntryModel) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        //getJornals();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

    }

    private void loadUiTheme (SharedPreferences sharedPreferences){

        String userTheme = sharedPreferences.getString(getResources().getString(R.string.key_uiTheme), "2");
        if (userTheme.equals("2")) {
            setTheme(R.style.LitStyle);
            isWhite = false;
        }
        else if (userTheme.equals("1")) {
            setTheme(R.style.ReddishLitStyle);
            isWhite = true;
        }
        else if (userTheme.equals("0")) {
            setTheme(R.style.BlueLitStyle);
            isWhite = true;
        }
     else if (userTheme.equals("3")) {
        setTheme(R.style.YellowLitStyle);
        isWhite=false;
    } else if (userTheme.equals("4")) {
        setTheme(R.style.BluishLitStyle);
        isWhite=true;
    } else if (userTheme.equals("5")) {
            setTheme(R.style.GreenishLitStyle);
            isWhite=false;
        } else if (userTheme.equals("6")) {
            setTheme(R.style.TacaoLitStyle);
            isWhite=false;
        }else if (userTheme.equals("8")) {
            setTheme(R.style.TyrianLitStyle);
            isWhite=true;
        }
        else if (userTheme.equals("7")) {
            setTheme(R.style.DarkLitStyle);
            isWhite=true;
            isDarkTheme = true;
        }

    }

    public  void loadWidgetColors(SharedPreferences sharedPreferences){
        String selectedFont = sharedPreferences.getString(getString(R.string.key_uiThemeFont), "6");
        if(selectedFont.equals("0")){
            Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.parisienneregular);
            FontUtils fontUtils = new FontUtils();
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
        } else if(selectedFont.equals("1")){
            Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.patrick_hand_sc);
            FontUtils fontUtils = new FontUtils();
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
        } else if( selectedFont.equals("2")) {
            Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.sofadi_one);
            FontUtils fontUtils = new FontUtils();
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
        } else if(selectedFont.equals("3")){
            FontUtils fontUtils = new FontUtils();
            Typeface myCustomFont = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
        }
        else if( selectedFont.equals("4")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.concert_one);
        FontUtils fontUtils = new FontUtils();
        fontUtils.applyFontToToolbar(toolbar, myCustomFont);
        
        }
        else if( selectedFont.equals("5")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.oleo_script);
        FontUtils fontUtils = new FontUtils();
        fontUtils.applyFontToToolbar(toolbar, myCustomFont);
    }
        else if( selectedFont.equals("6")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.pt_sans_narrow);
        FontUtils fontUtils = new FontUtils();
        fontUtils.applyFontToToolbar(toolbar, myCustomFont);
  
    }  else if( selectedFont.equals("7")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.roboto_condensed_light);
        FontUtils fontUtils = new FontUtils();
        fontUtils.applyFontToToolbar(toolbar, myCustomFont);
    }  else if( selectedFont.equals("8")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.shadows_into_light);
        FontUtils fontUtils = new FontUtils();
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
    } else if( selectedFont.equals("9")) {
        Typeface myCustomFont = ResourcesCompat.getFont(this, R.font.slabo_13px);
        FontUtils fontUtils = new FontUtils();
            fontUtils.applyFontToToolbar(toolbar, myCustomFont);
    }else {

        }
    }



    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(getResources().getString(R.string.key_uiTheme))) {
            loadUiTheme(sharedPreferences);
            MainActivity.this.recreate();
        } else if(key.equals(getResources().getString(R.string.key_uiThemeFont))){
            loadWidgetColors(sharedPreferences);
            MainActivity.this.recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState. putParcelableArrayList("JournalEntires", (ArrayList)journalList);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        journalList = savedInstanceState.getParcelableArrayList("JournalEntires");
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(journalList == null){

        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        //Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        TastyToast.makeText(this, "Please click BACK again to exit", TastyToast.LENGTH_SHORT, TastyToast.INFO);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 3000);
    }

/**
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        } else if( id == R.id.action_refresh){
            getJornals();
            return  true;
        } else{

        }

        return super.onOptionsItemSelected(item);
    }

**/
}
