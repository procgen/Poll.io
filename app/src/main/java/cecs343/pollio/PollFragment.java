package cecs343.pollio;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.firebase.auth.FirebaseAuth;

import java.sql.ParameterMetaData;
import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link PollFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PollFragment extends Fragment {
    private static final String PARAM_ROUTE = "Route of URL to retrieve from";
    private static final String PARAM_COLOR = "Color of fragment background";

    private ArrayList<PollItem> pollList = new ArrayList<>();
    private String route;
    private RecyclerView recyclerView;

    private OnFragmentInteractionListener mListener;

    public PollFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     *
     *      * @return A new instance of fragment PFragment.
     */
    // TODO: Rename and change types and number of parameters
    // Current we use no parameters, placeholders still there
    public static PollFragment newInstance(String route) {
        PollFragment fragment = new PollFragment();
        Bundle args = new Bundle();
        args.putString(PARAM_ROUTE, route);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (pollList.size() <= 0) {
                route = getArguments().getString(PARAM_ROUTE);
                pollList = Requestor.getPolls(getContext().getApplicationContext(), FirebaseAuth.getInstance().getUid(), route, new Requestor.HTTPCallback() {
                    @Override
                    public void onSuccess(){
                        if(route.equals("new")){
                            GPSListener.getPollsNearUser(pollList);
                        }
                        recyclerView.setAdapter(new PollRecyclerAdapter(getContext(), pollList));
                    }
                });
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_poll, container, false);

        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab_addnewpoll);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), NewPollActivity.class);
                startActivity(i);
            }
        });

        final SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.simpleSwipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pollList = Requestor.getPolls(getContext().getApplicationContext(), FirebaseAuth.getInstance().getUid(), route, new Requestor.HTTPCallback() {
                    @Override
                    public void onSuccess(){
                        if(route.equals("new")){
                            GPSListener.getPollsNearUser(pollList);
                        }
                        recyclerView.setAdapter(new PollRecyclerAdapter(getContext(), pollList));
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });

            }
        });

        //hardcode background colors.. because I'm tired
        recyclerView = view.findViewById(R.id.poll_recycler_view);
        if (route.equals("new")) {
            recyclerView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background1));
        }
        else if (route.equals("favorites")) {
            recyclerView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorBackgroundLight));
        }
        else if (route.equals("my")) {
            recyclerView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background3));
        }
        else if (route.equals("voted")) {
            recyclerView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.background4));
        }
        PollRecyclerAdapter recyclerAdapter = new PollRecyclerAdapter(getContext(), pollList);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        return view;
    }

    //Handle attaching fragment to activity
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
