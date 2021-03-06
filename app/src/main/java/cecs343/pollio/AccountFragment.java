package cecs343.pollio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.content.Intent;
import android.widget.TextView;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AccountFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnMyLocationClickListener {

    private Button logoutButton;
    private Button forgotPassButton;
    private TextView displaynameTextView;
    private GoogleSignInClient mGoogleSignInClient;

    private MapView mapView;
    private GoogleMap googleMap;
    private String displayName;
    //Firebase object
    private FirebaseAuth mAuth;

    private OnFragmentInteractionListener mListener;

    ArrayList<PollItem> pollList = new ArrayList<>();

    public AccountFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AccountFragment.
     */
    public static AccountFragment newInstance(String param1, String param2) {
        AccountFragment fragment = new AccountFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pollList = Requestor.getPolls(getContext().getApplicationContext(), FirebaseAuth.getInstance().getUid(), "new", new Requestor.HTTPCallback() {
            @Override
            public void onSuccess(){
                GPSListener.getPollsNearUser(pollList);
            }
        });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view =  inflater.inflate(R.layout.fragment_account, container, false);
        //find logout button
        logoutButton = view.findViewById(R.id.logout_button);
        //WELCOME THE USER----------------------------
        //display the user's display name
        displaynameTextView = view.findViewById(R.id.acct_user_name);
        Requestor.getRequest(getContext().getApplicationContext(), FirebaseAuth.getInstance().getUid(), "displayname", new Requestor.ObjHTTPCallback() {
            @Override
            public void onSuccess(JSONObject displayname){
                Log.d("WTF", displayname.toString());
                try {
                    displaynameTextView.setText(displayname.getString("displayname"));
                } catch (Exception e){
                    Log.d("HTTPRequest", "Failed to resolve JSON");
                }
            }
        });

        Requestor.getRequest(getContext().getApplicationContext(), FirebaseAuth.getInstance().getUid(), "stats", new Requestor.ObjHTTPCallback() {
            @Override
            public void onSuccess(JSONObject stats){
                try {
                    String votesOnMine = stats.getString("votesOnMine");
                    String myPollsCreated = stats.getString("myPollsCreated");
                    String myPollsFavorited = stats.getString("myPollsFavorited");
                    String myVotes = stats.getString("myVotes");
                    String totalVotes = stats.getString("totalVotes");

                    ((TextView)view.findViewById(R.id.stats_polls_voted_on)).setText(myVotes);
                    ((TextView)view.findViewById(R.id.stats_total_votes)).setText(totalVotes);
                    ((TextView)view.findViewById(R.id.stats_polls_made)).setText(myPollsCreated);
                    ((TextView)view.findViewById(R.id.stats_polls_favorited)).setText(myPollsFavorited);
                } catch (Exception e){
                    Log.d("HTTPRequest", "Failed to resolve JSON: " + e.getMessage());
                }
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();

            }
        });

        forgotPassButton = view.findViewById(R.id.changePass);

        forgotPassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), forgotPasswordActivity.class));

            }
        });
        //Welcome _______ DisplayName get from database

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view,savedInstanceState);
        mapView = view.findViewById(R.id.map);
        if(mapView != null){
            mapView.onCreate(null);
            mapView.onResume();
            mapView.getMapAsync(this);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        MapsInitializer.initialize(getContext());
        this.googleMap = googleMap;
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMyLocationButtonClickListener(this);
        googleMap.setOnMyLocationClickListener(this);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        for (int i = 0; i < pollList.size(); i++) {
            double x = pollList.get(i).getLatitude();
            double y = pollList.get(i).getLongitude();
            googleMap.addMarker(new MarkerOptions()
                    .position(new LatLng(x, y))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pollmapiconlavenderblkoutlinediffcolor))

            );
            Log.i("x", Double.toString(x));
            Log.i("y", Double.toString(y));
        }

        double currentLat = GPSListener.getInstance().getLatitude();
        double currentLong = GPSListener.getInstance().getLongitude();

        googleMap.addMarker(new MarkerOptions()
                .position(new LatLng(currentLat, currentLong))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        );
        CameraPosition current = CameraPosition.builder().target(new LatLng(currentLat, currentLong)).zoom(12).bearing(0).tilt(45).build();
        googleMap.moveCamera((CameraUpdateFactory.newCameraPosition(current)));
    }


    @Override
    public void onMyLocationClick(@NonNull Location location) {
        //Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onMyLocationButtonClick() {
        //Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }
    private void signOut(){
        mAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_sign_in_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(), gso);
        mGoogleSignInClient.signOut();


        Intent getToLogin = new Intent(getActivity(),LoginActivity.class);

        getToLogin.putExtra("finish", true);
        getToLogin.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // To clean up all activities
        startActivity(getToLogin);
        getActivity().finish();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

}
