package uni.lu.mics.mics_project.nmbd;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import uni.lu.mics.mics_project.nmbd.adapters.AdapterCallBack;
import uni.lu.mics.mics_project.nmbd.adapters.AdapterDoubleCallBack;
import uni.lu.mics.mics_project.nmbd.adapters.FriendListAdapter;
import uni.lu.mics.mics_project.nmbd.adapters.FriendRequestListAdapter;
import uni.lu.mics.mics_project.nmbd.adapters.FriendSearchListAdapter;
import uni.lu.mics.mics_project.nmbd.app.AppGlobalState;
import uni.lu.mics.mics_project.nmbd.app.service.ExtendedList.ExtendedListUser;
import uni.lu.mics.mics_project.nmbd.app.service.FriendRecommender;
import uni.lu.mics.mics_project.nmbd.app.service.Images.ImageViewUtils;
import uni.lu.mics.mics_project.nmbd.domain.model.Event;
import uni.lu.mics.mics_project.nmbd.domain.model.User;
import uni.lu.mics.mics_project.nmbd.infra.repository.RepoCallback;
import uni.lu.mics.mics_project.nmbd.infra.repository.RepoMultiCallback;
import uni.lu.mics.mics_project.nmbd.infra.repository.UserRepository;

import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity {

    final private String TAG = "FriendsActivity";
    //Friend search results
    private final ExtendedListUser searchResultList = new ExtendedListUser();
    //Friend Requests received
    private final ExtendedListUser friendReqList = new ExtendedListUser();
    //Friend List
    private final ExtendedListUser friendList = new ExtendedListUser();


    AppGlobalState globalState;
    UserRepository userRepo;
    //Reference to the user logged in
    private User currentUser;
    private String currentUserID;
    //View variables
    private RecyclerView mSearchResultRecyclerView;
    private FriendSearchListAdapter mFriendSearchListAdapter;
    private EditText searchEdit;
    private RecyclerView mFriendReqListRecyclerView;
    private FriendRequestListAdapter mFriendRequestListAdapter;
    private TextView frReqPendingLabel;
    private RecyclerView mFriendListRecyclerView;
    private FriendListAdapter mFriendListAdapter;
    private TextView friendsLabel;
    //Friend recommendation variable
    private LinearLayout friendRecom;


    private LinearLayout friendsReqLayout;
    private View friendsReqDivider;

    private boolean isEventParticipants = false;
    private Event currentEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        //Initialize the global state and database variables
        globalState = (AppGlobalState) getApplicationContext();
        userRepo = globalState.getRepoFacade().userRepo();
        //Retrieve current user
        Intent intent = getIntent();
        currentUser = (User) intent.getSerializableExtra("currentUser");
        currentUserID = currentUser.getId();
        //If the intent has an Event as extra then the activity should show the participants of the event
        if (intent.hasExtra("event")) {
            currentEvent = (Event) intent.getSerializableExtra("event");
            Log.d(TAG, "onCreate: Number of event participants: "+currentEvent.getEventParticipants().size());
            isEventParticipants = true;
        }

        //Initialize the different views in layout
        searchEdit = findViewById(R.id.friends_activity_search_edit);
        frReqPendingLabel = findViewById(R.id.friends_activity_req_pending_label);
        friendsLabel = findViewById(R.id.friends_activity_friends_list_label);
        //Attaching the recycler views to their corresponding View items
        mFriendReqListRecyclerView = findViewById(R.id.friends_activity_req_pending_recyclerview);
        mFriendListRecyclerView = findViewById(R.id.friends_activity_friends_recyclerview);
        mSearchResultRecyclerView = findViewById(R.id.friends_activity_search_result_recyclerview);
        friendsReqLayout = findViewById(R.id.friend_req_layout);
        friendsReqDivider = findViewById(R.id.friends_divider);
        friendRecom = findViewById(R.id.recom);
        //initialize the friend search recyclerView, friend request recycler view

        initializeFriendRecyclerView();
        initializeFriendReqRecyclerView();
        initializeSearchRecyclerView();

        //Checks if the activity is to view participants of an event, if so the search tool and friend recommendation are not used
        if(isEventParticipants){
            searchEdit.setVisibility(View.GONE);
            friendRecom.setVisibility(View.GONE);
            initializeParticipantsNotFriends();
        } else{
            setFriendRecommendation();
            initializeSearchEdit();
        }

        setupToolbar();
    }

    private void initializeParticipantsNotFriends(){
        searchResultList.clearLists();
        for (String id : currentEvent.getEventParticipants()){
            if (!currentUser.getFriendList().contains(id) && !currentUser.getFriendReqReceivedList().contains(id) && !currentUserID.equals(id)){
                addFriendToExtendedList(id, searchResultList, mFriendSearchListAdapter);
            }
        }
        mFriendSearchListAdapter.notifyDataSetChanged();
    }

    //Add listener to search edit view so Recycler view can be updated when there is a change
    private void initializeSearchEdit() {
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d(TAG, s.toString() + " is being searched");
                //If the text in the search edit view has changed and the it is not an empty string
                if (s.length() != 0) {
                    //Delete all the elements in the searchResultList to make space for the new results of the search
                    searchResultList.clearLists();
                    mFriendSearchListAdapter.notifyDataSetChanged();
                    //Compare entered string with database and returns matching results
                    userRepo.whereGreaterThanOrEqualTo("name", s.toString(), new RepoMultiCallback<User>() {
                        @Override
                        public void onCallback(ArrayList<User> models) {
                            for (User u : models) {
                                String nameSearched = u.getName();
                                String idOfSearched = u.getId();
                                //check that returned user is not current user and that invitation from that user has not been received/sent
                                Boolean isSearchedSameAsCurrentUser = nameSearched.equals(currentUser.getName());
                                Boolean isReqAlreadySent = currentUser.getFriendReqSentList().contains(idOfSearched);
                                Boolean isReqAlreadyReceived = currentUser.getFriendReqReceivedList().contains(idOfSearched);
                                Boolean isAlreadyFriend = currentUser.getFriendList().contains(idOfSearched);
                                if (!isSearchedSameAsCurrentUser && !isReqAlreadySent && !isReqAlreadyReceived && !isAlreadyFriend) {
                                    //add the the name and user id to the lists send to the recyclerview
                                    addFriendToExtendedList(idOfSearched, searchResultList, mFriendSearchListAdapter);
                                    Log.d(TAG, u.getName() + " of userID " + idOfSearched + "was found in search");
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public void updateFriendList() {
        //If there are no current friends then change the display message
        if (currentUser.getFriendList().size() == 0) {
            if (isEventParticipants){
                friendsLabel.setText("You do not have any friend participating in the event.");
            }else {
                friendsLabel.setText("You do not have any friend.");
            }
            mFriendListRecyclerView.setVisibility(View.INVISIBLE);
        } else {
            friendsLabel.setText("Friends: ");
            mFriendListRecyclerView.setVisibility(View.VISIBLE);
            friendList.clearLists();
            mFriendListAdapter.notifyDataSetChanged();
            for (String id : currentUser.getFriendList()) {
                if (isEventParticipants) {
                    if(currentEvent.getEventParticipants().contains(id)) {
                        addFriendToExtendedList(id, friendList, mFriendListAdapter);
                    }
                } else {
                    addFriendToExtendedList(id, friendList, mFriendListAdapter);
                }
            }
        }
    }

    public void updateFriendReqLists() {
        //If there are no friend request then the friend request label and recycler view can be set invisible
        friendReqList.clearLists();
        int pendingReq = 0;
        if (currentUser.getFriendReqReceivedList().size() == 0) {
            friendsReqLayout.setVisibility(View.GONE);
            mFriendReqListRecyclerView.setVisibility(View.GONE);
        } else {
            for (String id : currentUser.getFriendReqReceivedList()) {
                //Checks if the activity is to show event participants
                if(isEventParticipants){
                    if(currentEvent.getEventParticipants().contains(id)){
                        pendingReq++;
                        addFriendToExtendedList(id, friendReqList, mFriendRequestListAdapter);
                    }
                }else{
                    addFriendToExtendedList(id, friendReqList, mFriendRequestListAdapter);
                }
            }
        }
        Log.d(TAG, "updateFriendReqLists: Number of friend requests: "+friendReqList.getSize());
        //Check if the activity is to view participants of an event and checks if there is any participant that has sent a friend request to the user
        if(isEventParticipants && pendingReq==0){
                friendsReqLayout.setVisibility(View.GONE);
                mFriendReqListRecyclerView.setVisibility(View.GONE);
        }

    }

    public void initializeFriendRecyclerView() {
        mFriendListAdapter = new FriendListAdapter(FriendsActivity.this,
                friendList, new AdapterCallBack() {

            @Override
            public void onClickCallback(int p) {
                String unfriendUserID = friendList.getId(p);
                //Removes the friend ID from the list of current user object list of req received
                currentUser.removeFriendFromFriendList(unfriendUserID);
                //Updates the database
                userRepo.removeElement(currentUserID, "friendList", unfriendUserID);
                userRepo.removeElement(unfriendUserID, "friendList", currentUserID);
                //Show toast on successful completion
                Toast.makeText(FriendsActivity.this, "Unfriended: " + friendList.getName(p), Toast.LENGTH_SHORT).show();
                //Refresh the the list for the recycler view
                friendList.removeElement(p);
                mFriendListAdapter.notifyItemRemoved(p);
                if (isEventParticipants){
                    initializeParticipantsNotFriends();
                }
                if (currentUser.getFriendList().size() == 0) {
                    friendsLabel.setText("You do not have any friend.");
                    mFriendListRecyclerView.setVisibility(View.INVISIBLE);
                }
            }
        });
        // Connect the adapter with the recycler view.
        mFriendListRecyclerView.setAdapter(mFriendListAdapter);
        // Give the recycler view a default layout manager.
        mFriendListRecyclerView.setLayoutManager(new LinearLayoutManager(FriendsActivity.this));
        updateFriendList();
    }

    public void initializeFriendReqRecyclerView() {
        //Create an adapter and supply the data to be displayed
        mFriendRequestListAdapter = new FriendRequestListAdapter(FriendsActivity.this,
                friendReqList,
                new AdapterDoubleCallBack() {
                    @Override
                    public void onAcceptRequest(int p) {
                        String requestUserID = friendReqList.getId(p);
                        //Add the friend ID to the list of friends of the current user object
                        currentUser.addFriendToFriendList(friendReqList.getId(p));
                        //Removes the friend ID from the list of current user object list of req received
                        currentUser.removeFriendFromReqReceivedList(requestUserID);
                        //Updates the database
                        userRepo.removeElement(currentUserID, "friendReqReceivedList", requestUserID);
                        userRepo.addElement(currentUserID, "friendList", requestUserID);
                        userRepo.removeElement(requestUserID, "friendReqSentList", currentUserID);
                        userRepo.addElement(requestUserID, "friendList", currentUserID);
                        //Show toast on successful completion
                        Toast.makeText(FriendsActivity.this, "You are now friend with " + friendReqList.getName(p), Toast.LENGTH_SHORT).show();
                        //Refresh the the list for the recycler view
                        friendReqList.removeElement(p);
                        mFriendRequestListAdapter.notifyItemRemoved(p);

                        //Updates the friend list
                        updateFriendList();
                        updateFriendReqLists();
                        if (currentUser.getFriendReqReceivedList().size() == 0) {
                            friendsReqLayout.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onDeclineRequest(int p) {
                        String requestUserID = friendReqList.getId(p);
                        //Removes the friend ID from the list of current user object list of req received
                        currentUser.removeFriendFromReqReceivedList(requestUserID);
                        //Updates the database
                        userRepo.removeElement(currentUserID, "friendReqReceivedList", requestUserID);
                        //Show toast on successful completion
                        Toast.makeText(FriendsActivity.this, "Friend request from: " + friendReqList.getName(p) + " declined.", Toast.LENGTH_SHORT).show();
                        //Refresh the the list for the recycler view
                        friendReqList.removeElement(p);
                        mFriendRequestListAdapter.notifyDataSetChanged();
                        if (currentUser.getFriendReqReceivedList().size() == 0) {
                            friendsReqLayout.setVisibility(View.INVISIBLE);
                            mFriendReqListRecyclerView.setVisibility(View.INVISIBLE);
                        }
                    }
                });
        // Connect the adapter with the recycler view.
        mFriendReqListRecyclerView.setAdapter(mFriendRequestListAdapter);
        // Give the recycler view a default layout manager.
        mFriendReqListRecyclerView.setLayoutManager(new LinearLayoutManager(FriendsActivity.this));
        updateFriendReqLists();
    }

    public void sendFriendRequest(String sendUserID) {
        currentUser.addFriendToReqSentList(sendUserID);
        userRepo.addElement(currentUserID, "friendReqSentList", sendUserID);
        userRepo.addElement(sendUserID, "friendReqReceivedList", currentUserID);
        //Display a toast for success on sending friend request
        Log.d(TAG, "Friend request sent to " + sendUserID);
    }


    public void initializeSearchRecyclerView() {
        // Create an adapter and supply the data to be displayed.
        mFriendSearchListAdapter = new FriendSearchListAdapter(FriendsActivity.this,
                searchResultList,
                new AdapterCallBack() {
                    //Add Friend button pressed procedure
                    @Override
                    public void onClickCallback(int p) {
                        //Add friend request to the currentUser object and update database
                        String sendUserID = searchResultList.getId(p);
                        if(!isEventParticipants){
                        sendFriendRequest(sendUserID);
                        Toast.makeText(FriendsActivity.this, "Friend request sent to " + searchResultList.getName(p), Toast.LENGTH_LONG).show();
                        //Clear the search results and removes the search results from screen
                        searchResultList.removeElement(p);
                        mFriendSearchListAdapter.notifyItemRemoved(p);
                        searchResultList.clearLists();
                        mFriendSearchListAdapter.notifyDataSetChanged();
                        }else{
                            if (!currentUser.getFriendReqSentList().contains(sendUserID)){
                                sendFriendRequest(sendUserID);
                                Toast.makeText(FriendsActivity.this, "Friend request sent to " + searchResultList.getName(p), Toast.LENGTH_LONG).show();
                            } else{
                                Toast.makeText(FriendsActivity.this, "Friend request already sent to " + searchResultList.getName(p), Toast.LENGTH_LONG).show();
                            }
                        }

                    }
                });
        // Connect the adapter with the recycler view.
        mSearchResultRecyclerView.setAdapter(mFriendSearchListAdapter);
        // Give the recycler view a default layout manager.
        mSearchResultRecyclerView.setLayoutManager(new LinearLayoutManager(FriendsActivity.this));
    }

    public void addFriendToExtendedList(String id, final ExtendedListUser extList, final RecyclerView.Adapter adapter) {
        //Access database to find user corresponding to an id
        userRepo.findById(id, new RepoCallback<User>() {
            @Override
            public void onCallback(final User model) {
                //add the found model to the list
                extList.addNameID(model.getName(), model.getId());
                //Notifies adapter that the list has been updated so recyclerview can be updated
                adapter.notifyItemInserted(extList.getIdIndexOfLast());
            }
        });
    }

    public void setFriendRecommendation() {
        //Initially hide the friend recommendation layout
        friendRecom.setVisibility(View.GONE);
        //Run a thread as it could be a time consuming operation
        new Thread(new Runnable() {
            public void run() {
                //Arraylist to store all the friends of the current user's friends
                final ArrayList<String> friendsOfFriends = new ArrayList<>();
                //Retrieving data from firestore
                userRepo.getAll(new RepoMultiCallback<User>() {
                    @Override
                    public void onCallback(ArrayList<User> users) {
                        for (User user : users) {
                            Log.d(TAG, "onCallback: Users size:"+users.size());
                            if (currentUser.getFriendList().contains(user.getId())) {
                               Log.d(TAG, "user retrieved from friend list"+user.getName());
                                for (String friend : user.getFriendList()) {
                                    //adding all the friends of friends to the list
                                    friendsOfFriends.add(friend);
                                    Log.d(TAG, "setFriendRecommendation: "+ friendsOfFriends.size());
                                }
                            }
                        }
                        Log.d(TAG, "onCallback: " + friendsOfFriends.size());
                        //Using the FriendRecommender object to find which is the most common friend in the list
                        FriendRecommender fRecom = new FriendRecommender(currentUser, friendsOfFriends);
                        if (fRecom.getRecommendation() != null) {
                            final String recomID = fRecom.getRecommendation();
                            final int numFriendsCommon = fRecom.getNumCommon();
                            //Getting the most likely common friend User Object from the database
                            userRepo.findById(recomID, new RepoCallback<User>() {
                                @Override
                                public void onCallback(final User user) {
                                    //Sets the friend recommendation layout to visible
                                    friendRecom.setVisibility(View.VISIBLE);
                                    //Display the name of the friend
                                    TextView recomName = findViewById(R.id.recom_result_name);
                                    recomName.setText(user.getName());
                                    //Display the number of friends in common
                                    TextView recom_number = findViewById(R.id.recom_number_TextView);
                                    recom_number.setText("You have " + numFriendsCommon + " friends in common.");
                                    //Display the user picture
                                    ImageView recom_pic = findViewById(R.id.recom_pic_imageView);
                                    ImageViewUtils.displayUserCirclePicID(FriendsActivity.this, recomID, recom_pic);
                                    //sets the button
                                    Button sendReq = findViewById(R.id.recom_button);
                                    sendReq.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            sendFriendRequest(recomID);
                                            Toast.makeText(FriendsActivity.this, "Friend request sent to: " + user.getName(), Toast.LENGTH_SHORT).show();
                                            setFriendRecommendation();
                                        }
                                    });

                                }
                            });
                        }
                    }
                });
            }
        }).start();
    }

    public void backToHomepage() {
        Intent intent = new Intent(FriendsActivity.this, HomepageActivity.class);
        intent.putExtra("currentUser", currentUser);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed Called");
        backToHomepage();
    }

    private void backToEventActivity(){
        Intent intent = new Intent(FriendsActivity.this, EventActivity.class);
        intent.putExtra("currentUser", currentUser);
        intent.putExtra("currentEvent", currentEvent);
        Log.d(TAG, "backToEventActivity: Number of event participants: "+currentEvent.getEventParticipants().size());
        startActivity(intent);
    }

    private void setupToolbar() {
        ImageView profileImageView = findViewById(R.id.profile_pic);
        ImageViewUtils.displayUserCirclePicID(this, currentUser.getId(), profileImageView);
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendsActivity.this, ProfileActivity.class);
                intent.putExtra("currentUser", currentUser);
                startActivity(intent);
                finish();
            }
        });
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEventParticipants){
                    backToEventActivity();
                } else {
                    backToHomepage();
                }
            }
        });
    }


}




