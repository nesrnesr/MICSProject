package uni.lu.mics.mics_project.nmbd.app.service;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Arrays;
import java.util.List;

import uni.lu.mics.mics_project.nmbd.MainActivity;
import uni.lu.mics.mics_project.nmbd.R;

public class Authentification {

    private List<AuthUI.IdpConfig> providers;
    final private String TAG = "Authentification";

    public Authentification(){
        //Define the different possible authentification methods, Email or Google Account
        providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
                //new AuthUI.IdpConfig.PhoneBuilder().build(),
                //new AuthUI.IdpConfig.FacebookBuilder().build(),
                //new AuthUI.IdpConfig.TwitterBuilder().build()
        );
    }

    //Create the sign in page using the defined providers and with the app theme
    public Intent createSignInIntent() {
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.MyTheme)
                .setLogo(R.drawable.eventzy_logo)
                .build();
        return intent;
    }

    //Returns the authentificated user
    public FirebaseUser getCurrentUser(){
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    //Returns the ID of the authentificated user
    public String getAuthUid(){
        return getCurrentUser().getUid();
    }

    //Returns the name of the authentificated user
    public String getAuthDisplayName(){
        return getCurrentUser().getDisplayName();
    }

    //returns the email of the authentificated user
    public String getAuthEmail(){
        return getCurrentUser().getEmail();
    }

    //Checks if a user is already signed in
    public Boolean isUserSignedIn(){
        return (getCurrentUser()!=null);
    }

    //Change password to a new password
    public void updatePassword(String newPassword){
        getCurrentUser().updatePassword(newPassword)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "User password updated.");
                        }
                    }
                });
    }

    //Sign out the user
    public void signOut(final Context context, Class targetActivity){
        AuthUI.getInstance()
                .signOut(context)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        context.startActivity(new Intent(context, MainActivity.class));
                        //finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, ""+ e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }






}

