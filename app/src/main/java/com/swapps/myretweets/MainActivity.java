package com.swapps.myretweets;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.User;
import com.twitter.sdk.android.core.services.StatusesService;
import java.util.List;
import retrofit2.Call;

public class MainActivity extends AppCompatActivity {

    private final String API_KEY = "{API_KEY}";
    private final String API_SECRET = "{API_KEY_SECRET}";

    private ProgressDialog progressDialog;
    final Handler handler = new Handler();

    private User user;

    private StringBuilder text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(API_KEY, API_SECRET))
                .debug(true)
                .build();
        Twitter.initialize(config);

        if (TwitterCore.getInstance().getSessionManager().getActiveSession() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            init();
        }
    }

    private void init() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        text = new StringBuilder();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getRetweets();
                        }
                    });
                } catch (Exception e) {
                    Log.d("ERROR", e.getMessage());
                }
            }
        }).start();
    }

    private void getRetweets() {
        Call<User> callUser = TwitterCore.getInstance().getApiClient().getAccountService().verifyCredentials(true, true, true);
        callUser.enqueue(new Callback<User>() {
            @Override
            public void success(Result<User> result) {
                user = result.data;
                Toast.makeText(MainActivity.this, result.data.screenName, Toast.LENGTH_LONG).show();

                text.append("id:" + user.id + "\n");
                text.append("screen_name:" + user.screenName + "\n\n");

                getRetweets(user.id, user.screenName);
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
    }

    private void getRetweets(long id, String name) {
        StatusesService service = TwitterCore.getInstance().getApiClient().getStatusesService();

        Call<List<Tweet>> callTimeline = service.userTimeline(
                id, name, 100, null, null, false, false, false, true);

        callTimeline.enqueue(new Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                Toast.makeText(MainActivity.this, "timeline:" + result.data.toString(), Toast.LENGTH_LONG).show();

                for (Tweet tweet : result.data) {
                    if (tweet.retweeted) {
                        text.append("tweet_id:" + tweet.id + "\n");
                        text.append("user:" + tweet.user.screenName + "\n");
                        text.append("text:" + tweet.text.substring(0, 30).replaceAll("[\n]+", " ") + "\n");
                        text.append("created_at:" + tweet.createdAt + "\n");
                        text.append("-----------------------------------\n");
                    }
                }

                ((TextView) findViewById(R.id.textConsole)).setText(text.toString());
                progressDialog.dismiss();
            }

            @Override
            public void failure(TwitterException exception) {
                Toast.makeText(MainActivity.this, exception.getMessage(), Toast.LENGTH_LONG).show();
                progressDialog.dismiss();
            }
        });
    }
}