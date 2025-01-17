package com.suffixit.hrm_suffix.view.Fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.suffixit.hrm_suffix.R;
import com.suffixit.hrm_suffix.databinding.FragmentScrumDashboardBinding;
import com.suffixit.hrm_suffix.view.Activities.MainActivity;

public class ApplicationDashboardFragment extends Fragment {
    private FragmentScrumDashboardBinding binding;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        setUpOnBackPressed();
        binding = FragmentScrumDashboardBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.applicationDashboardToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new DashboadFragment(), null).addToBackStack(null).commit();
            }
        });

        binding.cardViewEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new LeaveApplicationFragment(),null).addToBackStack(null).commit();
            }
        });

        binding.cardViewScrumMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().beginTransaction().replace(R.id.FrameLayoutID, new LeaveStatusFragment(), null).addToBackStack(null).commit();

            }
        });
        return view;
    }

    private void setUpOnBackPressed() {
        OnBackPressedCallback onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdded()) {
                    setEnabled(false);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), onBackPressedCallback);

        getLifecycle().addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                onBackPressedCallback.remove();
            }
        });
    }

}
