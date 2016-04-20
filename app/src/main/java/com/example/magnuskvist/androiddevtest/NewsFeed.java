package com.example.magnuskvist.androiddevtest;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class NewsFeed extends Activity {

    ArrayList<String[]> arrayList = new ArrayList<>();
    ArrayList<Bitmap> bitmap = new ArrayList<>();
    ArrayList<String[]> timeArray = new ArrayList<>();

    ImageView timelineImage;
    viewAdapter adapter;
    ProgressDialog loadingDialog;

    int currentMax = 10; //Indicating how many posts to view at first. This will be incremented by 10 when the user hits the bottom of the listView.
    int maxItem = 0;
    boolean endFlag = false; //This flag gets high when there is no more items in the JSON/the link.
    boolean firstListLoad = true; //This is true until we load the first posts. This is to load the views correct, and som other aspects.



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_feed);

        //Setting up loadingDialog asap:
        loadingDialog = new ProgressDialog(NewsFeed.this);
        loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        loadingDialog.setMessage("Laster nyheter");
        loadingDialog.setIndeterminate(false);
        loadingDialog.setCancelable(false);
        loadingDialog.show();

        //Inner async class for loading from the net. The rest of the magic will happen here as well.
        new FetchJson().execute();
    }




    class FetchJson extends AsyncTask<String, String, String> {

        String jsonOutput;
        JSONObject jsonObject;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //If we are loading from the net, we want to show the loading dialog.
            if(!loadingDialog.isShowing()){
                loadingDialog.show();
            }

        }

        protected String doInBackground(String... args) {

            int min = currentMax - 10; //The minimum for these 10 posts. It will start at 0, and increment by 10.
            int max;
            long now = System.currentTimeMillis();


            String stringUrl = "http://ws.zooom.no/v1/articles/havornreiret?limit=10&offset=" + min; //Loading the JSON with the appropriate link.

            //Standard call to a httpconnection follows. The buffer is made quite large on purpose.
            StringBuilder response = new StringBuilder();
            try {
                URL url = new URL(stringUrl);
                HttpURLConnection httpconnection = (HttpURLConnection) url.openConnection();
                if (httpconnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader input = new BufferedReader(new InputStreamReader(httpconnection.getInputStream()), 16384);
                    String strLine;

                    while ((strLine = input.readLine()) != null) {
                        response.append(strLine);
                    }
                    input.close();
                }

                jsonOutput = response.toString();
                jsonObject = new JSONObject(jsonOutput);

            } catch (Exception e) {

            }

            //Fetching the info from the JSONobject
            try {
                JSONArray resultsArray = jsonObject.getJSONArray("items");

                for (int x = 0; x < 10; x++) {

                    //Here we are making a Stringarray, which later will be sent to an Arraylist. This Stringarray hold the info we need in the article.
                    String[] array = new String[5];

                    array[0] = (resultsArray.getJSONObject(x).getJSONObject("contents").getString("title"));
                    array[1] = (resultsArray.getJSONObject(x).getJSONObject("contents").getString("preamble"));
                    array[2] = (resultsArray.getJSONObject(x).getString("articleUrl"));
                    array[3] = (resultsArray.getJSONObject(x).getString("cover_image"));
                    array[4] = (resultsArray.getJSONObject(x).getJSONObject("meta").getString("published"));

                    //At the end we add this info to the global variable.
                    arrayList.add(array);

                    //For each time this goes well, it means we got 1 new item available. We increment maxItem to mark this.
                    maxItem++;

                }

            } catch (Exception e) {
                //If the last item is hit, the try will throw this exception and nothing more. It will then be the end of the newswall.
                //We then set the endFlag to true so that we can stop trying to load more items.
                endFlag = true;
            }


            //Time-date-stamp section:

            Date fullTimeStamp = null;
            String shortDateStamp;
            String timeTo = null;


            if(currentMax > maxItem) { //So that we don't try to load articles that are not there.
                max = maxItem;
            }else{
                max = currentMax;
            }

            for(int x = min; x < max; x++){

                String timeStampDate = arrayList.get(x)[4]; //Loading the info from the global ArrayList we created earlier.

                String[] dateInfo = new String[4]; // In this array we save items regarding time.
                // Note to self: dateInfo[]= fullTimeStamp[0], timeTo[1], dateStamp[2], dateStampState[3].

                try {
                    fullTimeStamp = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(timeStampDate.replace(",", " ")); //Could not parse with the ","/comma. Replaced with space.
                    dateInfo[0] = fullTimeStamp.toString(); //Saving the info in the array. We are using the placement-variable for understandings.


                    //To make the format on the time difference correct, we need to do some magic:
                    long timeDiff = now - fullTimeStamp.getTime();

                    if (timeDiff >= DateUtils.YEAR_IN_MILLIS) {
                        timeTo = (timeDiff / DateUtils.YEAR_IN_MILLIS) + " år";
                    }
                    else if (timeDiff >= DateUtils.WEEK_IN_MILLIS) {
                        timeTo = (timeDiff / DateUtils.WEEK_IN_MILLIS) + " u";
                    }
                    else if (timeDiff >= DateUtils.DAY_IN_MILLIS) {
                        timeTo = (timeDiff / DateUtils.DAY_IN_MILLIS) + " d";
                    }
                    else if (timeDiff >= DateUtils.HOUR_IN_MILLIS) {
                        timeTo = (timeDiff / DateUtils.HOUR_IN_MILLIS) + " t";
                    }
                    else if (timeDiff >= DateUtils.MINUTE_IN_MILLIS) {
                        timeTo = (timeDiff / DateUtils.MINUTE_IN_MILLIS) + " m";
                    }

                    dateInfo[1] = timeTo;

                } catch (Exception e) {
                }

                SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM"); //Here we want to get the short date in the "timeline". Formatting it down it just the day and month.

                shortDateStamp = outputFormat.format(fullTimeStamp);
                dateInfo[2] = shortDateStamp;

                String prevDate;

                try {

                    if( x>0 ) {
                        //Messy, but effective. Basically the same that happened to the current fullTimeStamp, just with the previous date. If its the first, the date is marked as Not available.
                        prevDate = outputFormat.format(new SimpleDateFormat("E dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(arrayList.get(x - 1)[4].replace(",", " ")));

                    }else{
                        prevDate="NA";
                    }

                } catch (Exception e) {

                    prevDate = "NA"; //In case of errors the date is also marked as not available.

                }

                //Here we check if the last date is the same as this one. We save whether this us true or not in the StringArray.
                if(prevDate.equals(shortDateStamp)){
                    dateInfo[3] = "true";
                }
                else{
                    dateInfo[3] = "false";
                }

                timeArray.add(dateInfo);
        }

            //Downloading images when every link is ready, instead of opening and closing the inputStream for every one. This is more time efficient.
            try {
                InputStream inputStream = null;
                for(int x = min; x < max; x++) {
                    //This if check whether the endflag is true OR the bitmap array is smaller than the maxItem number. If both is false there is no more images to load, and this wont happen.
                    //This will also throw an exception if false, because we are closing a null object. This is taken care of with the try/catch.
                    if(!endFlag || bitmap.size() < maxItem){
                        inputStream = (InputStream) new URL(arrayList.get(x)[3]).getContent();
                        bitmap.add(BitmapFactory.decodeStream(inputStream));
                    }
                }
                inputStream.close();
            } catch (Exception e) {
            }
            return null;
        }


        protected void onPostExecute(String file_url) {

            //When every image, text and timestamp is gathered we need to crunch some of these information. This is done here, in the postExecute.

            ListView listView = (ListView) findViewById(R.id.listView);

            //The first time this runs we need to initialize, further down the line we just need to notify the change to the adapter.

            if(firstListLoad) {
                firstListLoad = false;
                adapter = new viewAdapter(NewsFeed.this, arrayList); // We send the info needed by the custom item in the listView. This is not containing all the info at this point. But it will be updated along the way.
                listView.setAdapter(adapter); //Adding the adapter to the listView. This starts the process of populating it.

                timelineImage = (ImageView) findViewById(R.id.timelineImage);
                timelineImage.setBackgroundResource(R.drawable.timeline); //This is when we draw the line on the left side for the timeline. This is done here, so that it doesn't show up in the loadingscreen.

                //Using a scrollListener to monitor when we need to load more articles.
                listView.setOnScrollListener(new AbsListView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(AbsListView view, int scrollState) {
                    }

                    @Override
                    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                        //If the 10th item of this "page" is shown, load 10 more.
                        if (!endFlag) {//Unless we are done.

                            if (firstVisibleItem + visibleItemCount >= currentMax) {
                                currentMax += 10;

                                //Loading more JSON when we need to.
                                new FetchJson().execute();
                            }
                        }else{

                        }


                    }
                });

            }else{

                // If the data is updated, and the adapter is active, we need to notify that there are new data available. This will then draw the new items in the listView.
                adapter.notifyDataSetChanged();
            }


            if (endFlag){ //If everything is downloaded, stop listening. No need for using resources on this.
                listView.setOnScrollListener(null);

                //Also dismiss the loadingDialog to save memory.
                loadingDialog.dismiss();
            }

            //Cancel/hide the dialog when everything is ready to show.
            loadingDialog.cancel();
        }
    }


    //This is the adapter for the custom listItem.
    class viewAdapter extends BaseAdapter {

        //Standard class all the way to "getView" further down.

        Context context;
        ArrayList<String[]> data;

        private LayoutInflater inflater = null;

        public viewAdapter(Context context, ArrayList<String[]> data) {

            this.context = context;
            this.data = data;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public Object getItem(int position) {
            return data.get(position);

        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View view = convertView;


            if (view == null) {
                view = inflater.inflate(R.layout.custom_listitem, null);
            }


            TextView headingText = (TextView) view.findViewById(R.id.headingText);
            TextView preambleText = (TextView) view.findViewById(R.id.preambleText);
            TextView timeStampText = (TextView) view.findViewById(R.id.timestampText);
            TextView lowerDateStampText = (TextView) view.findViewById(R.id.lowerDateStampText);
            TextView upperDateStampText = (TextView) view.findViewById(R.id.upperDateStampText);
            ImageView circleImage = (ImageView) view.findViewById(R.id.circleImage);
            ImageView image = (ImageView) view.findViewById(R.id.coverImage);

            headingText.setText(data.get(position)[0]);
            timeStampText.setText(timeArray.get(position)[1]); //Loading directly from the global array, instead of the now local "data". This is to not have to throw the variables to the adapter explicitly.

            //To strip the text of its HTML-tags, we declare the text as HTML. Then we extract the text that are left without the HTML-tags.
            String formattedText = Html.fromHtml(data.get(position)[1]).toString();
            preambleText.setText(formattedText);


            // DateinfoArray: fullTimeStamp[0], timeTo[1], shortDateStamp[2], dateStampState[3]

            //Checks how the "string-flag" is set. If its true, the date of this article is the same as the last one, and we are showing a small dot instead of the dateStamp.
            //Because the BaseAdapter reuses Views, we need to set every single customListItem how we want it. We can go with the default settings from the XML.
            if (timeArray.get(position)[3].equals("true")) {
                RelativeLayout timeStampLayout = (RelativeLayout) view.findViewById(R.id.dateStampLayout);
                timeStampLayout.setBackgroundColor(Color.TRANSPARENT);
                upperDateStampText.setTextColor(Color.TRANSPARENT);
                lowerDateStampText.setTextColor(Color.TRANSPARENT);

                circleImage.setBackgroundResource(R.drawable.dot);

                //If the dates are different we need to show the dateStamp.
            } else if (timeArray.get(position)[3].equals("false")) {

                //Formatted down to two lines.
                String upperDateStampString = timeArray.get(position)[2].split(" ")[0];
                String lowerDateStampString = timeArray.get(position)[2].split(" ")[1];

                upperDateStampText.setText(upperDateStampString);
                lowerDateStampText.setText(lowerDateStampString);

                RelativeLayout timeStampLayout = (RelativeLayout) view.findViewById(R.id.dateStampLayout);
                timeStampLayout.setBackgroundResource(R.drawable.datestampback);
                upperDateStampText.setTextColor(Color.parseColor("#000000"));
                lowerDateStampText.setTextColor(Color.parseColor("#ff009688"));

                circleImage.setBackgroundColor(Color.TRANSPARENT);
            }

            //We want the pictures in the right, and same, format. We get the total width of the screen, and get approx. 72% of that.
            //The height (with a picture format of 16:9) is the width multiplied with 9/16.

            WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = windowManager.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int width = size.x * 8 / 11;
            int height = width * 9 / 16;

            //And lastly we add the image to the view.
            try{
                image.setImageBitmap(Bitmap.createScaledBitmap(bitmap.get(position), width, height, false));
            }
            catch (Exception e){
            }

            return view;
        }

    }
}
